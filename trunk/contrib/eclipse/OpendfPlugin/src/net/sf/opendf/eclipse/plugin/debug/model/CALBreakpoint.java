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
package net.sf.opendf.eclipse.plugin.debug.model;

import net.sf.opendf.eclipse.plugin.OpendfConstants;
import net.sf.opendf.eclipse.plugin.editors.CALEditor;

import org.eclipse.core.internal.resources.MarkerManager;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.LineBreakpoint;

/**
 * A CAL breakpoint
 * 
 * @author Rob Esser
 * @version 3rd April 2009
 *
 */
public class CALBreakpoint extends LineBreakpoint {
	private final CALEditor calEditor;

	public CALBreakpoint(final CALEditor calEditor, final int lineNumber) throws CoreException {
		this.calEditor = calEditor;

		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {

			public void run(IProgressMonitor monitor) throws CoreException {
				IMarker marker = new MarkerManager(calEditor.getResource()).createDebugMarker(calEditor.getTitle(), lineNumber);
				setMarker(marker);
				addMe();
			}
		};
		run(getMarkerRule(calEditor.getResource()), runnable);
	}

	public String getModelIdentifier() {
		return OpendfConstants.CAL_DEBUG_MODEL;
	}

	private void addMe() throws CoreException {
		DebugPlugin.getDefault().getBreakpointManager().addBreakpoint(this);
	}

	public CALEditor getCALEditor() {
		return calEditor;
	}
}

