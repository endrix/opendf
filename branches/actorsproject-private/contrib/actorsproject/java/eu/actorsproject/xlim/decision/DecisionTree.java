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
import eu.actorsproject.xlim.XlimIfModule;
import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.XlimTestModule;

/**
 * A DecisionTree models the scheduling decision of an action scheduler.
 * There are four kinds of nodes (known sub-classes):
 * DecisionNode a two-way decision, depending on a side-effect-free condition.
 * ActionNode   a leaf, which groups side effects (an action)
 * NullNode     an empty leaf (which needs blocking to avoid busy wait)
 * ParallelNode an internal node, which groups subtrees that are evaluated in
 *              parallel (corresponding to an Xlim "mutex" module)
 */
public abstract class DecisionTree implements XmlElement {

	private DecisionTree mParent;
	private int mIdentifier;
	private static int mNextIdentifier;
	
	
	public DecisionTree() {
		mIdentifier=mNextIdentifier++;
	}
	
	public DecisionTree getParent() {
		return mParent;
	}
	
	public void setParent(DecisionTree parent) {
		mParent=parent;
	}
	
	protected int getIdentifier() {
		return mIdentifier;
	}

	protected abstract XlimModule getModule();
	
	/**
	 * Moves token-availability tests towards the root of the decision tree
	 * by first finding the collection of tests that are common to all ActionNodes in the tree
	 * and then inserting DecisionNodes with simple (non-composite) AvailabilityTests.
	 * 
	 * For deterministic DDF-style actors, it is possible to perform all availability tests
	 * in this fashion, which results in single pinWaits (essentially blocking reads)
	 */
	public DecisionTree hoistAvailabilityTests() {
		PortMap dominatingTests = LinkedPortMap.empty();
		PortMap onAllPaths=hoistAvailabilityTests(dominatingTests);
		return hoistAllAvailabilityTests(onAllPaths);
	}
	
	
	protected DecisionTree hoistAllAvailabilityTests(PortMap onAllPaths) {
		DecisionTree newRoot=this;
		
		for (AvailabilityTest test: onAllPaths)
			newRoot=newRoot.hoistAvailabilityTest(test);
		
		return newRoot;
	}
	
	protected DecisionTree hoistAvailabilityTest(AvailabilityTest test) {
		XlimIfModule ifModule=sinkIntoIfModule();
		DecisionTree nullNode=new NullNode(ifModule.getElseModule());
		XlimTestModule testModule=ifModule.getTestModule();
		
		testModule.setDecision(test.getXlimSource());
		
		Condition newCond=new AvailabilityTest(testModule,
				                               test.getXlimSource(),
				                               test.getPort(),
				                               test.getTokenCount());
		return new DecisionNode(ifModule, newCond, this, nullNode);
	}
		
	/**
	 * Creates an if-module and moves ("sinks") the code of this DecisionTree 
	 * into the then-module of the new module.
	 * @return new XlimIfModule
	 */
	protected abstract XlimIfModule sinkIntoIfModule();
	
	protected abstract PortMap hoistAvailabilityTests(PortMap dominatingTests);
	
    /**
     * Decorate the NullNodes of a decision tree with the set of ports that may
     * have been tested for availability tokens (input ports) or space (output ports)
     * and the outcome of that test was failure.
     * This set can be used to formulate blocking conditions (to avoid busy waiting),
     * since the scheduling decision (the path leading to a NullNode) will not change
     * before the availability tests change (more tokens/space become available). 
     */
    public DecisionTree decorateNullNodes() {
    	LinkedPortMap assertedTests=LinkedPortMap.empty();
    	LinkedPortMap failedTests=LinkedPortMap.empty();
        return topDownPass(assertedTests, failedTests);
    }
    
    /**
     * @param assertedTests tests that succeeded on *all* paths from the root
	 * @param failedTests   tests that may have failed on *some* path from the
     *                      root of the decision tree to this node.
     * @return possibly updated decision tree
     */
    protected abstract DecisionTree topDownPass(PortMap assertedTests, PortMap failedTests);
    
    
    /**
     * Alters the underlying XLIM-representation so that it uses blocking wait
     */
    public abstract void generateBlockingWait();
    
	/* XmlElement interface */
	
	@Override
	public String getAttributeDefinitions() {
		return "ident=\""+getIdentifier()+"\"";
	}
}
