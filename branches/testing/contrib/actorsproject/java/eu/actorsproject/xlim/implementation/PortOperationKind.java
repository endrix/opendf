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

import java.util.Collection;
import java.util.List;


import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimStateCarrier;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.dependence.StateValueNode;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueOperator;
import eu.actorsproject.xlim.dependence.ValueUsage;
import eu.actorsproject.xlim.io.XlimAttributeList;
import eu.actorsproject.xlim.io.ReaderContext;
import eu.actorsproject.xlim.type.Signature;
import eu.actorsproject.xlim.type.TypeKind;
import eu.actorsproject.xlim.type.TypeRule;
import eu.actorsproject.xlim.type.VoidTypeRule;
import eu.actorsproject.xlim.util.Session;

class PortOperationKind extends OperationKind {
	private String mPortAttributeName;
	private boolean mModifiesPort;
	private String mIntAttributeName;
	
	PortOperationKind(String kind,
	                  TypeRule typeRule,
	                  String portAttributeName,
	                  boolean modifiesPort,
	                  String intAttributeName) {
		super(kind,typeRule);
		mPortAttributeName=portAttributeName;
		mModifiesPort=modifiesPort;
		mIntAttributeName=intAttributeName;
	}
	
	@Override
	public boolean mayAccessState(Operation op) {
		return true;
	}
	
	@Override
	public boolean mayModifyState(Operation op) {
		return mModifiesPort;
	}
	
	@Override
	public Operation create(List<? extends XlimSource> inputs,
			                    List<? extends XlimOutputPort> outputs,
			                    ContainerModule parent) {
		if (mModifiesPort)
			return new PortModificationOperation(this,inputs,outputs,parent);
		else
			return new PortAccessOperation(this,inputs,outputs,parent);
	}
	
	@Override
	public String getAttributeDefinitions(XlimOperation op) {
		XlimTopLevelPort port=op.getPortAttribute();
		assert(port!=null);
		String result=super.getAttributeDefinitions(op)
			+" "+mPortAttributeName+"=\""+port.getSourceName()+"\"";
		if (mIntAttributeName!=null) {
			Long l=op.getIntegerValueAttribute();
			if (l!=null)
				result+=" "+mIntAttributeName+"=\""+l+"\"" ;
		}
		if (op.hasBlockingStyle())
			result+=" style=\"blocking\"";
		else if (mModifiesPort)
			result+=" style=\"simple\"";
		if (op.isRemovable()==false)
			result+=" removable=\"no\"";
		return result;
	}
	
	@Override
	public void setAttributes(XlimOperation op,
			                  XlimAttributeList attributes, 
			                  ReaderContext context) {
		// Set port attribute
		String portName=attributes.getAttributeValue(mPortAttributeName);
		XlimTopLevelPort port=context.getTopLevelPort(portName);
		if (port!=null)
		    op.setPortAttribute(port);
		// Possibly there is also an integer attribute
		if (mIntAttributeName!=null) {
			Long attrib=getIntegerAttribute(mIntAttributeName,attributes);
			op.setIntegerValueAttribute(attrib);
		}
	}
}

/**
 * Represents the typerules
 * pinRead: void -> PortType, and
 * pinPeek: integer -> PortType
 */
class PinReadTypeRule extends TypeRule {

	PinReadTypeRule(Signature signature) {
		super(signature);
	}
	
	
	@Override
	public boolean matchesOutputs(List<? extends XlimOutputPort> outputs) {
		return outputs.size()==1;
	}


	@Override
	public int defaultNumberOfOutputs() {
		return 1;
	}
	
	
	@Override
	public XlimType defaultOutputType(List<? extends XlimSource> inputs, int i) {
		// Not possible to deduce ("port" attribute needed)
		return null;
	}

	@Override
	public XlimType actualOutputType(XlimOperation op, int i) {
		return op.getPortAttribute().getType();
	}

	@Override
	public boolean typecheck(XlimOperation op) {
		if (op.getPortAttribute()!=null) {
		    XlimType t=op.getOutputPort(0).getType();
		    // TODO: to strong to require exact match (e.g. different integer widths)?
		    return t==actualOutputType(op, 0);
		}
		else
			return true; // "port" required (defer typecheck)
	}
}

/**
 * Represents
 * pinWrite: T -> void, T assignable to port
 */
class PinWriteTypeRule extends VoidTypeRule {
	
	PinWriteTypeRule(Signature signature) {
		super(signature);
	}
	
	@Override
	public boolean typecheck(XlimOperation op) {
		XlimTopLevelPort port=op.getPortAttribute();
		if (port!=null) {
		    XlimType inT=op.getInputPort(0).getSource().getSourceType();
		    XlimType portT=port.getType();
		    TypeKind portKind=Session.getTypeFactory().getTypeKind(portT.getTypeName());
		    return portKind.hasConversionFrom(inT);
		}
		else
			return true; // "port" needed (defer typecheck)
	}
}

class PortAccessOperation extends Operation {
	protected XlimTopLevelPort mPort;
	private ValueUsage mPortAccess;
	
	public PortAccessOperation(OperationKind kind,
                               Collection<? extends XlimSource> inputs,
                               Collection<? extends XlimOutputPort> outputs,
                               ContainerModule parent) {
		super(kind,inputs,outputs,parent);
	}
	
	@Override
	public XlimTopLevelPort getPortAttribute() {
		return mPort;
	}
	
	@Override
	public boolean setPortAttribute(XlimTopLevelPort port) {
		if (mPort!=null)
		    throw new IllegalStateException("port attribute already set"); // Do this once!
		mPort=port;
		mPortAccess=new PortUsage();
		mKind.doDeferredTypecheck(this);
		return true;
	}
	
	protected ValueUsage getStateAccessViaAttribute() {
		return mPortAccess;
	}
	
	@Override
	public void removeReferences() {
		super.removeReferences();
		if (mPortAccess!=null) {
			mPortAccess.setValue(null);
			mPortAccess=null;
		}
	}
	
	/**
	 * @return additional attributes to show up in debug printouts
	 */
	@Override
	public String attributesToString() {
		XlimTopLevelPort port=getPortAttribute();
		if (port!=null)
			return port.getSourceName();
		else
			return "";
	}
	
	private class PortUsage extends ValueUsage {
		public PortUsage() {
			super(null /*value not set yet*/);
		}
		
		@Override
		public ValueOperator usedByOperator() {
			return PortAccessOperation.this;
		}
		
		@Override
		public XlimStateCarrier getStateCarrier() {
			return mPort;
		}
	}
}

class PortModificationOperation extends PortAccessOperation {
	private ValueNode mSideEffect;
	private boolean mBlockingStyle;
	
	public PortModificationOperation(OperationKind kind,
			Collection<? extends XlimSource> inputs,
			Collection<? extends XlimOutputPort> outputs,
			ContainerModule parent) {
		super(kind,inputs,outputs,parent);
	}
	
	@Override
	public boolean setPortAttribute(XlimTopLevelPort port) {
		super.setPortAttribute(port);
		// We trust the super class to checks that we can set a new port
		mSideEffect=new PortSideEffect();
		return true;
	}
	
	@Override
	public boolean isRemovable() {
		return false;
	}
	
	@Override
	public boolean hasBlockingStyle() {
		return mBlockingStyle;
	}
	
	@Override
	public boolean setBlockingStyle() {
		mBlockingStyle=true;
		return true;
	}
	
	@Override
	public void removeReferences() {
		// When removing the side-effect associated with this operation,
		// substitute the previous (input) value of the port
		// for the references made to the value of the removed side-effect
		ValueUsage input=getStateAccessViaAttribute();
		if (input!=null) {
			mSideEffect.substitute(input.getValue());
		}
		super.removeReferences();
	}
	
	@Override
	protected ValueNode getSideEffectViaAttribute() {
		return mSideEffect;
	}		
	
	private class PortSideEffect extends StateValueNode {
		@Override
		public XlimStateCarrier getStateCarrier() {
			return mPort;
		}
		
		@Override
		public ValueOperator getDefinition() {
			return PortModificationOperation.this;
		}

		@Override
		public ValueNode getDominatingDefinition() {
			ValueUsage input=getStateAccessViaAttribute();
			return input.getValue();
		}		
	}
}


