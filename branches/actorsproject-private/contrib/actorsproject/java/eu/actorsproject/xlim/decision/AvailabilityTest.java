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

import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimTopLevelPort;

/**
 * Represents the condition: pinAvail(port) >= numTokens
 */
public class AvailabilityTest extends Condition {

	private XlimTopLevelPort mPort;
	private int mNumTokens;
	
	public AvailabilityTest(XlimContainerModule container, 
			                XlimSource xlimSource,
			                XlimTopLevelPort port,
			                int numTokens) {
		super(container,xlimSource);
		mPort=port;
		mNumTokens=numTokens;
	}
		
	public XlimTopLevelPort getPort() {
		return mPort;
	}
	
	public int getTokenCount() {
		return mNumTokens;
	}
	
	@Override
	public String getTagName() {
		return "pinAvail";
	}

	
	@Override
	protected int assertedTokenCount(XlimTopLevelPort port) {
		return (mPort==port)? mNumTokens : 0;
	}

	@Override
	protected PortMap updateFailedTests(PortMap failedTests) {
		AvailabilityTest oldTest=failedTests.get(mPort);
		if (oldTest==null || oldTest.getTokenCount()>mNumTokens) {
            // Update (minimum number of tokens that may make this actor fireable again)
			failedTests=failedTests.add(this);
		}
		return failedTests;
	}
	
	
	/**
	 * Updates the collection of asserted (successful) token-availability tests
	 * @param portMap     gives the maximum asserted token availability for port
	 *                    on a path from the root of the decision tree to the
	 *                    "true" branch that is guarded by this condition
	 */

	@Override
	protected PortMap updateSuccessfulTests(PortMap successfulTests) {
		AvailabilityTest oldTest=successfulTests.get(mPort);
		if (oldTest==null || oldTest.getTokenCount()<mNumTokens)
			successfulTests=successfulTests.add(this); // Update (maximum number of tokens available)
		return successfulTests;
	}

	@Override
	public Condition updateCondition(PortMap successfulTests) {
		AvailabilityTest successful=successfulTests.get(mPort);
		
		if (successful!=null && successful.getTokenCount()>=mNumTokens) {
			// This AvailabilityTest is dominated by another, at least as strong, one
			return makeAlwaysTrue();
		}
		else
			return this;
	}
		
	@Override
	public String getAttributeDefinitions() {
		return super.getAttributeDefinitions()
		       + " port=\"" + mPort.getSourceName() 
		       + "\" size=\"" + mNumTokens + "\"";
	}
}
