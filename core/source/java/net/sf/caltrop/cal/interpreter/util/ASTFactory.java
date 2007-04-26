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

import net.sf.caltrop.cal.interpreter.InterpreterException;
import net.sf.caltrop.cal.interpreter.ast.*;
import net.sf.caltrop.util.ElementPredicate;
import net.sf.caltrop.util.Logging;
import net.sf.caltrop.util.TagNameAttributeValuePredicate;
import net.sf.caltrop.util.TagNamePredicate;
import net.sf.caltrop.util.Util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.print.attribute.standard.Finishings;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static net.sf.caltrop.util.Util.xpathEvalElement;
import static net.sf.caltrop.util.Util.xpathEvalElements;
import static net.sf.caltrop.util.Util.xpathEvalNodes;

/**
 * An ASTFactory creates an AST from a valid CalML/DOM representation of an actor.
 *
 * @author Jörn W. Janneck <jwj@acm.org>
 */

public class ASTFactory {

    ////////////////////////////////////////////////////////////////////////
    /////   AST creation methods
    ////////////////////////////////////////////////////////////////////////

    /**
     * Compute the AST corresponding to the Document, which is a DOM tree representing the
     * CalML description of an actor.
     *
     * @param doc The CalML/DOM tree.
     * @return The corresponding actor.
     */
    public static Actor buildActor(Document doc)
    {
        return buildPreprocessedActor(preprocessActor(doc));
    }

    /**
     * Compute the AST corresponding to the Document, which is a DOM tree representing the
     * preprocessed CalML description of an actor.
     *
     * @param doc The CalML/DOM tree.
     * @return The corresponding actor.
     */
    public static Actor buildPreprocessedActor(Node doc) {

        Node canonicalDoc = doc;

        Element e = null;
        if (canonicalDoc instanceof Element) {
        	e = (Element) canonicalDoc;
        } else if (canonicalDoc instanceof Document) {
        	e = ((Document)canonicalDoc).getDocumentElement();
        } else {
        	throw new RuntimeException("Cannot build actor from this object: " + canonicalDoc);
        }

        return createActor(e);
     }
    
    public static Node  preprocessActor(Document doc) {
    	return canonicalizeActor(doc);
    }

    /**
     * Compute the AST corresponding to the Document, which is a DOM tree representing the
     * CalML description of an actor.
     *
     * @param doc The CalML/DOM tree.
     * @return The corresponding actor.
     */
    public static Expression buildExpression(Node doc) {

        Node canonicalDoc = canonicalizeExprStmt(doc);
        Element e = null;
        if (canonicalDoc instanceof Element) {
        	e = (Element) canonicalDoc;
        } else if (canonicalDoc instanceof Document) {
        	e = ((Document)canonicalDoc).getDocumentElement();
        	if (!predExpr.test(e)) {
            	e = net.sf.caltrop.util.Util.uniqueElement(e, predExpr);
        	}
        } else {
        	throw new RuntimeException("Cannot build expression from this object: " + canonicalDoc);
        }
        
        return createExpression(e);
     }
    
    private static Expression createExpression(Element e) {
        assert e.getTagName().equals(tagExpr);

        Expression result;

        String exprkind = e.getAttribute(attrKind);

        if (exprkind.equals(valApplication))
            result = createExprApplication(e);
        else if (exprkind.equals(valLet))
            result = createExprLet(e);
        else if (exprkind.equals(valLambda))
            result = createExprLambda(e);
        else if (exprkind.equals(valProc))
            result = createExprProc(e);
        else if (exprkind.equals(valLiteral))
            result = createExprLiteral(e);
        else if (exprkind.equals(valVar))
            result = createExprVariable(e);
        else if (exprkind.equals(valIf))
            result = createExprIf(e);
        else if (exprkind.equals(valSet))
            result = createExprSet(e);
        else if (exprkind.equals(valList))
            result = createExprList(e);
        else if (exprkind.equals(valMap))
            result = createExprMap(e);
        else if (exprkind.equals(valEntry))
            result = createExprEntry(e);
        else if (exprkind.equals(valIndexer))
            result = createExprIndexer(e);
        else {
        	String xml = null;
        	try {
        		xml = Util.createXML(e);
        	} catch (Exception exc) {
        		xml = "<<cannot unparse: " + e.toString() + ">>";
        	}
        	throw new InterpreterException("Unknown expression type: '" + exprkind + "'. [XML: " + xml + "]");
        }

        return (Expression) annotateFreeVars(result, e);
    }
    
    private static Expression [] createExpressions(NodeList nl) {
    	Expression [] es = new Expression [nl.getLength()];
    	for (int i = 0; i < nl.getLength(); i++) {
    		es[i] = createExpression((Element)nl.item(i));
    	}
    	return es;
    }
    
    /**
     * Compute the AST corresponding to the Document, which is a DOM tree representing the
     * CalML description of an actor.
     *
     * @param doc The CalML/DOM tree.
     * @return The corresponding actor.
     */
    public static Statement [] buildStatements(Node doc) {

        Node canonicalDoc = canonicalizeExprStmt(doc);

        Element e = null;
        if (canonicalDoc instanceof Element) {
        	e = (Element) canonicalDoc;
        } else if (canonicalDoc instanceof Document) {
        	e = ((Document)canonicalDoc).getDocumentElement();
        } else {
        	throw new RuntimeException("Cannot build expression from this object: " + canonicalDoc);
        }
        

        try {
            String s = Util.createXML(e);
        } catch (Exception exc) {
            throw new RuntimeException("Cannot create statement.", exc);
        }

        List statements = net.sf.caltrop.util.Util.listElements(e, predStmt);
        Statement [] s = new Statement[statements.size()];
        for (int i = 0; i < s.length; i++) {
            s[i] = createStatement((Element)statements.get(i));
        }

        return s;
     }
    
    private static Statement  createStatement(Element e) {
        assert predStmt.test(e);
        String kind = e.getAttribute(attrKind);

        if (kind.equals(valAssign))
            return createStmtAssign(e);
        else if (kind.equals(valBlock))
            return createStmtBlock(e);
        else if (kind.equals(valCall))
            return createStmtCall(e);
        else if (kind.equals(valIf)) {
            return createStmtIf(e);
        } else if (kind.equals(valWhile)) {
            return createStmtWhile(e);
        } else {
            throw new InterpreterException("Unknown statement type: '" + kind + "'.");
        }
    }
    
    public static Import []  buildImports(List importElements) {
        Import [] imports = new Import[importElements.size()];
        for (int i = 0; i < imports.length; i++) {
            imports[i] = buildImport((Element)importElements.get(i));
        }
        return imports;
    }

    public static Import []  buildImports(NodeList importElements) {
        Import [] imports = new Import[importElements.getLength()];
        for (int i = 0; i < imports.length; i++) {
            imports[i] = buildImport((Element)importElements.item(i));
        }
        return imports;
    }

    public static Import buildImport(Element e) {
        assert e.getTagName().equals(tagImport);

        String alias = e.getAttribute(attrAlias);
        Element qid = net.sf.caltrop.util.Util.uniqueElement(e, predQID);
        List idl = net.sf.caltrop.util.Util.listElements(qid, predID);
        String [] qname = new String[idl.size()];
        for (int i = 0; i < qname.length; i++) {
            qname[i] = ((Element) idl.get(i)).getAttribute(attrName);
        }

        String kind = e.getAttribute(attrKind);
        if (kind.equals(valSingle)) {
            return new SingleImport(qname, alias);
        } else if (kind.equals(valPackage)) {
            return new PackageImport(qname);
        } else 
        	throw new InterpreterException("Unknown import kind: '" + kind + "'.");
    }

    public static GeneratorFilter [] buildGenerators(NodeList genList) {

        GeneratorFilter [] gf = new GeneratorFilter[genList.getLength()];
        for (int i = 0; i < genList.getLength(); i++) {
        	gf[i] = buildGenerator((Element)genList.item(i));
        }
        return gf;
    }
    
    public static GeneratorFilter buildGenerator(Element g) {
    	
    	Node canonicalDoc = canonicalizeExprStmt(g);
        Element e = null;
        if (canonicalDoc instanceof Element) {
        	e = (Element) canonicalDoc;
        } else if (canonicalDoc instanceof Document) {
        	e = ((Document)canonicalDoc).getDocumentElement();
        } else {
        	throw new RuntimeException("Cannot build expression from this object: " + canonicalDoc);
        }
    	
    	return  createGenerator(e);
    }
    

    


    ////////////////////////////////////////////////////////////////////////
    /////   private
    ////////////////////////////////////////////////////////////////////////


    private static Actor  createActor(Element e) {

        String packageName = "";
        
        Logging.dbg().info("Getting package name for " + e.getAttribute(attrName));
        // Try 'Package name=xxx' structure for package naming.
        Element pe = Util.xpathEvalElement("/Actor/Package", e);
        if (pe != null)
        {
            String p = pe.getAttribute(attrName);
            Logging.dbg().info("\tGenerated from Package name attribute: " + p);
            packageName = p;
        }
        else
        {
            // No package element, so there must be no Package
            // declaration.  Return the empty string unless we are
            // supporting historical usage, in which case we should
            // return 'cal'.  
            Logging.dbg().info("\tDefault Package name attribute \"\"");
            packageName = "";
        }

         List importl = net.sf.caltrop.util.Util.listElements(e, predImports);
         Import [] imports = buildImports(importl);

         String name = e.getAttribute(attrName);

         Decl [] pars = createDecls(e, predDeclPar);

         List ipl = net.sf.caltrop.util.Util.listElements(e, new TagNameAttributeValuePredicate(tagPort, attrKind, valInput));
         PortDecl [] inputPorts = new PortDecl[ipl.size()];
         for (int i = 0; i < inputPorts.length; i++) {
             inputPorts[i] = createPortDecl((Element)ipl.get(i));
         }

         List opl = net.sf.caltrop.util.Util.listElements(e, new TagNameAttributeValuePredicate(tagPort, attrKind, valOutput));
         PortDecl [] outputPorts = new PortDecl[opl.size()];
         for (int i = 0; i < outputPorts.length; i++) {
             outputPorts[i] = createPortDecl((Element)opl.get(i));
         }

         List initializerl = net.sf.caltrop.util.Util.listElements(e, predInitializer);
         Action [] initializers = new Action[initializerl.size()];
         for (int i = 0; i < initializers.length; i++) {
             initializers[i] = createAction(i, (Element)initializerl.get(i));
         }

         List actionl = net.sf.caltrop.util.Util.listElements(e, predAction);
         Action [] actions = new Action[actionl.size()];
         for (int i = 0; i < actions.length; i++) {
             actions[i] = createAction(i, (Element)actionl.get(i));
         }

         List stateVarl = net.sf.caltrop.util.Util.listElements(e, predDeclVar);
         Decl [] stateVars = new Decl [stateVarl.size()];
         for (int i = 0; i < stateVars.length; i++) {
             stateVars[i] = createDecl((Element)stateVarl.get(i));
         }
         
         ScheduleFSM fsm = createFSM(net.sf.caltrop.util.Util.optionalElement(e, predScheduleFSM));
         
         List [] priorities = createPriorities(net.sf.caltrop.util.Util.listElements(e, predPriority));
         
         Element eInv = Util.optionalElement(e, predInvariants);
         Expression [] invariants = null;
         if (eInv != null) {
        	 List invariantl = Util.listElements(eInv, predExpr);
             invariants = new Expression [invariantl.size()];
             for (int i = 0; i < invariants.length; i++) {
                 invariants[i] = createExpression((Element) invariantl.get(i));
             }
         }

         return new Actor(imports, name, packageName, pars, inputPorts, outputPorts, initializers, actions, stateVars, fsm, priorities, invariants);
    }

    private static PortDecl createPortDecl(Element e) {
        assert e.getTagName().equals(tagPort);

        String portName = e.getAttribute(attrName);
        Element type = net.sf.caltrop.util.Util.uniqueElement(e, predType);

        TypeExpr typeExpr = (type != null) ? createTypeExpr(type) : null;
        return new PortDecl(portName, typeExpr);
    }

    public static TypeExpr createTypeExpr(Element e) {
        assert predType.test(e);

        String typeName = e.getAttribute(attrName);
        
        List<Element> typeParameters = xpathEvalElements("Type", e);
        if (typeParameters.size() > 0) {
            TypeExpr [] typeExprs = new TypeExpr[typeParameters.size()];
            for (int i = 0; i < typeExprs.length; i++) {
                typeExprs[i] = createTypeExpr((Element) typeParameters.get(i));
            }
            return new TypeExpr(typeName, typeExprs);
        } else {
        	typeParameters = xpathEvalElements("Entry[@kind='Type']", e);
        	List<Element> valueParameters = xpathEvalElements("Entry[@kind='Expr']", e);
        	if (typeParameters.size() > 0 || valueParameters.size() > 0) {
        		Map tPars = new HashMap();
        		Map vPars = new HashMap();
        		for (Element e1 : typeParameters) {
        			Element t = xpathEvalElement("Type", e1);
        			tPars.put(e1.getAttribute(attrName), createTypeExpr(t));
        		}
        		
        		for (Element e1 : valueParameters) {
        			Element v = xpathEvalElement("Expr", e1);
        			vPars.put(e1.getAttribute(attrName), createExpression(v));
        		}
        		
        		return new TypeExpr(typeName, tPars, vPars);
        	} else {
        		return new TypeExpr(typeName);
        	}
        }
    }

    private static Decl [] createDecls(Element e, ElementPredicate pred) {
        List decll = net.sf.caltrop.util.Util.listElements(e, pred);
        Decl [] decls = new Decl [decll.size()];
        for (int i = 0; i < decls.length; i++) {
            decls[i] = createDecl((Element) decll.get(i));
        }
        return decls;
    }
    
    private static Decl[] createDecls(NodeList nl) {
    	Decl [] ds = new Decl [nl.getLength()];
    	for (int i = 0; i < nl.getLength(); i++) {
    		ds[i] = createDecl((Element)nl.item(i));
    	}
    	return ds;
    }

    private static Decl  createDecl(Element e) {
        assert predDecl.test(e);

        String name = e.getAttribute(attrName);
        Element initExpr = net.sf.caltrop.util.Util.uniqueElement(e, predExpr);
        boolean isAssignable = getBooleanAttribute(e, attrAssignable, false);
        boolean isMutable = getBooleanAttribute(e, attrMutable, false);
        Element type = net.sf.caltrop.util.Util.uniqueElement(e, predType);        
        TypeExpr typeExpr = (type != null) ? createTypeExpr(type) : null;
        return new Decl(typeExpr, name, (initExpr == null) ? null : createExpression(initExpr), isAssignable, isMutable);
    }

    private static InputPattern createInputPattern(Element e) {
        assert e.getTagName().equals(tagInput);

        String portname = e.getAttribute(attrPort);
        NodeList decll = e.getElementsByTagName(tagDecl);
        String [] variables = new String[decll.getLength()];
        for (int i = 0; i < variables.length; i++) {
            variables[i] = ((Element) decll.item(i)).getAttribute(attrName);
        }
        Element repeatelt = net.sf.caltrop.util.Util.uniqueElement(e, predRepeat);
        if (repeatelt == null) {
            return new InputPattern(portname, variables, null);
        } else {
            return new InputPattern(portname, variables, createExpression(net.sf.caltrop.util.Util.uniqueElement(repeatelt, predExpr)));
        }
    }

    private static OutputExpression createOutputExpression(Element e) {
        assert e.getTagName().equals(tagOutput);

        String portname = e.getAttribute(attrPort);
        List exprl = net.sf.caltrop.util.Util.listElements(e, new TagNamePredicate(tagExpr));
        Expression [] values = new Expression[exprl.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = createExpression((Element) exprl.get(i));
        }
        Element repeatelt = net.sf.caltrop.util.Util.uniqueElement(e, predRepeat);
        if (repeatelt == null) {
            return new OutputExpression(portname, values, null);
        } else {
            return new OutputExpression(portname, values, createExpression(net.sf.caltrop.util.Util.uniqueElement(repeatelt, predExpr)));
        }
    }

    private static Statement [] createStatements(Element e) {
        List lStmts = net.sf.caltrop.util.Util.listElements(e, predStmt);
        Statement [] body = new Statement [lStmts.size()];
        for (int i = 0; i < body.length; i++) {
            body[i] = createStatement((Element)lStmts.get(i));
        }
        return body;
    }

    private static StmtAssignment  createStmtAssign(Element e) {
        assert predStmtAssign.test(e);

        String name = e.getAttribute(attrName);
        Expression expr = createExpression(net.sf.caltrop.util.Util.uniqueElement(e, predExpr));

        Element argelement = net.sf.caltrop.util.Util.optionalElement(e, predArgs);
        Element entryElement = Util.optionalElement(e, predEntry);

        assert argelement == null || entryElement == null;

        if (argelement != null) {
            List indexl = net.sf.caltrop.util.Util.listElements(argelement, predExpr);
            Expression [] indices = new Expression[indexl.size()];

            for (int i = 0; i < indices.length; i++) {
                indices[i] = createExpression((Element) indexl.get(i));
            }
            return new StmtAssignment(name, expr, indices);
        } else if (entryElement != null) {
        	String fieldName = entryElement.getAttribute(attrName);
        	return new StmtAssignment(name, expr, fieldName);
        } else {
            return new StmtAssignment(name, expr);
        }
    }

    private static StmtBlock createStmtBlock(Element e) {

        Decl [] decls = createDecls(e, predDeclVar);
        Statement [] statements = createStatements(e);
        return new StmtBlock(decls, statements);
    }


    private static StmtCall createStmtCall(Element e) {
        assert predStmtCall.test(e);

        Expression p = createExpression(net.sf.caltrop.util.Util.uniqueElement(e, predExpr));

        Element argelement = net.sf.caltrop.util.Util.uniqueElement(e, predArgs);
        List argl = net.sf.caltrop.util.Util.listElements(argelement, predExpr); //could be empty, indicating noarg function.
        Expression [] args = new Expression[argl.size()];

        for (int i = 0; i < args.length; i++) {
            args[i] = createExpression((Element) argl.get(i));
        }
        return new StmtCall(p, args);
    }

    private static StmtIf createStmtIf(Element e) {
        assert predStmtIf.test(e);

        Expression condition = createExpression(net.sf.caltrop.util.Util.uniqueElement(e, predExpr));

        List blocks = net.sf.caltrop.util.Util.listElements(e, predStmtBlock);
        assert blocks.size() == 1 || blocks.size() == 2;

        Statement thenBranch = createStmtBlock((Element)blocks.get(0));
        Statement elseBranch = (blocks.size() == 2) ? createStmtBlock((Element)blocks.get(1)) : null;

        return new StmtIf(condition, thenBranch, elseBranch);
    }

    private static StmtWhile createStmtWhile(Element e) {
        assert predStmtWhile.test(e);

            Expression condition = createExpression(net.sf.caltrop.util.Util.uniqueElement(e, predExpr));
            Statement body = createStmtBlock(net.sf.caltrop.util.Util.uniqueElement(e, predStmtBlock));

            return new StmtWhile(condition, body);
    }

    private static Expression createExprEntry(Element e) {
        String name = e.getAttribute(attrName);
        Expression parentExpr = createExpression(net.sf.caltrop.util.Util.uniqueElement(e, predExpr));
        return new ExprEntry(name, parentExpr);
    }

    private static Expression createExprList(Element e) {
        List exprl = net.sf.caltrop.util.Util.listElements(e, predExpr);
        Expression [] exprs = new Expression[exprl.size()];

        for (int i = 0; i < exprs.length; i++) {
            exprs[i] = createExpression((Element) exprl.get(i));
        }
        
        NodeList genList = xpathEvalNodes("Generator", e);
        if (genList.getLength() > 0) {        	
        	GeneratorFilter [] gens = createGenerators(genList);
        	return new ExprList(exprs, gens);
        } else {
        	return new ExprList(exprs);        	
        }
    }

    private static Expression createExprSet(Element e) {
        List exprl = net.sf.caltrop.util.Util.listElements(e, predExpr);
        Expression [] exprs = new Expression[exprl.size()];

        for (int i = 0; i < exprs.length; i++) {
            exprs[i] = createExpression((Element) exprl.get(i));
        }
        
        NodeList genList = xpathEvalNodes("Generator", e);
        if (genList.getLength() > 0) {        	
        	GeneratorFilter [] gens = createGenerators(genList);
        	return new ExprSet(exprs, gens);
        } else {
        	return new ExprSet(exprs);        	
        }
    }

    private static Expression createExprMap(Element e) {
        List mapl = net.sf.caltrop.util.Util.listElements(e, predMapping);
        Expression [][] mappings = new Expression[mapl.size()][2];

        for (int i = 0; i < mappings.length; i++) {
            Element m = (Element)mapl.get(i);
            List exprl = net.sf.caltrop.util.Util.listElements(m, predExpr);
            assert exprl.size() == 2;
            mappings[i][0] = createExpression((Element) exprl.get(0));
            mappings[i][1] = createExpression((Element) exprl.get(1));
        }

        NodeList genList = xpathEvalNodes("Generator", e);
        if (genList.getLength() > 0) {        	
        	GeneratorFilter [] gens = createGenerators(genList);
        	return new ExprMap(mappings, gens);
        } else {
        	return new ExprMap(mappings);        	
        }
    }
    
    private static GeneratorFilter [] createGenerators(NodeList genList) {
    	GeneratorFilter [] gs = new GeneratorFilter [genList.getLength()];
    	for (int i = 0; i < genList.getLength(); i++) {
    		gs[i] = createGenerator((Element)genList.item(i));
    	}
    	return gs;
    }
    
    private static GeneratorFilter createGenerator(Element e) {
    	Decl [] ds = createDecls(xpathEvalNodes("Decl", e));
    	Expression expr = createExpression(xpathEvalElement("Expr", e));
    	NodeList nl = xpathEvalNodes("Filters/Expr", e);
    	if (nl.getLength() == 0) {
    		return new GeneratorFilter(ds, expr, null);
    	} else {
    		Expression [] filters = createExpressions(nl);
    		return new GeneratorFilter(ds, expr, filters);
    	}
    }

    private static Expression createExprIf(Element e) {
        List exprs = net.sf.caltrop.util.Util.listElements(e, predExpr);

        assert exprs.size() == 3;

        return new ExprIf(createExpression((Element) exprs.get(0)),
                          createExpression((Element) exprs.get(1)),
                          createExpression((Element) exprs.get(2)));
    }

    private static ExprIndexer createExprIndexer(Element e) {
        assert predExprIndexer.test(e);
        
        assert (Util.listElements(e,predExpr).size() == 1) : "malformed indexer expression";
        assert (Util.listElements(e,predArgs).size() == 1) : "malformed indexer argument expression";
        
        Expression structure = createExpression(net.sf.caltrop.util.Util.uniqueElement(e, predExpr));

        Element argelement = net.sf.caltrop.util.Util.uniqueElement(e, predArgs);
        List indexl = net.sf.caltrop.util.Util.listElements(argelement, predExpr); //could be empty, indicating noarg function.
        Expression [] indices = new Expression[indexl.size()];

        for (int i = 0; i < indices.length; i++) {
            indices[i] = createExpression((Element) indexl.get(i));
        }
        return new ExprIndexer(structure, indices);
    }

    private static ExprLambda createExprLambda(Element e) {
        assert e.getTagName().equals(tagExpr) && e.getAttribute(attrKind).equals(valLambda);

        List lParams = net.sf.caltrop.util.Util.listElements(e, predDeclPar);
        String [] pars = new String [lParams.size()];
        for (int i = 0; i < pars.length; i ++) {
            pars[i] = ((Element)lParams.get(i)).getAttribute(attrName);
        }

        List lDecls = net.sf.caltrop.util.Util.listElements(e, predDeclVar);
        Decl [] decls = new Decl [lDecls.size()];
        for (int i = 0; i < decls.length; i ++) {
            decls[i] = createDecl((Element)lDecls.get(i));
        }
        Expression body = createExpression(net.sf.caltrop.util.Util.uniqueElement(e, predExpr));
        return new ExprLambda(pars, decls, body);
    }

    private static ExprLet createExprLet(Element e) {
        assert e.getTagName().equals(tagExpr) && e.getAttribute(attrKind).equals(valLet);

        List lDecls = net.sf.caltrop.util.Util.listElements(e, predDeclVar);
        Decl [] decls = new Decl [lDecls.size()];
        for (int i = 0; i < decls.length; i ++) {
            decls[i] = createDecl((Element)lDecls.get(i));
        }
        Expression body = createExpression(net.sf.caltrop.util.Util.uniqueElement(e, predExpr));
        return new ExprLet(decls, body);
    }

    private static ExprProc createExprProc(Element e) {
        assert e.getTagName().equals(tagExpr) && e.getAttribute(attrKind).equals(valProc);

        List lParams = net.sf.caltrop.util.Util.listElements(e, predDeclPar);
        String [] pars = new String [lParams.size()];
        for (int i = 0; i < pars.length; i ++) {
            pars[i] = ((Element)lParams.get(i)).getAttribute(attrName);
        }

        List lDecls = net.sf.caltrop.util.Util.listElements(e, predDeclVar);
        Decl [] decls = new Decl [lDecls.size()];
        for (int i = 0; i < decls.length; i ++) {
            decls[i] = createDecl((Element)lDecls.get(i));
        }

        Statement [] body = createStatements(e);

        return new ExprProc(pars, decls, body);
    }


    /* FIXME, only handles subset of literals right now. */
    private static ExprLiteral createExprLiteral(Element e) {
        assert e.getTagName().equals(tagExpr) && e.getAttribute(attrKind).equals(valLiteral);

        String litkind = e.getAttribute(attrLiteralKind);
        String litvalue = e.getAttribute(attrValue);

        if (litkind.equals(valInteger))
            return new ExprLiteral(ExprLiteral.litInteger, litvalue);
        else if (litkind.equals(valReal))
            return new ExprLiteral(ExprLiteral.litReal, litvalue);
        else if (litkind.equals(valBoolean)) {
            String val = e.getAttribute(attrValue);
            if (val.equals("true") || val.equals("1"))
                return new ExprLiteral(ExprLiteral.litTrue);
            else
                return new ExprLiteral(ExprLiteral.litFalse);
        } else if (litkind.equals(valString))
            return new ExprLiteral(ExprLiteral.litString, litvalue);
        else if (litkind.equals(valCharacter))
            return new ExprLiteral(ExprLiteral.litChar, litvalue);
        else if (litkind.equals(valNull))
        	return new ExprLiteral(ExprLiteral.litNull);
        else throw new InterpreterException("Unknown literal type: " + litkind);
    }

    private static ExprVariable createExprVariable(Element e) {
        assert e.getTagName().equals(tagExpr) && e.getAttribute(attrKind).equals(valVar);

        return new ExprVariable(e.getAttribute(attrName));
    }

    private static ExprApplication createExprApplication(Element e) {
        assert predExprApplication.test(e);
        assert net.sf.caltrop.util.Util.listElements(e, predExpr).size() == 1 : "Function missing in application expression.";
        assert net.sf.caltrop.util.Util.listElements(e, predArgs).size() == 1 : "No arguments is application expression.";

        Expression function = createExpression(net.sf.caltrop.util.Util.uniqueElement(e, predExpr));

        Element argelement = net.sf.caltrop.util.Util.uniqueElement(e, predArgs);
        List argl = net.sf.caltrop.util.Util.listElements(argelement, predExpr); //could be empty, indicating noarg function.
        Expression [] args = new Expression[argl.size()];

        for (int i = 0; i < args.length; i++) {
            args[i] = createExpression((Element) argl.get(i));
        }
        return new ExprApplication(function, args);
    }

    private static Action createAction(int id, Element e) {
    	assert predAction.test(e) || predInitializer.test(e);
        
        Element eTag = net.sf.caltrop.util.Util.optionalElement(e, predQID);
        QID tag = (eTag == null) ? null : createQID(eTag);

        List inputl = net.sf.caltrop.util.Util.listElements(e, predInput);
        InputPattern [] inputpatterns = new InputPattern[inputl.size()];
        for (int i = 0; i < inputpatterns.length; i++) {
            inputpatterns[i] = createInputPattern((Element) inputl.get(i));
        }
        List outputl = net.sf.caltrop.util.Util.listElements(e, predOutput);
        OutputExpression [] outputexpressions = new OutputExpression[outputl.size()];
        for (int i = 0; i < outputexpressions.length; i++) {
            outputexpressions[i] = createOutputExpression((Element) outputl.get(i));
        }

        Statement [] stmts = createStatements(e);

        Decl [] decls = createDecls(e, predDeclVar);

        Element guardRoot = net.sf.caltrop.util.Util.uniqueElement(e, predGuards);
        Expression [] guards;
        if (guardRoot != null) {
            List guardl = net.sf.caltrop.util.Util.listElements(guardRoot, predExpr);
            guards = new Expression [guardl.size()];
            for (int i = 0; i < guards.length; i++) {
                guards[i] = createExpression((Element) guardl.get(i));
            }
        } else {
            guards = new Expression [0];
        }
        
        Element requiresRoot = net.sf.caltrop.util.Util.uniqueElement(e, predRequires);
        Expression [] preconditions = null;
        if (requiresRoot != null) {
            List requiresl = net.sf.caltrop.util.Util.listElements(requiresRoot, predExpr);
            preconditions = new Expression [requiresl.size()];
            for (int i = 0; i < preconditions.length; i++) {
                preconditions[i] = createExpression((Element) requiresl.get(i));
            }
        }
        
        Element ensuresRoot = net.sf.caltrop.util.Util.uniqueElement(e, predEnsures);
        Expression [] postconditions = null;
        if (ensuresRoot != null) {
            List ensuresl = net.sf.caltrop.util.Util.listElements(ensuresRoot, predExpr);
            postconditions = new Expression [ensuresl.size()];
            for (int i = 0; i < postconditions.length; i++) {
                postconditions[i] = createExpression((Element) ensuresl.get(i));
            }
        }
        
        Expression delay = null;
        Element eDelay = net.sf.caltrop.util.Util.optionalElement(e, predDelay);
        if (eDelay != null) {
        	delay = createExpression(net.sf.caltrop.util.Util.uniqueElement(eDelay, predExpr));
        }

        return (Action)addTextRange(e, new Action(id, tag, inputpatterns, outputexpressions, decls, guards, stmts, delay, preconditions, postconditions));
    }
    
    private static ScheduleFSM createFSM(Element e) {
    	if (e == null)
    		return null;
    	
    	String initialState = e.getAttribute(attrInitialState);
    	List transitionl = net.sf.caltrop.util.Util.listElements(e, predTransition);
    	Transition [] transitions = new Transition[transitionl.size()];
    	for (int i = 0; i < transitions.length; i++) {
    		transitions[i] = createTransition((Element)transitionl.get(i));
    	}
    	
    	ScheduleFSM fsm = new ScheduleFSM(transitions, initialState);
    	return fsm;
    }
    
    private static Transition  createTransition(Element e) {
    	String  fromState = e.getAttribute(attrFrom);
    	String toState = e.getAttribute(attrTo);
    	Element atags = net.sf.caltrop.util.Util.uniqueElement(e, predActionTags);
    	List tagl = net.sf.caltrop.util.Util.listElements(atags, predQID);
    	QID [] tags = new QID[tagl.size()];
    	for (int i = 0; i < tags.length; i++) {
    		tags[i] = createQID((Element)tagl.get(i));
    	}
    	return new Transition(fromState, toState, tags);
    }
    
    private static List []  createPriorities(List priorityElements) {
    	List [] priorities = new List [priorityElements.size()];
    	
    	for (int i = 0; i < priorityElements.size(); i++) {
    		priorities[i] = createPriority((Element) priorityElements.get(i));
    	} 
    	
    	return priorities;
    }
    
    private static List  createPriority(Element priorityElement) {
    	
    	List ps = new ArrayList();
    	List qids = net.sf.caltrop.util.Util.listElements(priorityElement, predQID);
    	for (Iterator i = qids.iterator(); i.hasNext(); ) {
    		Element e = (Element)i.next();
    		ps.add(createQID(e));
    	}
    	
    	return ps;
    }
    
    private static QID  createQID(Element e) {
    	List idl = net.sf.caltrop.util.Util.listElements(e, predID);
    	String [] ids = new String [idl.size()];
    	for (int i = 0; i < ids.length; i++) {
    		ids[i] = ((Element)idl.get(i)).getAttribute(attrName);
    	}
    	return new QID(ids);
    }
    
    private static List makeList(NodeList nl) {
    	List l = new ArrayList();
    	for (int i = 0; i < nl.getLength(); i++) {
    		l.add(nl.item(i));
    	}
    	return l;
    }
    
    private static ASTNode  addTextRange(Element e, ASTNode n) {
    	if (e.hasAttribute(attrTextBeginCol)) 
    		n.setAttribute(keyTextBeginCol, new Integer(e.getAttribute(attrTextBeginCol)));
    	if (e.hasAttribute(attrTextBeginLine)) 
    		n.setAttribute(keyTextBeginLine, new Integer(e.getAttribute(attrTextBeginLine)));
    	if (e.hasAttribute(attrTextEndCol)) 
    		n.setAttribute(keyTextEndCol, new Integer(e.getAttribute(attrTextEndCol)));
    	if (e.hasAttribute(attrTextEndLine)) 
    		n.setAttribute(keyTextEndLine, new Integer(e.getAttribute(attrTextEndLine)));
    	return n;
    }

    private static ASTNode annotateFreeVars(ASTNode node, Element e) {
        List freeVarElts = net.sf.caltrop.util.Util.listElements(e, predFreeVarNote);
        List freeVars = new ArrayList(freeVarElts.size());

        for (Iterator iterator = freeVarElts.iterator(); iterator.hasNext();) {
            Element freeVarNote = (Element) iterator.next();
            freeVars.add(freeVarNote.getAttribute(attrName));
        }

        node.setAttribute(AttributeKeys.KEYFREEVAR, freeVars);
        return node;
    }

    private static boolean  getBooleanAttribute(Element e, String a, boolean defaultValue) {
    	if (!e.hasAttribute(a))
    		return defaultValue;
    	
    	String s = e.getAttribute(a).trim().toLowerCase();
    	return "true".equals(s) || "yes".equals(s);
    }

    public static void main(String [] args) throws Exception {
        System.out.println("Reading actor '" + args[0] + "'...");
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(args[0]));
//        Actor a = create(doc);
//        System.out.println(a);
        String orig = Util.createXML(doc);
        System.out.println(orig);
        System.out.println("==================================================================");
        Node res = canonicalizeActor(doc);
        String result = Util.createXML(res);
        System.out.println(result);
    }


    final static String keyTextBeginLine = "text-begin-line";
    final static String keyTextBeginCol = "text-begin-col";
    final static String keyTextEndLine = "text-end-line";
    final static String keyTextEndCol = "text-end-col";

    final static String tagAction = "Action";
    final static String tagActionTags = "ActionTags";
    final static String tagArgs = "Args";
    final static String tagBlock = "Body";    // sic
    final static String tagDecl = "Decl";
    final static String tagDelay = "Delay";
    final static String tagEnsures = "Ensures";
    final static String tagEntry = "Entry";
    final static String tagExpr = "Expr";
    final static String tagGuards = "Guards";
    final static String tagID = "ID";
    final static String tagImport = "Import";
    final static String tagInitializer = "Initializer";
    final static String tagInput = "Input";
    final static String tagInvariants = "Invariants";
    final static String tagMapping = "Mapping";
    final static String tagNote = "Note";
    final static String tagOutput = "Output";
    final static String tagPort = "Port";
    final static String tagQID = "QID";
    final static String tagPriority = "Priority";
    final static String tagRepeat = "Repeat";
    final static String tagRequires = "Requires";
    final static String tagSchedule = "Schedule";
    final static String tagStmt = "Stmt";
    final static String tagTransition = "Transition";
    final static String tagType = "Type";
    
    final static String attrAlias = "alias";
    final static String attrAssignable = "assignable";
    final static String attrFrom = "from";
    final static String attrInitialState = "initial-state";
    final static String attrKind = "kind";
    final static String attrMutable = "mutable";
    final static String attrName = "name";
    final static String attrLiteralKind = "literal-kind";
    final static String attrTextBeginLine = "text-begin-line";
    final static String attrTextBeginCol = "text-begin-col";
    final static String attrTextEndLine = "text-end-line";
    final static String attrTextEndCol = "text-end-col";
    final static String attrTo = "to";
    final static String attrValue = "value";
    final static String attrPort = "port";

    final static String valAssign = "Assign";
    final static String valBlock = "Block";
    final static String valBoolean = "Boolean";
    final static String valCall = "Call";
    final static String valCharacter = "Character";
    final static String valEntry = "Entry";
    final static String valFreeVar = "freeVar";
    final static String valFsm = "fsm";
    final static String valIf = "If";
    final static String valIndexer = "Indexer";
    final static String valInput = "Input";
    final static String valOutput = "Output";
    final static String valLambda = "Lambda";
    final static String valLet = "Let";
    final static String valList = "List";
    final static String valLiteral = "Literal";
    final static String valInteger = "Integer";
    final static String valMap = "Map";
    final static String valNull = "Null";
    final static String valParameter = "Parameter";
    final static String valPackage = "package";
    final static String valProc = "Proc";
    final static String valReal = "Real";
    final static String valSet = "Set";
    final static String valSingle = "single";
    final static String valString = "String";
    final static String valVar = "Var";
    final static String valVariable = "Variable";  
    final static String valWhile = "While";
    final static String valApplication = "Application";

    final static ElementPredicate predAction = new TagNamePredicate(tagAction);
    final static ElementPredicate predActionTags = new TagNamePredicate(tagActionTags);
    final static ElementPredicate predArgs = new TagNamePredicate(tagArgs);
    // final static ElementPredicate predBlock = new TagNamePredicate(tagBlock);
    final static ElementPredicate predDecl = new TagNamePredicate(tagDecl);
    final static ElementPredicate predDeclPar = new TagNameAttributeValuePredicate(tagDecl, attrKind, valParameter);
    final static ElementPredicate predDeclVar = new TagNameAttributeValuePredicate(tagDecl, attrKind, valVariable);
    final static ElementPredicate predDelay = new TagNamePredicate(tagDelay);
    final static ElementPredicate predEnsures = new TagNamePredicate(tagEnsures);
    final static ElementPredicate predEntry = new TagNamePredicate(tagEntry);
    final static ElementPredicate predExpr = new TagNamePredicate(tagExpr);
    final static ElementPredicate predExprApplication = new TagNameAttributeValuePredicate(tagExpr, attrKind, valApplication);
    final static ElementPredicate predFreeVarNote = new TagNameAttributeValuePredicate(tagNote, attrKind, valFreeVar);
    final static ElementPredicate predGuards = new TagNamePredicate(tagGuards);
    final static ElementPredicate predID = new TagNamePredicate(tagID);
	static final ElementPredicate predExprIndexer = new TagNameAttributeValuePredicate(tagExpr, attrKind, valIndexer);
    final static ElementPredicate predImports = new TagNamePredicate(tagImport);
    final static ElementPredicate predInitializer = new TagNamePredicate(tagInitializer);
    final static ElementPredicate predInput = new TagNamePredicate(tagInput);
    final static ElementPredicate predInvariants = new TagNamePredicate(tagInvariants);
    final static ElementPredicate predMapping = new TagNamePredicate(tagMapping);
    final static ElementPredicate predOutput = new TagNamePredicate(tagOutput);
    final static ElementPredicate predPriority = new TagNamePredicate(tagPriority);
    final static ElementPredicate predQID = new TagNamePredicate(tagQID);
    final static ElementPredicate predRepeat = new TagNamePredicate(tagRepeat);
    final static ElementPredicate predRequires = new TagNamePredicate(tagRequires);
    final static ElementPredicate predScheduleFSM = new TagNameAttributeValuePredicate(tagSchedule, attrKind, valFsm);
    final static ElementPredicate predStmt = new TagNamePredicate(tagStmt);
    final static ElementPredicate predStmtAssign = new TagNameAttributeValuePredicate(tagStmt, attrKind, valAssign);
    final static ElementPredicate predStmtBlock = new TagNameAttributeValuePredicate(tagStmt, attrKind, valBlock);
    final static ElementPredicate predStmtCall = new TagNameAttributeValuePredicate(tagStmt, attrKind, valCall);
    final static ElementPredicate predStmtIf = new TagNameAttributeValuePredicate(tagStmt, attrKind, valIf);
    final static ElementPredicate predStmtWhile = new TagNameAttributeValuePredicate(tagStmt, attrKind, valWhile);
    final static ElementPredicate predTransition = new TagNamePredicate(tagTransition);
    final static ElementPredicate predType = new TagNamePredicate(tagType);

    private static Node   canonicalizeActor(Node doc)  { 

        try {
            if (actorTransformations == null) {
                actorTransformations = new Transformer [actorTransformationPaths.length];
                for (int i = 0; i < actorTransformations.length; i++) {
                    InputStream is = Util.class.getClassLoader().getResourceAsStream(actorTransformationPaths[i]);
                    try {
                        actorTransformations[i] = Util.createTransformer(is);
                    } catch (Throwable e) {
                        throw new RuntimeException("Could not create transformer '" + actorTransformationPaths[i] + "'.", e);
                    } finally {
                        is.close();
                    }

                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot create transformations.", e);
        }

        try {
            // Util.setSAXON();
            Node actor = Util.applyTransforms(doc, actorTransformations);
            // System.out.println(Util.createXML(actor));
            return actor;
        } catch (Exception e) {
            throw new RuntimeException("Cannot canonicalize actor.", e);
        }
    }

    static {
        // System.out.println("properties = " + System.getProperties());
        Util.setSAXON();
        // System.out.println("properties = " + System.getProperties());
    }

    private static String [] actorTransformationPaths = {
        "net/sf/caltrop/cal/transforms/BuildProductSchedule.xslt",
        "net/sf/caltrop/cal/transforms/CanonicalizePortTags.xslt",
        "net/sf/caltrop/cal/transforms/ReplaceOld.xslt",
        //"net/sf/caltrop/cal/transforms/ReplaceGenerators.xslt",
        "net/sf/caltrop/cal/transforms/ReplaceStmtGenerators.xslt",
        "net/sf/caltrop/cal/transforms/AddID.xslt",
        "net/sf/caltrop/cal/transforms/VariableAnnotator.xslt",
        "net/sf/caltrop/cal/transforms/ContextInfoAnnotator.xslt",
        "net/sf/caltrop/cal/transforms/CanonicalizeOperators.xslt",
        "net/sf/caltrop/cal/transforms/AnnotateFreeVars.xslt",
        "net/sf/caltrop/cal/transforms/DependencyAnnotator.xslt",
        "net/sf/caltrop/cal/transforms/VariableSorter.xslt"
    };

    private static Transformer [] actorTransformations = null;

    private static Node   canonicalizeExprStmt(Node doc)  {

        try {
            if (exprTransformations == null) {
                exprTransformations = new Transformer [exprTransformationPaths.length];
                for (int i = 0; i < exprTransformations.length; i++) {
                    InputStream is = Util.class.getClassLoader().getResourceAsStream(exprTransformationPaths[i]);
                    try {
                        exprTransformations[i] = Util.createTransformer(is);
                    } catch (Throwable e) {
                        throw new RuntimeException("Could not create transformer '" + exprTransformationPaths[i] + "'.", e);
                    } finally {
                        is.close();
                    }

                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot create transformations.", e);
        }

        try {
            // Util.setSAXON();
            Node actor = Util.applyTransforms(doc, exprTransformations);
            return actor;
        } catch (Exception e) {
            throw new RuntimeException("Cannot canonicalize expression.", e);
        }
    }

    private static String [] exprTransformationPaths = {
        //"net/sf/caltrop/cal/transforms/ReplaceGenerators.xslt",
        "net/sf/caltrop/cal/transforms/ReplaceStmtGenerators.xslt",
        "net/sf/caltrop/cal/transforms/AddID.xslt",
        "net/sf/caltrop/cal/transforms/VariableAnnotator.xslt",
        "net/sf/caltrop/cal/transforms/ContextInfoAnnotator.xslt",
        "net/sf/caltrop/cal/transforms/CanonicalizeOperators.xslt",
        "net/sf/caltrop/cal/transforms/AnnotateFreeVars.xslt",
        "net/sf/caltrop/cal/transforms/DependencyAnnotator.xslt",
        "net/sf/caltrop/cal/transforms/VariableSorter.xslt"
    };

    private static Transformer [] exprTransformations = null;

//
//    private static final String dbfiSaxon = "net.sf.saxon.om.DocumentBuilderFactoryImpl";
//
//    private static String [] xsltEngine = {
//        null, null, null, dbfiSaxon, dbfiSaxon, dbfiSaxon
//    };

}




