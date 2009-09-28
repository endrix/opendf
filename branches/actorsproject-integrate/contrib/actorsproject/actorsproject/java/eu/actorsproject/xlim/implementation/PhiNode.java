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

package eu.actorsproject.xlim.implementation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.NoSuchElementException;


import eu.actorsproject.util.Linkage;
import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimPhiNode;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.dependence.InputValueIteration;
import eu.actorsproject.xlim.dependence.PhiOperator;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueOperator;
import eu.actorsproject.xlim.dependence.ValueUsage;

class PhiNode extends Linkage<PhiNode> implements XlimPhiNode, Instruction, PhiOperator {
	
	private ArrayList<InputPort> mInputs;
	private OutputPort mOutput;
	private PhiContainerModule mParent;
	
	public PhiNode(XlimSource in1, XlimSource in2, XlimOutputPort out, PhiContainerModule parent) {
		if ((out instanceof OutputPort)==false)
			throw new IllegalArgumentException();
		mOutput=(OutputPort) out;
		
		mInputs=new ArrayList<InputPort>(2);
		mInputs.add(new InputPort(in1,this));
		mInputs.add(new InputPort(in2,this));
		
		mOutput.setParent(this);
		mParent = parent;
	}
	
	// Implements Linkage<PhiNode>.getElement()
	@Override
	public PhiNode getElement() {
		return this;
	}

	@Override
	public String getKind() {
		return "phi";
	}
	
	@Override
	public Iterable<InputPort> getInputPorts() {
		return mInputs;
	}

	@Override
	public int getNumInputPorts() {
		return 2;
	}

	@Override
	public InputPort getInputPort(int n) {
		return mInputs.get(n);
	}


	@Override
	public Iterable<OutputPort> getOutputPorts() {
		return Collections.singletonList(mOutput);
	}
	
	@Override
	public int getNumOutputPorts() {
		return 1;
	}

	@Override
	public OutputPort getOutputPort(int n) {
		if (n==0)
			return mOutput;
		else
			throw new NoSuchElementException();
	}

	@Override
	public Operation isOperation() {
		return null;
	}
	
	@Override
	public PhiContainerModule getParentModule() {
		return mParent;
	}
	
	@Override
	public boolean mayAccessState() {
		return (mInputs.get(0).isStateAccess() || mInputs.get(1).isStateAccess());
	}
	
	@Override
	public boolean mayModifyState() {
		return false;
	}
	
	@Override
	public boolean isReferenced() {
		return mOutput.isReferenced();
	}
	
	@Override
	public boolean isRemovable() {
		return true;
	}
	
	@Override
	public String getAttributeDefinitions() {
		return "";
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return new PortContainerWrapper(this);
	}
	
	@Override
	public String getTagName() {
		return "PHI";
	}
	
	/**
	 * Removes all references this phi-node makes to state variables and output ports
	 */
	public void removeReferences() {
		mInputs.get(0).removeReference();
		mInputs.get(1).removeReference();
	}
	
	@Override
	public PhiOperator getValueOperator() {
		return this;
	}
	
	@Override
	public String getOperatorName() {
		return "phi";
	}
	
	@Override
	public Iterable<ValueUsage> getUsedValues() {
		ArrayList<ValueUsage> usedValues=new ArrayList<ValueUsage>(3);
		usedValues.add(mInputs.get(0).getValueUsage());
		usedValues.add(mInputs.get(1).getValueUsage());
		usedValues.add(mParent.getTestModule().getValueUsage());
		return usedValues;
	}
	
	@Override
	public XlimModule usedInModule(ValueUsage usage) {
		/*
		 * Phi-nodes are special in that the usage is 
	     * attributed to the predecessor that corresponds to the usage
		 */
		if (usage==mInputs.get(0).getValueUsage()) {
			return mParent.predecessorModule(0); 
		}
		else if (usage==mInputs.get(1).getValueUsage()) {
			return mParent.predecessorModule(1);
		}
		else {
			TestModule testModule=mParent.getTestModule();
			if (usage==testModule.getValueUsage())
				return testModule;
			else
				throw new IllegalArgumentException("Value not used by this operator: "
						                            +usage.getValue().getUniqueId());
		}
	}
	
	@Override
	public ValueUsage getUsedValue(int path) {
		assert(path==0 || path==1);
		return mInputs.get(path).getValueUsage();
	}
	
	@Override
	public Iterable<ValueNode> getInputValues() {
		return new InputValueIteration(getUsedValues());
	}

	@Override
	public ValueNode getInputValue(int path) {
		assert(path==0 || path==1);
		return mInputs.get(path).getValue();
	}
	
	@Override
	public ValueNode getControlDependence() {
		return mParent.getTestModule().getValueUsage().getValue();
	}

	@Override
	public Iterable<ValueNode> getOutputValues() {
		return Collections.singletonList(mOutput.getValue());
	}
		
	@Override
	public ValueNode getOutput() {
		return mOutput.getValue();
	}

	@Override
	public boolean inLoop() {
		return false;
	}

	@Override
	public <Result, Arg> Result accept(ValueOperator.Visitor<Result, Arg> visitor, Arg arg) {
		return visitor.visitPhi(this, arg);
	}
	
	public String toString() {
		return mOutput.getUniqueId()+"=phi("
			+ mInputs.get(0).getSource().getUniqueId()
			+ "," + mInputs.get(1).getSource().getUniqueId() + ")";
	}
	
	public String attributesToString() {
		return "";
	}
}
