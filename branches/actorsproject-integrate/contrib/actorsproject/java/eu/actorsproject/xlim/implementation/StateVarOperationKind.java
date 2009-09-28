package eu.actorsproject.xlim.implementation;

import java.util.Collection;
import java.util.List;

import org.w3c.dom.NamedNodeMap;

import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimStateCarrier;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.dependence.StateValueNode;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueOperator;
import eu.actorsproject.xlim.dependence.ValueUsage;
import eu.actorsproject.xlim.io.ReaderContext;
import eu.actorsproject.xlim.type.Signature;
import eu.actorsproject.xlim.type.TypeKind;
import eu.actorsproject.xlim.type.TypeRule;
import eu.actorsproject.xlim.type.VoidTypeRule;
import eu.actorsproject.xlim.util.Session;



class StateVarOperationKind extends OperationKind {
	private String mAttributeName;
	private boolean mModifiesState;
	
	StateVarOperationKind(String kind, 
	                      TypeRule typeRule,
	                      boolean modifiesState,
	                      String attributeName) {
		super(kind,typeRule);
		mModifiesState=modifiesState;
		mAttributeName=attributeName;
	}
	
	@Override
	public boolean mayAccessState(Operation op) {
		return true;
	}
	
	@Override
	public boolean mayModifyState(Operation op) {
		return mModifiesState;
	}
	
	@Override
	public Operation create(List<? extends XlimSource> inputs,
			                    List<? extends XlimOutputPort> outputs,
			                    ContainerModule parent) {
		if (mModifiesState)
			return new AssignOperation(this,inputs,outputs,parent);
		else
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

/**
 * Represents the typerule
 * var_ref: integer -> ElementType
 */
class VarRefTypeRule extends TypeRule {

	VarRefTypeRule(Signature signature) {
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
		// The type of a state variable (initializer) is constructed using "list" and "int"
		// There is thus no way of telling different types apart (output type must be provided)
		throw new UnsupportedOperationException("VarRefTypeRule: defaultOutputType");
	}

	@Override
	public XlimType actualOutputType(XlimOperation op, int i) {
		return op.getStateVarAttribute().getInitValue().getCommonElementType();
	}

	@Override
	public boolean typecheck(XlimOperation op) {
		if (op.getStateVarAttribute()!=null) {
		    XlimType t=op.getOutputPort(0).getType();
		    // TODO: to strong to require exact match (e.g. different integer widths)?
		    return t==actualOutputType(op, 0);
		}
		else
			return true; // "port" required (defer typecheck)
		// The type of a state variable (initializer) is constructed using "list" and "int"
		// There is thus no way of telling different types apart (output type must be provided)
		// return op.getOutputPort(0).getType()!=null;
	}
}

/**
 * Represents
 * assign: T -> void,         T assignable to state variable
 * assign: integer,T -> void  T assignable to element of state variable
 */
class AssignTypeRule extends VoidTypeRule {
	
	AssignTypeRule(Signature signature) {
		super(signature);
	}
	
	@Override
	public boolean typecheck(XlimOperation op) {
		XlimStateVar target=op.getStateVarAttribute();
		if (target!=null) {
			int dataPort=op.getNumInputPorts()-1;
		    XlimType inT=op.getInputPort(dataPort).getSource().getSourceType();
		    XlimType targetT=target.getInitValue().getCommonElementType();
		    TypeKind targetKind=Session.getTypeFactory().getTypeKind(targetT.getTypeName());
		    return targetKind.hasConversionFrom(inT);
		}
		else
			return true; // "port" needed (defer typecheck)
	}
}

class StateVarOperation extends Operation {
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
		mKind.doDeferredTypecheck(this);
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


class AssignOperation extends StateVarOperation {
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
		mKind.doDeferredTypecheck(this);
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
		public ValueNode getDominatingDefinition() {
			return mStateVarAccess.getValue();
		}

		@Override 
		public <Result,Arg> Result accept(Visitor<Result,Arg> visitor, Arg arg) {
			boolean killed=(getNumInputPorts()==1);
			ValueNode old=mStateVarAccess.getValue();
			return visitor.visitAssign(this, old, killed, arg);
		}
	}
}

