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

package eu.actorsproject.xlim.absint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueOperator;
import eu.actorsproject.xlim.dependence.ValueUsage;

/**
 * Maintains the collection of constraints, while
 * creating a "sub-context".
 */
public class BagOfConstraints<T extends AbstractValue<T>> {

	private DemandContext<T> mContext;
	private Set<ValueNode> mInputNodes;
	private Map<ValueNode,T> mInputConstraints;
	private ConstraintEvaluator<T> mConstraintEvaluator;
	
	public BagOfConstraints(DemandContext<T> context, 
			                Set<ValueNode> inputNodes,
			                ConstraintEvaluator<T> constraintEvaluator) {
		mContext=context;
		mInputNodes=inputNodes;
		mConstraintEvaluator=constraintEvaluator;
		mInputConstraints=new HashMap<ValueNode,T>();
	}
	
	/**
	 * @return a sub-context, in which constraints on inputs are set
	 *         and dependent/cached values are re-evaluated
	 */
	public DemandContext<T> createSubContext() {
		if (mInputConstraints.isEmpty())
			return mContext; // don't create a new one
		else {
			DemandContext<T> subContext=mContext.createSubContext();
			Set<ValueOperator> invalidated=new HashSet<ValueOperator>();
			
			// Put new input values in sub-context 
			// and mark dependent values that are cached
			for (ValueNode node: mInputConstraints.keySet()) {
				subContext.put(node, mInputConstraints.get(node));
				findInvalidated(node, invalidated);
			}
			
			// Re-evaluate dependent values in subcontext
			while (invalidated.isEmpty()==false) {
				ValueOperator anOperator=invalidated.iterator().next();
				reEvaluate(anOperator,invalidated,subContext);
			}
			
			return subContext;
		}
	}
	
	/**
	 * @param node  a value node
	 * @return      abstract value of node in parent context
	 * 
	 * The value is specifically not dependent on a possible constraint of
	 * 'node', since this would make the final result dependent on the order,
	 * in which constraints are 'put'
	 */
	public T get(ValueNode node) {
		return mContext.get(node);
	}
	
	/**
	 * @param condition  a condition (value node) 
	 * @param newValue   a constrained value of node (true/false)
	 * @return           true if the bag of constraints remains consistent
	 *                   false if there is no feasible value for 'node'
	 */
	public boolean constrainCondition(ValueNode condition,
			                          boolean newValue) {
		assert(condition.getType().isBoolean());
		T aValue=mConstraintEvaluator.getAbstractValue(newValue);
		return putConstraint(condition, aValue);
	}
	
	/**
	 * @param node       a value node
	 * @param newValue   a constrained value of node
	 * @return           true if the bag of constraints remains consistent
	 *                   false if there is no feasible value for 'node'
	 */
	public boolean putConstraint(ValueNode node, T newValue) {
		if (newValue!=null) {
			T oldValue=mContext.get(node);
		
			// Form intersection?
			if (oldValue!=null) {
				newValue=newValue.intersect(oldValue).getAbstractValue();
				if (newValue==null || newValue.isEmpty())
					return false; // Contradiction: no value possible
				if (newValue.equals(oldValue))
					return true;  // No further constraint of 'node'
			}
			
			// input node?
			if (mInputNodes.contains(node))
				return putInputConstraint(node, newValue);
			else
				return mConstraintEvaluator.evaluate(node, newValue, this);
		}
		else
			return true;  // No further constraint of 'node'
	}
	
	private boolean putInputConstraint(ValueNode node, T newValue) {
		T oldValue=mInputConstraints.get(node);
		
		// Form intersection?
		if (oldValue!=null) {
			newValue=newValue.intersect(oldValue).getAbstractValue();
			if (newValue==null || newValue.isEmpty())
				return false; // Contradiction: no value possible
		}
		
		mInputConstraints.put(node, newValue);
		return true;
	}
	
	private void findInvalidated(ValueNode node, Set<ValueOperator> visited) {
		for (ValueUsage use: node.getUses()) {
			ValueOperator op=use.usedByOperator();
			if (op!=null && visited.contains(op)==false) {
				// Is there a cached evaluation?
				for (ValueNode output: op.getOutputValues())
					if (mContext.hasCachedValue(output)) {
						visited.add(op);
						findInvalidated(output,visited);
					}
			}
		}
	}
	
	private void reEvaluate(ValueOperator op,
			                Set<ValueOperator> invalidated, 
			                DemandContext<T> subContext) {
		// First make sure we have evaluated all inputs
		for (ValueNode input: op.getInputValues()) {
			ValueOperator def=input.getDefinition();
			if (def!=null && invalidated.contains(def))
				reEvaluate(def,invalidated,subContext);
		}
		
		subContext.evaluate(op);
		invalidated.remove(op);
	}
}
