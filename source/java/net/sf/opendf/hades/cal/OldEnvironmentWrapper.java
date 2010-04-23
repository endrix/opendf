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

package net.sf.opendf.hades.cal;

import java.util.Map;
import java.util.Set;

import net.sf.opendf.cal.i2.Configuration;
import net.sf.opendf.cal.i2.Environment;
import net.sf.opendf.cal.i2.ObjectSink;
import net.sf.opendf.cal.i2.environment.AbstractEnvironment;
import net.sf.opendf.cal.i2.types.Type;
import net.sf.opendf.cal.interpreter.environment.HashEnvironment;


/**
 * This class wraps a Map for use inside the Cal interpreter. The complete Elan 
 * environment appears as the local frame inside the Cal environment. 
 * 
 * @see elan.evaluation.Environment
 * 
 * @author jornj
 */

public class OldEnvironmentWrapper extends AbstractEnvironment {

	
	@Override
	protected int localGet(Object var, ObjectSink s) {
		if (outsideEnv.keySet().contains(var)) {
			s.putObject(configuration.convertJavaResult(outsideEnv.get(var)));
			return (int)NOPOS;
		} else {
			return (int)UNDEFINED;
		}
	}

	@Override
	protected void localGetByPos(int pos, ObjectSink s) {
	}

	@Override
	protected Object localGetVariableName(int varPos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Type localGetVariableType(int varPos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected int localSet(Object var, Object value) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected void localSetByPos(int varPos, Object value) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void freezeLocal() {
		// TODO Auto-generated method stub
		
	}

	
	public OldEnvironmentWrapper(Map outsideEnv, Environment parent, Configuration configuration) {
		super(parent);
		
		this.outsideEnv = outsideEnv;
		this.configuration = configuration;
	}
	
	public OldEnvironmentWrapper(Map outsideEnv, Configuration configuration) {
		this (outsideEnv, null, configuration);
	}
	
	//
	//  data
	//
	
	private Map outsideEnv;
	private Configuration configuration;
}
