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

package net.sf.caltrop.cal.interpreter.generator;

import java.util.Collection;
import java.util.Iterator;

import net.sf.caltrop.cal.interpreter.Context;
import net.sf.caltrop.cal.interpreter.environment.Environment;
import net.sf.caltrop.cal.interpreter.environment.SingleEntryEnvironment;


public class VariableGenerator implements Generator {
	
	public Environment next() {		
		while (true) {
			if (iterator == null) {
				Environment env = previous.next();
				if (env == null) {
					cbFinal();
					return null;
				}
				Object cval = env.get(collectionVar);
				Collection c = context.getCollection(cval);
				cbStart(c);
				iterator = c.iterator();
				localEnv = new SingleEntryEnvironment(env, context, localVar, null);
			} 
			if (iterator.hasNext()) {
				Object val = iterator.next();
				
				// NOTE: This modifies a visible variable. 
				//       Perhaps we should create a new environment each
				//       time.
				localEnv.set(localVar, val);  
				cbNext(val);
				return localEnv;
			} else {
				cbEnd();
				iterator = null;
			}
		}
	}
	
	public VariableGenerator(Generator previous, Context context, String localVar, String collectionVar, Callback cb) {
		this.previous = previous;
		this.context = context;
		this.localVar = localVar;
		this.collectionVar = collectionVar;
		this.cb = cb;
		
		this.localEnv = null;
		this.iterator = null;
	}
	
	public VariableGenerator(Generator previous, Context context, String localVar, String collectionVar) {
		this (previous, context, localVar, collectionVar, null);
	}
	
	public interface Callback {
		void iterStart(Collection c, Environment localEnv, String localVar, String collectionVar);

		void iterNext(Object val, Environment localEnv, String localVar, String collectionVar);
		
		void iterEnd(String localVar, String collectionVar);

		void iterFinal(String localVar, String collectionVar);
	}
	
	private Generator	previous;
	private Context		context;
	private String		collectionVar;
	private String      localVar;
	private Callback 	cb;

	private Iterator 	iterator;
	private Environment localEnv;
	
	private void  cbStart(Collection c) {
		if (cb != null) cb.iterStart(c, localEnv, localVar, collectionVar);
	}

	private void  cbNext(Object val) {
		if (cb != null) cb.iterNext(val, localEnv, localVar, collectionVar);
	}
	
	private void  cbEnd() {
		if (cb != null) cb.iterEnd(localVar, collectionVar);
	}

	private void  cbFinal() {
		if (cb != null) cb.iterFinal(localVar, collectionVar);
	}

}
