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

package net.sf.caltrop.cal.i2.generator;

import net.sf.caltrop.cal.i2.Evaluator;
import net.sf.caltrop.cal.i2.ObjectSink;
import net.sf.caltrop.cal.i2.Configuration;
import net.sf.caltrop.cal.i2.Type;
import net.sf.caltrop.cal.interpreter.ast.Expression;
import net.sf.caltrop.cal.i2.Environment;

public class Filter implements Generator {

	public Environment next() {
		while (true) {
			Environment env = previous.next();
			if (env == null) {
				cbFilterFinal();
				return null;
			}
			evaluator.evaluate(expr, env, objectSink);
			if (configuration.booleanValue(tmpValue)) {
				cbFilterAccept(env);
				return env;
			}
			cbFilterReject(env);
		}
	}
	
	public Filter(Generator previous, Expression expr, Configuration configuration, Callback cb) {
		this.previous = previous;
		this.expr = expr;
		this.cb = cb;
		this.configuration = configuration;
		evaluator = new Evaluator(null, configuration);
	}
	
	public Filter(Generator previous, Expression expr, Configuration configuration) {
		this (previous, expr, configuration, null);
	}
	
	public interface  Callback {
		void filterAccept(Expression expr, Environment env);

		void filterReject(Expression expr, Environment env);

		void filterFinal();		
	}
	
	private Object  tmpValue;
	
	private ObjectSink objectSink = new ObjectSink() {
		public void putObject(Object value) {
			tmpValue = value;
		}
	};
	
	private Generator 		previous;
	private Expression  	expr;
	private Evaluator		evaluator;
	private Configuration     configuration;
	private Callback cb;
	
	private void  cbFilterAccept(Environment env) {
		if (cb != null) cb.filterAccept(expr, env);
	}

	private void  cbFilterReject(Environment env) {
		if (cb != null) cb.filterReject(expr, env);
	}

	private void  cbFilterFinal() {
		if (cb != null) cb.filterFinal();
	}
}
