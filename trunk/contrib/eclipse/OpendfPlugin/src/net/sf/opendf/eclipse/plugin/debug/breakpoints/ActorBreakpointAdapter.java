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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTargetExtension;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Adapter to create breakpoints in Actor files.
 * 
 * @author Rob Esser
 * @version 3rd April 2009
 */
public class ActorBreakpointAdapter implements IToggleBreakpointsTargetExtension {
	
	/**
	 * 
	 * @see
	 * org.eclipse.debug.ui.actions.IToggleBreakpointsTarget#toggleLineBreakpoints
	 * (org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void toggleLineBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
		ITextEditor textEditor = getEditor(part);
		if (textEditor != null) {
			IResource resource = (IResource) textEditor.getEditorInput().getAdapter(IResource.class);
			ITextSelection textSelection = (ITextSelection) selection;
			int lineNumber = textSelection.getStartLine();
			IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(OpendfConstants.ID_OPENDF_DEBUG_MODEL);
			for (int i = 0; i < breakpoints.length; i++) {
				IBreakpoint breakpoint = breakpoints[i];
				if (breakpoint instanceof ILineBreakpoint && resource.equals(breakpoint.getMarker().getResource())) {
					if (((ILineBreakpoint) breakpoint).getLineNumber() == (lineNumber + 1)) {
						// remove
						breakpoint.delete();
						return;
					}
				}
			}
			// create line breakpoint (doc line numbers start at 0)
			ActorLineBreakpoint lineBreakpoint = new ActorLineBreakpoint(resource, lineNumber + 1);
			DebugPlugin.getDefault().getBreakpointManager().addBreakpoint(lineBreakpoint);
		}
	}

	/**
	 * 
	 * 
	 * @see
	 * org.eclipse.debug.ui.actions.IToggleBreakpointsTarget#canToggleLineBreakpoints
	 * (org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	public boolean canToggleLineBreakpoints(IWorkbenchPart part, ISelection selection) {
		return getEditor(part) != null;
	}

	/**
	 * Returns the editor being used to edit a Actor file, associated with the given
	 * part, or <code>null</code> if none.
	 * 
	 * @param part
	 *          workbench part
	 * @return the editor being used to edit a Actor file, associated with the given
	 *         part, or <code>null</code> if none
	 */
	private ITextEditor getEditor(IWorkbenchPart part) {
		if (part instanceof ITextEditor) {
			ITextEditor editorPart = (ITextEditor) part;
			IResource resource = (IResource) editorPart.getEditorInput().getAdapter(IResource.class);
			if (resource != null) {
				String extension = resource.getFileExtension();
				if (extension != null && extension.equals("cal")) {
					return editorPart;
				}
			}
		}
		return null;
	}

	/**
	 * 
	 * @see
	 * org.eclipse.debug.ui.actions.IToggleBreakpointsTarget#toggleMethodBreakpoints
	 * (org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void toggleMethodBreakpoints(IWorkbenchPart part, ISelection selection)
			throws CoreException {
	}

	/**
	 * 
	 * @seeorg.eclipse.debug.ui.actions.IToggleBreakpointsTarget#
	 * canToggleMethodBreakpoints(org.eclipse.ui.IWorkbenchPart,
	 * org.eclipse.jface.viewers.ISelection)
	 */
	public boolean canToggleMethodBreakpoints(IWorkbenchPart part, ISelection selection) {
		return false;
	}

	/**
	 * 
	 * @see org.eclipse.debug.ui.actions.IToggleBreakpointsTarget#toggleWatchpoints(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void toggleWatchpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
		String[] variableAndFunctionName = getVariableAndFunctionName(part, selection);
		if (variableAndFunctionName != null && part instanceof ITextEditor && selection instanceof ITextSelection) {
			ITextEditor editorPart = (ITextEditor) part;
			int lineNumber = ((ITextSelection) selection).getStartLine();
			IResource resource = (IResource) editorPart.getEditorInput().getAdapter(IResource.class);
			String var = variableAndFunctionName[0];
			String fcn = variableAndFunctionName[1];
			// look for existing watchpoint to delete
			IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(OpendfConstants.ID_OPENDF_DEBUG_MODEL);
			for (int i = 0; i < breakpoints.length; i++) {
				IBreakpoint breakpoint = breakpoints[i];
				if (breakpoint instanceof ActorWatchpoint && resource.equals(breakpoint.getMarker().getResource())) {
					ActorWatchpoint watchpoint = (ActorWatchpoint) breakpoint;
					String otherVar = watchpoint.getVariableName();
					String otherFcn = watchpoint.getFunctionName();
					if (otherVar.equals(var) && otherFcn.equals(fcn)) {
						breakpoint.delete();
						return;
					}
				}
			}
			// create watchpoint
			ActorWatchpoint watchpoint = new ActorWatchpoint(resource, lineNumber + 1, fcn, var, true, true);
			DebugPlugin.getDefault().getBreakpointManager().addBreakpoint(watchpoint);
		}
	}

	/**
	 * 
	 * @see
	 * org.eclipse.debug.ui.actions.IToggleBreakpointsTarget#canToggleWatchpoints
	 * (org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	public boolean canToggleWatchpoints(IWorkbenchPart part, ISelection selection) {
		return getVariableAndFunctionName(part, selection) != null;
	}

	/**
	 * Returns the variable and function names at the current line, or
	 * <code>null</code> if none.
	 * 
	 * @param part
	 *          text editor
	 * @param selection
	 *          text selection
	 * @return the variable and function names at the current line, or
	 *         <code>null</code> if none. The array has two elements, the first is
	 *         the variable name, the second is the function name.
	 */
	private String[] getVariableAndFunctionName(IWorkbenchPart part, ISelection selection) {
		ITextEditor editor = getEditor(part);
		if (editor != null && selection instanceof ITextSelection) {
			ITextSelection textSelection = (ITextSelection) selection;
			IDocumentProvider documentProvider = editor.getDocumentProvider();
			try {
				documentProvider.connect(this);
				IDocument document = documentProvider.getDocument(editor.getEditorInput());
				IRegion region = document.getLineInformationOfOffset(textSelection.getOffset());
				String string = document.get(region.getOffset(), region.getLength()).trim();
				if (string.startsWith("var ")) {
					String varName = string.substring(4).trim();
					String fcnName = getFunctionName(document, varName, document.getLineOfOffset(textSelection.getOffset()));
					return new String[] { varName, fcnName };
				}
			} catch (CoreException e) {
			} catch (BadLocationException e) {
			} finally {
				documentProvider.disconnect(this);
			}
		}
		return null;
	}

	/**
	 * Returns the name of the function containing the given variable defined at
	 * the given line number in the specified document.
	 * 
	 * @param document
	 *          Actor source file
	 * @param varName
	 *          variable name
	 * @param line
	 *          line number at which the variable is defined
	 * @return name of function defining the variable
	 */
	private String getFunctionName(IDocument document, String varName, int line) {
		// This is a simple guess at the function name - look for the labels
		// preceding the variable definition, and then see if there are any 'calls' to that
		// label. If none, assume the variable is in the "_main_" function
		String source = document.get();
		int lineIndex = line - 1;
		while (lineIndex >= 0) {
			try {
				IRegion information = document.getLineInformation(lineIndex);
				String lineText = document.get(information.getOffset(), information.getLength());
				if (lineText.startsWith(":")) {
					String label = lineText.substring(1);
					if (source.indexOf("call " + label) >= 0) {
						return label;
					}
				}
				lineIndex--;
			} catch (BadLocationException e) {
			}
		}
		return "_main_";
	}

	/**
	 * 
	 * @see org.eclipse.debug.ui.actions.IToggleBreakpointsTargetExtension#
	 * toggleBreakpoints(org.eclipse.ui.IWorkbenchPart,
	 * org.eclipse.jface.viewers.ISelection)
	 */
	public void toggleBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
		if (canToggleWatchpoints(part, selection)) {
			toggleWatchpoints(part, selection);
		} else {
			toggleLineBreakpoints(part, selection);
		}
	}

	/**
	 * 
	 * @see org.eclipse.debug.ui.actions.IToggleBreakpointsTargetExtension#canToggleBreakpoints(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	public boolean canToggleBreakpoints(IWorkbenchPart part, ISelection selection) {
		return canToggleLineBreakpoints(part, selection) || canToggleWatchpoints(part, selection);
	}
}
