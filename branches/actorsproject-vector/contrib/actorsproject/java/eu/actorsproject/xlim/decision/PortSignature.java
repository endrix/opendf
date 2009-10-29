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

package eu.actorsproject.xlim.decision;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import eu.actorsproject.xlim.XlimTopLevelPort;

/**
 * Represents a subtree (of the decision tree), in which all actions
 * have the same consumption/production rates.
 */
public class PortSignature  {
	
	private Map<XlimTopLevelPort,Integer> mPortMap;
	
	public PortSignature() {
		mPortMap=new HashMap<XlimTopLevelPort,Integer>();
	}

	public PortSignature(Map<XlimTopLevelPort,Integer> portMap) {
		mPortMap=portMap;
	}
		
	/**
	 * @param s1  a port signature
	 * @param s2  a port signature
	 * @return    the "intersection" of the port signature, in which a port appears
	 *            if it appears in both signatures with an associated rate that is
	 *            the minimum of the two signatures. 
	 */
	public static PortSignature intersect(PortSignature s1, PortSignature s2) {
		if (s1.equals(s2))
			return s1;
		else {
			HashMap<XlimTopLevelPort,Integer> portMap=new HashMap<XlimTopLevelPort,Integer>();
			
			for (Entry<XlimTopLevelPort,Integer> entry: s1.mPortMap.entrySet()) {
				XlimTopLevelPort port=entry.getKey();
				int portRate=s2.getPortRate(port);
				if (portRate!=0) {
					portRate=Math.min(portRate,entry.getValue());
					assert(portRate>0);
					portMap.put(port, portRate);
				}
			}
			
			return new PortSignature(portMap);
		}
	}

	public boolean isEmpty() {
		return mPortMap.isEmpty();
	}
	
	public Set<XlimTopLevelPort> getPorts() {
		return mPortMap.keySet();
	}
	
	public int getPortRate(XlimTopLevelPort port) {
		Integer rate=mPortMap.get(port);
		if (rate!=null)
			return rate;
		else
			return 0;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o!=null && o instanceof PortSignature)
			return mPortMap.equals(((PortSignature) o).mPortMap);
		else
			return false;
	}
	
	public boolean equals(PortSignature s) {
		return mPortMap.equals(s.mPortMap);
	}
	
	public int hashCode() {
		return mPortMap.hashCode();
	}
	
	@Override
	public String toString() {
		
		String inputs="";
		String inputDelimiter="";
		String outputs="";
		String outputDelimiter="";
		for (Map.Entry<XlimTopLevelPort, Integer> entry: mPortMap.entrySet()) {
			XlimTopLevelPort port=entry.getKey();
			Integer rate=entry.getValue();
			String descr=port.getName();
			if (rate!=1)
				descr+=":"+rate;

			if (port.getDirection()==XlimTopLevelPort.Direction.in) {
				inputs+=inputDelimiter+descr;
				inputDelimiter=",";
			}
			else {
				outputs+=outputDelimiter+descr;
				outputDelimiter=",";
			}
		}
		return inputs+"==>"+outputs;
	}	
}
