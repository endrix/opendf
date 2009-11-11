/* 
BEGINCOPYRIGHT X,ETH
	
	Copyright (c) 1999, Computer Engineering and Communication Networks Lab (TIK)
 	                    Swiss Federal Institute of Technology (ETH) Zurich, Switzerland	
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

package net.sf.opendf.hades.des.schedule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The AbstractScheduler class collects some administrative functionality that is common
 * to all schedulers, viz. the registration and invocation of postfire handlers and
 * simulation finalizers.
 * 
 * @see PostfireHandler
 * @see SimulationFinalizer 
 * 
 * @author jornj
 */
abstract public class AbstractScheduler implements Scheduler {

	public void addPostfireHandler(PostfireHandler ph) {
		postfireHandlers.add(ph);
	}

	public void removePostfireHandler(PostfireHandler ph) {
		postfireHandlers.remove(ph);
	}

	protected void postfire() {
		if (postfireHandlers.isEmpty())
			return;
	
		for (Iterator i = postfireHandlers.iterator(); i.hasNext(); ) {
			PostfireHandler ph = (PostfireHandler)i.next();
			ph.postfire();
		}
		postfireHandlers.clear();
	}

	public void registerSimulationFinalizer(SimulationFinalizer sf) {
		simulationFinalizers.add(sf);
	}

	public void unregisterSimulationFinalizer(SimulationFinalizer sf) {
		simulationFinalizers.remove(sf);
	}

	public void finalizeSimulation() {
		for (Iterator i = simulationFinalizers.iterator(); i.hasNext(); ) {
			SimulationFinalizer sf = (SimulationFinalizer)i.next();
			sf.finalizeSimulation();
		}
	}

	public Object getProperty(Object key) {
		if (properties == null)
			return null;
		return properties.get(key);
	}
	
	public Object setProperty(Object key,Object value) {
		if (properties == null)
			properties = new HashMap();
		return properties.put(key, value);
	}
	

	private Set postfireHandlers = new HashSet();

	private List simulationFinalizers = new ArrayList();

	private Map  properties = null;
}
