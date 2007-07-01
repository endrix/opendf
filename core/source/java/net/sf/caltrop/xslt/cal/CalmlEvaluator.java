// CalmlEvaluator.java
// Xilinx Confidential
// Copyright (c) 2005 Xilinx Inc

package net.sf.caltrop.xslt.cal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.caltrop.cal.ast.Expression;
import net.sf.caltrop.cal.ast.Import;
import net.sf.caltrop.cal.ast.PackageImport;
import net.sf.caltrop.cal.ast.SingleImport;
import net.sf.caltrop.cal.interpreter.Context;
import net.sf.caltrop.cal.interpreter.ExprEvaluator;
import net.sf.caltrop.cal.interpreter.InterpreterException;
import net.sf.caltrop.cal.interpreter.environment.Environment;
import net.sf.caltrop.cal.interpreter.util.ASTFactory;
import net.sf.caltrop.cal.interpreter.util.ImportUtil;
import net.sf.caltrop.cal.interpreter.util.Platform;
import net.sf.caltrop.cal.util.SourceReader;
import net.sf.caltrop.util.logging.Logging;
import net.sf.caltrop.util.xml.ElementPredicate;
import net.sf.caltrop.util.xml.TagNamePredicate;
import net.sf.caltrop.util.xml.Util;
import net.sf.saxon.dom.DocumentBuilderFactoryImpl;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static net.sf.caltrop.util.xml.Util.xpathEvalNode;

public class CalmlEvaluator
{
	
	private static boolean DO_DEBUG = false;
	 
// 	public static Node printXML(String s, Node n) throws Exception {
// 		System.out.println("------------------- " + s + ":");
// 		System.out.println(caltrop.xml.Util.createXML(n));
// 		System.out.println("-------------------");
// 		return n;
// 	}
	
	private static Document emptyDocument() throws Exception
	{
		//caltrop.main.Util.setDefaultDBFI(); 
		DOMImplementation di = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
		//System.out.println("DI: " + di.getClass().getName());
		
		return di.createDocument("", "CalmlEvaluatorResult", null);
	}
	
	private static Node toSaxon( Node xalan ) throws Exception
	{
		String string = net.sf.caltrop.util.xml.Util.createXML( xalan );
//		caltrop.main.Util.setSAXON();
		StringReader reader = new StringReader(string);
		return DocumentBuilderFactoryImpl.newInstance().newDocumentBuilder().parse(
				new org.xml.sax.InputSource(reader));          
	}
	
	public static boolean  isDefined (String var, Node env) throws Exception {
		try {	  
			Environment thisEnv = createConstantPropagationEnvironment(env, emptyDocument());
			boolean result;
			try {
				thisEnv.get(var);
				result = true;
			}
			catch (Exception e) {
				result = false;
			}
			return result;
		} catch (Throwable th) {
            Logging.user().warning(th.toString());
            Logging.dbg().warning(th.toString());
			throw new RuntimeException("Cannot evaluate expression.", th);
		}

	}

    /**
     * Evaluates the specified {@link Expression}, within the
     * specified {@link Environment}, to a resulting Object.  The type
     * of that object is determined by the specific platform and
     * context defined by the Environment.  
     *
     * @param plat A non-null {@link Platform} which defines the
     * context in which the expression will be evaluated
     * @param expr A non-null Expression that will be evaluated
     * @param env a non-null Environment which defines the imports to the
     * base platform.
     * @return an Object whose specific type is determined by the
     *{@link Context} of the specified platform <code>plat</code>
     */
    public static Object evaluateExprObj (Platform plat, Expression exprAst, Environment env) throws Exception
    {
		Object result;
		try {	  
			ExprEvaluator eval = new ExprEvaluator(plat.context(), env);
            // The Evaluator returns an object whose type is
            // defined by the platform context.  
			result = eval.evaluate(exprAst);
		}
        catch (InterpreterException ie)
        {
            // Logging.user().warning("Cannot evaluate expression: " + ie.getMessage());
            // Logging.dbg().warning(ie.toString());
            throw ie; // Just pass it up
        }
        catch (Throwable th) {
            Logging.dbg().warning(th.toString());
            Logging.dbg().throwing("CalmlEvaluator", "evaluateExprObj", th);
			throw new RuntimeException("Cannot evaluate expression.", th);
		}
		
		return result;
    }
    
    public static Node parseExpression(String expr) throws Exception {
    	Node n = toSaxon(SourceReader.parseExpr(expr).getDocumentElement());
		reallyEmbarrassingHackToMakeThingsWork(n);
		return xpathEvalNode("/Expression", n);
    }
    
	public static Node evaluateExpr( Node expr, Node env ) throws Exception
	{ 	
//		System.out.println("Eval: " + 
//		((expr instanceof Element) ? 
//		(((Element)expr).getTagName() + "::" + ((Element)expr).getAttribute("kind") + "#" + ((Element)expr).getAttribute("literal-kind") + "#" + ((Element)expr).getAttribute("value"))
//		: 
//		("##" + expr.getNodeValue()))
//		);
 
	    Element result;
	    
        // Because the constant propagation environment uses the
        // SystemBuilder context the evaluation will return a
        // TypedObject. 
        try
        {
            Expression exprAst = ASTFactory.buildExpression((Element)expr);
            Environment thisEnv = createConstantPropagationEnvironment(env, emptyDocument());
            TypedObject val = (TypedObject)evaluateExprObj(sbPlatform, exprAst, thisEnv);
            result = renderObject(val);
        }
        catch (InterpreterException e)
        {
        	result = renderError( e.getMessage() );
        }
        
		Node n = toSaxon(result);
		reallyEmbarrassingHackToMakeThingsWork(n);

		return n;
	}
	
	public static Node  checkTypes(Node nts, Node ntd) throws Exception {
		Document doc;
		try {
			doc = emptyDocument();
		} catch (Exception e2) {
			throw new RuntimeException("Cannot create empty DOM document.", e2);
		}
		
		TypedContext.TypeCheckResult tcr;
		Element res;
		try {
			Type ts = createType((Element)nts);
			Type td = createType((Element)ntd);
			tcr = TypedContext.checkTypes(ts, td);
			if (tcr.result >= 0) {
				res = doc.createElement("Okay");
				res.setAttribute("identical", (tcr.result == TypedContext.TypeCheckResult.resIdentical) ? "true" : "false");
				res.appendChild(doc.createTextNode(tcr.message));
			} else {
				res = doc.createElement("ERROR");
				res.setAttribute("code", Integer.toString(tcr.result));
				res.appendChild(doc.createTextNode(tcr.message));
			}
		} catch (Exception e) {
			res = doc.createElement("ERROR");
			res.setAttribute("code", "-666");
			res.appendChild(doc.createTextNode(e.getMessage()));			
		}
		
		Node n = toSaxon(res);
		reallyEmbarrassingHackToMakeThingsWork(n);

		return n;
	}
	
	public static Number  dataWidth(Node nt) throws Exception {
		try {
			Type t = createType((Element)nt);
			if (Type.nameBool.equals(t.getName())) {
				return new Integer(1);
			} else if (Type.nameInt.equals(t.getName())) {
				return (Number) t.getValueParameters().get(Type.vparSize);
			} else {
				return new Integer(-1);
			}
		} catch (Exception e) {
			return new Integer(-1);
		}
	}
	
	private static void  reallyEmbarrassingHackToMakeThingsWork(Node n) throws TransformerException, IOException {
		TransformerFactory xff = TransformerFactory.newInstance();
        Transformer serializer = xff.newTransformer();
        OutputStream os = new ByteArrayOutputStream();
        serializer.transform(new DOMSource(n),
                new StreamResult(os));
        os.close();
	}
	
	
	private static Environment  createConstantPropagationEnvironment(Node theEnv, Document doc) {
        
        Element eEnv;
        if (theEnv instanceof Element) {
            eEnv = (Element)theEnv;
        } else if (theEnv instanceof Document) {
            eEnv = ((Document)theEnv).getDocumentElement();
        } else {
            eEnv = null;
            Logging.dbg().warning("Environment is of unknown class: " + theEnv.getClass().getName());
        }
        
        
		Import [] imports = ASTFactory.buildImports(Util.listElements(eEnv, predImport));
		// System.out.println("IMPORTS: " + imports.length);
		if (DO_DEBUG) System.out.println("IMPORTS: " + imports.length);
		for (int i = 0; i < imports.length; i++) {
			if (imports[i] instanceof PackageImport) {
				if (DO_DEBUG) System.out.println(" package import: " + imports[i].getPackagePrefix());
			} else if (imports[i] instanceof SingleImport) {
				if (DO_DEBUG) System.out.println(" single import: " + imports[i].getPackagePrefix() + "::" + ((SingleImport)imports[i]).getClassName());
			}
		}
		Environment env0 = ImportUtil.handleImportList(sbPlatform.createGlobalEnvironment(),
				sbPlatform.getImportHandlers(CalmlEvaluator.class.getClassLoader()),
				imports,
				sbPlatform.getImportMappers());
		Environment env = TypedPlatform.theContext.newEnvironmentFrame(env0);
		
		List eBindings = net.sf.caltrop.util.xml.Util.listElements(eEnv, predDecl);
		for (Iterator i = eBindings.iterator(); i.hasNext(); ) {
			Element binding = (Element) i.next();
			Element value = Util.optionalElement(binding, predExpr);
			if (value == null) {
				value = doc.createElement("Expr");
				value.setAttribute(attrKind, valUndefined);
			}
			Element type = Util.uniqueElement(binding, predType);
			if (type == null)
            {
				Logging.dbg().severe("ERROR: Could not find type for object.");
            }
            
			String var = binding.getAttribute(attrName);
			TypedObject val = createTypedObject(value, type);
			if (env.isLocalVar(var)) {
				env.set(var, val);
			} else {
				env.bind(var, val);
			}
		}
		
		return env;
	}
	
	private static TypedObject  createTypedObject(Element value, Element type) {
		
		Type t = createType(type);
		return createTypedObject(value, t);
		
	}
	
	private static TypedObject  createTypedObject(Element value, Type t) {
		// FIXME: add other expression kinds, such as lists
		if (valUndefined.equals(value.getAttribute(attrKind))) {
			return new TypedObject(t, TypedContext.UNDEFINED);
		} else if (valLiteral.equals(value.getAttribute(attrKind))) {
			// FIXME: add other literal kinds
			if (valInteger.equals(value.getAttribute(attrLiteralKind))) {
				return (TypedObject)sbContext.createInteger(value.getAttribute(attrValue));
			} else if (valReal.equals(value.getAttribute(attrLiteralKind))) {
					return (TypedObject)sbContext.createReal(value.getAttribute(attrValue));
			} else if (valBoolean.equals(value.getAttribute(attrLiteralKind))) {
				String s = value.getAttribute(attrValue).trim().toLowerCase();
				boolean b = ("1".equals(s)) || "true".equals(s);
				return (TypedObject)sbContext.createBoolean(b);
			} else
				throw new InterpreterException("Unsupported literal kind: '" + value.getAttribute(attrLiteralKind) + "'.");
		} else if (valList.equals(value.getAttribute(attrKind))) {
			
			Type elementType = (Type)t.getTypeParameters().get(Type.tparType);
			List elements = Util.listElements(value, predExpr);
			
			List l = new ArrayList();
			for (Iterator i = elements.iterator(); i.hasNext(); ) {
				Element e = (Element)i.next();
				l.add(createTypedObject(e, elementType));
			}
			
			return (TypedObject)sbContext.createList(l);
		} else
			throw new InterpreterException("Unsupported expression kind: '" + value.getAttribute(attrKind) + "'.");  		
	}
	
	private static Type createType(Element type) {
		String name = type.getAttribute(attrName);
		Map tp = new HashMap();
		Map vp = new HashMap();
		List entries = Util.listElements(type, predEntry);
		for (Iterator i = entries.iterator(); i.hasNext(); ) {
			Element e = (Element)i.next();
			if (valType.equals(e.getAttribute(attrKind))) {
				Element typeEl = Util.uniqueElement(e, predType);
				tp.put(e.getAttribute(attrName), createType(typeEl));
			} else if (valExpr.equals(e.getAttribute(attrKind))) {
				Element expr = Util.uniqueElement(e, predExpr);
				try {
					vp.put(e.getAttribute(attrName), new Integer(expr.getAttribute(attrValue))); // FIXME: generalize
				} catch (Exception exc) { 
					throw new InterpreterException("Type parameter '" + e.getAttribute(attrName) + 
							"' does not resolve to a constant at compile time", exc); 
				} 
			}
		}
		Type t =  Type.create(name, tp, vp);
		return t;
	}
	
	private static Element  renderObject(TypedObject a) {
		
		Document doc;
		try {
			doc = emptyDocument();
		} catch (Exception e2) {
			throw new RuntimeException("Cannot create empty DOM document.", e2);
		}
		
		Element eExpr = null;
		try {
			eExpr = doRenderObject(a, doc);
//			System.out.println("eExpr: " + caltrop.main.Util.createXML(eExpr));
			
		} catch (Exception e) {
			eExpr = doc.createElement("Expr");
			eExpr.setAttribute("kind", "Undefined");
		}
		
		Element eNote = doc.createElement("Note");
		eNote.setAttribute("kind", "exprType");
		Element eType = doRenderType(a.getType(), doc);
		eNote.appendChild(eType);
		
		eExpr.appendChild(eNote);

//		try {
//		System.out.println("eExpr: " + caltrop.main.Util.createXML(eExpr));
//		} catch (Exception eee) {}

		return eExpr;	  
	}

	
	private static Element  renderError( String msg ) {
		
		Document doc;
		try {
			doc = emptyDocument();
		} catch (Exception e) {
			throw new RuntimeException("Cannot create empty DOM document.", e);
		}
		
		Element eExpr = doc.createElement( "Expr" );
	    eExpr.setAttribute("kind", "Undefined");
		
		Element eNote = doc.createElement("Note");
		eNote.setAttribute("kind", "Report");
		eNote.setAttribute("severity", "Error");
		eNote.setAttribute("subject", "unknown");
		eNote.setAttribute("id", "expression.cannotEvaluateOrType");
		eNote.setTextContent( msg );
		
		eExpr.appendChild( eNote );
		return eExpr;	  
	}
	
	private static Element  doRenderObject(TypedObject a, Document doc) throws Exception {
		
		Element expr = doc.createElement("Expr");
		doc.getDocumentElement().appendChild(expr);
		
		Object v = a.getValue();
		if (v == TypedContext.UNDEFINED) {
			expr.setAttribute("kind", "Undefined");
		} else if (v instanceof Boolean) {
			expr.setAttribute("kind", "Literal");
			expr.setAttribute("literal-kind", "Boolean");
			expr.setAttribute("value", ((Boolean)v).booleanValue() ? "1" : "0");
		} else if (v instanceof Integer || v instanceof Long || v instanceof BigInteger) {
			expr.setAttribute("kind", "Literal");
			expr.setAttribute("literal-kind", "Integer");
			expr.setAttribute("value", v.toString());
		} else if (v instanceof Float || v instanceof Double) {
			expr.setAttribute("kind", "Literal");
			expr.setAttribute("literal-kind", "Real");
			expr.setAttribute("value", v.toString());
		} else if (v instanceof String) {
			expr.setAttribute("kind", "Literal");
			expr.setAttribute("literal-kind", "String");
			expr.setAttribute("value", (String)v);
		} else if (v instanceof List) {
			expr.setAttribute("kind", "List");
			for (Iterator i = ((List)v).iterator(); i.hasNext(); ) {
				TypedObject e = (TypedObject)i.next();
				Node enode = doRenderObject(e, doc);
				expr.appendChild(enode);
			}
		} else {
			expr.setAttribute("kind", "Undefined");
		}
		
		return expr;
	}
	
	private static Element  doRenderType(Type t, Document doc) {
		
		Element eType = doc.createElement("Type");
		eType.setAttribute("name", t.getName());
		if (t.getTypeParameters() != null) {
			Map m = t.getTypeParameters();
			for (Iterator i = m.keySet().iterator(); i.hasNext(); ) {
				Object k = i.next();
				Type t1 = (Type)m.get(k);
				Element entry = doc.createElement("Entry");
				entry.setAttribute("kind", "Type");
				entry.setAttribute("name", k.toString());
				Element t1el = doRenderType(t1, doc);
				entry.appendChild(t1el);
				eType.appendChild(entry);
			}
		}
		
		if (t.getValueParameters() != null) {
			Map m = t.getValueParameters();
			for (Iterator i = m.keySet().iterator(); i.hasNext(); ) {
				Object k = i.next();
				Integer n = (Integer)m.get(k); // FIXME: generalize
				Element entry = doc.createElement("Entry");
				entry.setAttribute("kind", "Expr");
				entry.setAttribute("name", k.toString());
				Element e = doc.createElement("Expr");
				e.setAttribute("kind", "Literal");
				e.setAttribute("literal-kind", "Integer");
				e.setAttribute("value", n.toString());
				entry.appendChild(e);
				eType.appendChild(entry);
			}
		}
		
		return eType;
	}
	
	
	
	final static Context sbContext = TypedPlatform.theContext;
	final static Platform sbPlatform = TypedPlatform.thePlatform;
	
	final static ElementPredicate predDecl = new TagNamePredicate("Decl");
	final static ElementPredicate predEntry = new TagNamePredicate("Entry");
	final static ElementPredicate predExpr = new TagNamePredicate("Expr");
	final static ElementPredicate predImport = new TagNamePredicate("Import");
	final static ElementPredicate predType = new TagNamePredicate("Type");
	
	final static String attrName = "name";
	final static String attrKind = "kind";
	final static String attrLiteralKind = "literal-kind";
	final static String attrValue = "value";
	
	final static String valBoolean = "Boolean";
	final static String valExpr = "Expr";
	final static String valInteger = "Integer";
	final static String valReal = "Real";
	final static String valList = "List";
	final static String valLiteral = "Literal";
	final static String valType = "Type";
	final static String valUndefined = "Undefined";
	

}
