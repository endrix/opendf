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

package net.sf.caltrop.hades.cal_i2;

import java.util.Map;
import java.util.Set;

import net.sf.caltrop.cal.i2.Configuration;
import net.sf.caltrop.cal.i2.InterpreterException;
import net.sf.caltrop.cal.i2.ObjectSink;
import net.sf.caltrop.cal.i2.Type;
import net.sf.caltrop.cal.i2.UndefinedInterpreterException;
import net.sf.caltrop.cal.i2.UndefinedVariableException;
import net.sf.caltrop.cal.i2.environment.AbstractEnvironment;
import net.sf.caltrop.cal.i2.Environment;


/**
 * This class wraps a Map for use inside the Cal interpreter. The complete Elan 
 * environment appears as the local frame inside the Cal environment. 
 * 
 * @see elan.evaluation.Environment
 * 
 * @author jornj
 */

public class EnvironmentWrapper extends AbstractEnvironment implements Environment {

	//
	//	AbstractEnvironment
	//
	
	@Override
	protected int localGet(Object var, ObjectSink s) {

		if (outsideEnv.keySet().contains(var)) {
			s.putObject(outsideEnv.get(var));
			return (int)NOPOS;
		} else {
			return UNDEFINED;
		}
	}
	
	@Override
	protected void localGetByPos(int pos, ObjectSink s) {
		throw new UndefinedInterpreterException("Positional access undefined for EnvironmentWrapper.");
	}
	
	@Override
	protected Object localGetVariableName(int varPos) {
		throw new UndefinedInterpreterException("Positional access undefined for EnvironmentWrapper.");
	}
	
	@Override
	protected Type localGetVariableType(int varPos) {
		throw new UndefinedInterpreterException("Positional access undefined for EnvironmentWrapper.");
	}
	
	@Override
	protected int localSet(Object var, Object value) {
		return UNDEFINED;
	}
	
	@Override
	protected void localSetByPos(int varPos, Object value) {
		throw new UndefinedInterpreterException("Positional access undefined for EnvironmentWrapper.");
	}
	
	@Override
	public void freezeLocal() {
	}
	
	public EnvironmentWrapper(Map outsideEnv, Environment parent) {
		super(parent);
		
		this.outsideEnv = outsideEnv;
	}
		
	//
	//  data
	//
	
	private Map outsideEnv;
}
