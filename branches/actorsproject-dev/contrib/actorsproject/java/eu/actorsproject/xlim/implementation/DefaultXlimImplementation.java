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
import java.util.Collections;
import java.util.List;

import org.w3c.dom.NamedNodeMap;


import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimStateCarrier;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.dependence.CallNode;
import eu.actorsproject.xlim.dependence.CallSite;
import eu.actorsproject.xlim.dependence.StateValueNode;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueOperator;
import eu.actorsproject.xlim.dependence.ValueUsage;
import eu.actorsproject.xlim.io.ReaderContext;
import eu.actorsproject.xlim.io.ReaderPlugIn;
import eu.actorsproject.xlim.type.TypeFactory;
import eu.actorsproject.xlim.type.TypeKindPlugIn;

// TODO: this class has become overly large and complex, split/simplify!

/**
 * This is the XLIM instruction set, found adhoc by compiling lots of CAL sources.
 * There may be additional supported instructions and attributes that have semantic meaning...
 */

public class DefaultXlimImplementation implements ReaderPlugIn {

	/*
	 *  OPC_PINTOKENCOUNT seen in the wild?
	 *  OPC_PINSTALL seen in the wild?
	 *  OPC_GE   or is it $gte or do both exist?
	 *  OPC_XOR seen in the wild? 
	 */
		
	protected InstructionSet mInstructionSet;
	protected Factory mXlimFactory;
	protected TypeFactory mTypeFactory;
	
	public DefaultXlimImplementation() {
		mInstructionSet=new InstructionSet();
		mTypeFactory=TypeFactory.getInstance();
		mXlimFactory=new Factory(mInstructionSet, mTypeFactory);

		// Type rules
		TypeRule relOp=new RelOpTypeRule();
		TypeRule unaryIntOp=new IntOpTypeRule(1);
		TypeRule binIntOp=new IntOpTypeRule(2);
		TypeRule addOp=new AddOpTypeRule();
		TypeRule divOp=new DivOpTypeRule();
		TypeRule idOp=new IdentityTypeRule();
		
		// Operations found in XLIM spec 1.0 (September 18, 2007)
		register(new PortOperationPlugIn("pinRead",
				                         new DefaultTypeRule(0,1,"pinRead"),
				                         "portName", 
				                         true /* modifies port */, 
				                         null /* no size */));
		register(new PortOperationPlugIn("pinWrite",
				                         new DefaultTypeRule(1,0,"pinWrite"),
				                         "portName", 
				                         true /* modifies port */, 
				                         null /* no size */));
		register(new AssignPlugIn("assign", new AssignTypeRule(), "target"));
		register(new TaskOperationPlugIn("taskCall",
				                         new DefaultTypeRule(0,0,"taskCall"),
				                         "target"));

		// Operations found adhoc
		register(new PortOperationPlugIn("pinPeek",
				                         new DefaultTypeRule(1,1,"pinPeek"),
				                         "portName", 
				                         false /* doesn't modify port */, 
				                         null /* no size */));
		register(new PortOperationPlugIn("pinStatus",
				                         new BooleanOpTypeRule(0),
				                         "portName", 
				                         false /* doesn't modify port */, 
				                         null /* no size */));
		register(new StateVarOperationPlugIn("var_ref",
				                             new DefaultTypeRule(1,1,"var_ref"),
				                             "name"));
		register(new IntegerValueOperationPlugIn("$literal_Integer",
				                                 new DefaultTypeRule(0,1, "$literal_Integer"),
				                                 "value"));
		register(new PlugIn("$add",addOp));
		register(new PlugIn("$and", new RelaxedBooleanOpTypeRule())); // Any number of inputs (>=1)
		register(new PlugIn("bitand",binIntOp));
		register(new PlugIn("$div",divOp));
		register(new PlugIn("$eq",relOp));
		register(new PlugIn("$ge",relOp));
		register(new PlugIn("$gt",relOp));
		register(new PlugIn("$le",relOp));
		register(new PlugIn("$lt",relOp));
		register(new PlugIn("$mul",new MulTypeRule()));
		register(new PlugIn("$ne",relOp));
		register(new PlugIn("$negate",unaryIntOp));
		register(new PlugIn("$not",new BooleanOpTypeRule(1)));
		register(new PlugIn("bitnot",unaryIntOp));
		register(new PlugIn("$or",new BooleanOpTypeRule(2)));
		register(new PlugIn("bitor",binIntOp));
		register(new PlugIn("$sub",addOp));
		register(new PlugIn("bitxor",binIntOp));
		register(new PlugIn("lshift",new IntOpFixedWidthTypeRule(2,33)));
		register(new PlugIn("rshift",divOp));
		register(new PlugIn("urshift",divOp));
		register(new PlugIn("noop",idOp));
		register(new PlugIn("cast",new DefaultTypeRule(1,1,"cast")));
		register(new PlugIn("$selector",new SelectTypeRule()));

		// Added operations (not used by HDL compiler)
		register(new PortOperationPlugIn("pinAvail",
                                         new IntOpFixedWidthTypeRule(0,32),
                                         "portName", 
                                         false /* doesn't modify port */, 
                                         null /* no size */));
		register(new PortOperationPlugIn("pinWait",
				                         new DefaultTypeRule(0,0,"pinWait"),
				                         "portName", 
				                         false /* doesn't modify port */, 
				                         "size"));
		register(new IntegerValueOperationPlugIn("signExtend",
				                                 new DefaultTypeRule(1,1,"signExtend"),
				                                 "size"));
	}

	protected void register(PlugIn plugIn) {
		mInstructionSet.registerOperation(plugIn);
	}
	
	
	/*
	 *  Implementation of ReaderPlugIn
	 */
	
	@Override
	public Factory getFactory() {
		return mXlimFactory;
	}
	
	@Override
	public XlimType getType(String typeName, NamedNodeMap attributes) {
		TypeKindPlugIn typeKind=mTypeFactory.getTypeKindPlugIn(typeName);
		if (typeKind!=null)
			return typeKind.getType(attributes);
		else
			return null;
	}

	@Override
	public void setAttributes(XlimOperation op, 
			                  NamedNodeMap attributes,
			                  ReaderContext context) {
		OperationKind opPlug=mInstructionSet.getOperationPlugIn(op.getKind());
		opPlug.setAttributes(op, attributes, context);
	}
	
	protected static XlimType widest(List<? extends XlimSource> inputs) {
		XlimType result=null;
		int max=0;
		for (XlimSource source: inputs) {
			XlimType t=source.getSourceType();
			int width=t.getSize();
			if (result==null || width>max) {
				result=t;
				max=width;
			}
		}
		assert(result!=null);
		return result;
	}

	/*
	 * All the implementation details follow...
	 */
		
	protected static class PlugIn extends OperationKind {
		private TypeRule mTypeRule;
		
		public PlugIn(String kind, TypeRule typeRule) {
			super(kind);
			mTypeRule=typeRule;
		}
		
		@Override
		public boolean mayModifyState(Operation op) {
			return false;  // This default is overridden in PortPlugIn, AssignPlugIn and TaskOperationPlugIn
		}
		
		@Override
		public Operation create(List<? extends XlimSource> inputs,
				                    List<? extends XlimOutputPort> outputs,
				                    ContainerModule parent) {
			checkInputs(inputs);
			checkOutputs(outputs);
			return new Operation(this,inputs,outputs,parent);
		}	
		
		public List<XlimType> defaultOutputTypes(List<? extends XlimSource> inputs) {
			return mTypeRule.defaultOutputTypes(inputs);
		}

		protected void checkInputs(List<? extends XlimSource> inputs) {
			mTypeRule.checkInputs(inputs, mKindAttribute);
		}
		
		protected void checkOutputs(List<? extends XlimOutputPort> outputs) {
			mTypeRule.checkOutputs(outputs, mKindAttribute);
		}
	}
	
	
	class DefaultTypeRule extends TypeRule {
	
		String mKind;
		
		public DefaultTypeRule(int numInputs, int numOutputs, String kind) {
			super(numInputs, numOutputs);
			mKind=kind;
		}
		
		@Override
		public List<XlimType> defaultOutputTypes(List<? extends XlimSource> inputs) {
			if (inputs.isEmpty()) {
				return Collections.emptyList();
			}
			// pinRead/Peak needs the port to be set, var_ref needs the state variable
			// The output type is part of the specification of cast and sign-extend... 
			throw new UnsupportedOperationException("Cannot deduce type of outputs of "+mKind);
		}
	}
	
	class AssignTypeRule extends DefaultTypeRule {

		public AssignTypeRule() {
			super(2, 0, "assign");
		}

		@Override
		public void checkInputs(List<? extends XlimSource> inputs, String kind) {
			if (inputs.isEmpty() || inputs.size()>2)
				super.checkInputs(inputs, kind);
			// else: one or two inputs
			if (mNumInputs==2)
				checkThatInteger(inputs.get(0).getSourceType(), kind);
		}
	}
	
	class IdentityTypeRule extends TypeRule {
		
		public IdentityTypeRule() {
			super(1,1);
		}
		
		@Override
		public List<XlimType> defaultOutputTypes(List<? extends XlimSource> inputs) {
			return Collections.singletonList(inputs.get(0).getSourceType());
		}
	}
	
	abstract class BooleanOutputTypeRule extends TypeRule {
		
		public BooleanOutputTypeRule(int numInputs) {
			super(numInputs,1);
		}
		
		@Override
		public List<XlimType> defaultOutputTypes(List<? extends XlimSource> inputs) {
			return Collections.singletonList(mTypeFactory.createBoolean());
		}
		
		@Override
		public void checkOutputs(List<? extends XlimOutputPort> outputs, String kind) {
			super.checkOutputs(outputs, kind);
			checkThatBoolean(outputs.get(0).getType(), kind);
		}
	}
	
	class BooleanOpTypeRule extends BooleanOutputTypeRule {
		
		public BooleanOpTypeRule(int numInputs) {
			super(numInputs);
		}
		
		@Override
		public void checkInputs(List<? extends XlimSource> inputs, String kind) {
			super.checkInputs(inputs, kind);
			for (XlimSource source: inputs)
				checkThatBoolean(source.getSourceType(), kind);
		}
	}

	class RelaxedBooleanOpTypeRule extends BooleanOutputTypeRule {

		public RelaxedBooleanOpTypeRule() {
			super(2);
		}
		
		@Override
		public void checkInputs(List<? extends XlimSource> inputs, String kind) {
			if (inputs.isEmpty())
				super.checkInputs(inputs, kind);
			// else: take whatever number of inputs there are 
			for (XlimSource source: inputs)
				checkThatBoolean(source.getSourceType(), kind);
		}
	}
	
	class RelOpTypeRule extends BooleanOutputTypeRule {
		
		public RelOpTypeRule() {
			super(2);
		}
		
		@Override
		public void checkInputs(List<? extends XlimSource> inputs, String kind) {
			super.checkInputs(inputs, kind);
			checkThatIntegerOrBoolean(inputs.get(0).getSourceType(),
					                  inputs.get(1).getSourceType(),
					                  kind);
		}
	}
		
	class IntOpTypeRule extends TypeRule {
		
		public IntOpTypeRule(int numInputs) {
			super(numInputs,1);
		}
		
		@Override
		public List<XlimType> defaultOutputTypes(List<? extends XlimSource> inputs) {
			XlimType result=resultType(inputs);
			return Collections.singletonList(result);
		}
		
		protected XlimType resultType(List<? extends XlimSource> inputs) {
			return widest(inputs);
		}
		
		protected XlimType createInteger(int width) {
			if (width>33)
				width=33;
			return mTypeFactory.createInteger(width);
		}
		
		@Override
		public void checkInputs(List<? extends XlimSource> inputs, String kind) {
			super.checkInputs(inputs, kind);
			for (XlimSource source: inputs)
				checkThatInteger(source.getSourceType(), kind);		}
		
		@Override
		public void checkOutputs(List<? extends XlimOutputPort> outputs, String kind) {
			super.checkOutputs(outputs, kind);
			checkThatInteger(outputs.get(0).getType(), kind);
		}
	}
	
	class AddOpTypeRule extends IntOpTypeRule {
	
		public AddOpTypeRule() {
			super(2);
		}
		
		@Override
		protected XlimType resultType(List<? extends XlimSource> inputs) {
			XlimType result=super.resultType(inputs);
			return createInteger(result.getSize()+1);
		}
	}
	
	class MulTypeRule extends IntOpTypeRule {
		
		public MulTypeRule() {
			super(2);
		}
		
		@Override
		protected XlimType resultType(List<? extends XlimSource> inputs) {
			XlimType t1=inputs.get(0).getSourceType();
			XlimType t2=inputs.get(1).getSourceType();
			return createInteger(t1.getSize()+t2.getSize());
		}
	}
	
	class DivOpTypeRule extends IntOpTypeRule {
		
		public DivOpTypeRule() {
			super(2);
		}
		
		@Override
		protected XlimType resultType(List<? extends XlimSource> inputs) {
			XlimType t1=inputs.get(0).getSourceType();
			return createInteger(t1.getSize());
		}
	}
	
	class IntOpFixedWidthTypeRule extends IntOpTypeRule {
		
		private int mWidth;
		
		public IntOpFixedWidthTypeRule(int numInputs, int widthOutput) {
			super(numInputs);
			mWidth=widthOutput;
		}
	
		@Override
		protected XlimType resultType(List<? extends XlimSource> inputs) {
			return createInteger(mWidth);
		}
	}
	
	class SelectTypeRule extends TypeRule {
		
		public SelectTypeRule() {
			super(3,1);
		}
		
		@Override
		public List<XlimType> defaultOutputTypes(List<? extends XlimSource> inputs) {
			return Collections.singletonList(widest(inputs));
		}
		
		@Override
		public void checkInputs(List<? extends XlimSource> inputs, String kind) {
			super.checkInputs(inputs, kind);
			checkThatBoolean(inputs.get(0).getSourceType(), kind);
			checkThatIntegerOrBoolean(inputs.get(1).getSourceType(),
					                  inputs.get(2).getSourceType(),
					                  kind);
		}
	}
	protected static class PortAccessOperation extends Operation {
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
	
	protected static class PinWaitOperation extends PortAccessOperation {
		private Long mValue;
		private boolean mBlockingStyle;
		
		public PinWaitOperation(OperationKind kind,
                                           Collection<? extends XlimSource> inputs,
                                           Collection<? extends XlimOutputPort> outputs,
                                           ContainerModule parent) {
			super(kind,inputs,outputs,parent);
		}
		
		@Override
		public Long getIntegerValueAttribute() {
			return mValue;
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
		public boolean setIntegerValueAttribute(long value) {
			mValue=value;
			return true;
		}
		
		/**
		 * @return additional attributes to show up in debug printouts
		 */
		@Override
		public String attributesToString() {
			return Long.toString(mValue);
		}
	}
	
	protected static class PortModificationOperation extends PortAccessOperation {
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
		}
	}
	
	protected static class PortOperationPlugIn extends PlugIn {
		private String mPortAttributeName;
		private boolean mModifiesPort;
		private String mIntAttributeName;
		
		public PortOperationPlugIn(String kind,
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
			checkInputs(inputs);
			checkOutputs(outputs);
			if (mModifiesPort)
				return new PortModificationOperation(this,inputs,outputs,parent);
			else if (mIntAttributeName!=null)
				return new PinWaitOperation(this,inputs,outputs,parent);
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
				                  NamedNodeMap attributes, 
				                  ReaderContext context) {
			String portName=getAttribute(mPortAttributeName,attributes);
			XlimTopLevelPort port=context.getTopLevelPort(portName);
			if (port!=null)
			    op.setPortAttribute(port);
			if (mIntAttributeName!=null) {
				Integer attrib=getIntegerAttribute(mIntAttributeName,attributes);
				op.setIntegerValueAttribute(attrib);
			}
		}		
	}
	
	protected static class IntegerValueOperation extends Operation {
		private Long mValue;
		
		public IntegerValueOperation(OperationKind kind,
				Collection<? extends XlimSource> inputs,
				Collection<? extends XlimOutputPort> outputs,
				ContainerModule parent) {
			super(kind,inputs,outputs,parent);
		}
		
		@Override
		public Long getIntegerValueAttribute() {
			return mValue;
		}
		
		@Override
		public boolean setIntegerValueAttribute(long value) {
			mValue=value;
			return true;
		}
		
		@Override
		public String attributesToString() {
			return Long.toString(mValue);
		}
	}
	
	protected static class IntegerValueOperationPlugIn extends PlugIn {
		private String mAttributeName;
		
		public IntegerValueOperationPlugIn(String kind, 
				                           TypeRule typeRule,
				                           String attributeName) {
			super(kind,typeRule);
			mAttributeName=attributeName;
		}
		
		@Override
		public Operation create(List<? extends XlimSource> inputs,
				                    List<? extends XlimOutputPort> outputs,
				                    ContainerModule parent) {
			checkInputs(inputs);
			checkOutputs(outputs);
			return new IntegerValueOperation(this,inputs,outputs,parent);
		}
		
		@Override
		public String getAttributeDefinitions(XlimOperation op) {
			Long value=op.getIntegerValueAttribute();
			if (value!=null)
				return super.getAttributeDefinitions(op)+" "+mAttributeName+"=\""+value+"\"";
			else
				return super.getAttributeDefinitions(op);
		}
		
		@Override
		public void setAttributes(XlimOperation op,
				                  NamedNodeMap attributes, 
				                  ReaderContext context) {
			Integer value=getIntegerAttribute(mAttributeName,attributes);
			if (value!=null)
				op.setIntegerValueAttribute(value);
		}		
	}

	protected static class StateVarOperation extends Operation {
		protected XlimStateVar mStateVar;
		protected ValueUsage mStateVarAccess;
		
		public StateVarOperation(OperationKind kind,
				Collection<? extends XlimSource> inputs,
				Collection<? extends XlimOutputPort> outputs,
				ContainerModule parent) {
			super(kind,inputs,outputs,parent);
		}
	
		@Override
		public XlimStateVar getStateVarAttribute() {
			return mStateVar;
		}
		
		@Override
		public boolean setStateVarAttribute(XlimStateVar stateVar) {
			if (mStateVar!=null)
			    throw new IllegalStateException("state variable attribute already set"); // Do this once!
			mStateVar=stateVar;
			mStateVarAccess=new StateVarUsage();
			return true;
		}

		@Override
		public void removeReferences() {
			super.removeReferences();
			if (mStateVarAccess!=null) {
				mStateVarAccess.setValue(null);
				mStateVarAccess=null;
			}
		}

		protected ValueUsage getStateAccessViaAttribute() {
			return mStateVarAccess;
		}
		
		/**
		 * @return additional attributes to show up in debug printouts
		 */
		@Override
		public String attributesToString() {
			if (mStateVar!=null) {
				String name=mStateVar.getSourceName();
				if (name!=null)
					return name;
				else
					return mStateVar.getUniqueId();
			}
			else
				return "";
		}
		
		protected class StateVarUsage extends ValueUsage {
			StateVarUsage() {
				super(null /*no value yet*/);
			}
			
			@Override
			public ValueOperator usedByOperator() {
				return StateVarOperation.this;
			}
			
			@Override
			public XlimStateCarrier getStateCarrier() {
				return mStateVar;
			}
		}
	}
	
	protected static class StateVarOperationPlugIn extends PlugIn {
		private String mAttributeName;
		
		public StateVarOperationPlugIn(String kind, 
				                       TypeRule typeRule, 
				                       String attributeName) {
			super(kind,typeRule);
			mAttributeName=attributeName;
		}
		
		@Override
		public boolean mayAccessState(Operation op) {
			return true;
		}
		
		@Override
		public Operation create(List<? extends XlimSource> inputs,
				                    List<? extends XlimOutputPort> outputs,
				                    ContainerModule parent) {
			checkInputs(inputs);
			checkOutputs(outputs);
			return new StateVarOperation(this,inputs,outputs,parent);
		}
		
		@Override
		public String getAttributeDefinitions(XlimOperation op) {
			XlimStateVar stateVar=op.getStateVarAttribute();
			if (stateVar!=null)
				return super.getAttributeDefinitions(op)+" "+mAttributeName+"=\""+stateVar.getUniqueId()+"\"";
			else
				return super.getAttributeDefinitions(op);
		}
		
		@Override
		public void setAttributes(XlimOperation op,
				                  NamedNodeMap attributes, 
				                  ReaderContext context) {
			String ident=getAttribute(mAttributeName,attributes);
			XlimStateVar stateVar=context.getStateVar(ident);
			if (stateVar!=null)
			    op.setStateVarAttribute(stateVar);
		}
	}

	protected static class AssignOperation extends StateVarOperation {
		private ValueNode mSideEffect;
		
		public AssignOperation(OperationKind kind,
                Collection<? extends XlimSource> inputs,
                Collection<? extends XlimOutputPort> outputs,
                ContainerModule parent) {
			super(kind,inputs,outputs,parent);
		}
		
		
		@Override
		public boolean setStateVarAttribute(XlimStateVar stateVar) {
			if (mStateVar!=null)
			    throw new IllegalStateException("state variable attribute already set"); // Do this once!
			mStateVar=stateVar;
			mStateVarAccess=new AssignmentStateVarUsage();
			mSideEffect=new AssignmentSideEffect();
			return true;
		}
		
		@Override
		protected ValueNode getSideEffectViaAttribute() {
			return mSideEffect;
		}
				
		@Override
		public void removeReferences() {
			// When removing the side-effect associated with this assignment,
			// substitute the previous (input) value of the state variable
			// for the references made to the value of the removed side-effect
			if (mStateVarAccess!=null) {
				mSideEffect.substitute(mStateVarAccess.getValue());
			}
			super.removeReferences();
		}
		
		private class AssignmentStateVarUsage extends StateVarUsage {
			@Override
			public <Result,Arg> Result accept(Visitor<Result,Arg> visitor, Arg arg) {
				boolean killed=(AssignOperation.this.getNumInputPorts()==1);
				return visitor.visitAssign(this,mSideEffect,killed,arg);
			}
		}
		
		private class AssignmentSideEffect extends StateValueNode {
			@Override
			public XlimStateCarrier getStateCarrier() {
				return mStateVar;
			}
			
			@Override
			public ValueOperator getDefinition() {
				return AssignOperation.this;
			}
			
			@Override 
			public <Result,Arg> Result accept(Visitor<Result,Arg> visitor, Arg arg) {
				boolean killed=(getNumInputPorts()==1);
				ValueNode old=mStateVarAccess.getValue();
				return visitor.visitAssign(this, old, killed, arg);
			}
		}
	}
	
	protected static class AssignPlugIn extends StateVarOperationPlugIn {
	
		public AssignPlugIn(String kind, 
                            TypeRule typeRule, 
                            String attributeName) {
			super(kind,typeRule,attributeName);
		}
		
		@Override
		public boolean mayAccessState(Operation op) {
			return true;
		}
		
		@Override
		public boolean mayModifyState(Operation op) {
			return true;
		}
		
		@Override
		public Operation create(List<? extends XlimSource> inputs,
				                        List<? extends XlimOutputPort> emptyOutputs,
				                        ContainerModule parent) {
			int arity=inputs.size();
			if (arity!=1 && arity!=2)  // assign either takes one or two inputs
				checkInputs(inputs); // will throw an exception
			checkOutputs(emptyOutputs);
			
			return new AssignOperation(this,inputs,emptyOutputs,parent);
		}
	}
	
	protected static class TaskOperation extends Operation {
		private XlimTaskModule mTask;
		private CallSite mCallSite;
		
		public TaskOperation(OperationKind kind,
				Collection<? extends XlimSource> inputs,
				Collection<? extends XlimOutputPort> outputs,
				ContainerModule parent) {
			super(kind,inputs,outputs,parent);
			mCallSite=new CallSite(this);
		}
		
		@Override
		public XlimTaskModule getTaskAttribute() {
			return mTask;
		}
		
		@Override
		public CallSite getCallSite() {
			return mCallSite;
		}
						
		@Override
		public Iterable<ValueUsage> getUsedValues() {
			return mCallSite.getUsedValues();
		}
		
		@Override
		public Iterable<ValueNode> getInputValues() {
			return mCallSite.getInputValues();
		}
		
		@Override
		public Iterable<? extends ValueNode> getOutputValues() {
			return mCallSite.getOutputValues();
		}
		
		@Override
		public boolean setTaskAttribute(XlimTaskModule task) {
			if (mTask!=null)
				throw new IllegalStateException("task attribute already set"); // Do this once!
			mTask=task;
			CallNode myNode=getParentModule().getTask().getCallNode();
			CallNode calledNode=task.getCallNode();
			myNode.addCallSite(mCallSite);
			calledNode.addCaller(mCallSite);
			return true;
		}
	
		@Override
		public void removeReferences() {
			super.removeReferences();
			mCallSite.remove();
		}
		
		/**
		 * @return additional attributes to show up in debug printouts
		 */
		@Override
		public String attributesToString() {
			if (mTask!=null)
				return mTask.getName();
			else
				return "";
		}
	}
	
	protected static class TaskOperationPlugIn extends PlugIn {
		private String mAttributeName;
		
		public TaskOperationPlugIn(String kind, 
				                   TypeRule typeRule, 
				                   String attributeName) {
			super(kind,typeRule);
			mAttributeName=attributeName;
		}
		
		@Override
		public boolean mayAccessState(Operation op) {
			return true;
		}
		
		@Override
		public boolean mayModifyState(Operation op) {
			return true;
		}
		
		@Override
		public Operation create(List<? extends XlimSource> inputs,
				                    List<? extends XlimOutputPort> outputs,
				                    ContainerModule parent) {
			checkInputs(inputs);
			checkOutputs(outputs);
			return new TaskOperation(this,inputs,outputs,parent);
		}
		
		@Override
		public String getAttributeDefinitions(XlimOperation op) {
			XlimTaskModule task=op.getTaskAttribute();
			if (task!=null) {
				String name=task.getName();
				if (name!=null)
					return super.getAttributeDefinitions(op)+" "+mAttributeName+"=\""+name+"\"";
			}
			return super.getAttributeDefinitions(op);
		}
		
		@Override
		public void setAttributes(XlimOperation op,
				                  NamedNodeMap attributes, 
				                  ReaderContext context) {
			String ident=getAttribute(mAttributeName,attributes);
			XlimTaskModule task=context.getTask(ident);
			if (task!=null) {
			    op.setTaskAttribute(task);
			}
		}
	}
}
