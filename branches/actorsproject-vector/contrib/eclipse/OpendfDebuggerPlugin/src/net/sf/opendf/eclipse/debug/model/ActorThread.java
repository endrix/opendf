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

import java.util.HashMap;
import java.util.Map;

import net.sf.orcc.debug.DDPConstants;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IVariable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A debugger thread representing the execution of an individual actor.
 * 
 * @author Rob Esser
 * @author Matthieu Wipliez
 */
public class ActorThread extends OpendfThread {

	/**
	 * Breakpoint this thread is suspended at or <code>null</code> if none.
	 */
	private IBreakpoint currentBreakpoint;

	/**
	 * Table mapping stack frames to current variables
	 */
	private Map<IStackFrame, IVariable[]> stackVariables = new HashMap<IStackFrame, IVariable[]>();

	/**
	 * Constructs a new thread for the given target
	 * 
	 * @param target
	 *            execution engine
	 * @param the
	 *            name of the executable component
	 */
	public ActorThread(OpendfDebugTarget target, String componentName) {
		super(target, componentName);
	}

	@Override
	public boolean canStepInto() {
		// TODO can step into
		return false;
	}

	@Override
	public boolean canStepReturn() {
		// TODO can step return
		return false;
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
		return new IBreakpoint[] { currentBreakpoint };
	}

	@Override
	public IStackFrame[] getStackFrames() throws DebugException {
		// Here we create a stack frame to represent each active element. e.g.
		// actor
		if (isSuspended()) {
			try {
				JSONObject request = new JSONObject();
				request.put(DDPConstants.REQUEST, DDPConstants.REQ_STACK);
				request.put(DDPConstants.ATTR_ACTOR_NAME, getComponentName());
				JSONObject reply = sendRequest(request);
				JSONArray frames = reply.getJSONArray(DDPConstants.ATTR_FRAMES);

				IStackFrame[] theFrames = new IStackFrame[frames.length()];
				for (int i = 0; i < theFrames.length; i++) {
					theFrames[i] = new ActorStackFrame(this, frames
							.getJSONObject(i), i);
				}
				return theFrames;
			} catch (JSONException e) {
				throw newDebugExceptionJSON(DebugException.REQUEST_FAILED, e);
			}
		}

		return NOFRAMES;
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
	 * Returns the current variables for the given stack frame, or
	 * <code>null</code> if none.
	 * 
	 * @param frame
	 *            stack frame
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
	 * Handle all other events
	 * 
	 * @param event
	 */
	public void handleEvent(String event) {
		System.out.println(this.getClass().getSimpleName() + ".handleEvent: "
				+ event);
	}

	public void handleResumedEvent(String compName, String event) {
		if (getComponentName().equals(compName)) {
			// System.out.println("Resumed " + compName);
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

	public void handleStartedEvent() {
		fireEvent(new DebugEvent(this, DebugEvent.CREATE));
	}

	public void handleSuspendedEvent(String compName, String event) {
		if (getComponentName().equals(compName)) {
			// System.out.println("Suspended " + compName);
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
				// create a breakpoint object and fire event
				suspendedBy(null);
				// suspended(DebugEvent.BREAKPOINT);
			} else {
				System.err.println("unknown suspended event: " + type);
			}
		}
	}

	public void handleTerminatedEvent() {
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
	 * Step the actor
	 * 
	 * @see org.eclipse.debug.core.model.ISuspendResume#resume()
	 */
	public void resume() throws DebugException {
		try {
			JSONObject request = new JSONObject();
			request.put(DDPConstants.REQUEST, DDPConstants.REQ_RESUME);
			request.put(DDPConstants.ATTR_ACTOR_NAME, getComponentName());
			sendRequest(request);
		} catch (JSONException e) {
			throw newDebugExceptionJSON(DebugException.REQUEST_FAILED, e);
		}
	}

	/**
	 * Notification the target has resumed for the given reason. Clears any
	 * error condition that was last encountered and fires a resume event, and
	 * clears all cached variables for stack frames.
	 * 
	 * @param detail
	 *            reason for the resume
	 */
	private void resumed(int detail) {
		synchronized (stackVariables) {
			stackVariables.clear();
		}
		fireResumeEvent(detail);
	}

	/**
	 * Sets whether this thread is stepping
	 * 
	 * @param stepping
	 *            whether stepping
	 */
	private void setStepping(boolean stepping) {
		isStepping = stepping;
	}

	// /**
	// * Notification an error was encountered. Fires a breakpoint
	// * suspend event.
	// */
	// private void exceptionHit() {
	// suspended(DebugEvent.BREAKPOINT);
	// }

	/**
	 * Sets the current variables for the given stack frame. Called by Opendf
	 * stack frame when it is created.
	 * 
	 * @param frame
	 * @param variables
	 */
	protected void setVariables(IStackFrame frame, IVariable[] variables) {
		synchronized (stackVariables) {
			stackVariables.put(frame, variables);
		}
	}

	@Override
	public void stepInto() throws DebugException {
		// TODO step into
	}

	@Override
	public void stepOver() throws DebugException {
		// TODO step over
		System.out.println("step over");
	}

	@Override
	public void stepReturn() throws DebugException {
		// TODO step return
	}

	@Override
	public void suspend() throws DebugException {
		try {
			JSONObject request = new JSONObject();
			request.put(DDPConstants.REQUEST, DDPConstants.REQ_SUSPEND);
			request.put(DDPConstants.ATTR_ACTOR_NAME, getComponentName());
			sendRequest(request);
		} catch (JSONException e) {
			throw newDebugExceptionJSON(DebugException.REQUEST_FAILED, e);
		}
	}

	/**
	 * Notification the target has suspended for the given reason
	 * 
	 * @param detail
	 *            reason for the suspend
	 */
	private void suspended(int detail) {
		fireSuspendEvent(detail);
	}

	/**
	 * Notifies this thread it has been suspended by the given breakpoint.
	 * 
	 * @param breakpoint
	 *            breakpoint
	 */
	public void suspendedBy(IBreakpoint breakpoint) {
		currentBreakpoint = breakpoint;
		suspended(DebugEvent.BREAKPOINT);
	}

}
