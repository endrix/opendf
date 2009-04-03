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
import net.sf.opendf.eclipse.plugin.debug.model.ActorThread;
import net.sf.opendf.eclipse.plugin.debug.model.IOpendfEventListener;
import net.sf.opendf.eclipse.plugin.debug.model.OpendfDebugTarget;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.LineBreakpoint;

/**
 * Actor line breakpoint
 * 
 * @author Rob Esser
 * @version 3rd April 2009
 */
public class ActorLineBreakpoint extends LineBreakpoint implements IOpendfEventListener {
	
	// target currently installed in
	private OpendfDebugTarget fTarget;
	
	/**
	 * Default constructor is required for the breakpoint manager
	 * to re-create persisted breakpoints. After instantiating a breakpoint,
	 * the <code>setMarker(...)</code> method is called to restore
	 * this breakpoint's attributes.
	 */
	public ActorLineBreakpoint() {
	}
	
	/**
	 * Constructs a line breakpoint on the given resource at the given
	 * line number. The line number is 1-based (i.e. the first line of a
	 * file is line number 1). The Actor VM uses 0-based line numbers,
	 * so this line number translation is done at breakpoint install time.
	 * 
	 * @param resource file on which to set the breakpoint
	 * @param lineNumber 1-based line number of the breakpoint
	 * @throws CoreException if unable to create the breakpoint
	 */
	public ActorLineBreakpoint(final IResource resource, final int lineNumber) throws CoreException {
		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				IMarker marker = resource.createMarker(OpendfConstants.ACTOR_BREAKPOINT_MARKER);
				setMarker(marker);
				marker.setAttribute(IBreakpoint.ENABLED, Boolean.TRUE);
				marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
				marker.setAttribute(IBreakpoint.ID, getModelIdentifier());
				marker.setAttribute(IMarker.MESSAGE, "Line Breakpoint: " + resource.getName() + " [line: " + lineNumber + "]");
			}
		};
		run(getMarkerRule(resource), runnable);
	}
	
	/**
	 * @see org.eclipse.debug.core.model.IBreakpoint#getModelIdentifier()
	 */
	public String getModelIdentifier() {
		return OpendfConstants.OPENDF_DEBUG_MODEL;
	}
	
	/**
	 * Returns whether this breakpoint is a run-to-line breakpoint
	 * 
	 * @return whether this breakpoint is a run-to-line breakpoint
	 */
	public boolean isRunToLineBreakpoint() {
		return false;
	}
    
    /**
     * Installs this breakpoint in the given interprettor.
     * Registeres this breakpoint as an event listener in the
     * given target and creates the breakpoint specific request.
     * 
     * @param target Actor interpreter
     * @throws CoreException if installation fails
     */
    public void install(OpendfDebugTarget target) throws CoreException {
    	fTarget = target;
    	target.addEventListener(this);
    	createRequest(target);
    }
    
    /**
     * Create the breakpoint specific request in the target. Subclasses
     * should override.
     * 
     * @param target Actor interpreter
     * @throws CoreException if request creation fails
     */
    protected void createRequest(OpendfDebugTarget target) throws CoreException {
    	target.sendCommand("set " + (getLineNumber() - 1));
    }
    
    /**
     * Removes this breakpoint's event request from the target. Subclasses
     * should override.
     * 
     * @param target Actor interpreter
     * @throws CoreException if clearing the request fails
     */
    protected void clearRequest(OpendfDebugTarget target) throws CoreException {
    	target.sendCommand("clear " + (getLineNumber() - 1));
    }
    
    /**
     * Removes this breakpoint from the given interpreter.
     * Removes this breakpoint as an event listener and clears
     * the request for the interpreter.
     * 
     * @param target Actor interpreter
     * @throws CoreException if removal fails
     */
    public void remove(OpendfDebugTarget target) throws CoreException {
    	target.removeEventListener(this);
    	clearRequest(target);
    	fTarget = null;
    	
    }
    
    /**
     * Returns the target this breakpoint is installed in or <code>null</code>.
     * 
     * @return the target this breakpoint is installed in or <code>null</code>
     */
    protected OpendfDebugTarget getDebugTarget() {
    	return fTarget;
    }
    
    /**
     * Notify's the Actor interpreter that this breakpoint has been hit.
     */
    protected void notifyThread() {
    	if (fTarget != null) {
			try {
				IThread[] threads = fTarget.getThreads();
				if (threads.length == 1) {
	    			ActorThread thread = (ActorThread)threads[0];
	    			thread.suspendedBy(this);
	    		}
			} catch (DebugException e) {
			}    		
    	}
    }

	/**
     * Determines if this breakpoint was hit and notifies the thread.
     * 
     * @param event breakpoint event
     */
    private void handleHit(String event) {
    	int lastSpace = event.lastIndexOf(' ');
    	if (lastSpace > 0) {
    		String line = event.substring(lastSpace + 1);
    		int lineNumber = Integer.parseInt(line);
    		// breakpoints event line numbers are 0 based, model objects are 1 based
    		lineNumber++;
    		try {
				if (getLineNumber() == lineNumber) {
					notifyThread();
				}
    		} catch (CoreException e) {
    		}
    	}
    }

  	/**
	 * 
	 * Subclasses should override to handle their breakpoint specific event.
	 * 
	 * @see example.debug.core.model.IActorEventListener#handleEvent(java.lang.String)
	 */
	public void handleEvent(String event) {
		if (event.startsWith("suspended breakpoint")) {
			handleHit(event);
		}
	}
      
	@Override
	public void handleResumedEvent(String compName, String event) {
		System.out.println("ActorLineBreakpoint.handleResumedEvent");
	}

	@Override
	public void handleStartedEvent() {
		System.out.println("ActorLineBreakpoint.handleStartedEvent");
	}

	@Override
	public void handleSuspendedEvent(String compName, String event) {
		System.out.println("ActorLineBreakpoint.handleSuspendedEvent");
	}

	@Override
	public void handleTerminatedEvent() {
		System.out.println("ActorLineBreakpoint.handleTerminatedEvent");
	}		
}
