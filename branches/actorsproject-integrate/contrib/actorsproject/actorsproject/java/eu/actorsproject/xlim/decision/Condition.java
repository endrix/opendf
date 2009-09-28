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

import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.util.LiteralPattern;

/**
 * A Condition, which is associated with a DecisionNode.
 * Two subclasses represent relevant special cases:
 * AvailabilityTest  token/space availability on in-/out-port
 * Conjunction       a conjunction: c1 && c2 && ... && cn of simpler conditions
 *                   (all of which are asserted on the "true" branch of a decision node)
 */
public class Condition implements XmlElement {

	private XlimContainerModule mContainer;
	private XlimSource mXlimSource;
	
	public Condition(XlimContainerModule container, XlimSource xlimSource) {
		mContainer=container;
		mXlimSource=xlimSource;
	}
	
	public void addTo(Conjunction conjunction) {
		conjunction.add(this);
	}
	
	protected XlimSource getXlimSource() {
		return mXlimSource;
	}
	
	protected XlimContainerModule getXlimContainer() {
		return mContainer;
	}
	
	/**
	 * @param port 
	 * @return number of available tokens (on port) asserted by this condition
	 * 	       (0 if this condition contains no token-availability test on port)
	 */
	protected int assertedTokenCount(XlimTopLevelPort port) {
		return 0;
	}
	
	/**
     * Updates the collection of tested (and possibly failed) token-availability tests
     * @param failedTests Tests that may have failed on some path from
     *                    the root of the decision tree to this node.
	 * @return updated collection of tests
     */
    protected PortMap updateFailedTests(PortMap failedTests) {
    	return failedTests; // Do nothing
    }
    
    /**
    * Updates the collection of asserted (successful) token-availability tests
    * @param portMap     gives the maximum asserted token availability for port
    *                    on a path from the root of the decision tree to the
    *                    "true" branch that is guarded by this condition
     * @return updated collection of tests
    */
    protected PortMap updateSuccessfulTests(PortMap successfulTests) {
    	return successfulTests; // Do nothing!
    }
    
    /**
     * @param successfulTests tests known to be true
     * @return true if the condition was updated
     */
    public Condition updateCondition(PortMap successfulTests) {
    	return this; // Do nothing!
    }
    
    protected Condition makeAlwaysTrue() {
    	// Make if a constant "true" Condition
		mContainer.startPatchAtEnd();
		XlimOperation op=mContainer.addLiteral(true);
		mContainer.completePatchAndFixup();
		return new Condition(mContainer, op.getOutputPort(0));
    }
    
    private static LiteralPattern sTruePattern = new LiteralPattern(1);
    
    public boolean alwaysTrue() {
    	return sTruePattern.matches(mXlimSource);
    }
    
	@Override
	public String getTagName() {
		return "condition";
	}

	@Override
	public String getAttributeDefinitions() {
		XlimSource xlimCondition=getXlimSource();
		return "decision=\""+xlimCondition.getUniqueId()+"\"";
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return Collections.emptyList();
	}
}