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

import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimBlockElement;
import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimIfModule;
import eu.actorsproject.xlim.XlimModule;

/**
 * An ActionNode is a leaf of the decision tree, which is
 * associated with code that has side effects (ports, state variables).
 * This is an action firing (or what is left of it).
 */
public class ActionNode extends DecisionTree {

	protected XlimContainerModule mAction;
	
	public ActionNode(XlimContainerModule action) {
		assert(action!=null);
		mAction=action;
	}
	
	@Override
	protected XlimModule getModule() {
		return mAction;
	}
	
	@Override
	protected DecisionTree topDownPass(PortMap assertedTests, PortMap failedTests) {
		// ActionNode: Do nothing
		return this;
	}

	
	@Override
	protected XlimIfModule sinkIntoIfModule() {
		// Insert the IfModule in the current action module
		mAction.startPatchAtEnd();
		XlimIfModule result=mAction.addIfModule();
		mAction.completePatchAndFixup();

		// Move ("sink") action
		XlimContainerModule dest=result.getThenModule();
		dest.startPatchAtEnd();
		XlimBlockElement element=mAction.getChildren().iterator().next();
		while (element!=result) {
			dest.cutAndPaste(element);
			element=mAction.getChildren().iterator().next();
		}
        dest.completePatchAndFixup();
        
		// update program point
		mAction=dest;
		
		return result;
	}

	@Override
	protected PortMap hoistAvailabilityTests(PortMap dominatingTests) {
		// Return set of dominating token-availability tests 
		return dominatingTests;
	}

	@Override
    public void generateBlockingWait() {
		// ActionNode: Do nothing
	}
	
	/* XmlElement interface */

	@Override
	public String getTagName() {
		return "actionNode";
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return mAction.getChildren();
	}
}
