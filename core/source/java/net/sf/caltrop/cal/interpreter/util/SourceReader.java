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

package net.sf.caltrop.cal.interpreter.util;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.caltrop.cal.interpreter.ast.Actor;
import net.sf.caltrop.cal.interpreter.ast.Expression;
import net.sf.caltrop.cal.interpreter.ast.Statement;
import net.sf.caltrop.cal.parser.CalExpressionParser;
import net.sf.caltrop.cal.parser.CalStatementParser;
import net.sf.caltrop.cal.parser.Lexer;
import net.sf.caltrop.cal.parser.Parser;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 *  @author Jörn W. Janneck <janneck@eecs.berkeley.edu>
 */

public class SourceReader {
	
	public static Document  parseActor(Reader s) {
		Lexer calLexer = new Lexer(s);
		Parser calParser = new Parser(calLexer);
		try {
			return calParser.parseActor();
		}
		catch (Exception exc) {
			throw new RuntimeException("Cannot parse actor.", exc);
		}
	}
	
	public static Document  parseActor(String s) {
		return parseActor(new StringReader(s));
	}

	public static Actor  readActor(Reader s) {
		return ASTFactory.buildActor(parseActor(s));
	}
	
	public static Actor  readActor(String s) {
		return readActor(new StringReader(s));
	}
	
	public static Actor  readActorML(InputStream s) {
		try {
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
		Lexer calLexer = new Lexer(s);
		CalExpressionParser calParser = new CalExpressionParser(calLexer);
		try {
			return ASTFactory.buildExpression(calParser.doParse().getDocumentElement());
		} catch (Exception e) {
			throw new RuntimeException("Cannot create expression AST.", e);
		}
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
