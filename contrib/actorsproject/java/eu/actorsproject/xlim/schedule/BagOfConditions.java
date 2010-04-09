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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import eu.actorsproject.xlim.decision2.ActionNode;
import eu.actorsproject.xlim.decision2.AtomicCondition;
import eu.actorsproject.xlim.decision2.AvailabilityTest;
import eu.actorsproject.xlim.decision2.Condition;
import eu.actorsproject.xlim.decision2.Conjunction;
import eu.actorsproject.xlim.decision2.DecisionNode;
import eu.actorsproject.xlim.decision2.DecisionTree;
import eu.actorsproject.xlim.decision2.NullNode;

/**
 * Represents all conditions that appear in a decision tree
 */
public class BagOfConditions implements Iterable<Condition> {

	private List<Condition> mConditions;
	
	public BagOfConditions(DecisionTree t) {
		mConditions=new ArrayList<Condition>();
		
		// Visit all decision nodes and add condition
		DecisionTree.Visitor<Object, Object> visitor=
			new DecisionTree.Visitor<Object, Object>() {
			@Override
			public Object visitDecisionNode(DecisionNode decision, Object dummy) {
				// add the condition of each DecisionNode to the program slice
				addCondition(decision.getCondition());
				decision.getChild(true).accept(this,null);
				decision.getChild(false).accept(this,null);
				return null;
			}
			@Override
			public Object visitActionNode(ActionNode action, Object dummy) {
				return null;
			}
			@Override
			public Object visitNullNode(NullNode node, Object dummy) {
				return null;
			}
		};
		
		t.accept(visitor, null);
	}

	private void addCondition(Condition cond) {
		Condition.Visitor<Object, Object> visitor=new Condition.Visitor<Object, Object>() {
			@Override
			public Object visitAtomicCondition(AtomicCondition cond, Object dummy) {
				return null;
			}
			@Override
			public Object visitAvailabilityTest(AvailabilityTest cond, Object dummy)  {
				return null;
			}
			@Override
			public Object visitConjunction(Conjunction cond, Object dummy) {
				for (Condition term: cond.getTerms())
					addCondition(term);
				return null;
			}
		};
		
		cond.accept(visitor, null);
		mConditions.add(cond);
	}

	@Override
	public Iterator<Condition> iterator() {
		return mConditions.iterator();
	}
}
