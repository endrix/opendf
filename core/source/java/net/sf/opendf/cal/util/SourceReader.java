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

package net.sf.opendf.cal.util;


import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.opendf.cal.ast.Actor;
import net.sf.opendf.cal.ast.Expression;
import net.sf.opendf.cal.ast.Statement;
import net.sf.opendf.cal.interpreter.util.ASTFactory;
import net.sf.opendf.cal.parser.CalExpressionParser;
import net.sf.opendf.cal.parser.CalStatementParser;
import net.sf.opendf.cal.parser.Lexer;
import net.sf.opendf.cal.parser.Parser;
import net.sf.opendf.util.source.MultiErrorException;

import net.sf.opendf.util.xml.Util;
import net.sf.opendf.util.logging.Logging;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;



/**
 *  @author J�rn W. Janneck <janneck@eecs.berkeley.edu>
 */

public class SourceReader
{
    private static final boolean suppressActorChecks = System.getenv().containsKey("CAL_SUPPRESS_ACTOR_CHECKS");

    private static String [] actorPreprocessTransforms = {
        "net/sf/opendf/cal/transforms/BuildProductSchedule.xslt",
        "net/sf/opendf/cal/transforms/CanonicalizePortTags.xslt",

        // Copies Input Port Type to Input Decl
        "net/sf/opendf/cal/transforms/AddInputTypes.xslt",

        // Convert the old keyword to appropriate local var declaration
        "net/sf/opendf/cal/transforms/ReplaceOld.xslt",
        // Add $local to all local variables.
        "net/sf/opendf/cal/transforms/RenameLocalVars.xslt",

        // Defer inlining of fxn/procedure to the code generators
        //"net/sf/opendf/cal/transforms/Inline.xslt",

        // Done in elaborator after inlining
        //"net/sf/opendf/cal/transforms/AddID.xslt",
        //"net/sf/opendf/cal/transforms/VariableAnnotator.xslt",
        //"net/sf/opendf/cal/transforms/ContextInfoAnnotator.xslt",
        //"net/sf/opendf/cal/transforms/CanonicalizeOperators.xslt",
        //"net/sf/opendf/cal/transforms/AnnotateFreeVars.xslt",
        //"net/sf/opendf/cal/transforms/DependencyAnnotator.xslt",
        //"net/sf/opendf/cal/transforms/VariableSorter.xslt"
    };
    
	public static Document parseActor(Reader s) throws MultiErrorException
    {
        return parseActor(s,"unknown");
    }
    
    /**
     * May throw Exception if failure occurs during parsing.
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
                    "net/sf/opendf/cal/checks/semanticChecks.xslt",
                    // provide counts, terminate-on-error
                    "net/sf/opendf/cal/checks/problemSummary.xslt"
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

    /**
     * Canonicalize Actor elements.
     */
    public static Node actorPreprocess (Node node)
    {
    	try {
    		Node result = Util.applyTransformsAsResources(node, actorPreprocessTransforms);
    		return result;
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }

	public static Element  readExprDOM(Reader s) throws Exception {
		Lexer calLexer = new Lexer(s);
		CalExpressionParser calParser = new CalExpressionParser(calLexer);
		Document doc = calParser.doParse();
		return doc.getDocumentElement();
	}

	public static Element  readExprDOM(String s) {
		try {
			return readExprDOM(new StringReader(s));
		}
		catch (Exception exc) {
			throw new RuntimeException("Cannot parse expression: " + s, exc);
		}
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
	
	public static Actor readActorML(InputStream s) {
		try
        {
			Document  doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(s);
			return ASTFactory.buildActor(doc);
		} catch (Exception e) {
			throw new RuntimeException("Cannot create actor AST.", e);
		}
	}
	
	public static Actor  readPreprocessedActorML(InputStream s) {
		try {
			Document  doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(s);
			return ASTFactory.buildPreprocessedActor(doc);
		} catch (Exception e) {
			throw new RuntimeException("Cannot create actor AST.", e);
		}
	}
	
	public static Expression  readExpr(String s) {
		return readExpr(new StringReader(s));
	}
	
	public static Expression  readExpr(Reader s) {
		try {
			return ASTFactory.buildExpression(parseExpr(s).getDocumentElement());
		} catch (Exception e) {
			throw new RuntimeException("Cannot create expression AST.", e);
		}
	}
	
	public static Document parseExpr(String s) throws Exception
    {
		return parseExpr(new StringReader(s));
	}
	
	public static Document parseExpr(Reader s) throws Exception
    {
		Lexer calLexer = new Lexer(s);
		CalExpressionParser calParser = new CalExpressionParser(calLexer);
		return calParser.doParse();
	}
	
	
	public static Statement [] readStmt(String s) {
		return readStmt(new StringReader(s));
	}
	
	public static Statement [] readStmt(Reader s) {
		Lexer calLexer = new Lexer(s);
		CalStatementParser calParser = new CalStatementParser(calLexer);
		try {
			return ASTFactory.buildStatements(calParser.doParse().getDocumentElement());
		} catch (Exception e) {
			throw new RuntimeException("Cannot create expression AST.", e);
		}
	}
	
	
	
}