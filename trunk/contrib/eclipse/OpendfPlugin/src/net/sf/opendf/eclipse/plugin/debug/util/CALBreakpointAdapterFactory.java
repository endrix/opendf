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
package net.sf.opendf.eclipse.plugin.debug.util;

import net.sf.opendf.eclipse.plugin.OpendfConstants;
import net.sf.opendf.eclipse.plugin.editors.CALEditor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Supporting breakpoints
 * 
 * @author Rob Esser
 * @version 3rd April 2009
 *
 */
public class CALBreakpointAdapterFactory implements IAdapterFactory {

	@SuppressWarnings("unchecked")
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (!(adaptableObject instanceof CALEditor)) {
			return null;
		}
		CALEditor CALEditor = (CALEditor) adaptableObject;
		if (CALEditor != null) {
			return new CALToggleBreakpointsTarget();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public Class[] getAdapterList() {
		return new Class[] { IToggleBreakpointsTarget.class };
	}

	public static class CALToggleBreakpointsTarget implements IToggleBreakpointsTarget {

		public boolean canToggleLineBreakpoints(IWorkbenchPart part, ISelection selection) {
			return getCALEditor(part) != null;
		}

		public boolean canToggleMethodBreakpoints(IWorkbenchPart part, ISelection selection) {
			return false;
		}

		public boolean canToggleWatchpoints(IWorkbenchPart part, ISelection selection) {
			return false;
		}

		public void toggleLineBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
			CALEditor CALEditor = getCALEditor(part);
			ITextSelection txtSelection = (ITextSelection) selection;
			if (CALEditor == null || txtSelection == null) {
				return;
			}
			IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(OpendfConstants.CAL_DEBUG_MODEL);

			int lnr = txtSelection.getStartLine() + 1;
			if (breakpoints != null) {
				for (IBreakpoint bpnt : breakpoints) {
//					if (bpnt instanceof ILineBreakpoint && lnr == ((ILineBreakpoint) bpnt).getLineNumber() && bpnt instanceof CALBreakpoint && ((CALBreakpoint) bpnt).getCALEditor() == CALEditor) {
//						bpnt.delete();
//						return;
//					}
				}
			}
//			DebugPlugin.getDefault().getBreakpointManager().addBreakpoint(new CALBreakpoint(CALEditor, lnr));
		}

		private CALEditor getCALEditor(IWorkbenchPart part) {
			if (part instanceof CALEditor) {
				return (CALEditor) part;
			}
			return null;
		}

		public void toggleMethodBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
		}

		public void toggleWatchpoints(IWorkbenchPart part, ISelection selection) throws CoreException {

		}
	}
}
