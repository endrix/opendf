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

import net.sf.caltrop.cal.i2.environment.EnvironmentFrame;
import net.sf.caltrop.cal.i2.environment.LazyEnvironmentFrame;
import net.sf.caltrop.cal.i2.types.Type;
import net.sf.caltrop.cal.interpreter.ast.Decl;
import net.sf.caltrop.cal.interpreter.ast.ExprLambda;

/**
 * A LambdaFunction is a function object that is created as the result of evaluating
 * a lambda expression.
 *
 * @author Jörn W. Janneck <janneck@eecs.berkeley.edu>
 */

public class LambdaFunction implements Function
{

	public void apply(int n, Evaluator evaluator) {

		String [] params = lambda.getParameters();
		if (params.length != n)
			throw new InterpreterException("Illegal number of arguments. Got "
					+ n + ", expected " + params.length + ".");

		Object [] values = new Object[n];
		Type [] types = new Type[n];

		for (int i = 0; i < n; i++) {
			values[i] = evaluator.getValue(i);
			types[i] = null;  // TYPEFIXME
		}
		evaluator.pop(n);
		
		Environment e = new EnvironmentFrame(this.env, params, values, types);
		
		Decl [] decls = lambda.getDecls();
		
		if (decls != null && decls.length > 0) {
			e = new LazyEnvironmentFrame(e, decls, evaluator);
		}
		
		evaluator.evaluate(this.lambda.getBody(), e);
	}

	public LambdaFunction(Environment env, ExprLambda lambda) {
		this.lambda = lambda;
		this.env = env;
	}

	private ExprLambda  lambda;
	private Environment env;
}
