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

import org.eclipse.jface.text.*;
import org.eclipse.core.resources.IFile;
import net.sf.caltrop.eclipse.plugin.editors.outline.*;
import net.sf.caltrop.eclipse.plugin.CALPlugin;

public class CALDocumentListener implements IDocumentListener, Runnable {

	private IDocument document;
	private long stamp;
	private Thread parser;
	private boolean die;
	private CALDisplayManager displayManager;
	private CALContentOutlinePage outlinePage;
	private String kind;
	
	public CALDocumentListener( IFile file, IDocument doc, String k )
	{
		document = doc;
		kind = k;
		die = false;

		displayManager = new CALDisplayManager( file, document, kind );
		
        // Start looking for document changes
		parser = new Thread( this );
		parser.start();
	}
	
	public synchronized void setOutliner( CALContentOutlinePage op )
	{
		outlinePage = op;
	}
	
	private synchronized CALContentOutlinePage getOutliner( )
	{
		return outlinePage;
	}
	
	// Communication between running threads is through a single variable
	private synchronized long getStamp()
	{
		return stamp;
	}
	
	private synchronized void setStamp( DocumentEvent e )
	{
		stamp = e.getModificationStamp();
	}

	// The die flag will tell the parser thread to return.
	private synchronized boolean getDie()
	{
		return die;
	}
	
	public synchronized void kill( )
	{
		die = true;
	}
	
	public void documentAboutToBeChanged(DocumentEvent event)
	{
	  // Nothing to do. Method needed to complete the interface
	}

	public void documentChanged(DocumentEvent event)
	{
	  if( document != event.getDocument() )
	  {
		  CALPlugin.printLog("Whoops - who changed the document??");
	  }
	  
	  // Record the latest mod stamp. The stamps are an incrementing series.
	  setStamp( event );
	}
	
	// Separate parser thread
	public void run()
	{ long stamp;
	  long lastStamp = -1;    // Ensures parse on first doc change
	  
	  while( true )
	  {  
		  // Terminate the parser in an orderly fashion when the document is closed
		  if( getDie() )
		  {
			  return;
		  }

		  // Poll the modification stamp
		  stamp = getStamp();
	  
	      // 500mS delay to reduce thrashing while the user types
		  try { Thread.sleep( 500 ); } catch ( InterruptedException e ) { }
			
		  // Don't parse if there were additional changes in the sleep interval
		  if( stamp != getStamp() ) continue;

		  // If there was no change in stamp since the last successful parsing
		  // of the document don't bother to parSe again. (Success meaning that
		  // the document and the parsed version remained in sync, not that
		  // the doc was syntactically error-free).
		  if( stamp == lastStamp ) continue;
		  displayManager.parse( );
 
          // If there were additional changes during parsing then the parsed version and
          // the current document are out of sync and the parse is invalid.
		  // Also, wait until there is an outliner before posting the first result
		  if( stamp != getStamp() || getOutliner() == null ) continue;
		  
		  // Parse was valid - update the error markers
		  lastStamp = stamp;
		  displayManager.update( getOutliner() );
	   }
	}
	
}
