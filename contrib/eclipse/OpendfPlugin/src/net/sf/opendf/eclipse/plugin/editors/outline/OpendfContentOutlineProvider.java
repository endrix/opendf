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

import net.sf.opendf.eclipse.plugin.OpendfPlugin;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

// Note: unfortunately, jface also has a Document class!
import org.w3c.dom.Document;

public abstract class OpendfContentOutlineProvider implements ITreeContentProvider
{
  private OpendfContentNode root;
  private IEditorInput input;
	private IDocumentProvider documentProvider;

	protected final static String TAG_POSITIONS = "__tag_positions";
	protected IPositionUpdater positionUpdater = new DefaultPositionUpdater( TAG_POSITIONS );

  public abstract void updateDocument( Document document );
  
	public OpendfContentOutlineProvider( IDocumentProvider provider )
	{
		super();
		documentProvider = provider;
	}

	protected void setRoot( OpendfContentNode e )
	{    
    // Copy over the expanded states
    e.matchAndCopyExpanded( root );
    root = e;
	}
	
  public void setExpandedState( TreeViewer viewer )
  {
    if( root != null )
      root.setExpandedState( viewer );
  }
 
	public Object[] getChildren( Object obj )
	{	
		OpendfContentNode[] children;
		
		OpendfContentNode element = (obj == input) ? root : (OpendfContentNode) obj;
		
		if( element == null || (children = element.getChildren() ) == null ) return new Object[0];
		
		return children;
	}

	public Object getParent( Object obj )
	{
		if( obj instanceof OpendfContentNode )
			return ( (OpendfContentNode)obj ).getParent();
		
		return null;
	}

	public boolean hasChildren( Object obj )
	{
		if( obj == input ) return true;
		
		return ((OpendfContentNode)obj ).hasChildren();
	}

	public Object[] getElements( Object obj )
	{
		if( root == null  )
		{ 
			return new Object[0];
		}
		
		Object[] array = new Object[1];
		array[0] = root;
		return array;
	}

	public void dispose()
	{
	}

	public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
	{
		if( oldInput != null )
		{
			IDocument document = documentProvider.getDocument( oldInput );
			if (document != null)
			{
				try
				{
					document.removePositionCategory( TAG_POSITIONS );
				}
				catch( BadPositionCategoryException e )
				{
          OpendfPlugin.logErrorMessage( "Failed to remove position tags", e );
				}
				document.removePositionUpdater( positionUpdater );
			}
		}
		
		input = (IEditorInput) newInput;

		if( newInput != null )
		{
			IDocument document = documentProvider.getDocument( newInput );
			if( document != null )
			{
				document.addPositionCategory( TAG_POSITIONS );
				document.addPositionUpdater( positionUpdater );

			}
		}
	}
	
	public int getLineOffset( int line ) throws BadLocationException
	{
		return documentProvider.getDocument( input ).getLineOffset( line-1 );
	}
	
	public int getLineLength( int line ) throws BadLocationException
	{
		return documentProvider.getDocument( input ).getLineLength( line-1 );
	}
}
