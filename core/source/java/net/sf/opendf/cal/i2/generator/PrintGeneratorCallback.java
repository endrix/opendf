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

import java.util.Collection;

import net.sf.caltrop.cal.ast.Expression;
import net.sf.caltrop.cal.i2.Environment;


public class PrintGeneratorCallback implements GeneratorCallback {

	public void collectionEnd(String collectionVar, Expression expr) {
		System.out.println("end    collection: " + collectionVar);
	}

	public void collectionStart(String collectionVar, Expression expr,
			Object val, Environment env) {
		System.out.println("start  collection: " + collectionVar + " = " + val);
	}

	public void filterAccept(Expression expr, Environment env) {
		System.out.println("accept filter");
	}

	public void filterFinal() {
		System.out.println("final  filter");
	}

	public void filterReject(Expression expr, Environment env) {
		System.out.println("reject filter");
	}

	public void iterFinal(String localVar, String collectionVar) {
		System.out.println("final  iter: " + localVar + "(in " + collectionVar + ")");
	}

	public void iterEnd(String localVar, String collectionVar) {
		System.out.println("end    iter: " + localVar + "(in " + collectionVar + ")");
	}

	public void iterNext(Object val, Environment localEnv, String localVar,
			String collectionVar) {
		System.out.println("next   iter: " + localVar + " = " + val);
	}

	public void iterStart(Collection c, Environment localEnv,
			String localVar, String collectionVar) {
		System.out.println("start  iter: " + localVar + "(in " + collectionVar + " = " + c + ")");
	}

	public void seedFinal() {
		System.out.println("final  seed");		
	}

	public void seedStart() {
		System.out.println("start  seed");		
	}

	public void collectionFinal(String collectionVar, Expression expr) {
		System.out.println("final  collection: " + collectionVar);
	}

}
