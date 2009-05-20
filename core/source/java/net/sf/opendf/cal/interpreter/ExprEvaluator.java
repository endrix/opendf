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

package net.sf.opendf.cal.interpreter;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.opendf.cal.ast.Decl;
import net.sf.opendf.cal.ast.ExprApplication;
import net.sf.opendf.cal.ast.ExprEntry;
import net.sf.opendf.cal.ast.ExprIf;
import net.sf.opendf.cal.ast.ExprIndexer;
import net.sf.opendf.cal.ast.ExprLambda;
import net.sf.opendf.cal.ast.ExprLet;
import net.sf.opendf.cal.ast.ExprList;
import net.sf.opendf.cal.ast.ExprLiteral;
import net.sf.opendf.cal.ast.ExprMap;
import net.sf.opendf.cal.ast.ExprProc;
import net.sf.opendf.cal.ast.ExprSet;
import net.sf.opendf.cal.ast.ExprVariable;
import net.sf.opendf.cal.ast.Expression;
import net.sf.opendf.cal.ast.ExpressionVisitor;
import net.sf.opendf.cal.ast.GeneratorFilter;
import net.sf.opendf.cal.interpreter.environment.Environment;
import net.sf.opendf.cal.interpreter.generator.CollectionGenerator;
import net.sf.opendf.cal.interpreter.generator.Filter;
import net.sf.opendf.cal.interpreter.generator.Generator;
import net.sf.opendf.cal.interpreter.generator.GeneratorCallback;
import net.sf.opendf.cal.interpreter.generator.Seed;
import net.sf.opendf.cal.interpreter.generator.VariableGenerator;

/**
 * The expression evaluator interprets expressions in a given environment and context and computes the value
 * denoted by them.
 *
 * @author Jorn W. Janneck <janneck@eecs.berkeley.edu>
 * @see Expression
 * @see Context
 * @see Environment
 */

public class ExprEvaluator implements ExpressionVisitor {

    //
    //  ExprEvaluator
    //

    /**
     * Evaluate the expression and return its value. Evaluation takes place in the current environment,
     * typically the one passed to the constructor.
     * @param e The expression.
     * @return Its value.
     */

    public Object  evaluate(final Expression e) {
        e.accept(this);
        return value;
    }

    public Object evaluate(final Expression expr, final Environment tempEnv) {
        final Environment oldEnv = setEnvironment(tempEnv);
        final Object val = evaluate(expr);
        env = oldEnv;
        return val;
    }
    public ExprEvaluator(Context context, Environment env) {
        this.context = context;
        this.env = env;
    }

    //
    //  ExpressionVisitor
    //

    public void visitExprLiteral(ExprLiteral e) {
        /* FIXME: It's quite ugly to defer to the ast node for evaluation. The reason is to be able to
           efficiently cache the value and associate it with the ast node, so it does not have to be
           re-created over and over. Better way? Keep local cache as a map in the context? */
        value = e.value(context);
    }

    public void visitExprMap(ExprMap e) {
    	GeneratorFilter [] gs = e.getGenerators();
    	if (gs == null || gs.length == 0) {
            Map m = new HashMap();
            Expression [][] mappings = e.getMappings();
            for (int i = 0; i < mappings.length; i++) {
                Object k = evaluate(mappings[i][0]);
                Object v = evaluate(mappings[i][1]);
                m.put(k, v);
            }
            value = context.createMap(m);
    	} else {
    		Generator g = new Seed(getEnvironment());
    		for (int i = 0; i < gs.length; i++) {
    			g = new CollectionGenerator(g, gs[i].getCollectionExpr(), context, GeneratorCollectionVar);
    			Decl [] ds = gs[i].getVariables();
    			for (int j = 0; j < ds.length; j++) {
    				g = new VariableGenerator(g, context, ds[j].getName(), GeneratorCollectionVar);
    			}
    			Expression [] filters = gs[i].getFilters();
    			if (filters != null) {
        			for (int j = 0; j < filters.length; j++) {
        				g = new Filter(g, filters[j], context);
        			}    				
    			}
    		}
            Map m = new HashMap();
            Expression [][] mappings = e.getMappings();
    		Environment env = g.next();
    		while (env != null) {
    	        for (int i = 0; i < mappings.length; i++) {
    	            Object k = evaluate(mappings[i][0]);
    	            Object v = evaluate(mappings[i][1]);
    	            m.put(k, v);
    	        }
    			env = g.next();
    		}
    		value = context.createMap(m);
    	}
    }

    public void visitExprProc(ExprProc e) {
        value = context.createProcedure(new ProcProcedure(context, env, e));
    }

    public void visitExprSet(ExprSet e) {
    	GeneratorFilter [] gs = e.getGenerators();
    	if (gs == null || gs.length == 0) {
            Set s = new HashSet();
            Expression [] exprs = e.getElements();
            for (int i = 0; i < exprs.length; i++) {
                Expression expr = exprs[i];
                s.add(evaluate(expr));
            }
            value = context.createSet(s);
    	} else {
    		Generator g = new Seed(getEnvironment());
    		for (int i = 0; i < gs.length; i++) {
    			g = new CollectionGenerator(g, gs[i].getCollectionExpr(), context, GeneratorCollectionVar);
    			Decl [] ds = gs[i].getVariables();
    			for (int j = 0; j < ds.length; j++) {
    				g = new VariableGenerator(g, context, ds[j].getName(), GeneratorCollectionVar);
    			}
    			Expression [] filters = gs[i].getFilters();
    			if (filters != null) {
        			for (int j = 0; j < filters.length; j++) {
        				g = new Filter(g, filters[j], context);
        			}    				
    			}
    		}
            Set s = new HashSet();
    		Expression [] exprs = e.getElements();
    		Environment env = g.next();
    		while (env != null) {
        		for (int i = 0; i < exprs.length; i++) {
        			Expression expr = exprs[i];
        			s.add(evaluate(expr, env));
        		}
    			env = g.next();
    		}
    		value = context.createSet(s);
    	}
    }

    public void visitExprVariable(ExprVariable e) {
        value = env.get(e.getName());
    }

    public void visitExprApplication(ExprApplication e) {
        Object f = evaluate(e.getFunction());

        if (context.isFunction(f)) {
            Expression [] argExprs = e.getArgs();
            // TODO: this is really, really inefficient---we probably want to provide a faster
            // implementation at some point
            Object [] args = new Object[argExprs.length];
            for (int i = 0; i < argExprs.length; i++) {
                args[i] = evaluate(argExprs[i]);
            }
            value = context.applyFunction(f, args);
        } else { // must be a map
            if (e.getArgs().length != 1)
                throw new InterpreterException("Maps require only one argument, got " + e.getArgs().length + " (" + f + ").");
            value = context.applyMap(f, evaluate(e.getArgs()[0]));
        }
    }

    //FIXME i don't know wtf i'm doing.
    public void visitExprEntry(ExprEntry e) {

        value = context.selectField(evaluate(e.getEnclosingExpr()), e.getName());
    }

    public void visitExprIf(ExprIf e) {
    	final ExprIf expr = e;
        e.getCondition().accept(this);
        value = context.cond(value, 
        		new Context.Thunk() {
        	        public Object value() {
        		        return evaluate(expr.getThenExpr());
        	        }
                }, 
                new Context.Thunk() {
                	public Object value() {
                		return evaluate(expr.getElseExpr());
                	}
                }
        	);
    }

    public void visitExprIndexer(ExprIndexer e) {
        Object structure = evaluate(e.getStructure());
        Expression [] location = e.getLocation();
        Object [] locValues = new Object[location.length];
        for (int i = 0; i < locValues.length; i++)
            locValues[i] = evaluate(location[i]);
        value = context.getLocation(structure, locValues);
    }

    public void visitExprLambda(ExprLambda e) {
        value = context.createFunction(new LambdaFunction(context, env, e));
    }

    public void visitExprLet(ExprLet e) {
        Environment env = context.newEnvironmentFrame(getEnvironment());
        Decl [] decls = e.getDecls();
        for (int i = 0; i < decls.length; i++) {
            Decl d = decls[i];
            if (d.getInitialValue() == null) {
                env.bind(d.getName(), getContext().createNull());
            } else {
                env.bind(d.getName(), evaluateSynced(d.getInitialValue(), env));
            }
        }
        value = evaluateSynced(e.getBody(), env);
    }

    public void visitExprList(ExprList e) {
    	GeneratorCallback cb = null; // new PrintGeneratorCallback();

    	GeneratorFilter [] gs = e.getGenerators();
    	if (gs == null || gs.length == 0) {
    		List al = new ArrayList();
    		Expression [] exprs = e.getElements();
    		for (int i = 0; i < exprs.length; i++) {
    			Expression expr = exprs[i];
    			al.add(evaluate(expr));
    		}
    		value = context.createList(al);
    	} else {
    		Generator g = new Seed(getEnvironment(), cb);
    		for (int i = 0; i < gs.length; i++) {
    			g = new CollectionGenerator(g, gs[i].getCollectionExpr(), context, GeneratorCollectionVar, cb);
    			Decl [] ds = gs[i].getVariables();
    			for (int j = 0; j < ds.length; j++) {
    				g = new VariableGenerator(g, context, ds[j].getName(), GeneratorCollectionVar, cb);
    			}
    			Expression [] filters = gs[i].getFilters();
    			if (filters != null) {
        			for (int j = 0; j < filters.length; j++) {
        				g = new Filter(g, filters[j], context, cb);
        			}    				
    			}
    		}
    		List al = new ArrayList();
    		Expression [] exprs = e.getElements();
    		Environment env = g.next();
    		while (env != null) {
        		for (int i = 0; i < exprs.length; i++) {
        			Expression expr = exprs[i];
        			al.add(evaluate(expr, env));
        		}
    			env = g.next();
    		}
    		value = context.createList(al);
    	}
    }
    
    private final static String GeneratorCollectionVar = "$generator$collection$";

    //
    //  protected interface
    //

    protected Environment getEnvironment() {
        return env;
    }

    protected Environment setEnvironment(Environment e) {
        Environment oldEnv = env;
        env = e;
        return oldEnv;
    }

    protected Context     getContext() {
        return context;
    }

    protected synchronized Object evaluateSynced(final Expression expr, final Environment tempEnv) {
        final Environment oldEnv = setEnvironment(tempEnv);
        final Object val = evaluate(expr);
        env = oldEnv;
        return val;
    }

    private  Context     context;
    private  Environment env;

    private  Object      value;
}
