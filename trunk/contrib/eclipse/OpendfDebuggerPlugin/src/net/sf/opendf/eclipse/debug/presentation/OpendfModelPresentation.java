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
package net.sf.opendf.eclipse.debug.presentation;

import net.sf.opendf.eclipse.debug.breakpoints.ActorLineBreakpoint;
import net.sf.opendf.eclipse.debug.breakpoints.ActorWatchpoint;
import net.sf.opendf.eclipse.debug.model.ActorStackFrame;
import net.sf.opendf.eclipse.debug.model.ActorThread;
import net.sf.opendf.eclipse.debug.model.OpendfDebugTarget;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Renders debug elements
 * 
 * @author Rob Esser
 * @version 13th April 2009
 */
public class OpendfModelPresentation extends LabelProvider implements
		IDebugModelPresentation {

	/**
	 * 
	 * @see org.eclipse.debug.ui.IDebugModelPresentation#computeDetail(org.eclipse.debug.core.model.IValue,
	 *      org.eclipse.debug.ui.IValueDetailListener)
	 */
	public void computeDetail(IValue value, IValueDetailListener listener) {
		String detail = "";
		try {
			detail = value.getValueString();
		} catch (DebugException e) {
		}
		listener.detailComputed(value, detail);
	}

	@Override
	public String getEditorId(IEditorInput input, Object element) {
		// returns the editor id from the editor descriptor that could be
		// associated with the given element if it can be translated to an IFile

		IFile file = getIFile(element);
		if (file != null) {
			try {
				IEditorDescriptor editor = IDE.getEditorDescriptor(file);
				return editor.getId();
			} catch (PartInitException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	@Override
	public IEditorInput getEditorInput(Object element) {
		IFile file = getIFile(element);
		if (file != null) {
			return new FileEditorInput(file);
		}

		return null;
	}

	/**
	 * Tries to adapt element to an IFile.
	 * 
	 * @param element
	 *            An element.
	 * @return an IFile if successful, or <code>null</code> otherwise.
	 */
	private IFile getIFile(Object element) {
		IFile file = null;
		if (element instanceof IAdaptable) {
			file = (IFile) ((IAdaptable) element).getAdapter(IFile.class);
		}

		if (file == null) {
			if (element instanceof IFile) {
				file = (IFile) element;
			} else if (element instanceof ILineBreakpoint) {
				IMarker marker = ((ILineBreakpoint) element).getMarker();
				if (marker.getResource().getType() == IResource.FILE) {
					file = (IFile) marker.getResource();
				}
			}
		}

		return file;
	}

	/**
	 * Returns a label for the given stack frame
	 * 
	 * @param frame
	 *            a stack frame
	 * @return a label for the given stack frame
	 */
	private String getStackFrameText(ActorStackFrame frame) {
		try {
			return frame.getName() + " (line: " + frame.getLineNumber() + ")";
		} catch (DebugException e) {
		}
		return null;

	}

	/**
	 * Returns a label for the given debug target
	 * 
	 * @param target
	 *            debug target
	 * @return a label for the given debug target
	 */
	private String getTargetText(OpendfDebugTarget target) {
		if (target.isTerminated()) {
			return "<terminated>";
		} else {
			return "Opendf";
		}
	}

	/**
	 * 
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		if (element instanceof OpendfDebugTarget) {
			return getTargetText((OpendfDebugTarget) element);
		} else if (element instanceof ActorThread) {
			return getThreadText((ActorThread) element);
		} else if (element instanceof ActorStackFrame) {
			return getStackFrameText((ActorStackFrame) element);
		} else if (element instanceof ActorWatchpoint) {
			return getWatchpointText((ActorWatchpoint) element);
		}
		return null;
	}

	/**
	 * Returns a label for the given thread
	 * 
	 * @param thread
	 *            a thread
	 * @return a label for the given thread
	 */
	private String getThreadText(ActorThread thread) {
		String label = thread.getName();
		if (thread.isStepping()) {
			label += " (stepping)";
		} else if (thread.isSuspended()) {
			IBreakpoint[] breakpoints = thread.getBreakpoints();
			if (breakpoints.length == 0) {
				label += " (suspended)";
			} else {
				IBreakpoint breakpoint = breakpoints[0]; // there can only be
				// one in opendf
				if (breakpoint instanceof ActorLineBreakpoint) {
					ActorLineBreakpoint pdaBreakpoint = (ActorLineBreakpoint) breakpoint;
					if (pdaBreakpoint instanceof ActorWatchpoint) {
						try {
							ActorWatchpoint watchpoint = (ActorWatchpoint) pdaBreakpoint;
							label += " (watchpoint: "
									+ watchpoint.getSuspendType() + " "
									+ watchpoint.getVariableName() + ")";
						} catch (CoreException e) {
						}
					} else if (pdaBreakpoint.isRunToLineBreakpoint()) {
						label += " (run to line)";
					} else {
						label += " (suspended at line breakpoint)";
					}
				}
			}
		} else if (thread.isTerminated()) {
			label = "<terminated> " + label;
		}
		return label;
	}

	/**
	 * Returns a label for the given watchpoint.
	 * 
	 * @param watchpoint
	 * @return a label for the given watchpoint
	 */
	private String getWatchpointText(ActorWatchpoint watchpoint) {
		try {
			String label = watchpoint.getVariableName() + " ("
					+ watchpoint.getActorName() + ")";
			if (watchpoint.isAccess()) {
				label += " [read]";
			}
			if (watchpoint.isModification()) {
				label += " [write]";
			}
			return label;
		} catch (CoreException e) {
			return null;
		}
	}

	/**
	 * 
	 * @see org.eclipse.debug.ui.IDebugModelPresentation#setAttribute(java.lang.String,
	 *      java.lang.Object)
	 */
	public void setAttribute(String attribute, Object value) {
	}
}
