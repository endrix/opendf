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
 * This may include actors, connections, composites, etc. - anything that has
 * dynamic behaviour
 * 
 * @author Rob Esser
 * @version 20 March 2009
 */
public abstract class OpendfThread extends OpendfDebugElement implements
		IThread, IOpendfEventListener {

	public final static IVariable[] NOVARIABLES = new IVariable[0];
	public final static IStackFrame[] NOFRAMES = new IStackFrame[0];
	public final static IBreakpoint[] NOBREAKPOINTS = new IBreakpoint[0];

	/**
	 * Whether this thread is stepping
	 */
	protected boolean isStepping;

	/**
	 * Whether this thread is suspended
	 */
	protected boolean isSuspended;

	/**
	 * The actual name of this component
	 */
	private String componentName;

	/**
	 * Constructs a new thread for the given target
	 * 
	 * @param target
	 *            execution engine
	 * @param the
	 *            name of the executable component
	 */
	public OpendfThread(OpendfDebugTarget target, String componentName) {
		super(target);
		this.componentName = componentName;
		getOpendfDebugTarget().addEventListener(this);

		// threads are initially suspended
		isSuspended = true;
	}

	@Override
	public IStackFrame[] getStackFrames() throws DebugException {
		return NOFRAMES;
	}

	@Override
	public boolean hasStackFrames() throws DebugException {
		// by default, thread has no stack frames
		return false;
	}

	@Override
	public int getPriority() throws DebugException {
		return 0;
	}

	@Override
	public IStackFrame getTopStackFrame() throws DebugException {
		return null;
	}

	@Override
	public String getName() {
		return getComponentName()
				+ " "
				+ (isSuspended() ? "(suspended)" : (isStepping() ? "(stepping)"
						: "(running)"));
	}

	/**
	 * Return the name of the component this thread represents
	 */
	public String getComponentName() {
		return componentName;
	}

	@Override
	public IBreakpoint[] getBreakpoints() {
		return NOBREAKPOINTS;
	}

	/**
	 * Notifies this thread it has been suspended by the given breakpoint.
	 * 
	 * @param breakpoint
	 *            breakpoint
	 */
	public abstract void suspendedBy(IBreakpoint breakpoint);

	@Override
	public boolean canResume() {
		return isSuspended();
	}

	@Override
	public boolean canSuspend() {
		return !isSuspended();
	}

	@Override
	public boolean isSuspended() {
		return isSuspended && !isTerminated();
	}

	/**
	 * Sets whether this thread is suspended
	 * 
	 * @param suspended
	 *            whether suspended
	 */
	protected void setSuspended(boolean suspended) {
		isSuspended = suspended;
	}

	@Override
	public abstract void resume() throws DebugException;

	@Override
	public abstract void suspend() throws DebugException;

	@Override
	public boolean canStepInto() {
		return false;
	}

	@Override
	public boolean canStepOver() {
		return isSuspended();
	}

	@Override
	public boolean canStepReturn() {
		return false;
	}

	@Override
	public boolean isStepping() {
		return isStepping;
	}

	@Override
	public void stepInto() throws DebugException {
	}

	@Override
	public abstract void stepOver() throws DebugException;

	@Override
	public void stepReturn() throws DebugException {
	}

	@Override
	public boolean canTerminate() {
		return !isTerminated();
	}

	@Override
	public boolean isTerminated() {
		return getDebugTarget().isTerminated();
	}

	@Override
	public void terminate() throws DebugException {
		getDebugTarget().terminate();
	}

}
