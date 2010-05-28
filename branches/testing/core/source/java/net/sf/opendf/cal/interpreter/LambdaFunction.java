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
import net.sf.opendf.cal.ast.ExprLambda;
import net.sf.opendf.cal.interpreter.environment.Environment;

/**
 * A LambdaFunction is a function object that is created as the result of evaluating
 * a lambda expression.
 *
 * @author Jorn W. Janneck <janneck@eecs.berkeley.edu>
 */

public class LambdaFunction extends ExprEvaluator
                            implements Function
                             {

    public Object apply(Object[] args) {
        String [] params = lambda.getParameters();
        if (params.length != args.length)
            throw new InterpreterException("Illegal number of arguments. Got "
                                          + args.length + ", expected " + params.length + ".");
        Environment e = getContext().newEnvironmentFrame(getEnvironment());
        for (int i = 0; i < args.length; i++) {
            e.bind(params[i], args[i]);
        }
        Decl [] decls = lambda.getDecls();
        for (int i = 0; i < decls.length; i++) {
            Decl d = decls[i];
            if (d.getInitialValue() == null) {
                e.bind(d.getName(), getContext().createNull());
            } else {
                e.bind(d.getName(), evaluateSynced(d.getInitialValue(), e));
            }
        }
        return evaluateSynced(lambda.getBody(), e);
    }

    public int arity() {
        return lambda.getParameters().length;
    }

    public LambdaFunction(Context context, Environment env, ExprLambda lambda) {
        super(context, env);
        this.lambda = lambda;
    }

    private ExprLambda  lambda;
}