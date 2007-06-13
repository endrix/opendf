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

package net.sf.caltrop.cal.i2;

import net.sf.caltrop.cal.interpreter.ast.Decl;
import net.sf.caltrop.cal.interpreter.ast.Expression;
import net.sf.caltrop.cal.interpreter.ast.Statement;
import net.sf.caltrop.cal.interpreter.ast.StatementVisitor;
import net.sf.caltrop.cal.interpreter.ast.StmtAssignment;
import net.sf.caltrop.cal.interpreter.ast.StmtBlock;
import net.sf.caltrop.cal.interpreter.ast.StmtCall;
import net.sf.caltrop.cal.interpreter.ast.StmtIf;
import net.sf.caltrop.cal.interpreter.ast.StmtWhile;
import net.sf.caltrop.cal.i2.Environment;
import net.sf.caltrop.cal.i2.environment.LazyEnvironmentFrame;

/**
 * The statement evaluator interprets statements and potentially modifies the assignment of values to variables
 * in the environment. It extends the expression evaluator, which it uses to interpret expressions occurring
 * inside the statements.
 *
 * @author Jörn W. Janneck <janneck@eecs.berkeley.edu>
 * @see Statement
 * @see Evaluator
 * @see Environment
 */

public class Executor extends Evaluator implements StatementVisitor {

    //
    //  Executor
    //

    /**
     * Evaluate a statement in the current environment (originally the one passed into the evaluator
     * upon construction). This can modify the value assignments in the environment.
     *
     * @param s The statement.
     */

    public void  execute(Statement s) {
        s.accept(this);
    }

    public void  execute(Statement s, Environment env) {

    	Environment oldEnv = setEnvironment(env);
        s.accept(this);
        setEnvironment(oldEnv);
    }

    public Executor(Configuration configuration, Environment env) {
        super(env, configuration, new BasicOperandStack());
    }

    //
    //  StmtVisitor
    //

    public void visitStmtAssignment(StmtAssignment s) {
        Object v = valueOf(s.getVal());
        Expression [] loc = s.getLocation();
        String field = s.getField();
        if (loc != null) {
            Object [] locVals = new Object[loc.length];
            for (int i = locVals.length - 1; i >= 0; i--) {
                evaluate(loc[i]);
            }
            configuration.assign(v, env, s, locVals.length, stack);
        } else if (field != null) {
        	configuration.assignField(v, env, s, field);
        } else {
        	configuration.assign(v, env, s); 
        }
    } 

    public void visitStmtBlock(StmtBlock s) {
    	Environment localEnv = env;
        if (s.hasLocalDecls()) {
            Decl [] decls = s.getDecls();
            localEnv = new LazyEnvironmentFrame(env, decls, this);
        }
        Statement [] statements = s.getStatements();
        for (int i = 0; i < statements.length; i++) {
            this.execute(statements[i], localEnv);
        }
    }

    public void visitStmtIf(StmtIf s) {
        Object condition = valueOf(s.getCondition());
        if (configuration.booleanValue(condition)) {
            execute(s.getThenBranch());
        } else {
            if (s.getElseBranch() != null)
                execute(s.getElseBranch());
        }
    }

    public void visitStmtCall(StmtCall s) {
        Procedure procedure = (Procedure)valueOf(s.getProcedure());
        Object [] args = new Object [s.getArgs().length];
        for (int i = args.length; i >= 0; i--) {
            evaluate(s.getArgs()[i]);
        }
        procedure.call(args.length, this);
    }

    public void visitStmtWhile(StmtWhile s) {
        Expression condition = s.getCondition();
        Statement body = s.getBody();
        Object val = valueOf(condition);
        while (configuration.booleanValue(val)) {
            execute(body);
            val = valueOf(condition);
        }
    }
}
