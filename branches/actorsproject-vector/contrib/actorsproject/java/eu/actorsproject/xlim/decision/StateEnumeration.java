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

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.actorsproject.util.XmlPrinter;
import eu.actorsproject.xlim.absint.AbstractDomain;
import eu.actorsproject.xlim.absint.AbstractValue;
import eu.actorsproject.xlim.absint.BagOfConstraints;
import eu.actorsproject.xlim.absint.ConstraintEvaluator;
import eu.actorsproject.xlim.absint.Context;
import eu.actorsproject.xlim.absint.DemandContext;
import eu.actorsproject.xlim.absint.StateMapping;
import eu.actorsproject.xlim.absint.StateSummary;
import eu.actorsproject.xlim.dependence.DependenceComponent;
import eu.actorsproject.xlim.dependence.Location;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.StateLocation;

/**
 * Drives the enumeration of abstract state-space:
 * 1) Abstract state is stored per Mode (a mode is a subtree of the
 *    action scheduler/decision tree in which all actions/leaves have
 *    the same consumption and production rates).
 * 2) A worklist of unprocessed Modes is maintained
 * 3) Abstract interpretation of the decision tree and its action nodes
 *    updates the abstract state (and enqueues "dirty" Modes)
 * 4) Enumeration is completed when the abstract state is stable (work
 *    list is empty)
 */
public class StateEnumeration<T extends AbstractValue<T>> {

	private XmlPrinter mPrinter=new XmlPrinter(System.out);
	private boolean mTrace=false;
	
	private Set<ValueNode> mInputNodes;
	private StateMapping mMappingAtRoot;
	private DecisionTree mDecisionTree;
	private AbstractDomain<T> mDomain;
	private ConstraintEvaluator<T> mConstraintEvaluator;
	private DemandContext<T> mInitialState;
	
	private HashMap<ActionNode,StateSummary<T>> mAbstractState=
		new HashMap<ActionNode,StateSummary<T>>();
	private ArrayDeque<ActionNode> mWorkList=new ArrayDeque<ActionNode>();
	private HashSet<ActionNode> mOnWorkList=new HashSet<ActionNode>();
	private Map<ActionNode,DependenceComponent> mActionNodeComponents;

	public StateEnumeration(DecisionTree decisionTree, 
			                Map<ActionNode,DependenceComponent> actionNodeComponents,
			                Set<ValueNode> inputNodes,
			                AbstractDomain<T> domain,
			                ConstraintEvaluator<T> constraintEvaluator) {
		mDecisionTree=decisionTree;
		mActionNodeComponents=actionNodeComponents;
		mInputNodes=inputNodes;
		mMappingAtRoot=new StateMapping(inputNodes);
		mDomain=domain;
		mConstraintEvaluator=constraintEvaluator;
		mInitialState=computeInitialState();
	}
	
	/**
	 * Enumerates the state-space starting from the actor's initial state
	 * 
	 * @param initialState  summary of the actor's initial state
	 */
	public void enumerateStateSpace() {	
		if (mTrace) {
			mPrinter.println("<!-- decision tree -->");
			mPrinter.printElement(mDecisionTree);
			mPrinter.println();
			mPrinter.println("<!-- enumerating state space from initial state -->");
			print(mInitialState);
			mPrinter.increaseIndentation();
		}
		
		// Propagate state from 'root' to action nodes
		mDecisionTree.propagateState(mInitialState,this);
		
		if (mTrace) {
			mPrinter.decreaseIndentation();
		}
		
		while (mWorkList.isEmpty()==false) {
			ActionNode actionNode=mWorkList.remove();
			mOnWorkList.remove(actionNode);
		
			StateSummary<T> inputState=mAbstractState.get(actionNode);
			Context<T> context=mMappingAtRoot.createContext(inputState);
						
			if (mTrace) {
				mPrinter.println("<!-- enumerating state space from action node "
						         + actionNode.getIdentifier() + " -->");
				mPrinter.increaseIndentation();
			}
			
			evaluateActionNode(actionNode, context);

			// Create a new Context and propagate it from the root
			DemandContext<T> newInputContext=
				createInputContext(context, actionNode.getOutputMapping());
			
			if (mTrace) {
				mPrinter.println("<!-- result of evaluation -->");
				print(newInputContext);
				mPrinter.println();
			}		

			// Propagate state from 'root' to action nodes
			mDecisionTree.propagateState(newInputContext,this);

 			if (mTrace) {
				mPrinter.decreaseIndentation();
				mPrinter.println("<!-- end of propagation from action node "
						          + actionNode.getIdentifier() + " -->");
			}
		}
		
		if (mTrace) {
			mPrinter.println("<!-- State-space enumeration complete -->");
			mPrinter.println();
			for (StateSummary<T> summary: mAbstractState.values()) {
				mPrinter.println();
				mPrinter.printElement(summary);
			}
		}
	}
	
	public BagOfConstraints<T> createBagOfConstraints(DemandContext<T> context) {
		return new BagOfConstraints<T>(context,mInputNodes,mConstraintEvaluator);
	}
	
	/**
	 * Updates the state summary of 'actionNode'
	 * 
	 * @param actionNode  an ActionNode (leaf) of the decision tree
	 * @param context     the new mapping from value nodes to abstract values
	 *                    when reaching 'actionNode'
	 */
	public void updateState(ActionNode actionNode, Context<T> context) {
		boolean changed=false;
		StateSummary<T> summary=mAbstractState.get(actionNode);
		
		if (mTrace) {
			mPrinter.println();
			mPrinter.print("<!-- updateState, at action node "
					       + actionNode.getIdentifier());
		}
		
		if (summary==null) {
			summary=new StateSummary<T>("ActionNode "+actionNode.getIdentifier());
			
			mAbstractState.put(actionNode, summary);
			changed=true;
		}
		
		if (mMappingAtRoot.updateSummary(summary, context, false /* don't include ports */)) {
			changed=true;
			
			if (mTrace) {
				mPrinter.println(": (updated) -->");
				mPrinter.printElement(summary);
				mPrinter.println();
			}
		}
		else if (mTrace) {
			mPrinter.println(": (no change) -->");
			mPrinter.println();
		}
		
		if (changed && mOnWorkList.contains(actionNode)==false) {
			mWorkList.add(actionNode);
			mOnWorkList.add(actionNode);
		}
	}

	public ActionSchedule createActionSchedule() {
		SchedulingPhase initialPhase=mDecisionTree.createPhase(mInitialState,this);
		ActionSchedule actionSchedule=new ActionSchedule(initialPhase);

		// Process all actionNodes that were enumerated
		for (Map.Entry<ActionNode, StateSummary<T>> entry: mAbstractState.entrySet()) {
			ActionNode actionNode=entry.getKey();
			StateSummary<T> inputState=entry.getValue();
			Context<T> context=mMappingAtRoot.createContext(inputState);
			evaluateActionNode(actionNode, context);
			
			// Create a new Context and propagate it from the root
			DemandContext<T> newInputContext=
				createInputContext(context, actionNode.getOutputMapping());
			
			SchedulingPhase phase=mDecisionTree.createPhase(newInputContext, this);
			
			actionSchedule.addPhase(actionNode, phase);
		}
		
		return actionSchedule;
	}
		
	public DecisionTree.Characteristic printTransitions(ActionNode actionNode) {
		StateSummary<T> inputState=mAbstractState.get(actionNode);
		Context<T> context=mMappingAtRoot.createContext(inputState);
		
		evaluateActionNode(actionNode, context);
		DemandContext<T> newInputContext=
			createInputContext(context, actionNode.getOutputMapping());
		
		return mDecisionTree.printTransitions(newInputContext,this);
	}
	
	/**
	 * @return the initial state as an input context
	 */
	private DemandContext<T> computeInitialState() {
		DemandContext<T> initialState=new DemandContext<T>(mDomain);
		for (ValueNode input: mMappingAtRoot.getValueNodes()) {
			Location location=input.getLocation();
			assert(location!=null && location.isStateLocation());
			T aValue=mDomain.initialState(location.asStateLocation());
			initialState.put(input, aValue);
		}
		return initialState;
	}
	
	/**
	 * @param actionNode  an ActionNode (leaf) of the decision tree
	 * @param context     a mapping from value nodes to abstract valuese
	 */
	private void evaluateActionNode(ActionNode actionNode,
			                        Context<T> context) {
		// Evaluate actionNodeSlice
		DependenceComponent component=mActionNodeComponents.get(actionNode);
		component.evaluate(context, mDomain);
	}
	
	private DemandContext<T> createInputContext(Context<T> outputContext,
			                                    Map<StateLocation,ValueNode> stateMapping) {
		DemandContext<T> newInputContext=new DemandContext<T>(mDomain);
		for (ValueNode inputNode: mMappingAtRoot.getValueNodes()) {
			Location location=inputNode.getLocation();
			assert(location!=null && location.isStateLocation());
			
			ValueNode outputNode=stateMapping.get(location.asStateLocation());
			if (outputNode==null) {
				// actionNode transparent w.r.t. carrier (use input)
				outputNode=inputNode; 
			}
			T aValue=outputContext.get(outputNode);
			newInputContext.put(inputNode, aValue);
		}
		return newInputContext;
	}
	
	private void print(Context<T> context) {
		StateSummary<T> summary=mMappingAtRoot.createStateSummary(context, 
                                                                  false /* no ports */);
		mPrinter.printElement(summary);
	}
}
