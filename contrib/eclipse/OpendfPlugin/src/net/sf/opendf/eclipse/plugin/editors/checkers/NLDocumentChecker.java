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

// import java.util.*;

import org.eclipse.core.resources.IFile;

// import net.sf.opendf.eclipse.plugin.OpendfPlugin;
import org.eclipse.jface.text.IDocument;

import org.w3c.dom.Node;

import java.io.*;
// import javax.xml.transform.*;
import net.sf.opendf.nl.util.Lib;
// import net.sf.opendf.util.xml.Util;
import net.sf.opendf.util.source.*;

public class NLDocumentChecker extends OpendfDocumentChecker
{
  /*
  private static final String[] checks = 
  {
    // No checks yet
  };
  
  private List<Transformer> transforms;

    */  
  public NLDocumentChecker( IFile file, IDocument document )
  {
    super( file, document );
    
    /*
    transforms = new ArrayList<Transformer>( checks.length );
    
    for( int i=0; i<checks.length; i++ )
    {
      try
      {
        InputStream is = OpendfPlugin.getDefault().getClass().getClassLoader().getResourceAsStream( checks[i] );
        transforms.add( Util.createTransformer( is ) );
      }
      catch( Exception e )
      {
        OpendfPlugin.logErrorMessage( "Failed to construct NL semantic checker " + checks[i], e );
      }
    }
    */
  }

  public Node parseDocument( ) throws MultiErrorException
  {
    return Lib.parseNetwork( new StringReader( document.get() ) );
  }
  
  /*
  public Transformer[] getSemanticChecks()
  {
    return transforms.toArray( new Transformer[] {} );
  }
  */
 
}
