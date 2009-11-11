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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.absint.AbstractValue;
import eu.actorsproject.xlim.absint.Context;
import eu.actorsproject.xlim.absint.DemandContext;

/**
 * A NullNode represents an empty leaf in the decision tree.
 * This is the place where we must block to avoid busy wait.
 */
public class NullNode extends DecisionTree {

	private XlimContainerModule mProgramPoint;

	private static Map<XlimTopLevelPort,Integer> sEmptyPortMap
		= Collections.emptyMap();
	private static PortSignature sEmptyPortSignature
		= new PortSignature(sEmptyPortMap);
	
	public NullNode(XlimContainerModule programPoint) {
		mProgramPoint=programPoint;
	}

	@Override
	public NullNode isNullNode() {
		return this;
	}
	
	@Override
	public PortSignature requiredPortSignature() {
		return sEmptyPortSignature;
	}

	
	@Override
	public PortSignature getMode() {
		return null;
	}

	@Override
	protected XlimContainerModule getModule() {
		return mProgramPoint;
	}
	
	
	@Override
	protected DecisionTree hoistAvailabilityTests(PortSignature parentSignature, 
			                                      Map<XlimTopLevelPort, XlimOperation> pinAvailMap) {
		// No tests to hoist at the leaves
		return this;
	}

	
	@Override
	protected DecisionTree removeRedundantTests(Map<XlimTopLevelPort, Integer> successfulTests) {
		// No tests to remove at the leaves
		return this;
	}

	@Override
	protected void findPinAvail(Map<XlimTopLevelPort, XlimOperation> pinAvailMap) {
		// No pinAvails here...	
	}

	
	@Override
	protected void generateBlockingWaits(Map<XlimTopLevelPort, Integer> failedTests) {
		// Copy the map to avoid possible future modification
		Map<XlimTopLevelPort,Integer> copy=new HashMap<XlimTopLevelPort,Integer>(failedTests);
		PortSignature yieldAttribute=new PortSignature(copy);
		
		mProgramPoint.startPatchAtEnd();
 		XlimOperation yield=mProgramPoint.addOperation("yield");
 		yield.setGenericAttribute(yieldAttribute);
 		mProgramPoint.completePatchAndFixup();
	}

	@Override
	public <T extends AbstractValue<T>> DecisionTree foldDecisions(Context<T> context) {
		return this; // found a leaf node
	}

	
	@Override
	public <T extends AbstractValue<T>> void propagateState(DemandContext<T> context, StateEnumeration<T> stateEnumeration) {
		// Do nothing: no state has been modified on the path from the root of the decision tree
	}

	@Override
	protected <T extends AbstractValue<T>> 
	boolean createPhase(DemandContext<T> context, 
			                 StateEnumeration<T> stateEnum, 
			                 boolean blockOnNullNode, 
			                 List<DecisionTree> leaves) {
		if (blockOnNullNode==false)
			leaves.add(this); // means termination
		return false;  // *not* non-deterministic
	}
	
	@Override
	protected <T extends AbstractValue<T>> Characteristic 
	printModes(StateEnumeration<T> stateEnum, PortSignature inMode) {
		// Do nothing
		return Characteristic.EMPTY;  // no "next" modes
	}
	
	
	@Override
	public <T extends AbstractValue<T>> Characteristic 
	printTransitions(DemandContext<T> context, 
			         StateEnumeration<T> stateEnum, 
			         PortSignature inMode, 
			         boolean blockOnNullNode) {
		if (blockOnNullNode) {
			// Here NullNode means blocking -not termination
			return Characteristic.EMPTY;
		}
		else {
			// Here NullNode means termination (actor is dead)
			System.out.println("    --> terminal mode (NullNode)");
			return Characteristic.STATIC;
		}
	}
	
	@Override
	public void findActionNodes(List<ActionNode> actionNodes) {
		// No ActionNodes here: do nothing		
	}

	/* XmlElement interface */

	@Override
	public String getTagName() {
		return "nullNode";
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return Collections.emptyList();
	}
}
