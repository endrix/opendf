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

package net.sf.opendf.eclipse.plugin.editors.outline;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.w3c.dom.Node;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.swt.widgets.Display;

public abstract class OpendfContentOutlinePage extends ContentOutlinePage implements Runnable, ITreeViewerListener
{
	private ITextEditor editor;
	private IEditorInput input;
  private long stamp;
	
	public OpendfContentOutlinePage( ITextEditor ed )
	{
		super();
    stamp = -1;
		editor = ed;
 	}	
  
	public void setInput( Object obj )
	{
    // Must update the outline content when the editor input changes
    stamp = -1;
		input = (IEditorInput) obj;
	}

  public long getStamp()
  {
    return stamp;
  }
  
  // This method creates a content outline provider specialized for the particular document type
	public abstract OpendfContentOutlineProvider createContentOutlineProvider( IDocumentProvider provider );
  
  OpendfContentOutlineProvider contentOutlineProvider;
  
  public void treeCollapsed(TreeExpansionEvent event ) 
  {
    Object obj = event.getElement();
    
    if( obj instanceof OpendfContentNode )
    {
      ( (OpendfContentNode) obj).setExpanded( false );
    }
  }

  public void treeExpanded(TreeExpansionEvent event) 
  {
    Object obj = event.getElement();
    
    if( obj instanceof OpendfContentNode )
    {
      ( (OpendfContentNode) obj).setExpanded( true );
    }
  }

	public void createControl( Composite parent )
	{
		super.createControl(parent);
    
		TreeViewer viewer = getTreeViewer();
    
    if( contentOutlineProvider == null )
      contentOutlineProvider = createContentOutlineProvider( editor.getDocumentProvider() );
    
		viewer.setContentProvider( contentOutlineProvider );
		
    OpendfOutlineLabelProvider outlineLabelProvider = new OpendfOutlineLabelProvider();
		viewer.setLabelProvider( outlineLabelProvider );
		
		viewer.addSelectionChangedListener( this );
    viewer.addTreeListener( this );
    
		//control is created after input is set
		if( input != null )
			viewer.setInput( input );

	}

	public void selectionChanged( SelectionChangedEvent event )
	{
		super.selectionChanged( event );

		ISelection selection = event.getSelection();
		
		if (selection.isEmpty())
      editor.resetHighlightRange();
		else
		{
			OpendfContentNode node = (OpendfContentNode) ((IStructuredSelection) selection).getFirstElement();
			try
			{
				int start  = contentOutlineProvider.getLineOffset( node.getStart() );
				int length = contentOutlineProvider.getLineOffset( node.getEnd() )
				             + contentOutlineProvider.getLineLength( node.getEnd() )
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

  private boolean done;
  private synchronized void setDone( boolean v )
  {
    done = v;
  }
  private synchronized boolean getDone()
  {
    return done;
  }
  
  // This is called from a thread other than the main UI thread, and
  // the update must be done in the main UI thread
	public void outlineDocument( Node document, long s )
	{
    // Pass the document to the worker thread that will update the outline
    documentToOutline = document;
    
    // Start the worker and poll done flag
    setDone( false );
    Display.getDefault().asyncExec( this );

    while( ! getDone() )
    {
      // This will stall the parsing thread until the outliner has redrawn
      try
      {
        Thread.sleep( 500 );
      }
      catch( InterruptedException e )
      {
        // I think we can ignore this one
      }
     }
    
    stamp = s;

	}

  // Used to pass argument between threads
  private Node documentToOutline;  
  
  public void run()
  {
    contentOutlineProvider.updateDocument( documentToOutline );

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
        contentOutlineProvider.setExpandedState( viewer );
        control.setRedraw( true );
      }
    }
    setDone( true );
  }

}

