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
import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.XlimStateCarrier;
import eu.actorsproject.xlim.XlimTestModule;

public class StatePhiOperator extends Linkage<StatePhiOperator> implements PhiOperator {

	private ArrayList<ValueUsage> mInputs;
	private JoinValueNode mOutput;
	private XlimTestModule mTestModule;
	private XlimStateCarrier mStateCarrier;
	private boolean mIsLoopJoin;
	
	public StatePhiOperator(XlimTestModule testModule, XlimStateCarrier carrier, boolean isLoopJoin) {
		mOutput=new JoinValueNode();
		mTestModule=testModule;
		mStateCarrier=carrier;
		mIsLoopJoin=isLoopJoin;
		mInputs=new ArrayList<ValueUsage>(3);
		mInputs.add(new JoinStateUsage());
		mInputs.add(new JoinStateUsage());
		mInputs.add(testModule.getValueUsage());
	}
	
	@Override
	public Iterable<? extends ValueUsage> getUsedValues() {
		return mInputs;
	}
	
	@Override
	public ValueUsage getUsedValue(int path) {
		assert(path==0 || path==1);
		return mInputs.get(path);
	}
	
	@Override
	public Iterable<? extends ValueNode> getInputValues() {
		return new InputValueIteration(mInputs);
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
		return mTestModule.getParentModule();
	}	
	
	@Override
	public void removeReferences() {
		mInputs.get(0).setValue(null);
		mInputs.get(1).setValue(null);
	}
	
	@Override
	public ValueNode getControlDependence() {
		if (mInputs.size()==3)
			return mInputs.get(2).getValue();
		else
			return null;
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
	public String getOperatorName() {
		return "state-phi";
	}
	
	/**
	 * @return additional attributes to show up in debug printouts
	 */
	@Override 
	public String attributesToString() {
		XlimStateCarrier carrier=mOutput.getStateCarrier();
		String name=carrier.getSourceName();
		if (name==null)
			name=carrier.isStateVar().getUniqueId();
		return name;
	}
	
	public String toString() {
		String ctrlDep = (mInputs.size()==3)?
			ctrlDep=";"+mInputs.get(2).getValue().getUniqueId() : "";
		return mOutput.getUniqueId() + "=state-phi(" + attributesToString() + ";" +
		       mInputs.get(0).getValue().getUniqueId() + "," +
		       mInputs.get(1).getValue().getUniqueId() + ctrlDep + ")";	
	}
	
	
	/* implementation of Linkage */
	public StatePhiOperator getElement() {
		return this;
	}
	
	private class JoinStateUsage extends ValueUsage {
		
		JoinStateUsage() {
			super(null /* value not set yet */);
		}
		
		@Override
		public XlimStateCarrier getStateCarrier() {
			return mStateCarrier;
		}
		
		@Override
		public ValueOperator usedByOperator() {
			return StatePhiOperator.this;
		}
	}
	
	private class JoinValueNode extends StateValueNode {
		@Override
		public XlimStateCarrier getStateCarrier() {
			return mStateCarrier;
		}
		
		@Override
		public ValueOperator getDefinition() {
			return StatePhiOperator.this;
		}
	}
}
