/* 
BEGINCOPYRIGHT X
	
	Copyright (c) 2007, Xilinx Inc.
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
package net.sf.opendf.eclipse.plugin.debug.breakpoints;

import net.sf.opendf.eclipse.plugin.OpendfConstants;
import net.sf.opendf.eclipse.plugin.debug.model.OpendfDebugTarget;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IWatchpoint;

/**
 * A watchpoint.
 * 
 * @author Rob Esser
 * @version 3rd April 2009
 */
public class ActorWatchpoint extends ActorLineBreakpoint implements IWatchpoint {

	// 'read' or 'write' depending on what caused the last suspend for this watchpoint
	private String fLastSuspendType;

	// marker attributes
	public static final String ACCESS = "ACCESS";
	public static final String MODIFICATION = "MODIFICATION";
	public static final String FUNCTION_NAME = "FUNCTION_NAME";
	public static final String VAR_NAME = "VAR_NAME";

	/**
	 * Default constructor is required for the breakpoint manager to re-create
	 * persisted breakpoints. After instantiating a breakpoint, the
	 * <code>setMarker(...)</code> method is called to restore this breakpoint's
	 * attributes.
	 */
	public ActorWatchpoint() {
	}

	/**
	 * Constructs a line breakpoint on the given resource at the given line
	 * number. The line number is 1-based (i.e. the first line of a file is line
	 * number 1). The Actor VM uses 0-based line numbers, so this line number
	 * translation is done at breakpoint install time.
	 * 
	 * @param resource
	 *          file on which to set the breakpoint
	 * @param lineNumber
	 *          1-based line number of the breakpoint
	 * @param functionName
	 *          function name the variable is defined in
	 * @param varName
	 *          variable name that watchpoint is set on
	 * @param access
	 *          whether this is an access watchpoint
	 * @param modification
	 *          whether this in a modification watchpoint
	 * @throws CoreException
	 *           if unable to create the watchpoint
	 */
	public ActorWatchpoint(final IResource resource, final int lineNumber, final String functionName, final String varName, final boolean access, final boolean modification) throws CoreException {
		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				IMarker marker = resource.createMarker(OpendfConstants.ACTOR_WATCHPOINT_MARKER);
				setMarker(marker);
				setEnabled(true);
				ensureMarker().setAttribute(IMarker.LINE_NUMBER, lineNumber);
				ensureMarker().setAttribute(IBreakpoint.ID, getModelIdentifier());
				setAccess(access);
				setModification(modification);
				setVariable(functionName, varName);
				marker.setAttribute(IMarker.MESSAGE, "Watchpoint: " + resource.getName() + " [line: " + lineNumber + "]");
			}
		};
		run(getMarkerRule(resource), runnable);
	}

	/**
	 * @see org.eclipse.debug.core.model.IWatchpoint#isAccess()
	 */
	public boolean isAccess() throws CoreException {
		return getMarker().getAttribute(ACCESS, true);
	}

	/**
	 * @see org.eclipse.debug.core.model.IWatchpoint#setAccess(boolean)
	 */
	public void setAccess(boolean access) throws CoreException {
		setAttribute(ACCESS, access);
	}

	/**
	 * @see org.eclipse.debug.core.model.IWatchpoint#isModification()
	 */
	public boolean isModification() throws CoreException {
		return getMarker().getAttribute(MODIFICATION, true);
	}

	/**
	 * @see org.eclipse.debug.core.model.IWatchpoint#setModification(boolean)
	 */
	public void setModification(boolean modification) throws CoreException {
		setAttribute(MODIFICATION, modification);
	}

	/**
	 * @see org.eclipse.debug.core.model.IWatchpoint#supportsAccess()
	 */
	public boolean supportsAccess() {
		return true;
	}

	/**
	 * @see org.eclipse.debug.core.model.IWatchpoint#supportsModification()
	 */
	public boolean supportsModification() {
		return true;
	}

	/**
	 * Sets the variable and function names the watchpoint is set on.
	 * 
	 * @param functionName
	 *          function name
	 * @param variableName
	 *          variable name
	 * @throws CoreException
	 *           if an exception occurrs setting marker attribtues
	 */
	protected void setVariable(String functionName, String variableName) throws CoreException {
		setAttribute(VAR_NAME, variableName);
		setAttribute(FUNCTION_NAME, functionName);
	}

	/**
	 * Returns the name of the variable this watchpoint is set on.
	 * 
	 * @return the name of the variable this watchpoint is set on
	 * @throws CoreException
	 *           if unable to access the attribute
	 */
	public String getVariableName() throws CoreException {
		return getMarker().getAttribute(VAR_NAME, (String) null);
	}

	/**
	 * Returns the name of the function the variable associted with this
	 * watchpoint is defined in.
	 * 
	 * @return the name of the function the variable associted with this
	 *         watchpoint is defined in
	 * @throws CoreException
	 *           if unable to access the attribute
	 */
	public String getFunctionName() throws CoreException {
		return getMarker().getAttribute(FUNCTION_NAME, (String) null);
	}

	/**
	 * Sets the type of event that causes the last suspend event.
	 * 
	 * @param description
	 *          one of 'read' or 'write'
	 */
	public void setSuspendType(String description) {
		fLastSuspendType = description;
	}

	/**
	 * Returns the type of event that caused the last suspend.
	 * 
	 * @return 'read', 'write', or <code>null</code> if undefined
	 */
	public String getSuspendType() {
		return fLastSuspendType;
	}

	/**
	 * @see example.debug.core.breakpoints.ActorLineBreakpoint#createRequest(example.debug.core.model.ActorDebugTarget)
	 */
	protected void createRequest(OpendfDebugTarget target) throws CoreException {
		int flag = 0;
		if (isAccess()) {
			flag = flag | 1;
		}
		if (isModification()) {
			flag = flag | 2;
		}
		target.sendCommand("watch " + getFunctionName() + "::" + getVariableName() + " " + flag);
	}

	/**
	 * @see example.debug.core.breakpoints.ActorLineBreakpoint#clearRequest(example.debug.core.model.ActorDebugTarget)
	 */
	protected void clearRequest(OpendfDebugTarget target) throws CoreException {
		target.sendCommand("watch " + getFunctionName() + "::" + getVariableName() + " " + 0);
	}

	/**
	 * @see example.debug.core.model.IActorEventListener#handleEvent(java.lang.String)
	 */
	public void handleEvent(String event) {
		if (event.startsWith("suspended watch")) {
			handleHit(event);
		}
	}

	/**
	 * Determines if this breakpoint was hit and notifies the thread.
	 * 
	 * @param event
	 *          breakpoint event
	 */
	private void handleHit(String event) {
		String[] strings = event.split(" ");
		if (strings.length == 4) {
			String fv = strings[3];
			int j = fv.indexOf("::");
			if (j > 0) {
				String fcn = fv.substring(0, j);
				String var = fv.substring(j + 2);
				try {
					if (getVariableName().equals(var) && getFunctionName().equals(fcn)) {
						setSuspendType(strings[2]);
						notifyThread();
					}
				} catch (CoreException e) {
				}
			}
		}
	}
}
