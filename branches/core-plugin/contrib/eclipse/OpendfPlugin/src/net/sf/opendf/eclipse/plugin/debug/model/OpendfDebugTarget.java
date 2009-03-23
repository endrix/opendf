/* 
BEGINCOPYRIGHT X
	
	Copyright (c) 2009, Xilinx Inc.
	All rights reserved.
	
	Redistribution and use in source and binary forms, 
	with or without modification, are permitted provided 
	that the following conditions are met:
	- Redistributions of source code must retain the above 
	  copyright notice, this list of conditions and the 
	  following disclaimer.
	- Redistributions in binary form must reproduce the 
	  above copyright notice, this list of conditions and 
	  the following disclaimer in the documentation and/or 
	  other materials provided with the distribution.
	- Neither the name of the copyright holder nor the names 
	  of its contributors may be used to endorse or promote 
	  products derived from this software without specific 
	  prior written permission.
	
	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
	CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
	INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
	MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
	DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
	CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
	SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
	NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
	LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
	HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
	CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
	OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
	SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
	
ENDCOPYRIGHT
 */

package net.sf.opendf.eclipse.plugin.debug.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.IBreakpointManagerListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;

/**
 * Opendf debugger target.
 * 
 * @author Rob Esser
 * @version 20 March 2009
 */
public class OpendfDebugTarget extends OpendfDebugElement implements IDebugTarget, IBreakpointManagerListener, IOpendfEventListener {

	// associated system process of the execution engine
	private IProcess systemProcess;

	// containing launch object
	private ILaunch debugLauncher;

	// sockets to communicate with execution engine
	private Socket commandSocket;
	private PrintWriter commandWriter;
	private BufferedReader commandReader;
	private Socket eventSocket;
	private BufferedReader eventReader;

	// terminated state
	private boolean terminated = false;

	// threads
	private OpendfThread[] threads;

	// event dispatch job
	private EventDispatchJob eventDispatch;
	// event listeners
	private List<IOpendfEventListener> eventListeners = new ArrayList<IOpendfEventListener>();

	/**
	 * Constructs a new debug target in the given launch for the associated Opendf
	 * VM process.
	 * 
	 * @param launch containing launch
	 * @param process Opendf execution engine
	 * @param commandPort port to send requests to the execution engine
	 * @param eventPort port to read events from
	 * @exception CoreException if unable to connect to host
	 */
	public OpendfDebugTarget(ILaunch launch, IProcess process, int commandPort, int eventPort) throws CoreException {
		super(null);
		debugLauncher = launch;
		systemProcess = process;
		addEventListener(this);
		try {
			// give execution engine a chance to start
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			commandSocket = new Socket("localhost", commandPort);
			commandWriter = new PrintWriter(commandSocket.getOutputStream());
			commandReader = new BufferedReader(new InputStreamReader(commandSocket.getInputStream()));
			// give interpreter a chance to open next socket
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			eventSocket = new Socket("localhost", eventPort);
			eventReader = new BufferedReader(new InputStreamReader(eventSocket.getInputStream()));
		} catch (UnknownHostException e) {
			requestFailed("Unable to connect to Opendf Execution Engine", e);
		} catch (IOException e) {
			requestFailed("Unable to connect to Opendf Execution Engine", e);
		}
		//build up an array of threads representing individual actors
		threads = new OpendfThread[] { new ActorThread(this, "MyNameA"), new ActorThread(this, "MyNameB") };
		
		eventDispatch = new EventDispatchJob();
		eventDispatch.schedule();
		IBreakpointManager breakpointManager = getBreakpointManager();
		breakpointManager.addBreakpointListener(this);
		breakpointManager.addBreakpointManagerListener(this);
		// initialise error handling to suspend on 'unimplemented instructions'
		// and 'no such label' errors
		//sendCommand("eventstop unimpinstr 1");
		//sendCommand("eventstop nosuchlabel 1");
	}

	/**
	 * Registers the given event listener. The listener will be notified of events
	 * in the program being interpreted. Has no effect if the listener is already
	 * registered.
	 * 
	 * @param listener event listener
	 */
	public void addEventListener(IOpendfEventListener listener) {
		if (!eventListeners.contains(listener)) {
			eventListeners.add(listener);
		}
	}

	/**
	 * Removes registration of the given event listener. Has no effect if the listener is not
	 * currently registered.
	 * 
	 * @param listener event listener
	 */
	public void removeEventListener(IOpendfEventListener listener) {
		eventListeners.remove(listener);
	}

	/**
	 * Return the main debugger thread
	 * 
	 * @see org.eclipse.debug.core.model.IDebugTarget#getProcess()
	 */
	public IProcess getProcess() {
		return systemProcess;
	}

	/**
	 * Return the set of debugger threads (could be misused to represent each actor)
	 * 
	 * @see org.eclipse.debug.core.model.IDebugTarget#getThreads()
	 */
	public IThread[] getThreads() throws DebugException {
		return threads;
	}

	/**
	 * Does it have any threads
	 * 
	 * @see org.eclipse.debug.core.model.IDebugTarget#hasThreads()
	 */
	public boolean hasThreads() throws DebugException {
		return true;
	}

	/**
	 * Return the name of the debugger
	 * 
	 * @see org.eclipse.debug.core.model.IDebugTarget#getName()
	 */
	public String getName() throws DebugException {
		return "Opendf";
	}

	/**
	 * Are breakpoints supported - depends on debugger state
	 * 
	 * @see org.eclipse.debug.core.model.IDebugTarget#supportsBreakpoint(org.eclipse.debug.core.model.IBreakpoint)
	 */
	public boolean supportsBreakpoint(IBreakpoint breakpoint) {
		if (!isTerminated() && breakpoint.getModelIdentifier().equals(getModelIdentifier())) {
//			try {
//				String program = getLaunch().getLaunchConfiguration().getAttribute(DebugCorePlugin.ATTR_Opendf_PROGRAM, (String) null);
//				if (program != null) {
//					IResource resource = null;
//					if (breakpoint instanceof OpendfRunToLineBreakpoint) {
//						OpendfRunToLineBreakpoint rtl = (OpendfRunToLineBreakpoint) breakpoint;
//						resource = rtl.getSourceFile();
//					} else {
//						IMarker marker = breakpoint.getMarker();
//						if (marker != null) {
//							resource = marker.getResource();
//						}
//					}
//					if (resource != null) {
//						IPath p = new Path(program);
//						return resource.getFullPath().equals(p);
//					}
//				}
//			} catch (CoreException e) {
//			}
		}
		return false;
	}

	/**
	 * return the target of this debugging session
	 * 
	 * @see org.eclipse.debug.core.model.IDebugElement#getDebugTarget()
	 */
	public IDebugTarget getDebugTarget() {
		return this;
	}

	/**
	 * Return the launcher that invoked this session
	 * 
	 * @see org.eclipse.debug.core.model.IDebugElement#getLaunch()
	 */
	public ILaunch getLaunch() {
		return debugLauncher;
	}

	/**
	 * Return true if the session can be terminated
	 * 
	 * @see org.eclipse.debug.core.model.ITerminate#canTerminate()
	 */
	public boolean canTerminate() {
		return getProcess().canTerminate();
	}

	/**
	 * Return true if the session has terminated
	 * 
	 * @see org.eclipse.debug.core.model.ITerminate#isTerminated()
	 */
	public boolean isTerminated() {
		return terminated || getProcess().isTerminated();
	}

	/**
	 * Terminate debugging session
	 * 
	 * @see org.eclipse.debug.core.model.ITerminate#terminate()
	 */
	public void terminate() throws DebugException {
		sendCommand("exit");
	}

	/**
	 * Can the session be resumed?
	 * 
	 * @see org.eclipse.debug.core.model.ISuspendResume#canResume()
	 */
	public boolean canResume() {
		return !isTerminated() && isSuspended();
	}

	/**
	 * Can the session be suspended?
	 * 
	 * @see org.eclipse.debug.core.model.ISuspendResume#canSuspend()
	 */
	public boolean canSuspend() {
		return !isTerminated() && !isSuspended();
	}

	/**
	 * is it suspended
	 * 
	 * @see org.eclipse.debug.core.model.ISuspendResume#isSuspended()
	 */
	public boolean isSuspended() {
		if (isTerminated()) {
			return true;
		}
		//now see if all the threads have suspended
		for (int i = 0; i < threads.length; i++) {
			if (!threads[i].isSuspended()) {
				//thread still running
				return false;
			}
		}
		return true;
	}

	/*
	 * Resume execution
	 * 
	 * @see org.eclipse.debug.core.model.ISuspendResume#resume()
	 */
	public void resume() throws DebugException {
		for (int i = 0; i < threads.length; i++) {
			sendCommand("resume " + threads[i].getComponentName());
		}
	}

	/**
	 * Suspend execution
	 * 
	 * @see org.eclipse.debug.core.model.ISuspendResume#suspend()
	 */
	public void suspend() throws DebugException {
		for (int i = 0; i < threads.length; i++) {
			sendCommand("suspend " + threads[i].getComponentName());
		}
	}

	/**
	 * Add the breakpoint
	 * 
	 * @see org.eclipse.debug.core.IBreakpointListener#breakpointAdded(org.eclipse.debug.core.model.IBreakpoint)
	 */
	public void breakpointAdded(IBreakpoint breakpoint) {
		// if (supportsBreakpoint(breakpoint)) {
		// try {
		// if ((breakpoint.isEnabled() && getBreakpointManager().isEnabled()) ||
		// !breakpoint.isRegistered()) {
		// OpendfLineBreakpoint OpendfBreakpoint = (OpendfLineBreakpoint)breakpoint;
		// OpendfBreakpoint.install(this);
		// }
		// } catch (CoreException e) {
		// }
		// }
	}

	/**
	 * Remove the brealpoint
	 * 
	 * @see
	 * org.eclipse.debug.core.IBreakpointListener#breakpointRemoved(org.eclipse
	 * .debug.core.model.IBreakpoint, org.eclipse.core.resources.IMarkerDelta)
	 */
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
		// if (supportsBreakpoint(breakpoint)) {
		// try {
		// OpendfLineBreakpoint OpendfBreakpoint = (OpendfLineBreakpoint)breakpoint;
		// OpendfBreakpoint.remove(this);
		// } catch (CoreException e) {
		// }
		// }
	}

	/**
	 * Toggle the breakpoint
	 * 
	 * @see
	 * org.eclipse.debug.core.IBreakpointListener#breakpointChanged(org.eclipse
	 * .debug.core.model.IBreakpoint, org.eclipse.core.resources.IMarkerDelta)
	 */
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		if (supportsBreakpoint(breakpoint)) {
			try {
				if (breakpoint.isEnabled() && getBreakpointManager().isEnabled()) {
					breakpointAdded(breakpoint);
				} else {
					breakpointRemoved(breakpoint, null);
				}
			} catch (CoreException e) {
			}
		}
	}

	/**
	 * No we can't disconnect (temporarily) from the execution engine
	 * 
	 * @see org.eclipse.debug.core.model.IDisconnect#canDisconnect()
	 */
	public boolean canDisconnect() {
		return false;
	}

	/**
	 * We should never ever do this
	 * 
	 * @see org.eclipse.debug.core.model.IDisconnect#disconnect()
	 */
	public void disconnect() throws DebugException {
	}

	/**
	 * Well is we can't disconnect then we should never be disconnected
	 * 
	 * @see org.eclipse.debug.core.model.IDisconnect#isDisconnected()
	 */
	public boolean isDisconnected() {
		return false;
	}

	/**
	 * Don't believe in it
	 * 
	 * @see org.eclipse.debug.core.model.IMemoryBlockRetrieval#supportsStorageRetrieval()
	 */
	public boolean supportsStorageRetrieval() {
		return false;
	}

	/**
	 * Nothing to return (for now)
	 * 
	 * @see org.eclipse.debug.core.model.IMemoryBlockRetrieval#getMemoryBlock(long, long)
	 */
	public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException {
		return null;
	}

	/**
	 * Notification we have connected to the VM and it has started. Resume the VM.
	 */
	private void started() {
		fireCreationEvent();
		installDeferredBreakpoints();
		try {
			resume();
		} catch (DebugException e) {
		}
	}

	/**
	 * Install breakpoints that are already registered with the breakpoint
	 * manager.
	 */
	private void installDeferredBreakpoints() {
		IBreakpoint[] breakpoints = getBreakpointManager().getBreakpoints(getModelIdentifier());
		for (int i = 0; i < breakpoints.length; i++) {
			breakpointAdded(breakpoints[i]);
		}
	}

	/**
	 * Called when this debug target terminates.
	 */
	private synchronized void terminated() {
		terminated = true;
		threads = new OpendfThread[0];
		IBreakpointManager breakpointManager = getBreakpointManager();
		breakpointManager.removeBreakpointListener(this);
		breakpointManager.removeBreakpointManagerListener(this);
		fireTerminateEvent();
		removeEventListener(this);
	}

	/**
	 * Returns the values on the data stack (top down)
	 * 
	 * @return the values on the data stack (top down)
	 */
	public IValue[] getDataStack() throws DebugException {
		// String dataStack = sendRequest("data");
		// if (dataStack != null && dataStack.length() > 0) {
		// String[] values = dataStack.split("\\|");
		// IValue[] theValues = new IValue[values.length];
		// for (int i = 0; i < values.length; i++) {
		// String value = values[values.length - i - 1];
		// theValues[i] = new OpendfStackValue(this, value, i);
		// }
		// return theValues;
		// }
		return new IValue[0];
	}

	/**
	 * Send a command to the execution engine
	 * 
	 * @see example.debug.core.model.OpendfDebugElement#sendCommand(java.lang.String)
	 */
	public String sendCommand(String request) throws DebugException {
		synchronized (commandSocket) {
			commandWriter.println(request);
			commandWriter.flush();
			try {
				// wait for reply
				return commandReader.readLine();
			} catch (IOException e) {
				requestFailed("Request failed: " + request, e);
			}
		}
		return null;
	}

	/**
	 * When the breakpoint manager disables, remove all registered breakpoints
	 * requests from the VM. When it enables, reinstall them.
	 */
	public void breakpointManagerEnablementChanged(boolean enabled) {
		IBreakpoint[] breakpoints = getBreakpointManager().getBreakpoints(getModelIdentifier());
		for (int i = 0; i < breakpoints.length; i++) {
			if (enabled) {
				breakpointAdded(breakpoints[i]);
			} else {
				breakpointRemoved(breakpoints[i], null);
			}
		}
	}

	/**
	 * Returns whether popping the data stack is currently permitted
	 * 
	 * @return whether popping the data stack is currently permitted
	 */
	public boolean canPop() {
//		try {
//			return !isTerminated() && isSuspended() && getDataStack().length > 0;
//		} catch (DebugException e) {
//		}
		return false;
	}

	/**
	 * Pops and returns the top of the data stack
	 * 
	 * @return the top value on the stack
	 * @throws DebugException
	 *           if the stack is empty or the request fails
	 */
	public IValue pop() throws DebugException {
//		IValue[] dataStack = getDataStack();
//		if (dataStack.length > 0) {
//			sendCommand("poOpendfta");
//			return dataStack[0];
//		}
//		requestFailed("Empty stack", null);
		return null;
	}

	/**
	 * Returns whether pushing a value is currently supported.
	 * 
	 * @return whether pushing a value is currently supported
	 */
	public boolean canPush() {
		return !isTerminated() && isSuspended();
	}

	/**
	 * Pushes a value onto the stack.
	 * 
	 * @param value
	 *          value to push
	 * @throws DebugException
	 *           on failure
	 */
	public void push(String value) throws DebugException {
		sendCommand("pushdata " + value);
	}

	public void handleResumedEvent(String compName, String event) {
		//do nothing
	}

	public void handleStartedEvent() {
		started();
	}

	public void handleSuspendedEvent(String compName, String event) {
		//do nothing
	}

	public void handleTerminatedEvent() {
		terminated();
	}
	
	/**
	 * Listens to events from the Opendf execution engine and notifies all the listeners.
	 */
	class EventDispatchJob extends Job {

		public EventDispatchJob() {
			super("Opendf Debugger Event Dispatch");
			setSystem(true);
		}

		/**
		 * Notification the given event occurred in the target program being
		 * interpreted. The events are
		 * 
		 * started - the interpreter has started (guaranteed to be the first event sent)
		 * 
		 * terminated - the interpreter has terminated (guaranteed to be the last event sent)
		 * 
		 * suspended N:X - the interpreter has suspended component N and entered debug mode; 
		 * X is the cause of the suspension:
		 * 
		 *   breakpoint L - a breakpoint at line L was hit
		 *   client - a client request to suspend has completed
		 *   drop - a client request to drop a frame has completed
		 *   event E - an error was encountered, where E describes the error
		 *   step - a step request has completed
		 *   watch V A - a watchpoint for variable V was hit for reason A (read or write), on variable V
		 * 
		 * resumed N:X - the interpreter has resumed execution of component N in run
		 * mode; X is the cause of the resume:
		 * 
		 *   step - a step request has been initiated
	   *   client - a client request to resume has been initiated
		 * 
		 * @param event the event
		 * 
		 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		protected IStatus run(IProgressMonitor monitor) {
			String event = "";
			while (!isTerminated() && event != null) {
				try {
					event = eventReader.readLine();
					//parse events
					if (event.startsWith("resumed")) {
						int index = event.indexOf(":");
						String compName = event.substring(8, index);
						if (event.endsWith("step")) {
							event = "step";
						} else if (event.endsWith("client")) {
							event = "client";
						}
						for (IOpendfEventListener listener : eventListeners) {
							listener.handleResumedEvent(compName, event);
						}
					} else if (event.startsWith("suspended")) {
						int index = event.indexOf(":");
						String compName = event.substring(10, index);
						if (event.endsWith("client")) {
						} else if (event.endsWith("step")) {
							//suspended(DebugEvent.STEP_END);
						} else if (event.startsWith("suspended event")) {
							//exceptionHit();
						} 
						else if (event.endsWith("drop")) {
							//suspended(DebugEvent.STEP_END);
						}
						for (IOpendfEventListener listener : eventListeners) {
							listener.handleSuspendedEvent(compName, event);
						}
					} else if (event.equals("started")) {
						for (IOpendfEventListener listener : eventListeners) {
							listener.handleStartedEvent();
						}
					} else if (event.equals("terminated")) {
						for (IOpendfEventListener listener : eventListeners) {
							listener.handleTerminatedEvent();
						}
					} else {
						System.err.println("Unexpected event received from execution engine: " + event);
					}

				} catch (IOException e) {
					terminated();
				}
			}
			return Status.OK_STATUS;
		}

	}


}
