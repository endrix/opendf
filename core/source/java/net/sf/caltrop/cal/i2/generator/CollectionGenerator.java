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

import net.sf.caltrop.cal.i2.Configuration;
import net.sf.caltrop.cal.i2.Environment;
import net.sf.caltrop.cal.i2.Evaluator;
import net.sf.caltrop.cal.i2.environment.SingleEntryEnvironment;
import net.sf.caltrop.cal.interpreter.ast.Expression;

public class CollectionGenerator implements Generator {

	public Environment next() {
		cbEnd();
		Environment parentEnv = previous.next();
		if (parentEnv == null) {
			cbFinal();
			return null;			
		}

		evaluator.evaluate(expr, parentEnv);
		Object cval = evaluator.getValue(0);
		evaluator.pop();
		cbCollection(cval, parentEnv);
		return new SingleEntryEnvironment(parentEnv, collectionVar, cval, null);  // TYPEFIXME
	}
	
	public CollectionGenerator(Generator previous, Expression expr, Configuration configuration, String collectionVar, Callback cb) {
		this.previous = previous;
		this.expr = expr;
		this.collectionVar = collectionVar;
		this.cb = cb;	
		this.evaluator = new Evaluator(null, configuration);
	}
	
	public CollectionGenerator(Generator previous, Expression expr, Configuration configuration, String collectionVar) {
		this (previous, expr, configuration, collectionVar, null);
	}
	
	public interface Callback {
		void collectionStart(String collectionVar, Expression expr, Object val, Environment env);

		void collectionEnd(String collectionVar, Expression expr);
		
		void collectionFinal(String collectionVar, Expression expr);
	}
		
	//private 
	private Generator 	previous;
	private Expression 	expr;
	private String     	collectionVar;
	private Callback 	cb;
	
	private Evaluator  evaluator;
	
	private void  cbCollection(Object val, Environment env) {
		if (cb != null) cb.collectionStart(collectionVar, expr, val, env);
	}
	
	private void  cbEnd() {
		if (isFirst) {
			isFirst = false;
		} else {
			if (cb != null) cb.collectionEnd(collectionVar, expr);
		}
	}
	
	private boolean isFirst = true;

	private void  cbFinal() {
		if (cb != null) cb.collectionFinal(collectionVar, expr);
	}
}
