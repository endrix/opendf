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

package net.sf.caltrop.nl;

import static net.sf.caltrop.nl.util.Lib.createAttributes;
import static net.sf.caltrop.nl.util.Lib.substituteExpression;
import static net.sf.caltrop.util.Util.xpathEvalElement;
import static net.sf.caltrop.util.Util.xpathEvalElements;
import static net.sf.caltrop.util.Util.xpathEvalNodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.caltrop.cal.interpreter.Context;
import net.sf.caltrop.cal.interpreter.ExprEvaluator;
import net.sf.caltrop.cal.interpreter.ast.Decl;
import net.sf.caltrop.cal.interpreter.ast.Expression;
import net.sf.caltrop.cal.interpreter.ast.GeneratorFilter;
import net.sf.caltrop.cal.interpreter.environment.Environment;
import net.sf.caltrop.cal.interpreter.generator.CollectionGenerator;
import net.sf.caltrop.cal.interpreter.generator.Filter;
import net.sf.caltrop.cal.interpreter.generator.Generator;
import net.sf.caltrop.cal.interpreter.generator.Seed;
import net.sf.caltrop.cal.interpreter.generator.VariableGenerator;
import net.sf.caltrop.cal.interpreter.util.ASTFactory;
import net.sf.caltrop.nl.util.DOMFactory;
import net.sf.caltrop.nl.util.IDGenerator;
import net.sf.caltrop.util.CascadedMap;
import net.sf.caltrop.util.Logging;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class EntityExprEvaluator {
	
	public Object  evaluate(Element expr) {
		String kind = expr.getAttribute(attrKind);
		if (valInstantiation.equals(kind)) {
			String id = idgen.newID();
			String name = expr.getAttribute(attrName);
			ClassName cn = entityClassMap.get(name);
			if (cn == null) {  // NOTE: should we do this??
				Logging.dbg().info("Entity '" + name + "' defaults to top-level package.");
				cn = new ClassName(name, new String [] {});
			}
			
			Map<String, Element> pars = new HashMap<String, Element>();
			NodeList args = xpathEvalNodes("Arg", expr);
			for (int i = 0; i < args.getLength(); i++) {
				Element a = (Element)args.item(i);
				pars.put(a.getAttribute(attrName), substituteExpression(xpathEvalElement("Expr", a), substitutions, callback.getDOM()));
			}
			
			callback.addInstance(id, cn.name, cn.packageName, pars, createAttributes(xpathEvalElements("Attribute", expr), substitutions, callback.getDOM()));
			return id;
		} else if (valList.equals(kind)) {
			NodeList nlGenerators = xpathEvalNodes("Generator", expr);
			GeneratorFilter [] gs = ASTFactory.buildGenerators(nlGenerators);
			if (gs.length > 0) {
	    		Generator g = new Seed(env);
	    		for (int i = 0; i < gs.length; i++) {
	    			CollectionGCB cgcb = new CollectionGCB(xpathEvalElement("Expr", nlGenerators.item(i)));
	    			g = new CollectionGenerator(g, gs[i].getCollectionExpr(), context, GeneratorCollectionVar, cgcb);
	    			Decl [] ds = gs[i].getVariables();
	    			for (int j = 0; j < ds.length; j++) {
	    				VariableGCB vgcb = new VariableGCB(cgcb);
	    				g = new VariableGenerator(g, context, ds[j].getName(), GeneratorCollectionVar, vgcb);
	    			}
	    			Expression [] filters = gs[i].getFilters();
	    			if (filters != null) {
	        			for (int j = 0; j < filters.length; j++) {
	        				g = new Filter(g, filters[j], context);
	        			}				
	    			}
	    		}
	    		NodeList es = xpathEvalNodes("EntityExpr", expr);

	    		List al = new ArrayList();
    			Environment oldEnv = env;
    			env = g.next();
	    		while (env != null) {
	        		for (int i = 0; i < es.getLength(); i++) {
	        			al.add(evaluate((Element)es.item(i)));
	        		}
	    			env = g.next();
	    		}
	    		env = oldEnv;
				return al;
			} else {
				List res = new ArrayList();
				NodeList es = xpathEvalNodes("EntityExpr", expr);
				for (int i = 0; i < es.getLength(); i++) {
					res.add(this.evaluate((Element)es.item(i)));
				}
				return res;
			}
		} else if(valIf.equals(kind)) {
			Expression cond = ASTFactory.buildExpression(xpathEvalElement("Expr", expr));
			ExprEvaluator evaluator = new ExprEvaluator(context, env);
			boolean b = context.booleanValue(evaluator.evaluate(cond));
			List<Element> branches = xpathEvalElements("EntityExpr", expr);
			assert branches.size() == 2;
			
			return this.evaluate(branches.get(b ? 0 : 1));
		} else {
			throw new RuntimeException("Cannot evaluate entity expression kind '" + kind + "'.");
		}
	}

	private final static String GeneratorCollectionVar = "$generator$collection$";

	public EntityExprEvaluator(Environment env, Context context, Callback callback, Map<String, ClassName> entityClassMap, IDGenerator idgen) {
		this.env = env;
		this.context = context;
		this.callback = callback;
		this.idgen = idgen;
		this.entityClassMap = entityClassMap;
		this.substitutions = null;
	}
	
	public interface Callback {
		DOMFactory  getDOM();
		
		void addInstance(String id, String className, String [] packageName, Map<String, Element> pars, List<Element> attributes);
		void addDeclaration(String var, Element expr);
	}
	
	private Environment 			env;
	private Context 				context;
	private Callback				callback;
	private CascadedMap<String, String>		substitutions;
	private IDGenerator				idgen;
	private Map<String, ClassName> 	entityClassMap;
	
	
	private final static String tagArgs = "Args";
	private final static String tagExpr = "Expr";

	private static final String attrKind = "kind";
	private static final String attrLiteralKind = "literal-kind";
	private static final String attrName = "name";
	private static final String attrValue = "value";
	
	private final static String valIf = "If";
	private final static String valIndexer = "Indexer";
	private static final String valInstantiation = "Instantiation";
	private static final String valInteger = "Integer";
	private static final String valLet = "Let";
	private static final String valList = "List";
	private static final String valLiteral = "Literal";
	private static final String valVar = "Var";
	private static final String valVariable = "Variable";
	
	private class VariableGCB implements  VariableGenerator.Callback {

		public void iterFinal(String localVar, String collectionVar) {
		}

		public void iterEnd(String localVar, String collectionVar) {
			substitutions = (CascadedMap<String, String>)substitutions.getParent();
		}

		public void iterNext(Object val, Environment localEnv, String localVar, String collectionVar) {
			DOMFactory df = callback.getDOM();
			Element eExpr = df.createElement(tagExpr);
			eExpr.setAttribute(attrKind, valIndexer);
			Element eVar = df.createElement(tagExpr);
			eExpr.appendChild(eVar);
			eVar.setAttribute(attrKind, valVar);
			eVar.setAttribute(attrName, cgcb.getCurrentCollectionVar());
			Element eArgs = df.createElement(tagArgs);
			eExpr.appendChild(eArgs);
			Element eIndex = df.createElement(tagExpr);
			eArgs.appendChild(eIndex);
			eIndex.setAttribute(attrKind, valLiteral);
			eIndex.setAttribute(attrLiteralKind, valInteger);
			eIndex.setAttribute(attrValue, Integer.toString(currentIndex));
			
			String v = idgen.newID();
			callback.addDeclaration(v, substituteExpression(eExpr, substitutions.getParent(), df));
			substitutions.put(localVar, v);
			
			currentIndex += 1;
		}

		public void iterStart(Collection c, Environment localEnv, String localVar, String collectionVar) {
			currentIndex = 0;
			substitutions = new CascadedMap<String, String>(new HashMap<String, String>(), substitutions);
		}
		
		public VariableGCB(CollectionGCB cgcb) {
			this.cgcb = cgcb;
		}
		
		private CollectionGCB	cgcb;		

		private int  			currentIndex;
	}
	
	private class CollectionGCB implements CollectionGenerator.Callback {

		public void collectionEnd(String collectionVar, Expression expr) {
		}

		public void collectionStart(String collectionVar, Expression expr, Object val, Environment env) {
			currentCollectionVar = idgen.newID();
			callback.addDeclaration(currentCollectionVar, substituteExpression(eExpr, substitutions, callback.getDOM()));
		}
		
		public void collectionFinal(String collectionVar, Expression expr) {
		}
		
		public String  getCurrentCollectionVar() {
			return currentCollectionVar;
		}

		public CollectionGCB(Element eExpr) {
			this.eExpr = eExpr;
		}
		
		private Element 	eExpr;
		private String		currentCollectionVar;

	}
	
}
