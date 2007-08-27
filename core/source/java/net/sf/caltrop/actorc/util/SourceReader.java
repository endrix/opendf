/* 
BEGINCOPYRIGHT X,UC
	
	Copyright (c) 2007, Xilinx Inc.
	Copyright (c) 2003, The Regents of the University of California
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
	- Neither the names of the copyright holders nor the names 
	  of contributors may be used to endorse or promote 
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

package net.sf.caltrop.actorc.util;


import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.caltrop.actorc.parser.Lexer;
import net.sf.caltrop.actorc.parser.Parser;
import net.sf.caltrop.cal.ast.Actor;
import net.sf.caltrop.cal.ast.Expression;
import net.sf.caltrop.cal.ast.Statement;
import net.sf.caltrop.cal.interpreter.util.ASTFactory;
import net.sf.caltrop.util.source.MultiErrorException;
import net.sf.caltrop.util.source.LoadingErrorRuntimeException;

import net.sf.caltrop.util.xml.Util;
import net.sf.caltrop.util.logging.Logging;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;



/**
 *  @author Jörn W. Janneck <janneck@eecs.berkeley.edu>
 */

public class SourceReader
{
    private static final boolean suppressActorChecks = System.getenv().containsKey("CAL_SUPPRESS_ACTOR_CHECKS");
        
	public static Document parseActor(Reader s) throws MultiErrorException
    {
        return parseActor(s,"unknown");
    }
    
    /**
     * May throw LoadingErrorRuntimeException if failure occurs during parsing.
     */
	public static Document parseActor(Reader s, String name) throws MultiErrorException
	{
        Document doc = null;
        Lexer calLexer = new Lexer(s);
        Parser calParser = new Parser(calLexer);
        doc = calParser.parseActor(name);

        if (!suppressActorChecks)
        {
            try
            {
                // Basic error checking...
                Node res = Util.applyTransformsAsResources(doc, new String[] {
                    "net/sf/caltrop/cal/checks/semanticChecks.xslt",
                    // provide counts, terminate-on-error
                    "net/sf/caltrop/cal/checks/problemSummary.xslt"
                });
            }
            catch (Exception e)
            {
                // Catching exceptions keeps the XSLT fatal message from
                // terminating the process.
            }
        }
        
	    return doc;
	}

	public static Document parseActor(String s) throws MultiErrorException { return parseActor(s, "unknown"); }
	public static Document parseActor(String s, String name) throws MultiErrorException
	{
		return parseActor(new StringReader(s), name);
	}

	
	public static Actor readActor(Reader s) throws MultiErrorException { return readActor(s, "unknown"); }
	public static Actor readActor(Reader s, String name) throws MultiErrorException
    {
		return ASTFactory.buildActor(parseActor(s, name));
	}
	
	public static Actor readActor(String s) throws MultiErrorException { return readActor(s, "unknown"); }
	public static Actor readActor(String s, String name) throws MultiErrorException
    {
		return readActor(new StringReader(s), name);
	}
		
	
}
