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
 
package eu.actorsproject.xlim.schedule;

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
import eu.actorsproject.xlim.decision2.ActionNode;
import eu.actorsproject.xlim.decision2.Condition;
import eu.actorsproject.xlim.decision2.DecisionTree;
import eu.actorsproject.xlim.decision2.NullNode;
import eu.actorsproject.xlim.decision2.PortSignature;
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
		int iterations=1;
		Propagator statePropagation=new Propagator();
		
		if (mTrace) {
			mPrinter.println("<!-- decision tree -->");
			mPrinter.printElement(mDecisionTree);
			mPrinter.println();
			mPrinter.println("<!-- enumerating state space from initial state -->");
			print(mInitialState);
			mPrinter.increaseIndentation();
		}
		
		// Propagate state from 'root' to action nodes
		statePropagation.propagate(mDecisionTree,mInitialState);
		
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
			iterations=iterations+1;
			statePropagation.propagate(mDecisionTree,newInputContext);

 			if (mTrace) {
				mPrinter.decreaseIndentation();
				mPrinter.println("<!-- end of propagation from action node "
						          + actionNode.getIdentifier() + " -->");
			}
		}
		
		if (mTrace) {
			mPrinter.println("<!-- State-space enumeration complete -->");
			mPrinter.println("<!-- Number of iterations: "+iterations+" -->");
			mPrinter.println();
			for (StateSummary<T> summary: mAbstractState.values()) {
				mPrinter.println();
				mPrinter.printElement(summary);
			}
		}
	}
	
	/**
	 * @return the control-flow graph that corresponds to the state enumeration
	 */
	public ControlFlowGraph buildFlowGraph() {
		ControlFlowGraph flowGraph=new ControlFlowGraph();
		FlowGraphBuilder builder=new FlowGraphBuilder(flowGraph);
		BasicBlock initialBlock=builder.propagate(mDecisionTree, mInitialState);
		
		flowGraph.setInitialNode(initialBlock);
	    
		if (mTrace) {
			mPrinter.println("<!-- successors of initial state -->");
	    	mPrinter.printElement(initialBlock.getDecisionTree());
		}
	    
		// Process all actionNodes that were enumerated
		for (Map.Entry<ActionNode, StateSummary<T>> entry: mAbstractState.entrySet()) {
			ActionNode actionNode=entry.getKey();
			StateSummary<T> inputState=entry.getValue();
			Context<T> context=mMappingAtRoot.createContext(inputState);
			evaluateActionNode(actionNode, context);
			
			// Create a new Context and propagate it from the root
			DemandContext<T> newInputContext=
				createInputContext(context, actionNode.getOutputMapping());
			
			BasicBlock successor=builder.propagate(mDecisionTree, newInputContext);
			flowGraph.setSuccessor(actionNode, successor);
		
			if (mTrace) {
				mPrinter.println("<!-- successors of "+actionNode.getDescription()+" -->");
				mPrinter.printElement(successor);
			}
		}
		
		return flowGraph;
	}
	
	
	/**
	 * Updates the state summary of 'actionNode'
	 * 
	 * @param actionNode  an ActionNode (leaf) of the decision tree
	 * @param context     the new mapping from value nodes to abstract values
	 *                    when reaching 'actionNode'
	 */
	private void updateState(ActionNode actionNode, Context<T> context) {
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
		
	private class Constrainer implements ConditionConstrainer<T> {
	
		/**
		 * @param cond                   Condition to be constrained
		 * @param assertedValue          Value (false/true), to which the condition is constrained
		 * @param context                Context, in which to constrain the condition
		 * @param assertedPortSignature  an optional port signature (may be null). If given it
		 *                               expresses a token-availability assertion.
		 *                                
		 * @return a new context, in which the condition is asserted, or null if the assertion lead
		 *         to a contradiction.
		 */
		public DemandContext<T> constrainCondition(Condition cond, 
				                                   boolean assertedValue, 
				                                   DemandContext<T> context,
				                                   PortSignature assertedPortSignature) {
			switch (cond.evaluate(context, assertedPortSignature)) {
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
				BagOfConstraints<T> bag=new BagOfConstraints<T>(context,mInputNodes,mConstraintEvaluator);
			if (bag.constrainCondition(cond.getValue(), assertedValue))
				return bag.createSubContext(); // successful constraint
			else
				return null; // constraint caused inconsistency
			}	
		}
	}
	
	private class Propagator extends StatePropagation<T,Object> {

		Propagator() {
			super(new Constrainer());
		}

		@Override
		public Object visitActionNode(ActionNode action, DemandContext<T> state) {
			updateState(action, state);
			return null;
		}		
	}
	
	private class FlowGraphBuilder extends StatePropagation<T,BasicBlock> {
	
		private ControlFlowGraph mFlowGraph;
		
		FlowGraphBuilder(ControlFlowGraph flowGraph) {
			super(new Constrainer());
			mFlowGraph=flowGraph;
		}

		@Override
		protected BasicBlock createResult(Condition cond, BasicBlock ifTrue, BasicBlock ifFalse) {
			return mFlowGraph.addDecision(cond, ifTrue, ifFalse);
		}

		@Override
		public BasicBlock visitActionNode(ActionNode action, DemandContext<T> state) {
			return mFlowGraph.addAction(action);
		}

		@Override
		protected BasicBlock visitTerminalNode(NullNode node, DemandContext<T> state) {
			return mFlowGraph.addTerminalNode();
		}
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
