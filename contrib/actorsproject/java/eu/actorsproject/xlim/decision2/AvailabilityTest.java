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

import java.util.Map;

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimTopLevelPort;

/**
 * Represents the condition: pinAvail(port) >= numTokens
 */
public class AvailabilityTest extends AtomicCondition {

	private XlimOperation mPinOperation;
	private int mNumTokens;
	
	public AvailabilityTest(XlimOutputPort condition,
			                XlimOperation pinOperation,
			                int numTokens) {
		super(condition, condition.getValue());
		mPinOperation=pinOperation;
		mNumTokens=numTokens;
	}
	
	@Override
	public <Result, Arg> Result accept(Visitor<Result, Arg> visitor, Arg arg) {
		return visitor.visitAvailabilityTest(this,arg);
	}
	
	public XlimTopLevelPort getPort() {
		return mPinOperation.getPortAttribute();
	}
	
	public int getTokenCount() {
		return mNumTokens;
	}

	@Override
	public boolean testsAvailability() {
		return true;
	}
	
	@Override
	public boolean isImpliedBy(PortSignature portSignature) {
		int impliedRate=portSignature.getPortRate(getPort());
		if (impliedRate>=mNumTokens)
			return true;
		else
			return alwaysTrue();
	}

	@Override
	public boolean mayTestTokenAbsence(PortSignature availableTokens) {
		return availableTokens==null || isImpliedBy(availableTokens)==false;
	}
	
	
	@Override
	protected void findPinAvail(Map<XlimTopLevelPort, XlimOperation> pinAvailMap) {
		pinAvailMap.put(getPort(), mPinOperation);
	}

	@Override
	protected void updateDominatingTests(Map<XlimTopLevelPort, Integer> testsInAncestors) {
		XlimTopLevelPort port=getPort();
		Integer testedPortRate=testsInAncestors.get(port);
		if (testedPortRate==null || testedPortRate<mNumTokens)
			testsInAncestors.put(port, mNumTokens);
	}
	
	/* XmlElement interface */
	
	@Override
	public String getTagName() {
		return "pinAvail";
	}

	@Override
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		return super.getAttributeDefinitions(formatter)
		       + " port=\"" + getPort().getName() 
		       + "\" size=\"" + mNumTokens + "\"";
	}
}
