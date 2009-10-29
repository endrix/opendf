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

package eu.actorsproject.xlim.dependence;

import java.util.ArrayList;
import java.util.Collections;

import eu.actorsproject.util.Linkage;
import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.XlimPhiContainerModule;
import eu.actorsproject.xlim.absint.AbstractDomain;
import eu.actorsproject.xlim.absint.Context;

public class SideEffectPhiOperator extends Linkage<SideEffectPhiOperator> implements PhiOperator {

	private ArrayList<ValueUsage> mInputs;
	private SideEffectJoin mOutput;
	private XlimPhiContainerModule mParentModule;
	private Location mLocation;
	private boolean mIsLoopJoin;
	
	public SideEffectPhiOperator(XlimPhiContainerModule parent, Location location, boolean isLoopJoin) {
		mOutput=new SideEffectJoin();
		mParentModule=parent;
		mLocation=location;
		mIsLoopJoin=isLoopJoin;
		mInputs=new ArrayList<ValueUsage>(2);
		mInputs.add(new JoinStateUsage());
		mInputs.add(new JoinStateUsage());
	}

	private ValueUsage getTestValueUsage() {
		return mParentModule.getTestModule().getValueUsage();
	}
	
	@Override
	public Iterable<ValueUsage> getUsedValues() {
		ArrayList<ValueUsage> usedValues=new ArrayList<ValueUsage>(3);
		usedValues.add(mInputs.get(0));
		usedValues.add(mInputs.get(1));
		usedValues.add(getTestValueUsage());
		return usedValues;
	}
	
	@Override
	public ValueUsage getUsedValue(int path) {
		assert(path==0 || path==1);
		return mInputs.get(path);
	}
	
	@Override
	public Iterable<? extends ValueNode> getInputValues() {
		return new InputValueIteration(getUsedValues());
	}

	@Override
	public ValueNode getInputValue(int path) {
		assert(path==0 || path==1);
		return mInputs.get(path).getValue();
	}
	
	@Override
	public Iterable<? extends ValueNode> getOutputValues() {
		return Collections.singletonList(mOutput);
	}

	@Override
	public ValueNode getOutput() {
		return mOutput;
	}
	
	@Override
	public XlimModule getParentModule() {
		// return the enclosing loop/if module
		return mParentModule;
	}	
	
	
	@Override
	public XlimModule usedInModule(ValueUsage usage) {
		/*
		 * Phi-nodes are special in that the usage is 
	     * attributed to the predecessor that corresponds to the usage
		 */
		if (usage==mInputs.get(0)) {
			return mParentModule.predecessorModule(0); 
		}
		else if (usage==mInputs.get(1)) {
			return mParentModule.predecessorModule(1);
		}
		else if (usage==getTestValueUsage())
			return mParentModule.getTestModule();
		else
			throw new IllegalArgumentException("Value not used by this operator: "+usage.getValue().getUniqueId());
	}

	@Override
	public void removeReferences() {
		ValueNode dominatingDefinition=mOutput.getDominatingDefinition();
		mInputs.get(0).setValue(null);
		mInputs.get(1).setValue(null);
		// Replace possible remaining uses
		mOutput.substitute(dominatingDefinition);
	}
	
	@Override
	public ValueNode getControlDependence() {
		return getTestValueUsage().getValue();
	}

	@Override
	public boolean inLoop() {
		return mIsLoopJoin;
	}

	
	@Override
	public <Result, Arg> Result accept(Visitor<Result, Arg> visitor, Arg arg) {
		return visitor.visitPhi(this, arg);
	}

	@Override
	public <T> boolean evaluate(Context<T> context, AbstractDomain<T> domain) {
		return domain.evaluate(this, context);
	}

	@Override
	public String getOperatorName() {
		return "state-phi";
	}
	
	/**
	 * @return additional attributes to show up in debug printouts
	 */
	@Override 
	public String attributesToString() {
		Location location=mOutput.actsOnLocation();
		return location.getDebugName();
	}
	
	public String toString() {
		ValueNode decision=getControlDependence();
		String ctrlDep = (decision!=null)? ","+decision.getUniqueId() : "";
		return mOutput.getUniqueId() + "=state-phi(" + attributesToString() + ";" +
		       mInputs.get(0).getValue().getUniqueId() + "," +
		       mInputs.get(1).getValue().getUniqueId() + ctrlDep + ")";	
	}
	
	
	@Override
	public String getTagName() {
		return "state-phi";
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		ArrayList<XmlElement> children=new ArrayList<XmlElement>(3);
		children.add(mInputs.get(0));
		children.add(mInputs.get(1));
		children.add(mOutput);
		return children;
	}

	@Override
	public String getAttributeDefinitions() {
		return "";
	}
	
	/* implementation of Linkage */
	public SideEffectPhiOperator getElement() {
		return this;
	}
	
	private class JoinStateUsage extends ValueUsage {
		
		JoinStateUsage() {
			super(null /* value not set yet */);
		}
		
		@Override
		public boolean needsFixup() {
			return true;
		}
		
		@Override
		public Location getFixupLocation() {
			return mLocation;
		}
		
		@Override
		public ValueOperator usedByOperator() {
			return SideEffectPhiOperator.this;
		}
	}
	
	private class SideEffectJoin extends SideEffect {
		
		@Override
		public Location actsOnLocation() {
			return mLocation;
		}
		@Override
		public ValueOperator getDefinition() {
			return SideEffectPhiOperator.this;
		}

		@Override
		public ValueNode getDominatingDefinition() {
			// Traverse along the path of input value #0 
			// (avoid back-edge of loop, which is #1)
			// Until we're in a module that encloses the test module
			XlimModule container=SideEffectPhiOperator.this.getParentModule();
			ValueNode value=getInputValue(0);
			ValueOperator def=value.getDefinition();
			while (def!=null && container.leastCommonAncestor(def.getParentModule())==container) {
				value=value.getDominatingDefinition();
				def=value.getDefinition();
			}
			
			return value;
		}
	}
}
