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

package net.sf.caltrop.eclipse.plugin.editors.outline;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.w3c.dom.*;
import org.eclipse.jface.text.BadLocationException;

public class CALContentOutlinePage extends ContentOutlinePage
{
	private ITextEditor editor;
	private IEditorInput input;
	
	public CALContentOutlinePage( ITextEditor ed )
	{
		super();
		editor = ed;
	}	

	public void setInput( Object obj )
	{
		input = (IEditorInput) obj;
	}
	
	private CALOutlineContentProvider outlineContentProvider;
	private CALOutlineLabelProvider outlineLabelProvider;

	public void createControl( Composite parent )
	{
		super.createControl(parent);

		TreeViewer viewer = getTreeViewer();
		outlineContentProvider = new CALOutlineContentProvider( editor.getDocumentProvider() );
		viewer.setContentProvider( outlineContentProvider );
		
		outlineLabelProvider = new CALOutlineLabelProvider();
		viewer.setLabelProvider( outlineLabelProvider );
		
		viewer.addSelectionChangedListener( this );

		//control is created after input is set
		if( input != null )
			viewer.setInput( input );
	}

	public void selectionChanged( SelectionChangedEvent event )
	{
		super.selectionChanged( event );

		ISelection selection = event.getSelection();
		
		if (selection.isEmpty()) editor.resetHighlightRange();
		else
		{
			CALContentNode node = (CALContentNode) ((IStructuredSelection) selection).getFirstElement();
			try
			{
				int start  = outlineContentProvider.getLineOffset( node.getStart() );
				int length = outlineContentProvider.getLineOffset( node.getEnd() )
				             + outlineContentProvider.getLineLength( node.getEnd() )
				             - start - 1;
				editor.setHighlightRange( start, length, true);
			}
			catch( IllegalArgumentException x )
			{
				editor.resetHighlightRange();
			}
			catch( BadLocationException x )
			{
				editor.resetHighlightRange();
			}
		}
	}

	public void update( Document actor )
	{
		outlineContentProvider.setRoot(  CALContentNode.contentTree( actor.getDocumentElement() ) );
		
		// set the input so that the parser can be called
		// update the tree viewer state
		TreeViewer viewer = getTreeViewer();

		if( viewer != null )
		{
			Control control = viewer.getControl();
			if( control != null && !control.isDisposed() )
			{
				control.setRedraw( false );
				viewer.setInput( input );
				viewer.expandToLevel( TreeViewer.ALL_LEVELS );
				control.setRedraw( true );
			}
		}
	}
}

