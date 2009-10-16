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
import java.util.List;
import java.util.Map;

import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimTestModule;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.absint.AbstractValue;
import eu.actorsproject.xlim.absint.Context;

/**
 * Represents the condition: c1 && c2 && ... && cn
 */
public class Conjunction extends Condition {

	private ArrayList<Condition> mConditions;
	
	public Conjunction(XlimOutputPort xlimCond) {
		super(xlimCond, xlimCond.getValue());
		mConditions=new ArrayList<Condition>();
	}
	
	private Conjunction(XlimOutputPort xlimCond, ArrayList<Condition> conditions) {
		super(xlimCond, xlimCond.getValue());
		mConditions=conditions;
	}
	
	@Override
	public void addTo(Conjunction conjunction) {
		conjunction.mConditions.addAll(mConditions);
	}
	
	protected void add(Condition condition) {
		mConditions.add(condition);
	}

	@Override
	protected boolean alwaysTrue() {
		for (Condition cond: mConditions)
			if (cond.alwaysTrue()==false)
				return false;
		return true;
	}

	public <T extends AbstractValue<T>> 
	Evaluation evaluate(Context<T> context, PortSignature optSignature) {
		Evaluation result=Evaluation.ALWAYS_TRUE;
		
		for (Condition cond: mConditions) {
			switch (cond.evaluate(context, optSignature)) {
			case TRUE_OR_FALSE:
				result=Evaluation.TRUE_OR_FALSE;
				break;
			case ALWAYS_FALSE:
				return Evaluation.ALWAYS_FALSE;
			case ALWAYS_TRUE:
			default:
			}
		}
		return result;
	}
	
	@Override
	public boolean isImpliedBy(PortSignature portSignature) {
		for (Condition cond: mConditions) {
			if (!cond.isImpliedBy(portSignature))
				return false;
		}
		return true;
	}
	
	@Override
	public boolean dependsOnInput(PortSignature availableTokens) {
		for (Condition cond: mConditions) {
			if (cond.dependsOnInput(availableTokens))
				return true;
		}
		return false;
	}
	
	@Override
	protected void findPinAvail(Map<XlimTopLevelPort, XlimOperation> pinAvailMap) {
		for (Condition cond: mConditions) 
			cond.findPinAvail(pinAvailMap);
	}
	
	@Override
	protected void updateDominatingTests(Map<XlimTopLevelPort, Integer> testsInAncestors) {
		for (Condition cond: mConditions)
			cond.updateDominatingTests(testsInAncestors);
		
	}

	@Override
	protected Condition removeRedundantTests(PortSignature portSignature, 
			                                 XlimTestModule testModule) {
		// We should have checked that there is something left of the condition...
		assert(isImpliedBy(portSignature)==false);
		
		// First check if we can keep the condition "as is"
		boolean keepAsIs=true;
		for (Condition cond: mConditions)
			if (cond.isImpliedBy(portSignature)) {
				keepAsIs=false;
				break;
			}
		
		if (keepAsIs)
			return this;
		else {
			// Update condition
			ArrayList<Condition> newConditions=new ArrayList<Condition>();
			
			for (Condition cond: mConditions) {
				if (cond.isImpliedBy(portSignature)==false)
					newConditions.add(cond);
			}
			
			if (newConditions.isEmpty())
				return null;
			else if (newConditions.size()==1)
				return newConditions.get(0);
			else {
				List<XlimSource> sources=new ArrayList<XlimSource>();
				for (Condition cond: newConditions)
					sources.add(cond.getXlimSource());
				
				// Add an updated $and operation
				testModule.startPatchAtEnd();
				XlimOperation xlimAnd=testModule.addOperation("$and", sources);
				XlimOutputPort result=xlimAnd.getOutputPort(0);
				testModule.setDecision(result);
				testModule.completePatchAndFixup();
				
				return new Conjunction(result, newConditions);
			}
		}
	}

// TODO: needs fixing if we need this one (specialized Conjunctions have no ValueNode)
//	@Override
//	protected Condition specialize(Mode mode) {
//		assert(isImpliedByMode(mode)==false); // There should be something left...
//		ArrayList<Condition> conditions=new ArrayList<Condition>();
//		boolean changed=false;
//		
//		// Remove implied condition, keep specialized ones	
//		for (Condition cond: mConditions) {
//			if (cond.isImpliedByMode(mode))
//				changed=true;
//			else {
//				Condition newCond=cond.specialize(mode);
//				conditions.add(newCond);
//				if (newCond!=cond)
//					changed=true;
//			}
//		}
//				
//		if (conditions.size()==1)
//			return conditions.get(0);
//		else if (changed)
//			return new Conjunction(conditions);
//		else
//			return this; // No specialization 
//	}
	
	/* XmlElement interface */

	@Override
	public String getTagName() {
		return "conjunction";
	}
	
	public String getAttributeDefinitions() {
		return "";
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return mConditions;
	}	
}