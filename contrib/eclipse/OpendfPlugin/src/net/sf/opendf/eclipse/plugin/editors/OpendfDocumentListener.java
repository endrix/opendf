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

package net.sf.opendf.eclipse.plugin.editors;

import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.DocumentEvent;
import org.w3c.dom.Node;

import net.sf.opendf.eclipse.plugin.editors.outline.*;
import net.sf.opendf.eclipse.plugin.editors.checkers.*;
import net.sf.opendf.util.source.*;

public class OpendfDocumentListener implements IDocumentListener, Runnable
{
	private OpendfDocumentChecker documentChecker;
  
	private long    stamp;
	private boolean terminated;
	
  private OpendfContentOutlinePage outlinePage;

	public OpendfDocumentListener( OpendfDocumentChecker checker )
	{
		documentChecker = checker;
    outlinePage = null;
    
    // Start looking for document changes
    terminated = false;
    stamp = 0;
    
    Thread thread = new Thread( this );
    thread.start();
	}

  public void setOutlinePage( OpendfContentOutlinePage outline )
  {
    outlinePage = outline;
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
	private synchronized boolean isTerminated()
	{
		return terminated;
	}
	
	public synchronized void kill( )
	{
    terminated = true;
	}
	
	public void documentAboutToBeChanged( DocumentEvent event )
	{
	  // Nothing to do. Method needed to complete the interface
	}

	public void documentChanged( DocumentEvent event )
	{
	  // Record the latest mod stamp. The stamps are an incrementing series.
	  setStamp( event );
	}
	
	// Separate parser thread
	public void run()
	{
    long parsingStamp   = -1;    // Ensures parse on first doc change
    Node document = null;
    
	  while( true )
	  {  
		  // Terminate the parser in an orderly fashion when the document is closed
		  if( isTerminated() )
		  {
			  return;
		  }

      // Send altered documents to the outliner
      if( outlinePage != null && document != null && outlinePage.getStamp() != parsingStamp )
      {
        outlinePage.outlineDocument( document, parsingStamp );
      }
 
      // Poll the document modification stamp
      long currentStamp = getStamp();

	    // 500mS delay to reduce thrashing while the user types
		  try
      {
        Thread.sleep( 500 ); 
      } 
      catch ( InterruptedException e )
      { 
        // We can ignore this one
      }

      // If document has changed during the sleep interval do not parse
      if( getStamp() != currentStamp ) continue;
      
		  // If the parsing is up to date, no parsing required
		  if( parsingStamp == currentStamp ) continue;

      try
      {        
        document = documentChecker.parseDocument();
        
        if( currentStamp == getStamp() )
        {
          // No syntax errors were found, document did not change during parse
          parsingStamp = currentStamp;
          documentChecker.checkSemanticsAndReport( document );
        }
        else
        {
          document = null;
        }
      }
      catch( MultiErrorException syntaxErrors )
      {
        // There were syntax errors
        document = null;
        
        // If the document did not change post the syntax errors
        if( currentStamp == getStamp() )
        {
          documentChecker.postSyntaxErrors( syntaxErrors );
          parsingStamp = currentStamp;
        }
      }
	  }
	}
	
}
