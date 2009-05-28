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

import java.util.ArrayList;

import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimIfModule;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimTestModule;


/**
 * A DecisionNode selects either of two children, depending on the
 * outcome of an associated (side-effect free) condition.
 */
public class DecisionNode extends DecisionTree {
	protected XlimIfModule mIfModule;
	protected Condition mCondition;
	protected DecisionTree mIfTrue, mIfFalse;
	
	public DecisionNode(XlimIfModule ifModule,
			            Condition condition,
			            DecisionTree ifTrue,
			            DecisionTree ifFalse) {
		mIfModule=ifModule;
		mCondition=condition;
		mIfTrue=ifTrue;
		mIfFalse=ifFalse;
		ifTrue.setParent(this);
		ifFalse.setParent(this);
	}
	
	
	/**
	 * @param path the path (true/false) from the test
	 * @return the node in the decision diagram, which corresponds to path
	 */
	public DecisionTree getChild(boolean path) {
		return (path)? mIfTrue : mIfFalse;
	}
	
	@Override
	protected XlimIfModule getModule() {
		return mIfModule;
	}
		
	
	@Override
	protected XlimIfModule sinkIntoIfModule() {
		XlimContainerModule container=mIfModule.getParentModule();
		container.startPatchBefore(mIfModule);
		XlimIfModule result=container.addIfModule();
		result.getThenModule().cutAndPaste(mIfModule);
		container.completePatchAndFixup();
	
		return result;
	}

	
	/** 
	 * @param dominatingTests tests that are satisfied on the path to this DecisionNode
	 * @return the tests that are performed on all paths to ActionNodes
	 */
	@Override
	protected PortMap hoistAvailabilityTests(PortMap dominatingTests) {
		// First update dominatingTests and process ifTrue-branch
		PortMap ifTrueTests=mCondition.updateSuccessfulTests(dominatingTests);
		ifTrueTests=mIfTrue.hoistAvailabilityTests(ifTrueTests);
		
		// Then process ifFalse-branch
		PortMap ifFalseTests=mIfFalse.hoistAvailabilityTests(dominatingTests);
		
		if (ifFalseTests==null)
			return ifTrueTests; // No ActionNodes on ifFalse-branch
		else if (ifTrueTests==null)
			return ifFalseTests; // No ActionNodes on ifTrue-branch
		else {
			
			// Hoist tests that are on all ifTrue-branches, but not all ifFalse-branches
			for (AvailabilityTest test: ifTrueTests.diffMax(ifFalseTests))
				mIfTrue=mIfTrue.hoistAvailabilityTest(test);
			
			// Now hoist tests on all ifFalse-branches, but not all ifTrue branches
			for (AvailabilityTest test: ifFalseTests.diffMax(ifTrueTests))
				mIfFalse=mIfFalse.hoistAvailabilityTest(test);
						
			PortMap onBothPaths=ifTrueTests.intersectMin(ifFalseTests);
			
			// Result is updated map of tests on all paths
			return onBothPaths;
		}
	}


	/**
     * @param assertedTests tests that succeeded on *all* paths from the root
	 * @param failedTests   tests that may have failed on *some* path from the
     *                      root of the decision tree to this node.
     * @return possibly updated decision tree
     */
	@Override
    protected DecisionTree topDownPass(PortMap assertedTests, PortMap failedTests) {
		// Update condition using the asserted tests
		Condition newCondition=mCondition.updateCondition(assertedTests);
		if (newCondition!=mCondition) {
			XlimSource decision=newCondition.getXlimSource();
			XlimTestModule testModule=mIfModule.getTestModule();

			mCondition=newCondition;
			
		    testModule.startPatchAtEnd();
		    testModule.setDecision(decision);
		    testModule.completePatchAndFixup();
		}
		
		// Process if-true branch
		PortMap newAssertions=mCondition.updateSuccessfulTests(assertedTests);
		mIfTrue=mIfTrue.topDownPass(newAssertions, failedTests);
		
		if (mCondition.alwaysTrue()) 
			return mIfTrue;
		else {
			// process if-false branch
			failedTests=mCondition.updateFailedTests(failedTests);
			mIfFalse=mIfFalse.topDownPass(assertedTests, failedTests);
			return this;
		}
	}
    	
    
 	/**
     * Alters the underlying XLIM-representation so that it uses blocking wait
     */
 	@Override
    public void generateBlockingWait() {
    	mIfTrue.generateBlockingWait();
    	mIfFalse.generateBlockingWait();
    }
    
 	/* XmlElement interface */

	@Override
	public String getTagName() {
		return "decisionNode";
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		ArrayList<XmlElement> children=new ArrayList<XmlElement>(3);
		children.add(mCondition);
		children.add(mIfTrue);
		children.add(mIfFalse);
		return children;
	}
}
