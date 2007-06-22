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

package net.sf.caltrop.eclipse.plugin.editors;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.*;

import net.sf.caltrop.eclipse.plugin.editors.outline.*;

public class CALEditor extends TextEditor
{
	private CALColorManager colorManager;
    private CALDocumentProvider documentProvider;
    private CALDocumentListener listener;
	private CALContentOutlinePage outlinePage;

	public CALEditor()
	{
		super();
		colorManager = new CALColorManager();
		setSourceViewerConfiguration( new CALConfiguration( colorManager ) );
		
		documentProvider = new CALDocumentProvider( );
		setDocumentProvider( documentProvider );
		
		listener = null;
	}
	
	// Use this callback to signal the parser thread to terminate (return)
	public void dispose()
	{
		if( listener != null ) listener.kill();
		colorManager.dispose();
		super.dispose();
	}

	// Notifies us when there is an association to an input file
	protected void doSetInput( IEditorInput input ) throws CoreException
	{

		try
		{
			super.doSetInput( input );
			
			String kind = "Actor";
			if( input.getName().contains(".nl") ) kind = "Network";
		
			// Now we can start a listener since we have the associated input
			IDocument document = documentProvider.getDocument( input );
			listener = new CALDocumentListener( ((IFileEditorInput) input).getFile(), document, kind );
			if( outlinePage != null )
				listener.setOutliner( outlinePage );
			
			document.addDocumentListener( listener );
		}
		catch( ClassCastException e )
		{
			// No parsing support available when editing outside a project
		}
	}

	public Object getAdapter( Class required )
	{
		
		if( IContentOutlinePage.class.equals( required ) )
		{
			if( outlinePage == null )
			{
				outlinePage = new CALContentOutlinePage( this );
				if( getEditorInput() != null )
					outlinePage.setInput( getEditorInput() );

				if( listener != null )
					listener.setOutliner( outlinePage );
			}
			return outlinePage;
		}

		return super.getAdapter( required );
		
	}
}
