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

import java.util.Iterator;

import eu.actorsproject.xlim.XlimTopLevelPort;

/**
 * An implementation of PortMap, the data structure that is used to 
 * keep track of asserted/refuted token-availability tests. 
 */
class LinkedPortMap implements PortMap {

	private LinkedPortMap mBasedOn;
	private boolean mFirstApperence;
	private int mDepth;
	private AvailabilityTest mTest;

	private static LinkedPortMap sEmpty=new LinkedPortMap();

	public static LinkedPortMap empty() {
		return sEmpty;
	}
	
	private LinkedPortMap() {
		mDepth=0;
		mFirstApperence=true;
	}
	
	private LinkedPortMap(LinkedPortMap basedOn, AvailabilityTest test) {
		assert(test!=null);
		mBasedOn=basedOn;
		mTest=test;
		mFirstApperence=(basedOn.get(test.getPort())==null);
		mDepth=basedOn.mDepth+1;
	}
	
	@Override
	public LinkedPortMap add(AvailabilityTest test) {
		return new LinkedPortMap(this, test);
	}

	@Override
	public AvailabilityTest get(XlimTopLevelPort port) {
		LinkedPortMap portMap=this;
		
		while (portMap.mTest!=null) 
			if (portMap.mTest.getPort()==port)
				return portMap.mTest;
			else
				portMap=portMap.mBasedOn;
		return null;
	}

	private static LinkedPortMap leastCommonAncestor(LinkedPortMap p1, 
			                                         LinkedPortMap p2) {
		while (p1.mDepth>p2.mDepth)
			p1=p1.mBasedOn;
		while (p2.mDepth>p1.mDepth)
			p2=p2.mBasedOn;
		
		// Since at least sEmpty (at depth 0) is common, there is an LCA
		while (p1!=p2) {
			p1=p1.mBasedOn;
			p2=p2.mBasedOn;
		}
		return p1;
	}
	
	/**
	 * Intersects two PortMaps (with respect to the ports) and select
	 * an AvailabilityTest with minimal tokenCount if the PortMaps have
	 * different AvailabilityTests for a particular port.
	 * @param portMap
	 * @return intersection of this PortMap and "portMap"
	 * 
	 * This operation is useful when combining the assertions made on
	 * two paths. 
	 */
	@Override
	public LinkedPortMap intersectMin(PortMap portMap) {
		if (!(portMap instanceof LinkedPortMap))
			throw new IllegalArgumentException();
		
		LinkedPortMap otherPortMap=(LinkedPortMap) portMap;
		LinkedPortMap lca=leastCommonAncestor(this,otherPortMap);
		return intersectMin(otherPortMap,lca);
	}

	private LinkedPortMap intersectMin(LinkedPortMap otherMap, LinkedPortMap lca) {
		if (this==lca)
			return this; // Common ancestor to both port maps
		else {
			LinkedPortMap result=mBasedOn.intersectMin(otherMap, lca);
			XlimTopLevelPort port=mTest.getPort();
			AvailabilityTest otherTest=otherMap.get(port);
			
			if (otherTest==null)
				return result; // Not in other portMap
			else if (otherTest.getTokenCount() < mTest.getTokenCount())
				return result.add(otherTest); // smaller in other portMap
			else if (result==mBasedOn)
				return this; // We can use this PortMap as is
			else
				return result.add(mTest);
		}
	}
	
	/**
	 * @param portMap
	 * @return The difference of this PortMap and "portMap"
	 * 
	 * An AvailabilityTest is in the difference if either:
	 * (a) "portMap" has no association for the port of the test, or
	 * (b) the test of this PortMap has a higher token count than
	 *     the corresponding test in "portMap"
	 *     
	 * This operation is useful when finding the assertions made on
	 * one path but not on another one.
	 */
	@Override
	public LinkedPortMap diffMax(PortMap portMap) {
		if (!(portMap instanceof LinkedPortMap))
			throw new IllegalArgumentException();
		
		LinkedPortMap linkedPortMap=(LinkedPortMap) portMap;
		LinkedPortMap lca=leastCommonAncestor(this,linkedPortMap);
		
		return diffMax(this,linkedPortMap,lca);
	}
	
	private LinkedPortMap diffMax(LinkedPortMap thisMap, LinkedPortMap otherMap, LinkedPortMap lca) {
		if (this==lca)
			return sEmpty; // Rest is common to both maps
		else {
			LinkedPortMap result=mBasedOn.diffMax(thisMap, otherMap, lca);
			XlimTopLevelPort port=mTest.getPort();
			
			// First make sure that mTest is "the current" association for port
			if (thisMap.get(port)==mTest) {
				AvailabilityTest otherTest=otherMap.get(port);
				if (otherTest==null || otherTest.getTokenCount() < mTest.getTokenCount())
					return result.add(mTest);
			}
			return result;
		}
	}
	
	@Override
	public Iterator<AvailabilityTest> iterator() {
		return new LinkedPortMapIterator();
	}

	class LinkedPortMapIterator implements Iterator<AvailabilityTest> {

		private LinkedPortMap mPortMap=LinkedPortMap.this;
		private LinkedPortMap mPtr=LinkedPortMap.this;

		@Override
		public boolean hasNext() {
			while (mPtr.mFirstApperence==false) {
				mPtr=mPtr.mBasedOn;
			}
			return mPtr.mTest!=null;
		}

		@Override
		public AvailabilityTest next() {
			// mPtr points at the first apperence...
			XlimTopLevelPort port=mPtr.mTest.getPort();
			mPtr=mPtr.mBasedOn;
			// ...but we return the one added last
			return mPortMap.get(port);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("remove()");
		}
	}
}
