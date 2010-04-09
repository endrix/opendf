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

package eu.actorsproject.xlim.decision2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
	
	private static Map<XlimTopLevelPort,Integer> sEmptyMap=Collections.emptyMap();
	private static PortSignature sEmptySignature=new PortSignature(sEmptyMap);
	
	public PortSignature() {
		mPortMap=new HashMap<XlimTopLevelPort,Integer>();
	}

	public PortSignature(Map<XlimTopLevelPort,Integer> portMap) {
		mPortMap=portMap;
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
	/**
	 * @return the empty port signature
	 */
	public static PortSignature emptyPortSignature() {
		return sEmptySignature;
	}
	
	public boolean isSubsetOf(PortSignature s) {
		if (mPortMap.size() > s.mPortMap.size())
			return false; // there is at least one entry that's not in s
		else {
			for (Entry<XlimTopLevelPort,Integer> entry: mPortMap.entrySet()) {
				if (s.getPortRate(entry.getKey()) < entry.getValue())
					return false;
			}
			// all entries checked
			return true;
		}
	}
	
	/**
	 * @param s1  a port signature
	 * @param s2  a port signature
	 * @return    the "intersection" of the port signatures, in which a port appears
	 *            if it appears in both signatures with an associated rate that is
	 *            the minimum of the two signatures. 
	 */
	public static PortSignature intersect(PortSignature s1, PortSignature s2) {
		if (s1.equals(s2))
			return s1;
		else {
			HashMap<XlimTopLevelPort,Integer> portMap=new HashMap<XlimTopLevelPort,Integer>();
			boolean s1isSubSet=true;
			boolean s2isSubSet=true;
			
			for (Entry<XlimTopLevelPort,Integer> entry: s1.mPortMap.entrySet()) {
				XlimTopLevelPort port=entry.getKey();
				int portRate2=s2.getPortRate(port);
				if (portRate2!=0) {
					int portRate=entry.getValue();
					if (portRate2<portRate) {
						portRate=portRate2;
						s1isSubSet=false; // s2's port rate is lower
					}
					else if (portRate<portRate2)
						s2isSubSet=false; // s1's port rate is lower
					
					assert(portRate>0);
					portMap.put(port, portRate);
				}
				else
					s1isSubSet=false; // s2's port rate is lower
			}

			if (s1isSubSet)
				return s1;
			else if (s2isSubSet)
				return s2;
			else
				return new PortSignature(portMap);
		}
	}

	/**
	 * @param s1  a port signature
	 * @param s2  a port signature
	 * @return    the "union" of the port signatures, in which a port appears
	 *            if it appears in either of the signatures with an associated 
	 *            rate that is the maximum of the two signatures. 
	 */
	public static PortSignature union(PortSignature s1, PortSignature s2) {
		if (s1.equals(s2))
			return s1;
		else {
			HashMap<XlimTopLevelPort,Integer> portMap=new HashMap<XlimTopLevelPort,Integer>();
			boolean s1isSuperSet=true;
			boolean s2isSuperSet=true;
			
			// First compare the entries of s1 to those of s2
			for (Entry<XlimTopLevelPort,Integer> entry: s1.mPortMap.entrySet()) {
				XlimTopLevelPort port=entry.getKey();
				int portRate=entry.getValue();
				int portRate2=s2.getPortRate(port);
				if (portRate2>portRate) {
					portRate=portRate2;
					s1isSuperSet=false; // s2's port rate is higher
				}
				else if (portRate>portRate2)
					s2isSuperSet=false; // s1's port rate is higer
				
				portMap.put(port, portRate);
			}
			
			// Then check for entries in s2, but not s1
			for (Entry<XlimTopLevelPort,Integer> entry: s2.mPortMap.entrySet()) {
				XlimTopLevelPort port=entry.getKey();
				if (s1.getPortRate(port)==0) {
					portMap.put(port, entry.getValue());
					s1isSuperSet=false; // s2's port rate is higher
				}
			}
			
			if (s1isSuperSet)
				return s1;
			else if (s2isSuperSet)
				return s2;
			else
				return new PortSignature(portMap);
		}
	}
	
	public boolean isEmpty() {
		return mPortMap.isEmpty();
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
	
	public Iterable<PortRatePair> asXmlElements() {
	
		return new Iterable<PortRatePair>() {
			public Iterator<PortRatePair> iterator() {
				return new PortRateIterator();
			}
		};
	}
	
	private class PortRateIterator implements Iterator<PortRatePair> {
	
		private Iterator<Map.Entry<XlimTopLevelPort, Integer>> mIterator=
			mPortMap.entrySet().iterator();

		public boolean hasNext() {
			return mIterator.hasNext();
		}

		public PortRatePair next() {
			Map.Entry<XlimTopLevelPort, Integer> entry=mIterator.next();
			return new PortRatePair(entry.getKey(), entry.getValue());
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}	
}
