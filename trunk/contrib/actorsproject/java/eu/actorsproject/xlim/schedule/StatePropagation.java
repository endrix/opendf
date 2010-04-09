/* 
 * Copyright (c) Ericsson AB, 2010
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

import java.util.Iterator;

import eu.actorsproject.xlim.absint.AbstractValue;
import eu.actorsproject.xlim.absint.DemandContext;
import eu.actorsproject.xlim.decision2.ActionNode;
import eu.actorsproject.xlim.decision2.Condition;
import eu.actorsproject.xlim.decision2.DecisionNode;
import eu.actorsproject.xlim.decision2.DecisionTree;
import eu.actorsproject.xlim.decision2.NullNode;
import eu.actorsproject.xlim.decision2.PortSignature;

/**
 * Propagates abstract state from the root to the leaves of a decision tree,
 * while asserting/refuting conditions as differnent paths are taken at decisions
 */
public abstract class StatePropagation<Domain extends AbstractValue<Domain>, Result> 
	implements DecisionTree.Visitor<Result, DemandContext<Domain>> {
	
	private boolean mNullNodeMeansBlocking;  // NullNode = blocking or termination?
	private ConditionConstrainer<Domain> mPlugIn;
	
	public StatePropagation(ConditionConstrainer<Domain> plugIn) {
		mPlugIn=plugIn;
	}
	
	/**
	 * @param root   Root of decision tree
	 * @param state  State to be propagated to the leaves
	 * @return result of propagation
	 */
	public Result propagate(DecisionTree root, DemandContext<Domain> state) {
		return root.accept(this, state);
	}
	
	/**
	 * @param cond     a Condition
	 * @param ifTrue   result if condition is true
	 * @param ifFalse  result if condition is false
	 * @return result of decision
	 * 
	 * Unless overridden, null results are created for decisions
	 */
	protected Result createResult(Condition cond, Result ifTrue, Result ifFalse) {
		return null;
	}

	@Override
	public Result visitDecisionNode(DecisionNode decision, DemandContext<Domain> state) {
		// Constrain the condition to true/false and propagate state along corresponding path
		Condition cond=decision.getCondition();
		PortSignature assertedPortSignature=decision.requiredPortSignature();
		Result trueResult;
		
		DemandContext<Domain> trueContext=mPlugIn.constrainCondition(cond,true,state,assertedPortSignature);
		if (trueContext!=null)
			trueResult=decision.getChild(true).accept(this, trueContext);
		else {
			// else: contradiction -path infeasible (we know condition to be false)
			return decision.getChild(false).accept(this, state);
		}
			
		Iterator<? extends Condition> terms=cond.getTerms().iterator();
		return refuteConjunction(terms,decision,state,trueResult);
	}
		
	private Result refuteConjunction(Iterator<? extends Condition> terms,
			                         DecisionNode decision,
			                         DemandContext<Domain> state,
			                         Result trueResult) {
		PortSignature assertedPortSignature=decision.requiredPortSignature();
		Condition term=terms.next();
	
		if (terms.hasNext()) {
			// Recurse while there are more terms that can be refuted
			DemandContext<Domain> trueContext=mPlugIn.constrainCondition(term,true,state,assertedPortSignature);
			assert(trueContext!=null);
			trueResult=refuteConjunction(terms,decision,trueContext,trueResult);
		}
		
		DemandContext<Domain> falseContext=mPlugIn.constrainCondition(term,false,state,assertedPortSignature);
		if (falseContext!=null) {
			// Refuting an AvailabilityTest --> NullNode means blocking (not termination)
			boolean whatNullMeant=mNullNodeMeansBlocking;
			if (term.testsAvailability())
				mNullNodeMeansBlocking=true;
			Result falseResult=decision.getChild(false).accept(this, falseContext);
			mNullNodeMeansBlocking=whatNullMeant;
			return createResult(term,trueResult,falseResult);
		}
		else {
			// else: contradiction -path infeasible- (we know term to be true)
			return trueResult;
		}
	}
	
	/**
	 * @param action  an action
	 * @param state   state propagated to action
	 * @return result of action node (null unless overridden)
	 */
	@Override
	public Result visitActionNode(ActionNode action, DemandContext<Domain> state) {
		return null;
	}

	@Override
	public Result visitNullNode(NullNode node, DemandContext<Domain> state) {
		if (mNullNodeMeansBlocking)
			return visitBlockingNode(node,state);
		else
			return visitTerminalNode(node,state);
	}
	
	/**
	 * @param node   a NullNode, which represents blocking
	 * @param state  state propagated to node
	 * @return result of the blocking NullNode (null unless overridden)
	 */
	
	protected Result visitBlockingNode(NullNode node, DemandContext<Domain> state) {
		return null;
	}
	
	/**
	 * @param node   a NullNode, which represents actor termination
	 * @param state  state propagated to node
	 * @return result of the terminal NullNode (null unless overridden)
	 */
	
	protected Result visitTerminalNode(NullNode node, DemandContext<Domain> state) {
		return null;
	}
}
