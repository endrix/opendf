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


import java.util.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import net.sf.caltrop.cal.interpreter.util.SourceReader;
import net.sf.caltrop.cal.parser.*;
import net.sf.caltrop.util.*;
import net.sf.caltrop.eclipse.plugin.CALPlugin;
import org.eclipse.jface.text.*;
import org.eclipse.ui.texteditor.MarkerUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import java.io.*;
import net.sf.caltrop.eclipse.plugin.editors.outline.*;
import org.eclipse.swt.widgets.Display;
import javax.xml.transform.*;

public class CALDisplayManager implements Runnable
{
  public static final String PROBLEM_MARKER_ID = IMarker.PROBLEM;

  private IDocument document;
  private IFile file;
  
  private static Transformer checker = null;
  private static boolean transformerFailed = false;
  
  public CALDisplayManager( IFile f, IDocument d )
  {
	  document = d;
	  file = f;
  }
  
  private boolean parseSucceeded;
  private int errorLine;
  private int errorColumn;
  private String errorMessage;
  private Document actor;

  // Reading or writing the actor is synchronized because
  // the outliner update happens in a different thread.
  private synchronized Document getActor()
  {
	  return actor;
  }
  
  private synchronized void setActor( Document dom )
  {
	  actor = dom;
  }
  
  public void parse( )
  {
	  parseSucceeded = false;
	  errorLine = 0;
	  errorMessage = null;
  
  	try
  	{
  		Document dom = SourceReader.parseActor( document.get() );
  		setActor( dom );
  		parseSucceeded = true;
  	}
    catch (ParserErrorException pe)
    {
        GenericError error = pe.getErrors().get(0);
        // A bit of a hack, but the parser does not give line number
        // for EOF.
        if (error.getReason().indexOf("\"EOF\"") >= 0)
        {
  	  		errorMessage = "Incomplete actor";
  	  	    errorLine = document.getNumberOfLines();
  	  	    errorColumn = 1;
        }
        else
        {
            errorMessage = error.getReason() + error.getLineNumber() + " " + error.getColumnNumber();
            errorLine = error.getLineNumber();
            errorColumn = error.getColumnNumber();
        }
    }
    catch (Exception e)
    {
        StringWriter sw = new StringWriter();
        e.printStackTrace( new PrintWriter( sw ) );
        errorMessage = "Unknown parsing problem (check character constants?)" + e.toString();
    }
  }
  
  private boolean updateDone;
  
  private synchronized void setDone()
  {
	  updateDone = true;
  }
  
  private synchronized void clearDone()
  {
	  updateDone = false;
  }
  private synchronized boolean testDone()
  {
	  return updateDone;
  }
  
  public void update( CALContentOutlinePage op )
  {	 	  
	  // Remove previous markers
	  try
	  {
		  file.deleteMarkers( null, true, IResource.DEPTH_ZERO );
	  }
	  catch( CoreException e )
	  { 
		// Unexpected condition
	  }

	  if( ! parseSucceeded )
	  {
	    // Add a problem marker
	    try
	    {		  
		  Map<String, Object> map = new HashMap<String, Object>();
		  MarkerUtilities.setLineNumber( map, errorLine );
		  map.put( IMarker.SEVERITY, new Integer( IMarker.SEVERITY_ERROR ) );
		  MarkerUtilities.setMessage( map, errorMessage );
		  
		  if( errorLine > 0 )
		  {
			  try
			  {
				  IRegion r = document.getLineInformation( errorLine - 1 );
				  int start = r.getOffset();
				  if( errorColumn > 0 && errorColumn <= r.getLength() )
					  start += errorColumn - 1;
				  int end = start + 1;
				  if( end >= document.getLength() ) end = document.getLength() - 1;
				  MarkerUtilities.setCharStart( map, start );
				  MarkerUtilities.setCharEnd( map, end );
				  // CALPlugin.printLog("Line " + errorLine + " = offset " + start + " to " + end );
			  }
			  catch( BadLocationException e)
			  {
				  // Forget about position?
			  }
			  
			  map.put( IMarker.LOCATION, "line " + errorLine + " col " + errorColumn );
		  }
		  else
		  {
			  map.put( IMarker.LOCATION, "unknown" );		  
		  }
		  
		  MarkerUtilities.createMarker( file, map, PROBLEM_MARKER_ID );

	    }
	    catch( CoreException e )
	    { 
		  // Unexpected condition
	    }
	  }
	  else  
	  {
		  // Put in markers for semantic checks
		  if( checker == null && !transformerFailed )
		  {
			  String checkFile = "net/sf/caltrop/cal/checks/semanticChecks.xslt";
			  
			  try
			  {
				  InputStream is = CALPlugin.getDefault().getClass().getClassLoader().getResourceAsStream( checkFile );
				  checker = Util.createTransformer( is );
			  }
			  catch( Exception e )
			  {
				  transformerFailed = true;
				  CALPlugin.printLog("Failed to construct semantic checker" );
			  }
		  }
		  
		  if( checker != null )
			  try
		      {
				  Node result = Util.applyTransform( (Node) actor, checker );
				  for( Node n = result.getFirstChild(); n != null; n = n.getNextSibling() )
				  {
					  if( ! n.getNodeName().equals("Note") ) continue;
				      if( ! ((Element)n).getAttribute( "kind" ).equals( "Report" ) ) continue;
				      
					  Map<String, Object> map = new HashMap<String, Object>();
					  
					  String severity = ((Element)n).getAttribute( "severity" );
					  String location = ((Element)n).getAttribute( "subject" );
					  String message = ((Element)n).getTextContent().trim();
					  
					  int isev = IMarker.SEVERITY_INFO;
					  if( severity.equals("Error") ) isev = IMarker.SEVERITY_ERROR;
					  else if (severity.equals("Warning") ) isev = IMarker.SEVERITY_WARNING;
					  
					  map.put( IMarker.SEVERITY, new Integer( isev ) );
					  MarkerUtilities.setMessage( map, message );
					  map.put( IMarker.LOCATION, location );		  

					  MarkerUtilities.createMarker( file, map, PROBLEM_MARKER_ID );
				  }

			  }
		      catch( Exception e )
		      {
		    	  // Oh well, nice try
		      }
		  
		  if( op != null )
	      {		  
		      // Update the outliner in the UI thread
		      setOutlinePage( op );
		      clearDone();
		      Display.getDefault().asyncExec( this );
		  
		      while( ! testDone() )
		      {
			      // Stall the parsing thread until the outliner has redrawn
			      try
			      {
				      Thread.sleep( 250 );
			      }
			      catch( InterruptedException e )
			      {
			 	  
			      }
		      }
	      }
	  }
  }
  
  CALContentOutlinePage outlinePage;
  
  private CALContentOutlinePage getOutlinePage()
  {
	  return outlinePage;
  }
  
  private void setOutlinePage( CALContentOutlinePage op )
  {
	  outlinePage = op;
  }
  
  // Separate outliner update thread. This runs in the UI thread at
  // lower priority and will not unduly burden the UI. 
  public void run()
  {
	  getOutlinePage().update( getActor() );
	  setDone();
  }

}
