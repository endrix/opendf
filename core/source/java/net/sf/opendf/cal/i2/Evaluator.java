package net.sf.opendf.cal.i2;


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
import net.sf.opendf.cal.i2.environment.LazyEnvironmentFrame;
import net.sf.opendf.cal.i2.generator.CollectionGenerator;
import net.sf.opendf.cal.i2.generator.Filter;
import net.sf.opendf.cal.i2.generator.Generator;
import net.sf.opendf.cal.i2.generator.Seed;
import net.sf.opendf.cal.i2.generator.VariableGenerator;

public class Evaluator implements ExpressionVisitor, OperandStack, ObjectSink {

    //
    //  Evaluator
    //

    /**
     * Evaluate the expression and place its value on the stack.
     * 
     * @param e The expression.
     */
	
	public Object valueOf(final Expression e, Environment env) {
		evaluate(e, env);
		return stack.pop();
	}
	
	public Object valueOf(final Expression e) {
		evaluate(e);
		return stack.pop();
	}

    public void evaluate(final Expression e) {
        e.accept(this);
    }
    
    public void evaluate(final Expression e, Environment env) {
    	Environment oldEnv = this.env;
    	this.env = env;
    	evaluate(e);
    	this.env = oldEnv;
    }

    public void evaluate(final Expression e, Environment env, ObjectSink s) {
    	evaluate(e, env);
    	s.putObject(stack.pop());
    }
    
    public OperandStack  getStack() {
    	return stack;
    }
    
    public Configuration  getConfiguration() {
    	return configuration;
    }
    
    public Environment  getEnvironment() {
    	return env;
    }

    protected Environment  setEnvironment(Environment env) {
    	Environment oldEnv = this.env;
    	this.env = env;
    	return oldEnv;
    }
    
    public Evaluator(Environment env, Configuration os) {
    	this (env, os, new BasicOperandStack());
    }
    
    protected Evaluator(Environment env, Configuration os, OperandStack stack) {
        this.env = env;
        this.configuration = os;
        this.stack = stack;
    }
    
    protected Object  tosValue() {
    	return stack.getValue(0);
    }
    
    //
    //  ExpressionVisitor
    //

    public void visitExprLiteral(ExprLiteral e) {
    	
    	e.pushValue(configuration, stack);
    }

    public void visitExprMap(ExprMap e) {
    	GeneratorFilter [] gs = e.getGenerators();
    	if (gs == null || gs.length == 0) {
            Object m = configuration.createEmptyMap();
            Expression [][] mappings = e.getMappings();
            for (int i = 0; i < mappings.length; i++) {
                configuration.addMapping(m, valueOf(mappings[i][0]), valueOf(mappings[i][1]));
            }
            stack.push(m);
    	} else {
    		Generator g = new Seed(env);
    		for (int i = 0; i < gs.length; i++) {
    			g = new CollectionGenerator(g, gs[i].getCollectionExpr(), this.configuration, GeneratorCollectionVar);
    			Decl [] ds = gs[i].getVariables();
    			for (int j = 0; j < ds.length; j++) {
    				g = new VariableGenerator(g, ds[j].getName(), GeneratorCollectionVar);
    			}
    			Expression [] filters = gs[i].getFilters();
    			if (filters != null) {
        			for (int j = 0; j < filters.length; j++) {
        				g = new Filter(g, filters[j], this.configuration);
        			}    				
    			}
    		}
    		Object m = configuration.createEmptyMap();
            Expression [][] mappings = e.getMappings();
    		Environment env = g.next();
    		while (env != null) {
    	        for (int i = 0; i < mappings.length; i++) {
                    configuration.addMapping(m, valueOf(mappings[i][0], env), valueOf(mappings[i][1], env));
    	        }
    			env = g.next();
    		}
    		stack.push(m);
    	}
    }

    public void visitExprProc(ExprProc e) {
    	stack.push(new ProcProcedure(env, e));
    }

    public void visitExprSet(ExprSet e) {
    	GeneratorFilter [] gs = e.getGenerators();
    	if (gs == null || gs.length == 0) {
            Object s = configuration.createEmptySet();
            Expression [] exprs = e.getElements();
            for (Expression expr : exprs) {
                configuration.addSetElement(s, valueOf(expr));
            }
            push(s);
    	} else {
    		Generator g = new Seed(env);
    		for (int i = 0; i < gs.length; i++) {
    			g = new CollectionGenerator(g, gs[i].getCollectionExpr(), configuration, GeneratorCollectionVar);
    			Decl [] ds = gs[i].getVariables();
    			for (int j = 0; j < ds.length; j++) {
     				g = new VariableGenerator(g, ds[j].getName(), GeneratorCollectionVar);
    			}
    			Expression [] filters = gs[i].getFilters();
    			if (filters != null) {
        			for (int j = 0; j < filters.length; j++) {
        				g = new Filter(g, filters[j], configuration);
        			}    				
    			}
    		}
            Object s = configuration.createEmptySet();
    		Expression [] exprs = e.getElements();
    		Environment env = g.next();
    		while (env != null) {
        		for (Expression expr : exprs) {
        			configuration.addSetElement(s, valueOf(expr, env));
        		}
    			env = g.next();
    		}
    		push(s);
    	}
    }

    public void visitExprVariable(ExprVariable e) {
    	push(configuration.lookupVariable(env, e));
    }

    public void visitExprApplication(ExprApplication e) {
    	Function f = (Function)valueOf(e.getFunction());

    	Expression [] argExprs = e.getArgs();
    	for (int i = argExprs.length - 1; i >= 0; i--) {
    		evaluate(argExprs[i], env);
    	}
    	f.apply(argExprs.length, this);
    }

    public void visitExprEntry(ExprEntry e) {

        push(configuration.selectField(valueOf(e.getEnclosingExpr()), e.getName()));
    }

    public void visitExprIf(ExprIf e) {

    	push(configuration.cond(this, e));
    }

    public void visitExprIndexer(ExprIndexer e) {
    	
        Object structure = valueOf(e.getStructure());

        Expression [] location = e.getLocation();
        for (int i = location.length - 1; i >= 0; i--)
        	evaluate(location[i]);
        configuration.indexInto(structure, location.length, stack);
    }

    public void visitExprLambda(ExprLambda e) {
        push(new LambdaFunction(env, e));
    }

    public void visitExprLet(ExprLet e) {
        Decl [] decls = e.getDecls();
        if (decls.length == 0) {
        	evaluate(e.getBody());
        } else {
        	Environment env2 = new LazyEnvironmentFrame(env, decls, this);
        	evaluate(e.getBody(), env2);
        }
    }

    public void visitExprList(ExprList e) {
    	GeneratorFilter [] gs = e.getGenerators();
    	if (gs == null || gs.length == 0) {
    		Object al = configuration.createEmptyList();
    		Expression [] exprs = e.getElements();
    		for (Expression expr : exprs) {
    			configuration.addListElement(al, valueOf(expr));
    		}
    		push(al);
    	} else {
    		Generator g = new Seed(env);
    		for (int i = 0; i < gs.length; i++) {
    			g = new CollectionGenerator(g, gs[i].getCollectionExpr(), configuration, GeneratorCollectionVar);
    			Decl [] ds = gs[i].getVariables();
    			for (int j = 0; j < ds.length; j++) {
    				g = new VariableGenerator(g, ds[j].getName(), GeneratorCollectionVar);
    			}
    			Expression [] filters = gs[i].getFilters();
    			if (filters != null) {
        			for (int j = 0; j < filters.length; j++) {
        				g = new Filter(g, filters[j], configuration);
        			}    				
    			}
    		}
    		Object al = configuration.createEmptyList();
    		Expression [] exprs = e.getElements();
    		Environment env = g.next();
    		while (env != null) {
    			for (Expression expr : exprs) {
    				configuration.addListElement(al, valueOf(expr, env));
    			}
    			env = g.next();
    		}
    		push(al);
    	}
    }
    
    private final static String GeneratorCollectionVar = "$generator$collection$";
    
    //
    //  OperandStack
    //
    
    public Object getValue(int n) {
    	return stack.getValue(n);
    }
    
    public Object pop() {
    	return stack.pop();
    }
    
    public void pop(int n) {
    	stack.pop(n);
    }
    
    public void push(Object v) {
    	stack.push(v);
    }
    
    public void replaceWithResult(int n, Object v) {
    	stack.replaceWithResult(n, v);
    }
    
    public int size() {
    	return stack.size();
    }
    
	//
	//  ObjectSink 
	//
	
	public void  putObject(Object value) {
		stack.push(value);
	}
	
    //
    //  data
    //

    protected Environment		env;
    protected Configuration  	configuration;
    protected OperandStack 		stack;
}
