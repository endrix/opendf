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

import net.sf.opendf.cal.ast.Decl;
import net.sf.opendf.cal.ast.Expression;
import net.sf.opendf.cal.ast.Statement;
import net.sf.opendf.cal.ast.StatementVisitor;
import net.sf.opendf.cal.ast.StmtAssignment;
import net.sf.opendf.cal.ast.StmtBlock;
import net.sf.opendf.cal.ast.StmtCall;
import net.sf.opendf.cal.ast.StmtForeach;
import net.sf.opendf.cal.ast.StmtIf;
import net.sf.opendf.cal.ast.StmtWhile;
import net.sf.opendf.cal.interpreter.environment.Environment;

/**
 * The statement evaluator interprets statements and potentially modifies the assignment of values to variables
 * in the environment. It extends the expression evaluator, which it uses to interpret expressions occurring
 * inside the statements.
 *
 * @deprecated Replacing this class with new interpreter.
 *
 * @author Jörn W. Janneck <janneck@eecs.berkeley.edu>
 * @see Statement
 * @see ExprEvaluator
 * @see Context
 * @see Environment
 */

public class StmtEvaluator extends ExprEvaluator implements StatementVisitor {

    //
    //  StmtEvaluator
    //

    /**
     * Evaluate a statement in the current environment (originally the one passed into the evaluator
     * upon construction). This can modify the value assignments in the environment.
     *
     * @param s The statement.
     */

    public void  evaluate(Statement s) {
        s.accept(this);
    }

    public StmtEvaluator(Context context, Environment env) {
        super(context, env);
    }

    //
    //  StmtVisitor
    //

    public void visitStmtAssignment(StmtAssignment s) {
        Object v = evaluate(s.getVal());
        Expression [] loc = s.getLocation();
        String field = s.getField();
        if (loc != null) {
            Object [] locVals = new Object[loc.length];
            for (int i = 0; i < locVals.length; i++) {
                locVals[i] = evaluate(loc[i]);
            }
            getEnvironment().set(s.getVar(), locVals, v);
        } else if (field != null) {
        	getContext().modifyField(getEnvironment().get(s.getVar()), field, v);
        } else {
            getEnvironment().set(s.getVar(), v);
        }
    }

    public void visitStmtBlock(StmtBlock s) {
        Environment env = this.getEnvironment();
        if (s.hasLocalDecls()) {
            Environment local = getContext().newEnvironmentFrame(env);
            Decl [] decls = s.getDecls();
            for (int i = 0; i < decls.length; i++) {
                Expression e = decls[i].getInitialValue();
                Object val = (e == null) ? getContext().createNull() : evaluateSynced(e, local);
                local.bind(decls[i].getName(), val);
            }
            setEnvironment(local);
        }
        Statement [] statements = s.getStatements();
        for (int i = 0; i < statements.length; i++) {
            this.evaluate(statements[i]);
        }
        setEnvironment(env);
    }

    public void visitStmtIf(StmtIf s) {
        Object condition = evaluate(s.getCondition());
        if (getContext().booleanValue(condition)) {
            evaluate(s.getThenBranch());
        } else {
            if (s.getElseBranch() != null)
                evaluate(s.getElseBranch());
        }
    }

    public void visitStmtCall(StmtCall s) {
        Object procedure = evaluate(s.getProcedure());
        Object [] args = new Object [s.getArgs().length];
        for (int i = 0; i < args.length; i++) {
            args[i] = evaluate(s.getArgs()[i]);
        }
        getContext().callProcedure(procedure, args);
    }

    public void visitStmtWhile(StmtWhile s) {
        Expression condition = s.getCondition();
        Statement body = s.getBody();
        Object val = evaluate(condition);
        while (getContext().booleanValue(val)) {
            evaluate(body);
            val = evaluate(condition);
        }
    }

    public void visitStmtForeach(StmtForeach s) {
    	// THIS CLASS IS DEPRECATED
	}



    //
    //  protected
    //

    protected synchronized void  evaluate(Statement s, Environment e) {
        Environment oldEnv = setEnvironment(e);
        evaluate(s);
        setEnvironment(oldEnv);
    }

}
