package eu.actorsproject.xlim.implementation;

import java.util.Collection;
import java.util.List;


import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.XlimTypeKind;
import eu.actorsproject.xlim.dependence.Location;
import eu.actorsproject.xlim.dependence.SideEffect;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueOperator;
import eu.actorsproject.xlim.dependence.ValueUsage;
import eu.actorsproject.xlim.io.XlimAttributeList;
import eu.actorsproject.xlim.io.ReaderContext;
import eu.actorsproject.xlim.type.Signature;
import eu.actorsproject.xlim.type.TypeKind;
import eu.actorsproject.xlim.type.TypeRule;
import eu.actorsproject.xlim.type.VoidTypeRule;


class LocationOperationKind extends OperationKind {
	private String mAttributeName;
	private boolean mIsAssign;
	
	LocationOperationKind(String kind, 
	                      TypeRule typeRule,
	                      boolean isAssign,
	                      String attributeName) {
		super(kind,typeRule);
		mIsAssign=isAssign;
		mAttributeName=attributeName;
	}
	
	@Override
	public boolean dependsOnLocation(Operation op) {
		return true;
	}
		
	@Override
	public boolean modifiesLocation(Operation op) {
		return mIsAssign || super.modifiesLocation(op);
	}
	
	@Override
	public boolean mayModifyState(Operation op) {
		if (mIsAssign) {
			Location loc=op.getLocation();
			return loc==null || loc.isStateLocation();
		}
		else
			return false;
	}
	
	@Override
	public Operation create(List<? extends XlimSource> inputs,
			                    List<? extends XlimOutputPort> outputs,
			                    ContainerModule parent) {
		if (mIsAssign)
			return new AssignOperation(this,inputs,outputs,parent);
		else
			return new LocationOperation(this,inputs,outputs,parent); 
	}
	
	@Override
	public String getAttributeDefinitions(XlimOperation op, XmlAttributeFormatter formatter) {
		String attributes=super.getAttributeDefinitions(op, formatter);
		Location location=op.getLocation();
		if (location!=null) {
			assert(location.hasSource());
			XlimSource source=location.getSource();
			attributes=formatter.addAttributeDefinition(attributes,
					                                    mAttributeName, source,
					                                    source.getUniqueId());
		}
		return attributes;
	}
	
	@Override
	public void setAttributes(XlimOperation op,
			                  XlimAttributeList attributes, 
			                  ReaderContext context) {
		String ident=getRequiredAttribute(mAttributeName,attributes);
		XlimSource source=context.getSource(ident);
		
		if (source!=null) {
			Location location=source.getLocation();
			if (location!=null) {
				op.setLocation(location);
			}
			else {
				throw new RuntimeException("Operation kind=\""+getKindAttribute()
						                   + "\" not applicable to output port "
			    	                       + mAttributeName + "=\"" + ident + "\"");
			}
		}
		else {
			throw new RuntimeException("No such source: "+ mAttributeName + "=\""+ident+"\"");
		}
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
		Location location=op.getLocation();
		if (location!=null) {
		    XlimType resultT=op.getOutputPort(0).getType();
		    XlimType sourceT=location.getType();
		    if (sourceT!=null) {
		    	return findElementType(sourceT,resultT);
		    }
		}
		return null; // Doesn't typecheck
	}

	@Override
	public boolean typecheck(XlimOperation op) {
		if (op.getLocation()!=null) {
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
	
	/**
	 * @param sourceT  type of aggregate variable
	 * @param resultT  result type of index operation
	 * @return the element type of sourceT, which matches resultT
	 * 
	 * If sourceT is a 1-dimensional vector/List the element type is the scalar element type.
	 * If it is a matrix (List of List), it might be a vector or a scalar, and so on.
	 */
	private XlimType findElementType(XlimType sourceT, XlimType resultT) {
		// Find the actual sourceT (the one that matches outT)
		// It might be the element-type of sourceT, or
		// the element-type of that type (if sourceT is a matrix), and so on
		while (sourceT.isList()) {
			sourceT=sourceT.getTypeParameter("type");
			// TODO: to strong to require exact match (e.g. different integer widths)?
			// conversion from sourceT to resultT sufficient?
			if (sourceT==resultT)
				return sourceT;
		}
		return null; // error, no applicable element type
	}
}

/**
 * Represents
 * assign: T -> void,         T assignable to (entire) state variable
 */
class AssignTypeRule extends VoidTypeRule {
	
	AssignTypeRule(Signature signature) {
		super(signature);
	}
	
	@Override
	public boolean typecheck(XlimOperation op) {
		Location location=op.getLocation();
		if (location!=null) {
		    XlimType inT=op.getInputPort(0).getSource().getType();
		    XlimType targetT=location.getType();
		    return mayAssign(inT,targetT);
		}
		else
			return true; // "port" needed (defer typecheck)
	}
	
	protected boolean mayAssign(XlimType inT, XlimType targetT) {
		XlimTypeKind targetKind=targetT.getTypeKind();
		// TODO: we should add the necessessary stuff to XlimTypeKind!
		assert(targetKind instanceof TypeKind);
	    return ((TypeKind) targetKind).hasConversionFrom(inT);
	}
}

/**
 * Represents
 * assign: integer,T -> void  T assignable to element of aggregate state variable
 */
class IndexedAssignTypeRule extends AssignTypeRule {
	IndexedAssignTypeRule(Signature signature) {
		super(signature);
	}
	
	@Override
	public boolean typecheck(XlimOperation op) {
		Location location=op.getLocation();
		if (location!=null) {
			// int indexPort=0;
			int dataPort=1;
		    XlimType inT=op.getInputPort(dataPort).getSource().getType();
		    XlimType targetT=location.getType();
		    if (targetT!=null && targetT.isList()) {
		    	// Find the actual targetT (the one that matches inT)
		    	// It might be the element-type of targetT, or
		    	// the element-type of that type (if targetT is a matrix), and so on
		    	while (targetT.isList()) {
		    		targetT=targetT.getTypeParameter("type");
		    		// TODO: to strong to require exact match (e.g. different integer widths)?
		    		// conversion from inT to targetT sufficient?
		    		if (mayAssign(inT,targetT))
		    			return true;
		    	}
		    }
	    	return false;
		}
		else
			return true; // "port" needed (defer typecheck)
	}
}

/**
 * Represents operations that has a location (XlimSource) attribute:
 * var_ref and assign
 */
class LocationOperation extends Operation {
	protected Location mLocation;
	protected ValueUsage mStateVarAccess;
	
	public LocationOperation(OperationKind kind,
			Collection<? extends XlimSource> inputs,
			Collection<? extends XlimOutputPort> outputs,
			ContainerModule parent) {
		super(kind,inputs,outputs,parent);
	}

	@Override
	public Location getLocation() {
		return mLocation;
	}
	
	@Override
	public boolean setLocation(Location location) {
		if (mLocation!=null)
		    throw new IllegalStateException("state variable attribute already set"); // Do this once!
		mLocation=location;
		mStateVarAccess=new LocationReference();
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
		if (mLocation!=null) {
			return mLocation.getDebugName();
		}
		else
			return "";
	}
	
	protected class LocationReference extends ValueUsage {
		LocationReference() {
			super(null /*no value yet*/);
		}
		
		@Override
		public ValueOperator usedByOperator() {
			return LocationOperation.this;
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
		public void setValue(ValueNode value) {
			if (value!=null) {
				Location location=value.getLocation();
				assert(location!=null && location.hasSource());
				mLocation=location;
			}
			super.setValue(value);
		}
	}
}


class AssignOperation extends LocationOperation {
	private ValueNode mSideEffect;
	
	public AssignOperation(OperationKind kind,
            Collection<? extends XlimSource> inputs,
            Collection<? extends XlimOutputPort> outputs,
            ContainerModule parent) {
		super(kind,inputs,outputs,parent);
	}
	
	
	@Override
	public boolean setLocation(Location location) {
		if (mLocation!=null)
		    throw new IllegalStateException("location attribute already set"); // Do this once!
		if (getNumInputPorts()==1 && location.isStateLocation()==false)
			throw new IllegalArgumentException("complete assignment only applicable to state variables");
		mLocation=location;
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
	
	private class AssignmentStateVarUsage extends LocationReference {
		@Override
		public void setValue(ValueNode value) {
			assert(value==null || sameLocation(value));
			super.setValue(value);
		}
		
		private boolean sameLocation(ValueNode value) {
			// We can't set a new value unless it has the same location
			Location location=value.getLocation();
			assert(location!=null && location.hasSource());
			return location==mLocation;
		}
		
		@Override
		public <Result,Arg> Result accept(Visitor<Result,Arg> visitor, Arg arg) {
			boolean killed=(AssignOperation.this.getNumInputPorts()==1);
			return visitor.visitAssign(this,mSideEffect,killed,arg);
		}
	}
	
	private class AssignmentSideEffect extends SideEffect {
		
		@Override
		public Location getLocation() {
			return mLocation;
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

