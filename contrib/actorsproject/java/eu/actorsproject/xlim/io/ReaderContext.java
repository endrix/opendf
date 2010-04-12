/* 
 * Copyright (c) Ericsson AB, 2009
 * Author: Carl von Platen (carl.von.platen@ericsson.com)
 * All rights reserved.
 *
 * License terms:
 *
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met:
 *     * Redistributions of source code must retain the above 
 *       copyright notice, this list of conditions and the 
 *       following disclaimer.
 *     * Redistributions in binary form must reproduce the 
 *       above copyright notice, this list of conditions and 
 *       the following disclaimer in the documentation and/or 
 *       other materials provided with the distribution.
 *     * Neither the name of the copyright holder nor the names 
 *       of its contributors may be used to endorse or promote 
 *       products derived from this software without specific 
 *       prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package eu.actorsproject.xlim.io;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTopLevelPort;


/**
 * Symbol table, which is produced by the XlimReader
 */
public class ReaderContext {
	protected HashMap<String,XlimTopLevelPort> mTopLevelPorts=new HashMap<String,XlimTopLevelPort>();
	protected HashMap<String,XlimTaskModule> mTasks=new HashMap<String,XlimTaskModule>();
	protected HashMap<String,XlimStateVar> mStateVars=new LinkedHashMap<String,XlimStateVar>();
	protected HashMap<String,XlimTypeDef> mTypeDefs=new LinkedHashMap<String,XlimTypeDef>();
	protected HashMap<String,XlimOutputPort> mOutputPorts;
	
	public XlimTopLevelPort getTopLevelPort(String name) {
		return mTopLevelPorts.get(name);
	}
	
	public XlimTaskModule getTask(String name) {
		return mTasks.get(name);
	}
	
	public XlimStateVar getStateVar(String name) {
		return mStateVars.get(name);
	}
	
	public Map<String,XlimStateVar> getOriginalStateVars() {
		return Collections.unmodifiableMap(mStateVars);
	}
	
	public XlimSource getSource(String name) {
		if (mOutputPorts!=null) {
			XlimOutputPort port=mOutputPorts.get(name);
			if (port!=null)
				return port;
		}
		XlimStateVar stateVar=mStateVars.get(name);
		if (stateVar!=null) {
			return stateVar;
		}
		else
			return null;
	}	
	
	public XlimTypeDef getTypeDef(String name) {
		return mTypeDefs.get(name);
	}
}
