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

package net.sf.opendf.eclipse.plugin.editors.checkers;

import static net.sf.opendf.util.xml.Util.xpathEvalElements;

import java.util.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.BadLocationException;
import net.sf.opendf.util.source.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import javax.xml.transform.*;
import net.sf.opendf.util.xml.Util;
import net.sf.opendf.eclipse.plugin.OpendfPlugin;

public abstract class OpendfDocumentChecker
{
  
  private IFile file;
  protected IDocument document;

  public static final String PROBLEM_MARKER_ID = "org.eclipse.core.resources.problemmarker";

  public OpendfDocumentChecker( IFile f, IDocument d )
  {
    file     = f;
    document = d;
  }
  
  // Must be extended for either CAL or NL
  public abstract Node parseDocument( ) throws MultiErrorException;
  // public abstract Transformer[] getSemanticChecks();

  public void postSyntaxErrors( MultiErrorException me )
  {
	  // Remove previous markers
	  try
	  {
		  file.deleteMarkers( null, true, IResource.DEPTH_ZERO );
	  }
	  catch( CoreException e )
	  { 
      OpendfPlugin.logErrorMessage("Unhandled exception while deleting file markers", e);
	  }
 
    List<GenericError> ge = me.getErrors();
    
    for( int i=0; i<ge.size(); i++ )
    {
      // Hack to detect end-of-file case
      if( ge.get(i).getReason().indexOf("\"EOF\"") >= 0 )
        createMarker( "Premature end of file", null, IMarker.SEVERITY_ERROR,
                      document.getNumberOfLines(), 1  );
      else
    	  createMarker( ge.get(i).getReason(), null, IMarker.SEVERITY_ERROR, 
                      ge.get(i).getLineNumber(), ge.get(i).getColumnNumber() );
    }
	}
  
  private void createMarker( String message, String location, int severity, int line, int col )
  {
    try
    {     
      IMarker marker = file.createMarker( PROBLEM_MARKER_ID );
      marker.setAttribute( IMarker.SEVERITY, severity );
      
      try
      {
        // Translate line, col to document position
        IRegion r = document.getLineInformation( line - 1 );
        
        int start = r.getOffset();
        if( col > 0 && col <= r.getLength() )
          start += col - 1;
        
        int end = start + 1;
        
        if( end >= document.getLength() ) end = document.getLength() - 1;
        
        marker.setAttribute( IMarker.CHAR_START, start );
        marker.setAttribute( IMarker.CHAR_END  , end );
      }
      catch( BadLocationException e )
      {
        // give up and tag the first character of the document
        marker.setAttribute( IMarker.CHAR_START, 0 );
        marker.setAttribute( IMarker.CHAR_END  , 0 );
      }
      
      String loc = location;
    
      if( location == null )
      {
        if( line > 0 )
        {
          loc = "line " + line;
          if( col > 0 )
            loc = loc + " col " + col;
        }
        else loc = "unknown";
      }
    
      marker.setAttribute( IMarker.LOCATION, loc );
      marker.setAttribute( IMarker.MESSAGE , message );

    }
    catch( CoreException e )
    { 
      OpendfPlugin.logErrorMessage( "Failed to create marker", e );
    }
  }
  
  public void checkSemanticsAndReport( Node document )
  {

	  try
    {
      file.deleteMarkers( null, true, IResource.DEPTH_ZERO );

      // Node result = Util.applyTransforms( (Node) document, getSemanticChecks() );
      
  		List<Element> e = xpathEvalElements("//Note[@kind='Report'][@severity='Error']", document );
      reportList( e, IMarker.SEVERITY_ERROR );
          
      e = xpathEvalElements( "/*/Note[@kind='Report'][@severity='Warning']", document );
      reportList( e, IMarker.SEVERITY_WARNING );
		  
      e = xpathEvalElements( "/*/Note[@kind='Report'][not( @severity='Error' ) ][not( @severity='Warning' ) ]", document );
      reportList( e, IMarker.SEVERITY_INFO );
		  
    }
    catch( Exception e )
    {
      OpendfPlugin.logErrorMessage( "Failed to post semantic checks", e );
    }
  }
  
  static final String attrLine = "text-begin-line";
  static final String attrCol  = "text-begin-col";
  static final String attrLoc  = "subject";
 
  private void reportList( List<Element> list, int severity )
  {
    for( int i = 0; i < list.size(); i++ )
    {
      Element e = list.get( i );
      List<Element> location = xpathEvalElements( "Note[@kind='report-location'][1]", (Node) e );
        
      int line = 1;
      int col  = 1;
        
      if( location.size() > 0 )
      {
        Element loc = location.get( 0 );
          
        if( loc.hasAttribute( attrLine ) )
          line = Integer.parseInt( loc.getAttribute( attrLine ) );

        if( loc.hasAttribute( attrCol ) )
          col = Integer.parseInt( loc.getAttribute( attrCol ) );
      }
        
      createMarker( e.getTextContent().trim(), e.getAttribute( attrLoc ), severity, line, col );
    }   
  }
}
