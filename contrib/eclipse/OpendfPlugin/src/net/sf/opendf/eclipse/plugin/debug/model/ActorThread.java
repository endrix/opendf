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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IVariable;

/**
 * A debugger thread representing the execution of an individual actor.
 * 
 * @author Rob Esser
 * @version 20 March 2009
 */
public class ActorThread extends OpendfThread {
	
	/**
	 * Breakpoint this thread is suspended at or <code>null</code>
	 * if none.
	 */
	private IBreakpoint currentBreakpoint;
	
	/**
	 * Table mapping stack frames to current variables
	 */
	private Map<IStackFrame, IVariable[]> stackVariables = new HashMap<IStackFrame, IVariable[]>();
	
	/**
	 * Constructs a new thread for the given target
	 * 
	 * @param target execution engine
	 * @param the name of the executable component
	 */
	public ActorThread(OpendfDebugTarget target, String componentName) {
		super(target, componentName);
	}
	
	/**
	 * Here we create a stack frame to represent each active element. e.g. actor
	 * 
	 * @see org.eclipse.debug.core.model.IThread#getStackFrames()
	 */
	public IStackFrame[] getStackFrames() throws DebugException {
		if (isSuspended()) {
			String framesData = sendCommand("stack " + getComponentName());
			if (framesData != null) {
				String[] frames = framesData.split("#");
				IStackFrame[] theFrames = new IStackFrame[frames.length];
				for (int i = 0; i < frames.length; i++) {
					String data = frames[i];
					theFrames[frames.length - i - 1] = new ActorStackFrame(this, data, i);
				}
				return theFrames;
			}
		}
		return NOFRAMES;
	}
	
	/**
	 * Only if the actor is suspended does it have stack frames
	 * 
	 * @see org.eclipse.debug.core.model.IThread#hasStackFrames()
	 */
	public boolean hasStackFrames() throws DebugException {
		return isSuspended();
	}
	
	/**
	 * Return the top of the current stack frame
	 * 
	 * @see org.eclipse.debug.core.model.IThread#getTopStackFrame()
	 */
	public IStackFrame getTopStackFrame() throws DebugException {
		assert hasStackFrames();
		IStackFrame[] frames = getStackFrames();
		if (frames.length > 0) {
			return frames[0];
		}
		return null;
	}
	
	/**
	 * Return all the breakpoints associated with this thread
	 * 
	 * @see org.eclipse.debug.core.model.IThread#getBreakpoints()
	 */
	public IBreakpoint[] getBreakpoints() {
		if (currentBreakpoint == null) {
			return new IBreakpoint[0];
		}
		return new IBreakpoint[]{currentBreakpoint};
	}
	
	/**
	 * Notifies this thread it has been suspended by the given breakpoint.
	 * 
	 * @param breakpoint breakpoint
	 */
	public void suspendedBy(IBreakpoint breakpoint) {
		currentBreakpoint = breakpoint;
		suspended(DebugEvent.BREAKPOINT);
	}
	
	/**
	 * Step the actor
	 * 
	 * @see org.eclipse.debug.core.model.ISuspendResume#resume()
	 */
	public void resume() throws DebugException {
		sendCommand("resume " + getComponentName());
	}
	
	/**
	 * Suspend the execution of the component
	 * 
	 * @see org.eclipse.debug.core.model.ISuspendResume#suspend()
	 */
	public void suspend() throws DebugException {
	    sendCommand("suspend " + getComponentName());
	}
	
	/**
	 * Is the current location at a function call?
	 * 
	 * @see org.eclipse.debug.core.model.IStep#canStepInto()
	 */
	public boolean canStepInto() {
		System.out.println("Am I at a function call?");
		return false;
	}
	
	/**
	 * Is the current location at the end of a function?
	 * 
	 * @see org.eclipse.debug.core.model.IStep#canStepReturn()
	 */
	public boolean canStepReturn() {
		System.out.println("Am I at a function end?");
		return false;
	}
	
	/**
	 * Step into a function
	 * 
	 * @see org.eclipse.debug.core.model.IStep#stepInto()
	 */
	public void stepInto() throws DebugException {
		System.out.println("Attempting to step into a function");
	}
	
	/** step out of a function
	 * 
	 * @see org.eclipse.debug.core.model.IStep#stepReturn()
	 */
	public void stepReturn() throws DebugException {
		System.out.println("Attempting to step out of a function");
	}
	
	/**
	 * Step the actor
	 * 
	 * @see org.eclipse.debug.core.model.IStep#stepOver()
	 */
	public void stepOver() throws DebugException {
		sendCommand("step " + getComponentName());
	}
	
	/**
	 * Sets whether this thread is stepping
	 * 
	 * @param stepping whether stepping
	 */
	private void setStepping(boolean stepping) {
		isStepping = stepping;
	}
	
	/**
	 * Sets whether this thread is suspended
	 * 
	 * @param suspended whether suspended
	 */
	private void setSuspended(boolean suspended) {
		isSuspended = suspended;
	}

	/**
	 * Notification the target has resumed for the given reason.
	 * Clears any error condition that was last encountered and
	 * fires a resume event, and clears all cached variables
	 * for stack frames.
	 * 
	 * @param detail reason for the resume
	 */
	private void resumed(int detail) {
		synchronized (stackVariables) {
			stackVariables.clear();
		}
		fireResumeEvent(detail);
	}
	
	/**
	 * Notification the target has suspended for the given reason
	 * 
	 * @param detail reason for the suspend
	 */
	private void suspended(int detail) {
		fireSuspendEvent(detail);
	}

//	/**
//     * Notification an error was encountered. Fires a breakpoint
//     * suspend event.
//     */
//    private void exceptionHit() {
//    	suspended(DebugEvent.BREAKPOINT);
//    }  
	
	/**
	 * Sets the current variables for the given stack frame. Called
	 * by Opendf stack frame when it is created.
	 * 
	 * @param frame
	 * @param variables
	 */
	protected void setVariables(IStackFrame frame, IVariable[] variables) {
		synchronized (stackVariables) {
			stackVariables.put(frame, variables);
		}
	}
	
	/**
	 * Returns the current variables for the given stack frame, or
	 * <code>null</code> if none.
	 * 
	 * @param frame stack frame
	 * @return variables or <code>null</code>
	 */
	protected IVariable[] getVariables(IStackFrame frame) {
		synchronized (stackVariables) {
			IVariable[] variables = (IVariable[]) stackVariables.get(frame);
			if (variables == null) {
				return new IVariable[0];
			}
			return variables;
		}
	}
	
	/**
	 * Pops the top frame off the call stack.
	 *
	 * @throws DebugException
	 */
	public void pop() throws DebugException {
		sendCommand("drop " + getComponentName());
	}
	
	/**
	 * Returns whether this thread can pop the top stack frame.
	 *
	 * @return whether this thread can pop the top stack frame
	 */
	public boolean canPop() {
		try {
			return getStackFrames().length > 1;
		} catch (DebugException e) {
		}
		return false;
	}
	
	public void handleResumedEvent(String compName, String event) {
		if (getComponentName().equals(compName)) {
			// clear previous state
			currentBreakpoint = null;
			setStepping(false);
			setSuspended(false);
			if (event.endsWith("step")) {
				setStepping(true);
				resumed(DebugEvent.STEP_OVER);
			} else if (event.endsWith("client")) {
				resumed(DebugEvent.CLIENT_REQUEST);
			} else if (event.endsWith("drop")) {
				resumed(DebugEvent.STEP_RETURN);
			}
		}
	}

	public void handleSuspendedEvent(String compName, String event) {
		if (getComponentName().equals(compName)) {
			// clear previous state
			currentBreakpoint = null;
			setStepping(false);
			setSuspended(true);
			int index = event.indexOf(":");
			String type = event.substring(index + 1);
			if (type.startsWith("client")) {
				suspended(DebugEvent.CLIENT_REQUEST);
			} else if (type.startsWith("step")) {
				suspended(DebugEvent.STEP_END);
			} else if (type.startsWith("drop")) {
				suspended(DebugEvent.STEP_END);
			} else if (type.startsWith("breakpoint")) {
				//create a breakpoint object and fire event
	    	suspendedBy(null);
	    	//suspended(DebugEvent.BREAKPOINT);
			} else {
				System.err.println("unknown suspended event: " + type);
			}
		}
	}
	/*
  		client - a client request to suspend has completed
  		drop - a client request to drop a frame has completed
  		step - a step request has completed
  		
  		event E - an error was encountered, where E describes the error
  		breakpoint L - a breakpoint at line L was hit
  		watch V A - a watchpoint for variable V was hit for reason A
  			(read or write), on variable V
	 */

	public void handleStartedEvent() {
		fireEvent(new DebugEvent(this, DebugEvent.CREATE));
	}

	public void handleTerminatedEvent() {
	}

	
}
