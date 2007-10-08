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

package net.sf.caltrop.hades.cal;

import java.util.Map;
import java.util.Set;

import net.sf.caltrop.cal.interpreter.Context;
import net.sf.caltrop.cal.interpreter.InterpreterException;
import net.sf.caltrop.cal.interpreter.UndefinedVariableException;
import net.sf.caltrop.cal.interpreter.environment.AbstractEnvironment;
import net.sf.caltrop.cal.interpreter.environment.Environment;
import net.sf.caltrop.cal.interpreter.environment.HashEnvironment;


/**
 * This class wraps a Map for use inside the Cal interpreter. The complete Elan 
 * environment appears as the local frame inside the Cal environment. 
 * 
 * @see elan.evaluation.Environment
 * 
 * @author jornj
 */

public class OldEnvironmentWrapper extends AbstractEnvironment {
	
	public Object get(Object variable) {
		
		if (outsideEnv.keySet().contains(variable)) {
			return context.fromJavaObject(outsideEnv.get(variable));
		} else if (parent != null) {
			return parent.get(variable);
		} else {
			throw new UndefinedVariableException("Variable: "  + variable);
		}
	}
	
	public void set(Object variable, Object value) {
		
		try {
			outsideEnv.put(variable, value);
		} catch (InterpreterException e) {
			parent.set(variable, value);
		}
	}
	
	public void bind(Object variable, Object value) {
		
		outsideEnv.put(variable, value);
	}
	
	public Set localVars() {
		
		return outsideEnv.keySet();
	}
	
	public Environment newFrame(Environment parent) {
		
		return parent.newFrame(this);
	}
	
	public void freezeLocal() {
	}
	
	public OldEnvironmentWrapper(Map outsideEnv, Environment parent, Context context) {
		super(parent, context);
		
		this.outsideEnv = outsideEnv;
		this.context = context;
	}
	
	public OldEnvironmentWrapper(Map outsideEnv, Context context) {
		this (outsideEnv, new HashEnvironment(context), context);
	}
	
	//
	//  data
	//
	
	private Map outsideEnv;
	private Context context;
}
