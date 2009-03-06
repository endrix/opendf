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

import java.util.Map;

import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimTopLevelPort;

public class AvailabilityTest extends Condition {

	private XlimTopLevelPort mPort;
	private int mNumTokens;
	
	public AvailabilityTest(XlimSource condition, 
			                XlimTopLevelPort port,
			                int numTokens) {
		super(condition);
		mPort=port;
		mNumTokens=numTokens;
	}
	
	@Override
	public String getTagName() {
		return "pinAvail";
	}

	@Override
	protected Object updatePortMap(Map<XlimTopLevelPort,Integer> map) {
		Integer oldValue=map.get(mPort);
		if (oldValue==null || oldValue>mNumTokens)
			map.put(mPort, mNumTokens);
		return oldValue;
	}
	
	@Override
	protected void restorePortMap(Map<XlimTopLevelPort,Integer> map, Object oldValue) {
		if (oldValue==null)
			map.remove(mPort);
		else
			map.put(mPort, (Integer) oldValue);
	}
	
	@Override
	public String getAttributeDefinitions() {
		return super.getAttributeDefinitions()
		       + " port=\"" + mPort.getSourceName() 
		       + "\" size=\"" + mNumTokens + "\"";
	}
}
