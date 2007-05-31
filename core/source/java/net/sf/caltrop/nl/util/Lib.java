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

package net.sf.caltrop.nl.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.caltrop.cal.interpreter.Context;
import net.sf.caltrop.nl.parser.Lexer;
import net.sf.caltrop.nl.parser.Parser;
import net.sf.caltrop.util.CascadedMap;
import net.sf.caltrop.util.ParserErrorException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import static net.sf.caltrop.util.Util.xpathEvalElement;

public class Lib {
	
	public static Document  readNL(String fileName) {
		try {
			return readNL(new FileInputStream(fileName), fileName);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Could not find NL file '" + fileName + "'.", e);
		}
	}
	
	public static Document  readNL(InputStream s) {
		return readNL(s, "<unknown>");
	}
	
	public static Document  readNL(Reader r) {
		return readNL(r, "<unknown>");
	}
	
	private static Document  readNL(InputStream s, String fileName) {
		return readNL(new InputStreamReader(s), fileName);
	}

	private static Document  readNL(Reader r, String fileName) {
		try {
			Lexer lexer = new Lexer(r);
			Parser parser = new Parser(lexer);
			return parser.parseNetwork(fileName);			
		} 
		catch (ParserErrorException exc) {
			throw new RuntimeException("Error parsing NL.", exc);
		}
	}

	public static Element  substituteExpression(Element expr, Map<String, String> s, DOMFactory dom) {
		Set<String> vars = (s == null) ? Collections.EMPTY_SET : s.keySet();
		if (vars.isEmpty()) {
			return (Element)dom.clone(expr);
		} else {
			Element e = dom.createElement(tagExpr);
			e.setAttribute(attrKind, valLet);
			for (String var : vars) {
				Element d = dom.createElement(tagDecl);
				e.appendChild(d);
				d.setAttribute(attrKind, valVariable);
				d.setAttribute(attrName, var);
				Element de = dom.createElement(tagExpr);
				d.appendChild(de);
				de.setAttribute(attrKind, valVar);
				de.setAttribute(attrName, s.get(var));
			}
			Element expr2 = (Element)dom.clone(expr);
			e.appendChild(expr2);
			return e;
		}
	}
	
	public static Element  renderObject(Object a, Context context, DOMFactory dom) {
		
		Element expr = dom.createElement("Expr");
		
		if (context.isBoolean(a)) {
			expr.setAttribute("kind", "Literal");
			expr.setAttribute("literal-kind", "Boolean");
			expr.setAttribute("value", context.booleanValue(a) ? "1" : "0");
		} else if (context.isInteger(a)) {
			expr.setAttribute("kind", "Literal");
			expr.setAttribute("literal-kind", "Integer");
			expr.setAttribute("value", context.asBigInteger(a).toString());
		} else if (context.isReal(a)) {
			expr.setAttribute("kind", "Literal");
			expr.setAttribute("literal-kind", "Real");
			expr.setAttribute("value", Double.toString(context.realValue(a)));
		} else if (context.isString(a)) {
			expr.setAttribute("kind", "Literal");
			expr.setAttribute("literal-kind", "String");
			expr.setAttribute("value", context.stringValue(a));
		} else if (context.isList(a)) {
			expr.setAttribute("kind", "List");
			for (Object e : context.getList(a)) {
				Element enode = renderObject(e, context, dom);
				expr.appendChild(enode);
			}
		} else {
			expr.setAttribute("kind", "Undefined");
		}
		
		return expr;
	}

	
	public static List<Element>  createAttributes(List<Element> attrs, Map <String, String> substitutions, DOMFactory dom) {
		List<Element> as = new ArrayList<Element>();
		
		for (Element a : attrs) {
			as.add(createAttribute(a, substitutions, dom));
		}
		return as;
	}
	
	private static Element  createAttribute(Element a, Map<String, String> subs, DOMFactory dom) {
		Element e = dom.createElement(tagAttribute);
		e.setAttribute(attrName, a.getAttribute(attrName));
		String kind = a.getAttribute(attrKind);
		if (valValue.equals(kind)) {
			e.setAttribute(attrKind, valValue);
			e.appendChild(substituteExpression(xpathEvalElement("Expr", a), subs, dom));
		} else if (valFlag.equals(kind)){
			e.setAttribute(attrKind, valFlag);			
		} else if (valType.equals(kind)){
			throw new RuntimeException("Type attributes NYI.");
		} else {
			throw new RuntimeException("Cannot handle attributes of kind '" + kind + "'.");
		}
		
		return e;
	}
	
	private static final String attrKind = "kind";
	private static final String attrName = "name";

	private static final String tagAttribute = "Attribute";
	private static final String tagDecl = "Decl";
	private static final String tagExpr = "Expr";

	private static final String valFlag = "Flag";
	private static final String valLet = "Let";
	private static final String valType = "Type";
	private static final String valValue = "Value";
	private static final String valVar = "Var";
	private static final String valVariable = "Variable";
}
