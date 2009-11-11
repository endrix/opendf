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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimIfModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimTestModule;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.absint.AbstractValue;
import eu.actorsproject.xlim.absint.BagOfConstraints;
import eu.actorsproject.xlim.absint.Context;
import eu.actorsproject.xlim.absint.DemandContext;
import eu.actorsproject.xlim.dependence.DependenceSlice;


/**
 * A DecisionNode selects either of two children, depending on the
 * outcome of an associated (side-effect free) condition.
 */
public class DecisionNode extends DecisionTree {
	private XlimIfModule mIfModule;
	private Condition mCondition;
	private DecisionTree mIfTrue, mIfFalse;
	private PortSignature mPortSignature, mMode;
	
	public DecisionNode(XlimIfModule ifModule,
			            Condition condition,
			            DecisionTree ifTrue,
			            DecisionTree ifFalse) {
		mIfModule=ifModule;
		mCondition=condition;
		mIfTrue=ifTrue;
		mIfFalse=ifFalse;
		determinePortSignature();
	}
	
	
	@Override
	public PortSignature requiredPortSignature() {
		return mPortSignature;
	}

	
	@Override
	public PortSignature getMode() {
		return mMode;
	}


	private void determinePortSignature() {
		PortSignature s1=mIfTrue.requiredPortSignature();
		
		if (mIfFalse.isNullNode()!=null && mCondition.isImpliedBy(s1)) {
			// This decision just tests the availability of tokens,
			// which is required by the true path (and blocks otherwise)
			mPortSignature=s1;
			mMode=mIfTrue.getMode();
		}
		else {
			// Mode is a port signature that is shared by both paths
			PortSignature mode1=mIfTrue.getMode();
			PortSignature mode2=mIfFalse.getMode();
			if (mode1!=null && mode2!=null && mode1.equals(mode2))
				mMode=mode1;
			else
				mMode=null;
			
			// Port signature is the "intersection" of both paths
			// (the minimum port rate)
			PortSignature s2=mIfFalse.requiredPortSignature();
			mPortSignature=PortSignature.intersect(s1,s2);
		}
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
	public void findActionNodes(List<ActionNode> actionNodes) {
		mIfTrue.findActionNodes(actionNodes);
		mIfFalse.findActionNodes(actionNodes);
	}


//  TODO: if we need this one we have to fix Conjunctions (no proper ValueNode)
//	@Override
//	protected DecisionTree generateActionSelector(Mode mode) {
//		// Can the decision node be removed entirely?
//		DecisionTree ifTrue=mIfTrue.generateActionSelector(mode);
//		if (mCondition.isImpliedByMode(mode)) {
//			return ifTrue;
//		}
//		else {
//			// Specialize condition and both sub-trees with respect to 'mode'
//			DecisionTree ifFalse=mIfFalse.generateActionSelector(mode);
//			Condition newCond=mCondition.specialize(mode);
//		
//			if (newCond!=mCondition || ifTrue!=mIfTrue || ifFalse!=mIfFalse) {
//				return new DecisionNode(mIfModule, newCond, ifTrue, ifFalse);
//			}
//			else
//				return this; // No specialization, keep "as is"
//		}
//	}

	@Override
	protected DecisionTree hoistAvailabilityTests(PortSignature parentSignature, Map<XlimTopLevelPort, XlimOperation> pinAvailMap) {
		
		mIfTrue=mIfTrue.hoistAvailabilityTests(mPortSignature, pinAvailMap);
		mIfFalse=mIfFalse.hoistAvailabilityTests(mPortSignature, pinAvailMap);
		
		DecisionNode updatedTree=this;
		XlimContainerModule container=mIfModule.getParentModule();
		
		container.startPatchBefore(mIfModule);
		for (XlimTopLevelPort port: mPortSignature.getPorts()) {
			int requiredPortRate=mPortSignature.getPortRate(port);
			int testedRate=parentSignature.getPortRate(port);
			
			// If this decision node is the first one that requires this
			// particular token rate, we should hoist the test to this position
			// (this will render the test redundant in any sub-trees)
			
			if (testedRate<requiredPortRate) {				
				// First, create a new If-Module and sink current tree
				// into its Then-module
				XlimIfModule newIf=container.addIfModule();
				newIf.getThenModule().cutAndPaste(updatedTree.mIfModule);
				
				// Create a NullNode to match the new (empty) Else-Module
				NullNode nullNode=new NullNode(newIf.getElseModule());
				
				// Create the availability test
				AvailabilityTest newTest=createAvailabilityTest(port, 
						                                        requiredPortRate,
						                                        pinAvailMap,
						                                        newIf.getTestModule());
				
				// Create the new DecisionNode
				updatedTree=new DecisionNode(newIf,newTest,updatedTree,nullNode);
			}
		}
		container.completePatchAndFixup();
		
		return updatedTree;
	}
	
	@Override
	protected void findPinAvail(Map<XlimTopLevelPort, XlimOperation> pinAvailMap) {
		mCondition.findPinAvail(pinAvailMap);
		mIfTrue.findPinAvail(pinAvailMap);
		mIfFalse.findPinAvail(pinAvailMap);
	}

	private AvailabilityTest createAvailabilityTest(XlimTopLevelPort port,
			                                        int numTokens,
			                                        Map<XlimTopLevelPort, XlimOperation> pinAvailMap,
			                                        XlimTestModule testModule) {
		XlimOperation pinOperation=pinAvailMap.get(port);
		assert(pinOperation!=null); 

		XlimOutputPort condition;
		if (pinOperation.getKind().equals("pinAvail")) {
			XlimOperation literal=testModule.addLiteral(numTokens);
			XlimOperation ge=testModule.addOperation("$ge",
					                                 pinOperation.getOutputPort(0),
					                                 literal.getOutputPort(0));
			condition=ge.getOutputPort(0);
		}
		else {
			assert(pinOperation.getKind().equals("pinStatus") && numTokens==1);
			condition=pinOperation.getOutputPort(0);
		}
		testModule.setDecision(condition);
		
		return new AvailabilityTest(condition, pinOperation, numTokens);
	}

	
	@Override
	protected DecisionTree removeRedundantTests(Map<XlimTopLevelPort, Integer> successfulTests) {
		PortSignature portSignature=new PortSignature(successfulTests);
		
		// Process true-branch
		if (mCondition.dependsOnInput(portSignature)) {
			// The condition has additional token availability tests
			// (not in successfulTests) we need to update the map
			Map<XlimTopLevelPort,Integer> updatedMap=
				new HashMap<XlimTopLevelPort,Integer>(successfulTests);
			mCondition.updateDominatingTests(updatedMap);
			mIfTrue=mIfTrue.removeRedundantTests(updatedMap);
		}
		else {
			// No need to update successfulTests
			mIfTrue=mIfTrue.removeRedundantTests(successfulTests);
		}
		
		// Is the entire test (and the false-branch) redundant?
		if (mCondition.isImpliedBy(portSignature)) {
			// Cut Then-module and paste it before the if-module
			// then remove the if-module
			XlimContainerModule container=mIfModule.getParentModule();
			container.startPatchBefore(mIfModule);
			container.cutAndPasteAll(mIfModule.getThenModule());
			container.remove(mIfModule);
			container.completePatchAndFixup();
			return mIfTrue;
		}
		else {
			// Simplify condition
			mCondition=mCondition.removeRedundantTests(portSignature, 
					                                   mIfModule.getTestModule());
			// Process false-branch
			mIfFalse=mIfFalse.removeRedundantTests(successfulTests);
			return this;
		}
	}


	@Override
	protected void generateBlockingWaits(Map<XlimTopLevelPort, Integer> failedTests) {
		mIfTrue.generateBlockingWaits(failedTests);
		
		if (mCondition.dependsOnInput(null)) {
			// There may be additional failed tests on false branch
			Map<XlimTopLevelPort,Integer> updatedTests=
				new HashMap<XlimTopLevelPort,Integer>(failedTests);
			mCondition.updateDominatingTests(updatedTests);
			mIfFalse.generateBlockingWaits(updatedTests);
		}
		else {
			// No need to update failedTests
			mIfFalse.generateBlockingWaits(failedTests);
		}
	}


	@Override
	public void createDependenceSlice(DependenceSlice slice) {
		slice.add(mCondition.getValue());
		mIfTrue.createDependenceSlice(slice);
		mIfFalse.createDependenceSlice(slice);
	}
	
	
	@Override
	public <T extends AbstractValue<T>> DecisionTree foldDecisions(Context<T> context) {
		switch (mCondition.evaluate(context, null)) {
		case ALWAYS_TRUE:
			return mIfTrue.foldDecisions(context);
		case ALWAYS_FALSE:
			return mIfFalse.foldDecisions(context);
		default:
			return this; // not possible to fold condition
		}
	}

	private <T extends AbstractValue<T>> 
	DemandContext<T> constrainCondition(boolean assertedValue,
			                            DemandContext<T> context,
			                            StateEnumeration<T> stateEnumeration) {
		switch (mCondition.evaluate(context, mPortSignature)) {
		case ALWAYS_TRUE:
			// No point asserting an ALWAYS_TRUE condition
			// Can't refute it
			return (assertedValue)? context : null;
		case ALWAYS_FALSE:
			// No point refuting an ALWAYS_FALSE condition
			// Can't assert it
			return (assertedValue)? null : context;
		case TRUE_OR_FALSE:
		default:
			// Try asserting/refuting condition		
			BagOfConstraints<T> bag=stateEnumeration.createBagOfConstraints(context);
			if (bag.constrainCondition(mCondition.getValue(), assertedValue))
				return bag.createSubContext(); // successful constraint
			else
				return null; // constraint caused inconsistency
		}
	}
	
	@Override
	public <T extends AbstractValue<T>> 
	void propagateState(DemandContext<T> context, 
			            StateEnumeration<T> stateEnumeration) {
		
		// Assert true/false respectively and propagate new contexts
		// provided they are consistent (free from contradictions)
		DemandContext<T> trueContext=constrainCondition(true, context, stateEnumeration);
		if (trueContext!=null)
			mIfTrue.propagateState(trueContext, stateEnumeration);

		DemandContext<T> falseContext=constrainCondition(false, context, stateEnumeration);
		if (falseContext!=null)
			mIfFalse.propagateState(falseContext, stateEnumeration);
	}
	
	
	@Override
	protected <T extends AbstractValue<T>> 
	boolean createPhase(DemandContext<T> context, 
			                 StateEnumeration<T> stateEnum, 
			                 boolean blockOnNullNode, 
			                 List<DecisionTree> leaves) {
		boolean isNonDeterministic=false;
		int firstTrueLeaf=leaves.size();
		
		// Assert true/false respectively and propagate new contexts
		// provided they are consistent (free from contradictions)
		DemandContext<T> trueContext=constrainCondition(true, context, stateEnum);
		if (trueContext!=null) {
			if (mIfTrue.createPhase(trueContext, stateEnum, blockOnNullNode, leaves))
				isNonDeterministic=true;
		}
		boolean leavesInTruePath=(firstTrueLeaf!=leaves.size());
		
		int firstFalseLeaf=leaves.size();
		DemandContext<T> falseContext=constrainCondition(false, context, stateEnum);
		if (falseContext!=null) {
			// If we are on the "false" branch of an availability test, a NullNode
			// means blocking. Otherwise, NullNodes mean termination
			boolean block=blockOnNullNode || leavesInTruePath && mCondition.dependsOnInput(mPortSignature); 
			if (mIfFalse.createPhase(falseContext, stateEnum, block, leaves))
				isNonDeterministic=true;
		}
		boolean leavesInFalsePath=(firstFalseLeaf!=leaves.size());
		
		if (leavesInTruePath && leavesInFalsePath && isNonDeterministic==false) {
			// Both paths have leaves = the characteristics of the test matters
			if (mCondition.dependsOnInput(mPortSignature)) {
				// Verify that the test depends on a port that is not consumed by all leaves
				// (we may have accounted for leaves that are not reachable in this context)
//				PortSignature actualSignature=leaves.get(firstTrueLeaf).leastCommonPortSignature();
//				for (int i=firstTrueLeaf+1; i<leaves.size(); ++i) {
//					PortSignature s=leaves.get(i).leastCommonPortSignature();
//					actualSignature=PortSignature.intersect(actualSignature, s);
//				}
//				
//				if (mCondition.dependsOnInput(actualSignature))
					isNonDeterministic=true;
			}
		}
		
		return isNonDeterministic;
	}


	@Override
	public <T extends AbstractValue<T>> 
	Characteristic printTransitions(DemandContext<T> context, 
			                        StateEnumeration<T> stateEnumeration, 
			                        PortSignature inMode, 
			                        boolean blockOnNullNode) {
		Characteristic cTrue=Characteristic.EMPTY;
		Characteristic cFalse=Characteristic.EMPTY;
		
		if (inMode==null) {
			inMode=getMode();
			if (inMode!=null)
				System.out.println("    --> Mode: "+inMode);
		}
		
		// Assert true/false respectively and propagate new contexts
		// provided they are consistent (free from contradictions)
		DemandContext<T> trueContext=constrainCondition(true, context, stateEnumeration);
		if (trueContext!=null)
			cTrue=mIfTrue.printTransitions(trueContext, stateEnumeration, inMode, blockOnNullNode);

		DemandContext<T> falseContext=constrainCondition(false, context, stateEnumeration);
		if (falseContext!=null) {
			// If we are on the "false" branch of an availability test, a NullNode
			// means blocking. Otherwise, NullNodes mean termination
			boolean block=blockOnNullNode || trueContext!=null && mCondition.dependsOnInput(mPortSignature); 
			cFalse=mIfFalse.printTransitions(falseContext, stateEnumeration, inMode, block);
		}
	
		if (cTrue==Characteristic.EMPTY) {
			// The characteristics of the test doesn't matter
			return cFalse;
		}
		else if (cFalse==Characteristic.EMPTY) {
			// The characteristics of the test doesn't matter
			return cTrue;
		}
		else if (inMode!=null) {
			// Within a mode we are at worst STATIC
			return Characteristic.STATIC;
		}
		else {
			// The worst case of this test and any test in the children
			if (cTrue==Characteristic.NON_DETERMINISTIC 
				|| cFalse==Characteristic.NON_DETERMINISTIC
				|| mCondition.dependsOnInput(mPortSignature)) 
				return Characteristic.NON_DETERMINISTIC;
			else
				return Characteristic.STATE_DEPENDENT;
		}
	}
	

	
	@Override
	protected <T extends AbstractValue<T>> Characteristic 
	printModes(StateEnumeration<T> stateEnum, PortSignature inMode) {
		if (inMode==null) {
			inMode=getMode();
			if (inMode!=null)
				System.out.println("Mode: "+inMode);
		}
		Characteristic cTrue=mIfTrue.printModes(stateEnum, inMode);
		Characteristic cFalse=mIfFalse.printModes(stateEnum, inMode);
		
		// return the worst case
		return (cTrue.compareTo(cFalse)>0)? cTrue : cFalse;
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
