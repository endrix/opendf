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
import java.util.Collection;
import java.util.Iterator;

import eu.actorsproject.util.Linkage;
import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimBlockElement;
import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.absint.AbstractDomain;
import eu.actorsproject.xlim.absint.Context;
import eu.actorsproject.xlim.dependence.CallSite;
import eu.actorsproject.xlim.dependence.FixupContext;
import eu.actorsproject.xlim.dependence.InputValueIteration;
import eu.actorsproject.xlim.dependence.Location;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueOperator;
import eu.actorsproject.xlim.dependence.ValueUsage;
import eu.actorsproject.xlim.io.XlimAttributeList;
import eu.actorsproject.xlim.io.ReaderContext;

class Operation extends Linkage<AbstractBlockElement> 
                       implements XlimOperation, Instruction, AbstractBlockElement {

	protected OperationKind mKind;
	private ContainerModule mParent;
	private ArrayList<InputPort> mInputs;
	private ArrayList<OutputPort> mOutputs;
	
	public Operation(OperationKind kind,
	                 Collection<? extends XlimSource> inputs,
	                 Collection<? extends XlimOutputPort> outputs,
	                 ContainerModule parent) {
		mKind=kind;
		mParent=parent;
		mInputs = new ArrayList<InputPort>(inputs.size());
		mOutputs = new ArrayList<OutputPort>(outputs.size());
		for (XlimSource source: inputs)
			mInputs.add(new InputPort(source,this));
		for (XlimOutputPort xlimOut: outputs) {
			if ((xlimOut instanceof OutputPort)==false)
				throw new IllegalArgumentException();
			OutputPort out=(OutputPort) xlimOut;
			mOutputs.add(out);
			out.setParent(this);
		}
    }
	
	/*
	 * Implementation of XlimInstruction
	 */
	
	@Override
	public ContainerModule getParentModule() {
		return mParent;
	}
	
	
	public XlimModule usedInModule(ValueUsage usage) {
		return mParent;
	}

	@Override
	public void setParentModule(ContainerModule parent) {
		mParent=parent;
	}

	public Iterable<InputPort> getInputPorts() {
		return mInputs;
	}

	@Override
	public int getNumInputPorts() {
		return mInputs.size();
	}
	
	@Override
	public InputPort getInputPort(int n) {
		return mInputs.get(n);
	}
	
	public Iterable<OutputPort> getOutputPorts() {
		return mOutputs;
	}
	
	@Override
	public int getNumOutputPorts() {
		return mOutputs.size();
	}
	
	@Override
	public OutputPort getOutputPort(int n) {
		return mOutputs.get(n);
	}	
	
	@Override
	public Operation isOperation() {
		return this;
	}
	
	@Override
	public boolean dependsOnLocation() {
		return mKind.dependsOnLocation(this);
	}
	
	
	public boolean modifiesLocation() {
		return mKind.modifiesLocation(this);
	}

	protected ValueUsage getStateAccessViaAttribute() {
		return null;
	}
	
	protected ValueNode getSideEffectViaAttribute() {
		return null;
	}
	
	@Override
	public boolean modifiesState() {
		return mKind.mayModifyState(this);
	}

	@Override
	public boolean isReferenced() {
		for (int i=0; i<getNumOutputPorts(); ++i)
			if (getOutputPort(i).isReferenced())
				return true;
		return false;
	}
		
	/*
	 * Implementation of XlimOperation
	 */
		
	@Override
	public String getKind() {
		return mKind.getKindAttribute();
	}

	OperationKind getOperationKind() {
		return mKind;
	}
	
	@Override
	public Long getIntegerValueAttribute() {
		return null;
	}

	@Override
	public String getValueAttribute() {
		return null;
	}
	
	@Override
	public XlimTopLevelPort getPortAttribute() {
		return null;
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public XlimTaskModule getTaskAttribute() {
		return null;
	}

	@Override 
	public CallSite getCallSite() {
		return null;
	}
	
	@Override
	public boolean isRemovable() {
		return true;
	}
	
	@Override
	public boolean hasBlockingStyle() {
		return false;
	}
	
	@Override
	public Object getGenericAttribute() {
		return null;
	}
	
	@Override
	public boolean setIntegerValueAttribute(long value) {
		return false;
	}

	@Override
	public boolean setValueAttribute(String value) {
		return false;
	}
	
	@Override
	public boolean setPortAttribute(XlimTopLevelPort port) {
		return false;
	}

	@Override
	public boolean setLocation(Location location) {
		return false;
	}

	@Override
	public boolean setTaskAttribute(XlimTaskModule task) {
		return false;
	}

	@Override
	public boolean setBlockingStyle() {
		return false;
	}
	
	@Override
	public boolean setGenericAttribute(Object value) {
		return false;
	}
	
	void setAttributes(XlimAttributeList attributes, ReaderContext context) {
		mKind.setAttributes(this,attributes,context);
	}
	
	@Override
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		return mKind.getAttributeDefinitions(this, formatter);
	}


	/*
	 * Implementation of XlimElement
	 */
	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return new PortContainerWrapper(this);
	}

	@Override
	public String getTagName() {
		return "operation";
	}

	/*
	 * Implementation of AbstractBlockElement
	 */
	
	
	/**
	 * Removes all references this operation makes to state variables and output ports
	 */
	@Override
	public Linkage<AbstractBlockElement> getLinkage() {
		return this;
	}

	@Override
	public AbstractBlockElement getElement() {
		return this;
	}
	
	@Override
	public void removeReferences() {
		for (int i=0; i<getNumInputPorts(); ++i) {
			InputPort input=getInputPort(i);
			input.removeReference();
		}
	}

	@Override
	public void substituteStateValueNodes() {
		// Substitute StateValueNodes (definitions) in the operations that use them
		// so that this operation can be moved
		if (modifiesLocation()) {
			for (ValueNode output: getOutputValues()) {
				ValueNode domDef=output.getDominatingDefinition();
				output.substitute(domDef);
			}
		}
	}
	
	/**
	 * Sets (or updates) dependence links (of stateful resources) in Operation
	 * computes the set of exposed uses and new values
	 * @param context (keeps track of exposed uses and new definitions)
	 */
	@Override
	public void fixupAll(FixupContext context) {
		if (dependsOnLocation())
			context.fixup(getUsedValues());
		if (modifiesLocation())
			context.setNewValues(getOutputValues());
	}

	/**
	 * Propagates new values to uses in this element
	 * @param context
	 */
	@Override
	public void propagateNewValues(FixupContext context) {
		if (dependsOnLocation())
			context.propagateNewValues(getUsedValues());
		// Terminate propagation of new values when we find an old definition
		if (modifiesLocation())
			context.endPropagation(getOutputValues());
	}

	/**
	 * Resolves exposed uses by looking for definitions
	 * @param context
	 */
	@Override
	public void resolveExposedUses(FixupContext context) {
		// Look for definitions that match the exposed uses
		if (modifiesLocation())
			context.resolveExposedUses(getOutputValues());
		// Look for use of resources that match exposed uses
		// (and grab the definition they are associated with)
		if (dependsOnLocation())
			context.resolveExposedUsesViaUse(getUsedValues());
	}

	@Override
	public <Result, Arg> Result accept(XlimBlockElement.Visitor<Result, Arg> v, Arg arg) {
		return v.visitOperation(this,arg);
	}

	@Override
	public String toString() {
		return mKind.textRepresentation(this);
	}

	/**
	 * @return additional attributes to show up in debug printouts
	 */
	public String attributesToString() {
		return "";
	}

	/*
	 * Implementation of Instruction
	 */
	
	@Override
	public <Result, Arg> Result accept(ValueOperator.Visitor<Result, Arg> visitor, Arg arg) {
		return visitor.visitOperation(this, arg);
	}

	@Override
	public <T> boolean evaluate(Context<T> context, AbstractDomain<T> domain) {
		return domain.evaluate(this, context);
	}
	
	@Override
	public ValueOperator getValueOperator() {
		return this;
	}
	
	@Override
	public String getOperatorName() {
		return getKind();
	}
	
	@Override
	public Iterable<? extends ValueUsage> getUsedValues() {
		return new Iterable<ValueUsage>() {
			public UsedValuesIterator iterator() { return new UsedValuesIterator(Operation.this); }
		};
	}
	
	@Override
	public Iterable<? extends ValueNode> getInputValues() {
		return new InputValueIteration(getUsedValues());
	}
	
	@Override
	public Iterable<? extends ValueNode> getOutputValues() {
		return new Iterable<ValueNode>() {
			public OutputValueIterator iterator() { return new OutputValueIterator(Operation.this); }
		};
	}
	
	protected static class UsedValuesIterator implements Iterator<ValueUsage> {
		private Iterator<InputPort> mPortIterator;
		private ValueUsage mViaAttribute;
		
		public UsedValuesIterator(Operation op) {
			mPortIterator=op.getInputPorts().iterator();
			mViaAttribute=op.getStateAccessViaAttribute();
		}
		
		public boolean hasNext() {
			return mViaAttribute!=null || mPortIterator.hasNext();
		}
	
		public ValueUsage next() {
			if (mViaAttribute!=null) {
				ValueUsage usage=mViaAttribute;
				mViaAttribute=null;
				return usage;
			}
			else
				return mPortIterator.next().getValueUsage();
		}
	
		public void remove() {
			throw new UnsupportedOperationException();
		}	
	}
	
	protected static class OutputValueIterator implements Iterator<ValueNode> {

		private Iterator<OutputPort> mPortIterator;
		private ValueNode mViaAttribute;
	
		public OutputValueIterator(Operation op) {
			mPortIterator=op.getOutputPorts().iterator();
			mViaAttribute=op.getSideEffectViaAttribute();
		}
		
		public boolean hasNext() {
			return mPortIterator.hasNext() || mViaAttribute!=null;
		}
	
		public ValueNode next() {
			if (mPortIterator.hasNext())
				return mPortIterator.next().getValue();
			else {
				ValueNode result=mViaAttribute;
				mViaAttribute=null;
				return result;
			}
		}
	
		public void remove() {
			throw new UnsupportedOperationException();
		}	
	}
}