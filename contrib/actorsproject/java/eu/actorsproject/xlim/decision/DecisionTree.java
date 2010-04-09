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

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.absint.AbstractValue;
import eu.actorsproject.xlim.absint.Context;
import eu.actorsproject.xlim.absint.DemandContext;
import eu.actorsproject.xlim.dependence.DependenceSlice;


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

	private int mIdentifier;  // Identifier for print-outs
	
	private static int mNextIdentifier;
	
	public DecisionTree() {
		mIdentifier=mNextIdentifier++;
	}

	public NullNode isNullNode() {
		return null;
	}
	
	/**
	 * @return the port signature (consumption/production rates) that is common
	 *         to all leaf-nodes of the tree.
	 *         
	 * The port signature indicates the port (and number of tokens) that can be
	 * read/written using blocking operations, since all action nodes in the tree
	 * require them). A possible NullNode that indicates termination of the actor
	 * makes the common port signature empty.
	 */
	public abstract PortSignature requiredPortSignature();
	
	/**
	 * @return the port signature (consumption/production rates) that is shared
	 *         by all leaf-node of the tree (or null if port signature differ).
	 *         
	 * A non-null mode indicates that the port signature of all possible "next" 
	 * actions, which are reachable from this tree, have identical signatures.
	 */
	public abstract PortSignature getMode();
	
	public abstract void findActionNodes(List<ActionNode> actionNodes);
		
	/**
	 * @return "optimized" decision tree in which constant conditions have been folded
	 *         and token availability tests have been hoisted towards the root (to avoid
	 *         repeated tests).
	 */
	public DecisionTree optimizeDecisionTree() {
		Map<XlimTopLevelPort,XlimOperation> pinAvailMap=new HashMap<XlimTopLevelPort,XlimOperation>();
		Map<XlimTopLevelPort,Integer> successfulTests=new HashMap<XlimTopLevelPort,Integer>();
		PortSignature emptySignature=new PortSignature();
		
		findPinAvail(pinAvailMap);
		DecisionTree root=hoistAvailabilityTests(emptySignature, pinAvailMap);
		return root.removeRedundantTests(successfulTests);
	}
	
	/**
	 * Creates a port-to-pinAvail look-up by traversing AvailabilityTests.
	 * An action scheduler must have a unique evaluation of pinAvail for each port
	 * -not to take incorrect scheduling decisions for timing-dependent actors.
	 * The look-up helps maintaining this property when creating new tests.
	 * 
	 * @param pinAvailMap  port-to-pinAvail look-up
	 */
	protected abstract void findPinAvail(Map<XlimTopLevelPort,XlimOperation> pinAvailMap);

	/**
	 * @param parentSignature  requiredSignature of parent node
	 * @param pinAvailMap      port-to-pinAvail operation look-up
	 * 
	 * @return decision tree in which token availability tests have been hoisted towards 
	 *         the root (to avoid repeated tests).
	 */
	protected abstract DecisionTree hoistAvailabilityTests(PortSignature parentSignature, 
			                                               Map<XlimTopLevelPort, XlimOperation> pinAvailMap);
	
	
	protected abstract DecisionTree removeRedundantTests(Map<XlimTopLevelPort,Integer> successfulTests);
	
	/**
	 * Adds blocking waits (yield operations) to NullNodes 
	 */
	public void generateBlockingWaits() {
		Map<XlimTopLevelPort,Integer> failedTests=new HashMap<XlimTopLevelPort,Integer>();
		generateBlockingWaits(failedTests);
	}
	
	/**
	 * Adds blocking waits (yield operations) to NullNodes
	 * 
	 * @param failedTests  token that could alter the scheduling decision
	 *                     (used to derive the blocking condition).
	 */
	protected abstract void generateBlockingWaits(Map<XlimTopLevelPort,Integer> failedTests);
	
	/**
	 * Evaluates decisions using an abstract representation of the state.
	 * 
	 * @param context  a mapping from value nodes to abstract values
	 * @return         Either a leaf (ModeNode, ActionNode or NullNode) or an internal
	 *                 node (DecisionNode), which cannot be "folded" given the context/domain.
	 */
	public abstract<T extends AbstractValue<T>> DecisionTree foldDecisions(Context<T> context);
	
	/**
	 * Traverses the decision tree, in search of the "next" mode (or modes).
	 * Decisions are partially evaluated using the given context.
	 * Action nodes are interpreted abstractly and the result is used to update the state-space
	 * enumeration.
	 * @param context          the mapping from value nodes to abstract values
	 * @param stateEnumeration the driver of the state-space enumeration
	 */
	public abstract<T extends AbstractValue<T>> void 
		propagateState(DemandContext<T> context,StateEnumeration<T> stateEnumeration);

	/*
	 * Prints the result of state enumeration
	 */
	
	public enum Characteristic {
		EMPTY,             // no next mode found
		STATIC,            // static next mode
		STATE_DEPENDENT,   // we had to look at state
		NON_DETERMINISTIC  // we had to look at fifos
	};
	
	protected <T extends AbstractValue<T>> Characteristic printModes(StateEnumeration<T> stateEnum) {
		return printModes(stateEnum,null);
	}
	
	protected abstract <T extends AbstractValue<T>> 
	Characteristic printModes(StateEnumeration<T> stateEnum, PortSignature inMode);

	public <T extends AbstractValue<T>> Characteristic
	printTransitions(DemandContext<T> context, StateEnumeration<T> stateEnum) {
		return printTransitions(context,stateEnum,null, false);
	}
	
	protected abstract <T extends AbstractValue<T>> Characteristic 
	printTransitions(DemandContext<T> context, 
			         StateEnumeration<T> stateEnum, 
			         PortSignature inMode, 
			         boolean blockOnNullNode);
	
	
	/**
	 * @param context          a mapping from ValueNodes to abstract values
	 * @param stateEnum        the result of a state enumeration
	 * 
	 * @return                 a SchedulingPhase, which represents the possible scheduling
	 *                         decisions given the state abstraction 'context'. 
	 */
	
	public <T extends AbstractValue<T>>
	SchedulingPhase createPhase(DemandContext<T> context, StateEnumeration<T> stateEnum) {
		List<DecisionTree> leaves=new ArrayList<DecisionTree>();
		boolean isDeterministic=!createPhase(context,stateEnum,false,leaves);
		return new SchedulingPhase(leaves,isDeterministic);
	}
	
	/**
	 * @param context          a mapping from ValueNodes to abstract values
	 * @param stateEnum        the result of a state enumeration
	 * @param blockOnNullNode  true if NullNodes signify blocking, false for termination
	 *                         (it is true on the path from failed token availability tests)
	 * @param leaves           Visited leaf nodes
	 * 
	 * @return                 true if FIFOs were tested for absence of tokens in order to
	 *                         select action (non-deterministc),
	 *                         false if transition is Khan-style (blocking reads sufficient).
	 *                         
	 */
	protected abstract <T extends AbstractValue<T>> 
	boolean createPhase(DemandContext<T> context, 
			                 StateEnumeration<T> stateEnum, 
			                 boolean blockOnNullNode,
			                 List<DecisionTree> leaves);

	/**
	 * @param slice  Slice of program that is used by the decision tree
	 *               
	 * The decision tree is scanned root-to-leaves after conditions that depend
	 * on stateful resources. The leaves of the mode-selection tree are the ModeNodes.
	 * The leaves of the action-selection trees are the ActionNodes (NullNode for
	 * the terminal mode). 
	 */
	public void createDependenceSlice(DependenceSlice slice) {
		// Default (for leaves) is to do nothing
	}
	
	// TODO: do we need this guy?
	protected abstract XlimModule getModule();
		
	/* XmlElement interface */

	protected int getIdentifier() {
		return mIdentifier;
	}

	@Override
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		return "ident=\""+getIdentifier()+"\"";
	}
}
