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
import static net.sf.caltrop.util.xml.Util.xpathEvalElement;
import static net.sf.caltrop.util.xml.Util.xpathEvalElements;
import static net.sf.caltrop.util.xml.Util.xpathEvalNodes;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.caltrop.cal.ast.Decl;
import net.sf.caltrop.cal.ast.Expression;
import net.sf.caltrop.cal.ast.GeneratorFilter;
import net.sf.caltrop.cal.interpreter.Context;
import net.sf.caltrop.cal.interpreter.ExprEvaluator;
import net.sf.caltrop.cal.interpreter.environment.Environment;
import net.sf.caltrop.cal.interpreter.generator.CollectionGenerator;
import net.sf.caltrop.cal.interpreter.generator.Filter;
import net.sf.caltrop.cal.interpreter.generator.Generator;
import net.sf.caltrop.cal.interpreter.generator.Seed;
import net.sf.caltrop.cal.interpreter.generator.VariableGenerator;
import net.sf.caltrop.cal.interpreter.util.ASTFactory;
import net.sf.caltrop.nl.util.DOMFactory;
import net.sf.caltrop.nl.util.IDGenerator;
import net.sf.caltrop.util.logging.CascadedMap;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;



public class StructureStmtEvaluator {
		
		public void execute(Element stmt) {
			String kind = stmt.getAttribute(attrKind);
			if (valConnection.equals(kind)) {
				NodeList nl = xpathEvalNodes(tagPortSpec, stmt);
				assert nl.getLength() == 2;
				// NOTE: somewhat hackish to use string arrays here, but in this localized situation, 
				//	     introducing a separate type just for this purpose would seem somewhat heavy
				String [] inPort = evalPortSpec((Element)nl.item(0), env);
				String [] outPort = evalPortSpec((Element)nl.item(1), env);
				assert inPort.length == 2 && outPort.length == 2;
				
				List<Element> attributes = createAttributes(xpathEvalElements("Attribute", stmt), substitutions, callback.getDOM());
				callback.connect(inPort[0], inPort[1], outPort[0], outPort[1], attributes);
			} else if (valIf.equals(kind)) {
				if (context.booleanValue(evaluateExpr(xpathEvalElement("Expr", stmt)))) {
					executeBlock(xpathEvalElement("StructureBlock[@kind='Then']", stmt));
				} else {
					executeBlock(xpathEvalElement("StructureBlock[@kind='Else']", stmt));					
				}
			} else if (valForeach.equals(kind)) {
				NodeList nlGenerators = xpathEvalNodes("Generator", stmt);
				GeneratorFilter [] gs = ASTFactory.buildGenerators(nlGenerators);
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
				List<Element> ss = xpathEvalElements("StructureStmt", stmt);

				Environment oldEnv = env;
				env = g.next();
				while (env != null) {
					for (int i = 0; i < ss.size(); i++) {
						execute(ss.get(i));
					}
					env = g.next();
				}
				env = oldEnv;
			} else {
				throw new RuntimeException("Cannot execute structure statement kind '" + kind + "'.");
			}
		}

		private final static String GeneratorCollectionVar = "$generator$collection$";

		public StructureStmtEvaluator(Environment env, Context context, Callback callback, Map<String, Object> entityEnv, IDGenerator idgen) {
			this.env = env;
			this.entityEnv = entityEnv;
			this.context = context;
			this.callback = callback;
			this.idgen = idgen;
			
			this.substitutions = null;
		}

		
		private Map<String, Object>			entityEnv;
		private Environment 				env;
		private CascadedMap<String, String>	substitutions;
		private Context						context;
		private Callback					callback;
		private IDGenerator					idgen;
		
		
		public interface  Callback {
			void  addDeclaration(String var, Element expr);
			void  connect(String src, String srcport, String dst, String dstport, List<Element> attributes);
			DOMFactory  getDOM();
		}
		
		private void  executeBlock(Element block) {
			if (block == null)
				return;
			for (Element stmt : xpathEvalElements("StructureStmt", block)) {
				execute(stmt);
			}
		}
		
		private Object  evaluateExpr(Element expr) {
			Expression e = ASTFactory.buildExpression(expr);
			ExprEvaluator eval = new ExprEvaluator(context, env);
			return eval.evaluate(e);
		}
		
		private String [] evalPortSpec(Element ps, Environment env) {
			String [] p = new String [2];
			String kind = ps.getAttribute(attrKind);
			if (valLocal.equals(kind)) {
				p[0] = null;
				p[1] = xpathEvalElement("PortRef", ps).getAttribute(attrName);
			} else if (valEntity.equals(kind)){
				p[0] = evalEntityRef(xpathEvalElement("EntityRef", ps), env);
				p[1] = xpathEvalElement("PortRef", ps).getAttribute(attrName);				
			} else {
				throw new RuntimeException("Cannot process PortSpec kind '" + kind + "'.");
			}
			
			return p;
		}
		
		private String evalEntityRef(Element er, Environment env) {
			
			String eName = er.getAttribute(attrName);
			Object val = entityEnv.get(eName);
			if (val == null) {
				throw new RuntimeException("Undefined entity ref: '" + eName + "'.");
			}
			List<Element> indexExprs = xpathEvalElements("Expr", er);
			for (Element indexExpr : indexExprs) {
				int i = context.intValue(evaluateExpr(indexExpr));
				if (! (val instanceof List))
					throw new RuntimeException("Dimensionality mismatch---too many indices.");
				if (i >= ((List)val).size())
					throw new ArrayIndexOutOfBoundsException("Index " + i + " does not exist, length is " + ((List)val).size() + ".");
				val = ((List)val).get(i);
			}
			if (val instanceof String)
				return (String)val;
			else 
				throw new RuntimeException("Dimensionality mismatch---insufficient number of indices.");
		}

		
		private static final String attrKind = "kind";
		private static final String attrLiteralKind = "literal-kind";
		private static final String attrName = "name";
		private static final String attrValue = "value";
		
		private static final String tagArgs = "Args";
		private static final String tagExpr = "Expr";
		private static final String tagPortRef = "PortRef";
		private static final String tagPortSpec = "PortSpec";

		private static final String valConnection = "Connection";
		private static final String valEntity = "Entity";
		private static final String valIf = "If";
		private static final String valIndexer = "Indexer";
		private static final String valInteger = "Integer";
		private static final String valLiteral = "Literal";
		private static final String valLocal = "Local";
		private static final String valForeach = "Foreach";
		private static final String valVar = "Var";

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
