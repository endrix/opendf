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

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;

/**
 * A debugger thread representing an active element in an opendf simulation.
 * This may include actors, connections, composites, etc. - anything that has dynamic behaviour
 * 
 * @author Rob Esser
 * @version 20 March 2009
 */
public abstract class OpendfThread extends OpendfDebugElement implements IThread, IOpendfEventListener {
	
	public final static IVariable[] NOVARIABLES = new IVariable[0];
	public final static IStackFrame[] NOFRAMES = new IStackFrame[0];
	public final static IBreakpoint[] NOBREAKPOINTS = new IBreakpoint[0];

	/**
	 * Whether this thread is stepping
	 */
	protected boolean isStepping = false;
	
	/**
	 * Whether this thread is suspended
	 */
	protected boolean isSuspended = false;

	//The actual name of this component
	private String componentName = "No name";

	
	/**
	 * Constructs a new thread for the given target
	 * 
	 * @param target execution engine
	 * @param the name of the executable component
	 */
	public OpendfThread(OpendfDebugTarget target, String componentName) {
		super(target);
		this.componentName = componentName;
		getOpendfDebugTarget().addEventListener(this);
	}
	
	/**
	 * Here we create a stack frame to represent the execution stack
	 * 
	 * @see org.eclipse.debug.core.model.IThread#getStackFrames()
	 */
	public IStackFrame[] getStackFrames() throws DebugException {
		return NOFRAMES;
	}
	
	/**
	 * Default no stack frames
	 * 
	 * @see org.eclipse.debug.core.model.IThread#hasStackFrames()
	 */
	public boolean hasStackFrames() throws DebugException {
		return false;
	}
	
	/**
	 * Default priority 0
	 * 
	 * @see org.eclipse.debug.core.model.IThread#getPriority()
	 */
	public int getPriority() throws DebugException {
		return 0;
	}
	
	/**
	 * Return the top of the current stack frame
	 * 
	 * @see org.eclipse.debug.core.model.IThread#getTopStackFrame()
	 */
	public IStackFrame getTopStackFrame() throws DebugException {
		return null;
	}
	
	/**
	 * Return the name of this thread
	 * 
	 * @see org.eclipse.debug.core.model.IThread#getName()
	 */
	public String getName() {
		return getComponentName() + " " + (isSuspended() ? "(suspended)" : (isStepping() ? "(stepping)" : "(running)"));
	}
	
	/**
	 * Return the name of the component this thread represents
	 */
	public String getComponentName() {
		return componentName;
	}

	/**
	 * Return all the breakpoints associated with this thread
	 * 
	 * @see org.eclipse.debug.core.model.IThread#getBreakpoints()
	 */
	public IBreakpoint[] getBreakpoints() {
		return NOBREAKPOINTS;
	}
	
	/**
	 * Notifies this thread it has been suspended by the given breakpoint.
	 * 
	 * @param breakpoint breakpoint
	 */
	public abstract void suspendedBy(IBreakpoint breakpoint);
	
	/**
	 * Can we resume the execution of this thread
	 * 
	 * @see org.eclipse.debug.core.model.ISuspendResume#canResume()
	 */
	public boolean canResume() {
		return isSuspended();
	}
	
	/**
	 * Can this thread be suspended?
	 * 
	 * @see org.eclipse.debug.core.model.ISuspendResume#canSuspend()
	 */
	public boolean canSuspend() {
		return !isSuspended();
	}
	
	/**
	 * 
	 * @see org.eclipse.debug.core.model.ISuspendResume#isSuspended()
	 */
	public boolean isSuspended() {
		return isSuspended && !isTerminated();
	}
	
	/**
	 * Sets whether this thread is suspended
	 * 
	 * @param suspended whether suspended
	 */
	protected void setSuspended(boolean suspended) {
		isSuspended = suspended;
	}

	/**
	 * Resume the execution of this component
	 * 
	 * @see org.eclipse.debug.core.model.ISuspendResume#resume()
	 */
	public abstract void resume() throws DebugException;
	
	/**
	 * Suspend the execution of this component
	 * 
	 * @see org.eclipse.debug.core.model.ISuspendResume#suspend()
	 */
	public abstract void suspend() throws DebugException;
	
	/**
	 * @see org.eclipse.debug.core.model.IStep#canStepInto()
	 */
	public boolean canStepInto() {
		return false;
	}
	
	/**
	 * @see org.eclipse.debug.core.model.IStep#canStepOver()
	 */
	public boolean canStepOver() {
		return isSuspended();
	}
	
	/**
	 * @see org.eclipse.debug.core.model.IStep#canStepReturn()
	 */
	public boolean canStepReturn() {
		return false;
	}
	
	/**
	 * @see org.eclipse.debug.core.model.IStep#isStepping()
	 */
	public boolean isStepping() {
		return isStepping;
	}
	
	/**
	 * @see org.eclipse.debug.core.model.IStep#stepInto()
	 */
	public void stepInto() throws DebugException {
	}
	
	/**
	 * A simple step
	 * 
	 * @see org.eclipse.debug.core.model.IStep#stepOver()
	 */
	public abstract void stepOver() throws DebugException;
	
	/**
	 * @see org.eclipse.debug.core.model.IStep#stepReturn()
	 */
	public void stepReturn() throws DebugException {
	}
	
	/**
	 * @see org.eclipse.debug.core.model.ITerminate#canTerminate()
	 */
	public boolean canTerminate() {
		return !isTerminated();
	}
	
	/**
	 * Has the session terminated?
	 * 
	 * @see org.eclipse.debug.core.model.ITerminate#isTerminated()
	 */
	public boolean isTerminated() {
		return getDebugTarget().isTerminated();
	}
	
	/**
	 * Terminate debugging session
	 * 
	 * @see org.eclipse.debug.core.model.ITerminate#terminate()
	 */
	public void terminate() throws DebugException {
		getDebugTarget().terminate();
	}

}
