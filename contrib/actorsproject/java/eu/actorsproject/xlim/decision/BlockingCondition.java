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

import java.util.Collections;

import eu.actorsproject.util.Pair;
import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimTopLevelPort;

/**
 * Represents a blocking condition: pinAvail(port) >= N
 */
public class BlockingCondition extends Pair<XlimTopLevelPort,Integer> 
                               implements XmlElement, Comparable<BlockingCondition> {

	public BlockingCondition(XlimTopLevelPort port, int tokenCount) {
		super(port,tokenCount);
	}
	
	public BlockingCondition(AvailabilityTest failedTest) {
		super(failedTest.getPort(), failedTest.getTokenCount());
	}
			
	public XlimTopLevelPort getPort() {
		return mFirst;
	}
	
	public int getTokenCount() {
		return mSecond;
	}
	
	@Override
	public String getTagName() {
		return "pinWait";
	}

	@Override
	public int compareTo(BlockingCondition otherCond) {
		// First compare port directions (inputs < outputs)
		XlimTopLevelPort.Direction thisDir=getPort().getDirection();
		XlimTopLevelPort.Direction otherDir=otherCond.getPort().getDirection();
		int result=thisDir.compareTo(otherDir);
		
		if (result==0) {
			// Second, compare port names
			String thisPortName=getPort().getSourceName();
			String otherPortName=otherCond.getPort().getSourceName();
			result=thisPortName.compareTo(otherPortName);
			
			// Third, compare token counts
			if (result==0)
				result=getTokenCount()-otherCond.getTokenCount();
		}
		return result;
	}
	
	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return Collections.emptyList();
	}
	
	@Override
	public String getAttributeDefinitions() {
		return "portName=\""+getPort().getSourceName()+"\" size=\""+getTokenCount()+"\"";
	}
}
