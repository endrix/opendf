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

package net.sf.opendf.cal.i2.environment;

import net.sf.opendf.cal.ast.Expression;
import net.sf.opendf.cal.i2.Environment;
import net.sf.opendf.cal.i2.Evaluator;
import net.sf.opendf.cal.i2.ObjectSink;
import net.sf.opendf.cal.i2.types.Type;

/**
 * A simple thunk encapsulates an expression, together with its context and an environment. Bound to a
 * variable, it defers the evaluation of that expression until the variable is referenced.
 */

public class Thunk implements Environment.VariableContainer, ObjectSink {
	
	public Object value() {
        if (value == this) {
            evaluator.evaluate(expr, env, this);
            expr = null;            // release ref to expr
        }
        return value;
    }

    public void freeze() {
        if (value == this) {
            evaluator.evaluate(expr, env, this);
            expr = null;            // release ref to expr
        }
    }
    
    public void putObject(Object value) {
    	this.value = value;
    	this.type = type;
    }
    
    public Thunk(final Expression expr, final Evaluator evaluator, final Environment env) {
        this.expr = expr;
        this.evaluator = evaluator;
        this.env = env;
        value = this;
        type = null;
    }

    private Expression 		expr;
    private Environment 	env;
    private Evaluator 		evaluator;

    private Object 			value;
    private Type 			type;
}