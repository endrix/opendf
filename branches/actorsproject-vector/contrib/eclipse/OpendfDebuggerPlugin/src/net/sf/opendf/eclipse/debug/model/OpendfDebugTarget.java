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

package net.sf.opendf.eclipse.debug.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import net.sf.opendf.eclipse.debug.OpendfDebugConstants;
import net.sf.opendf.eclipse.debug.breakpoints.ActorLineBreakpoint;
import net.sf.opendf.eclipse.debug.breakpoints.ActorRunToLineBreakpoint;
import net.sf.orcc.debug.DDPConstants;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Opendf debugger target.
 * 
 * @author Rob Esser
 * @version 24 March 2009
 */
public class OpendfDebugTarget extends OpendfDebugElement implements
		IDebugTarget, IBreakpointManagerListener, IOpendfEventListener {

	/**
	 * Listens to events from the Opendf execution engine and notifies all the
	 * listeners.
	 */
	class EventDispatchJob extends Job {

		public EventDispatchJob() {
			super("Opendf Debugger Event Dispatch");
			setSystem(true);
		}

		/**
		 * Notify listeners in a thread safe manner
		 */
		private void notifyOtherEvents(String event) {
			Object[] listeners = eventListeners.toArray();
			for (Object listener : listeners) {
				((IOpendfEventListener) listener).handleEvent(event);
			}
		}

		/**
		 * Notify listeners in a thread safe manner
		 * 
		 * @param compName
		 * @param event
		 */
		private void notifyResumedListeners(String compName, String event) {
			Object[] listeners = eventListeners.toArray();
			for (Object listener : listeners) {
				((IOpendfEventListener) listener).handleResumedEvent(compName,
						event);
			}
		}

		/**
		 * Notify listeners in a thread safe manner
		 */
		private void notifyStartedListeners() {
			Object[] listeners = eventListeners.toArray();
			for (Object listener : listeners) {
				((IOpendfEventListener) listener).handleStartedEvent();
			}
		}

		/**
		 * Notify listeners in a thread safe manner
		 * 
		 * @param compName
		 * @param event
		 */
		private void notifySuspendedListeners(String compName, String event) {
			Object[] listeners = eventListeners.toArray();
			for (Object listener : listeners) {
				((IOpendfEventListener) listener).handleSuspendedEvent(
						compName, event);
			}
		}

		/**
		 * Notify listeners in a thread safe manner
		 */
		private void notifyTerminatedListeners() {
			Object[] listeners = eventListeners.toArray();
			for (Object listener : listeners) {
				((IOpendfEventListener) listener).handleTerminatedEvent();
			}
		}

		/**
		 * Notification the given event occurred in the target program being
		 * interpreted. The events are
		 * 
		 * started - the interpreter has started (guaranteed to be the first
		 * event sent)
		 * 
		 * terminated - the interpreter has terminated (guaranteed to be the
		 * last event sent)
		 * 
		 * suspended N:X - the interpreter has suspended component N and entered
		 * debug mode; X is the cause of the suspension:
		 * 
		 * breakpoint L - a breakpoint at line L was hit client - a client
		 * request to suspend has completed drop - a client request to drop a
		 * frame has completed event E - an error was encountered, where E
		 * describes the error step - a step request has completed watch V A - a
		 * watchpoint for variable V was hit for reason A (read or write), on
		 * variable V
		 * 
		 * resumed N:X - the interpreter has resumed execution of component N in
		 * run mode; X is the cause of the resume:
		 * 
		 * step - a step request has been initiated client - a client request to
		 * resume has been initiated
		 * 
		 * @param event
		 *            the event
		 * 
		 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		protected IStatus run(IProgressMonitor monitor) {
			String event = "";
			while (!isTerminated() && event != null) {
				try {
					event = eventReader.readLine();
					System.out.println("Event received:  " + event);

					// parse events
					if (event.startsWith("resumed")) {
						int index = event.indexOf(":");
						String compName = event.substring(8, index);
						if (event.endsWith("step")) {
							event = "step";
						} else if (event.endsWith("client")) {
							event = "client";
						}
						notifyResumedListeners(compName, event);
					} else if (event.startsWith("suspended")) {
						int index = event.indexOf(":");
						String compName = event.substring(10, index);
						if (event.endsWith("client")) {
						} else if (event.endsWith("step")) {
							// suspended(DebugEvent.STEP_END);
						} else if (event.startsWith("suspended event")) {
							// exceptionHit();
						} else if (event.endsWith("drop")) {
							// suspended(DebugEvent.STEP_END);
						}
						notifySuspendedListeners(compName, event);
					} else if (event.equals("started")) {
						notifyStartedListeners();
					} else if (event.equals("terminated")) {
						notifyTerminatedListeners();
					} else {
						notifyOtherEvents(event);
					}

				} catch (IOException e) {
					terminated();
				}
			}
			return Status.OK_STATUS;
		}

	}

	private BufferedReader commandReader;

	// sockets to communicate with execution engine
	private Socket commandSocket;
	private PrintWriter commandWriter;
	// containing launch object
	private ILaunch debugLauncher;
	// event dispatch job
	private EventDispatchJob eventDispatch;

	/**
	 * event listeners
	 */
	private List<IOpendfEventListener> eventListeners;

	private BufferedReader eventReader;

	private Socket eventSocket;

	private boolean isSuspended = false;

	// terminated state
	private boolean isTerminated = false;
	// associated system process of the execution engine
	private IProcess systemProcess;

	// threads
	private OpendfThread[] threads;

	/**
	 * Constructs a new debug target in the given launch for the associated
	 * Opendf VM process.
	 * 
	 * @param launch
	 *            containing launch
	 * @param process
	 *            Opendf execution engine
	 * @param commandPort
	 *            port to send requests to the execution engine
	 * @param eventPort
	 *            port to read events from
	 * @exception CoreException
	 *                if unable to connect to host
	 */
	public OpendfDebugTarget(ILaunch launch, IProcess process, int commandPort,
			int eventPort) throws CoreException {
		super(null);

		debugLauncher = launch;
		systemProcess = process;

		eventListeners = new ArrayList<IOpendfEventListener>();
		addEventListener(this);

		try {
			// give execution engine a chance to start
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			commandSocket = new Socket("localhost", commandPort);
			commandWriter = new PrintWriter(commandSocket.getOutputStream());
			commandReader = new BufferedReader(new InputStreamReader(
					commandSocket.getInputStream()));
			// give interpreter a chance to open next socket
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			eventSocket = new Socket("localhost", eventPort);
			eventReader = new BufferedReader(new InputStreamReader(eventSocket
					.getInputStream()));
		} catch (UnknownHostException e) {
			requestFailed("Unable to connect to Execution Engine", e);
		} catch (IOException e) {
			requestFailed("Unable to connect to Execution Engine", e);
		}

		eventDispatch = new EventDispatchJob();
		eventDispatch.schedule();

		IBreakpointManager breakpointManager = getBreakpointManager();
		breakpointManager.addBreakpointListener(this);
		breakpointManager.addBreakpointManagerListener(this);
	}

	/**
	 * Registers the given event listener. The listener will be notified of
	 * events in the program being interpreted. Has no effect if the listener is
	 * already registered.
	 * 
	 * @param listener
	 *            event listener
	 */
	public void addEventListener(IOpendfEventListener listener) {
		if (!eventListeners.contains(listener)) {
			// System.out.println("Added listener: " + listener);
			eventListeners.add(listener);
		}
	}

	@Override
	public void breakpointAdded(IBreakpoint breakpoint) {
		if (supportsBreakpoint(breakpoint)) {
			try {
				if ((breakpoint.isEnabled() && getBreakpointManager()
						.isEnabled())
						|| !breakpoint.isRegistered()) {
					ActorLineBreakpoint actorBreakpoint = (ActorLineBreakpoint) breakpoint;
					actorBreakpoint.install(this);
				}
			} catch (CoreException e) {
			}
		}
	}

	/**
	 * Toggle the breakpoint
	 * 
	 * @see org.eclipse.debug.core.IBreakpointListener#breakpointChanged(org.eclipse
	 *      .debug.core.model.IBreakpoint,
	 *      org.eclipse.core.resources.IMarkerDelta)
	 */
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		if (supportsBreakpoint(breakpoint)) {
			try {
				if (breakpoint.isEnabled()
						&& getBreakpointManager().isEnabled()) {
					breakpointAdded(breakpoint);
				} else {
					breakpointRemoved(breakpoint, null);
				}
			} catch (CoreException e) {
			}
		}
	}

	/**
	 * When the breakpoint manager disables, remove all registered breakpoints
	 * requests from the VM. When it enables, reinstall them.
	 */
	public void breakpointManagerEnablementChanged(boolean enabled) {
		IBreakpoint[] breakpoints = getBreakpointManager().getBreakpoints(
				getModelIdentifier());
		for (int i = 0; i < breakpoints.length; i++) {
			if (enabled) {
				breakpointAdded(breakpoints[i]);
			} else {
				breakpointRemoved(breakpoints[i], null);
			}
		}
	}

	@Override
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
		if (supportsBreakpoint(breakpoint)) {
			try {
				ActorLineBreakpoint actorBreakpoint = (ActorLineBreakpoint) breakpoint;
				actorBreakpoint.remove(this);
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
	 * Returns whether popping the data stack is currently permitted
	 * 
	 * @return whether popping the data stack is currently permitted
	 */
	public boolean canPop() {
		// try {
		// return !isTerminated() && isSuspended() && getDataStack().length > 0;
		// } catch (DebugException e) {
		// }
		return false;
	}

	/**
	 * Returns whether pushing a value is currently supported.
	 * 
	 * @return whether pushing a value is currently supported
	 */
	public boolean canPush() {
		return !isTerminated() && isSuspended();
	}

	@Override
	public boolean canResume() {
		return !isTerminated() && isSuspended();
	}

	@Override
	public boolean canSuspend() {
		return !isTerminated() && !isSuspended();
	}

	@Override
	public boolean canTerminate() {
		// no process, so no "getProcess().canTerminate();"
		return !isTerminated;
	}

	@Override
	public void disconnect() throws DebugException {
		// we should never do this
	}

	private String[] getActorNames() throws DebugException {
		try {
			JSONObject request = new JSONObject();
			request.put(DDPConstants.REQUEST, DDPConstants.REQ_GET_COMPONENTS);
			JSONObject reply = sendRequest(request);
			JSONArray array = reply.getJSONArray(DDPConstants.ATTR_COMPONENTS);
			String[] names = new String[array.length()];
			for (int i = 0; i < names.length; i++) {
				names[i] = array.getString(i);
			}

			return names;
		} catch (JSONException e) {
			throw newDebugExceptionJSON(DebugException.REQUEST_FAILED, e);
		}
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

	@Override
	public IDebugTarget getDebugTarget() {
		return this;
	}

	@Override
	public ILaunch getLaunch() {
		return debugLauncher;
	}

	@Override
	public IMemoryBlock getMemoryBlock(long startAddress, long length)
			throws DebugException {
		// nothing to return for now
		return null;
	}

	@Override
	public String getName() throws DebugException {
		return "Opendf";
	}

	@Override
	public IProcess getProcess() {
		return systemProcess;
	}

	@Override
	public IThread[] getThreads() throws DebugException {
		if (!isTerminated) {
			if (threads == null) {
				// ask the execution engine for the active actors and other
				// stuff
				String[] actorNames = getActorNames();

				// add in new threads
				List<OpendfThread> threadList = new ArrayList<OpendfThread>();
				for (int i = 0; i < actorNames.length; i++) {
					String newName = actorNames[i];
					threadList.add(new ActorThread(this, newName));
				}

				threads = (OpendfThread[]) threadList
						.toArray(new OpendfThread[0]);
			}
		}

		return threads;
	}

	/**
	 * Handle all other events
	 * 
	 * @param event
	 */
	public void handleEvent(String event) {
		System.out.println(this.getClass().getSimpleName() + ".handleEvent: "
				+ event);
	}

	public void handleResumedEvent(String compName, String event) {
		// isSuspended = false;
	}

	public void handleStartedEvent() {
		started();
	}

	public void handleSuspendedEvent(String compName, String event) {
		isSuspended = true;
	}

	public void handleTerminatedEvent() {
		terminated();
	}

	@Override
	public boolean hasThreads() throws DebugException {
		return true;
	}

	/**
	 * Install breakpoints that are already registered with the breakpoint
	 * manager.
	 */
	private void installDeferredBreakpoints() {
		IBreakpoint[] breakpoints = getBreakpointManager().getBreakpoints(
				getModelIdentifier());
		for (int i = 0; i < breakpoints.length; i++) {
			breakpointAdded(breakpoints[i]);
		}
	}

	@Override
	public boolean isDisconnected() {
		// we cannot disconnect so we should never be disconnected
		return false;
	}

	@Override
	public boolean isSuspended() {
		return isTerminated() || isSuspended;
	}

	@Override
	public boolean isTerminated() {
		// no process, so no " || getProcess().isTerminated()"
		return isTerminated;
	}

	/**
	 * Pops and returns the top of the data stack
	 * 
	 * @return the top value on the stack
	 * @throws DebugException
	 *             if the stack is empty or the request fails
	 */
	public IValue pop() throws DebugException {
		// IValue[] dataStack = getDataStack();
		// if (dataStack.length > 0) {
		// sendCommand("poOpendfta");
		// return dataStack[0];
		// }
		// requestFailed("Empty stack", null);
		return null;
	}

	/**
	 * Pushes a value onto the stack.
	 * 
	 * @param value
	 *            value to push
	 * @throws DebugException
	 *             on failure
	 */
	public void push(String value) throws DebugException {
		// TODO push value
		System.out.println("TODO: push value");
	}

	/**
	 * Removes registration of the given event listener. Has no effect if the
	 * listener is not currently registered.
	 * 
	 * @param listener
	 *            event listener
	 */
	public void removeEventListener(IOpendfEventListener listener) {
		// System.out.println("Removed listener: " + listener);
		synchronized (eventListeners) {
			eventListeners.remove(listener);
		}
	}

	@Override
	public void resume() throws DebugException {
		// resuming the target means resuming the threads
		for (int i = 0; i < threads.length; i++) {
			threads[i].resume();
		}
		fireResumeEvent(DebugEvent.CLIENT_REQUEST);
	}

	/**
	 * Send a command to the execution engine
	 * 
	 * @see example.debug.core.model.OpendfDebugElement#sendRequest(java.lang.String)
	 */
	public JSONObject sendRequest(JSONObject request) throws DebugException {
		synchronized (commandSocket) {
			commandWriter.println(request);
			commandWriter.flush();
			try {
				// wait for reply
				String reply = commandReader.readLine();
				if (reply == null) {
					throw newDebugException("could not get reply",
							DebugException.REQUEST_FAILED, null);
				} else {
					return new JSONObject(reply);
				}
			} catch (IOException e) {
				throw newDebugException("I/O error when sending request: "
						+ request, DebugException.REQUEST_FAILED, e);
			} catch (JSONException e) {
				throw newDebugExceptionJSON(DebugException.REQUEST_FAILED, e);
			}
		}
	}

	/**
	 * Notification we have connected to the VM and it has started. Resume the
	 * VM.
	 */
	private void started() {
		fireCreationEvent();
		installDeferredBreakpoints();
		// try {
		// getThreads();
		// suspend();
		// } catch (DebugException e) {
		// }//
	}

	/**
	 * Are breakpoints supported - depends on debugger state
	 * 
	 * @see org.eclipse.debug.core.model.IDebugTarget#supportsBreakpoint(org.eclipse.debug.core.model.IBreakpoint)
	 */
	public boolean supportsBreakpoint(IBreakpoint breakpoint) {
		if (!isTerminated()
				&& breakpoint.getModelIdentifier().equals(getModelIdentifier())) {
			try {
				String program = getLaunch().getLaunchConfiguration()
						.getAttribute(OpendfDebugConstants.ID_PLUGIN,
								(String) null);
				if (program != null) {
					IResource resource = null;
					if (breakpoint instanceof ActorRunToLineBreakpoint) {
						ActorRunToLineBreakpoint rtl = (ActorRunToLineBreakpoint) breakpoint;
						resource = rtl.getSourceFile();
					} else {
						IMarker marker = breakpoint.getMarker();
						if (marker != null) {
							resource = marker.getResource();
						}
					}
					if (resource != null) {
						IPath p = new Path(program);
						return resource.getFullPath().equals(p);
					}
				}
			} catch (CoreException e) {
			}
		}
		return false;
	}

	@Override
	public boolean supportsStorageRetrieval() {
		// not supported
		return false;
	}

	@Override
	public void suspend() throws DebugException {
		// suspending the target means resuming the threads
		for (int i = 0; i < threads.length; i++) {
			threads[i].suspend();
		}
		fireSuspendEvent(DebugEvent.CLIENT_REQUEST);
	}

	@Override
	public void terminate() throws DebugException {
		try {
			JSONObject request = new JSONObject();
			request.put(DDPConstants.REQUEST, DDPConstants.REQ_EXIT);
			sendRequest(request);
		} catch (JSONException e) {
			throw newDebugExceptionJSON(DebugException.REQUEST_FAILED, e);
		}
	}

	/**
	 * Called when this debug target terminates.
	 */
	private void terminated() {
		try {
			isTerminated = true;
			// remove threads from the listeners
			for (int i = 0; i < threads.length; i++) {
				removeEventListener(threads[i]);
			}
			threads = new OpendfThread[0];
			IBreakpointManager breakpointManager = getBreakpointManager();
			breakpointManager.removeBreakpointListener(this);
			breakpointManager.removeBreakpointManagerListener(this);
			removeEventListener(this);
			fireTerminateEvent();
		} catch (Exception e) {
			System.err.println(e);
		}
	}

}
