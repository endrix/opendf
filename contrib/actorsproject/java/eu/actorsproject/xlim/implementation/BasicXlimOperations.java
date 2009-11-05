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

import java.util.List;

import eu.actorsproject.xlim.XlimInputPort;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.type.CastTypeRule;
import eu.actorsproject.xlim.type.FixIntegerTypeRule;
import eu.actorsproject.xlim.type.FixOutputTypeRule;
import eu.actorsproject.xlim.type.IntegerTypeRule;
import eu.actorsproject.xlim.type.Signature;
import eu.actorsproject.xlim.type.TypeFactory;
import eu.actorsproject.xlim.type.TypeKind;
import eu.actorsproject.xlim.type.TypePattern;
import eu.actorsproject.xlim.type.TypeRule;
import eu.actorsproject.xlim.type.VarArgSignature;
import eu.actorsproject.xlim.type.VoidTypeRule;
import eu.actorsproject.xlim.type.WildCardTypePattern;
import eu.actorsproject.xlim.util.Session;
import eu.actorsproject.xlim.util.XlimFeature;

/**
 * The minimal set of XLIM operations (the ones used by HDL compiler)
 *
 */
public class BasicXlimOperations extends XlimFeature {

	@Override
	public void initialize(InstructionSet instructionSet) {
		addPortOperations(instructionSet);
		addStateVarOperations(instructionSet);
		addGenericOperations(instructionSet);
		addTaskCall(instructionSet);
		addBooleanOperations(instructionSet);
		addIntegerOperations(instructionSet);
	}

	/**
	 * Adds all port operations operations to 's':
	 * pinRead, pinWrite, pinPeek, pinStatus
	 */

	private void addPortOperations(InstructionSet s) {
		TypeFactory fact=Session.getTypeFactory();
		TypeKind intTypeKind=fact.getTypeKind("int");
		XlimType boolType=fact.create("bool");
		boolean mayHaveRepeat=true;
		
		/*
		 * Operations found in XLIM spec 1.0 (September 18, 2007)
		 */
		
		// pinRead: void -> PortType
		OperationKind pinRead=new PortOperationKind("pinRead",
                new PinReadTypeRule(null, mayHaveRepeat),
                "portName", 
                true /* modifies port */, 
                null /* no size */);
		s.registerOperation(pinRead);
		
		// pinWrite: T -> void, T assignable to port
		Signature wildCard=new Signature(new WildCardTypePattern());
		OperationKind pinWrite=new PortOperationKind("pinWrite",
                new PinWriteTypeRule(wildCard, mayHaveRepeat),
                "portName", 
                true /* modifies port */, 
                null /* no size */);
		s.registerOperation(pinWrite);
		
		/*
		 * Operations found adhoc (by compiling lots of CAL code)
		 */
		
		// pinPeek: integer -> PortType
		OperationKind pinPeek=new PortOperationKind("pinPeek",
                new PinReadTypeRule(new Signature(intTypeKind), false /* no repeat */),
                "portName", 
                false /* doesn't modify port */, 
                null /* no size */);
		s.registerOperation(pinPeek);
		
		// pinStatus: void -> bool
		OperationKind pinStatus=new PortOperationKind("pinStatus",
                new FixOutputTypeRule(null, boolType),
                "portName", 
                false /* doesn't modify port */, 
                null /* no size */);
		s.registerOperation(pinStatus);
	}

	/**
	 * Adds assign and var_ref to 's'
	 */

	private void addStateVarOperations(InstructionSet s) {
		TypeFactory fact=Session.getTypeFactory();
		TypeKind intTypeKind=fact.getTypeKind("int");
		
		/*
		 * In XLIM spec 1.0 (September 18, 2007)
		 */
		
		// assign: T -> void
		TypePattern wildcardPattern=new WildCardTypePattern();
		Signature scalarWildCard=new Signature(wildcardPattern);
		OperationKind assign1=new LocationOperationKind("assign", 
                                                        new AssignTypeRule(scalarWildCard),
                                                        true, /* modifies state */
                                                        "target");
		s.registerOperation(assign1);
		
		// assign: (integer,T) -> void
		Signature assign2Signature=new Signature(intTypeKind,wildcardPattern);
		OperationKind assign2=new LocationOperationKind("assign", 
                new IndexedAssignTypeRule(assign2Signature),
                true, /* modifies state */
                "target");
		s.registerOperation(assign2);
		
		/*
		 * Found adhoc (by compiling lots of CAL code)
		 */
		
		// var_ref: integer -> T
		OperationKind var_ref=new LocationOperationKind("var_ref", 
                new VarRefTypeRule(new Signature(intTypeKind)),
                false, /* modifies state */
                "name");
		s.registerOperation(var_ref);
	}


	/**
	 * Adds cast and the generic versions of noop and $select
	 */

	private void addGenericOperations(InstructionSet s) {
		// cast: S->T, S converts to T
		OperationKind cast=new OperationKind("cast", new CastTypeRule());
		s.registerOperation(cast);
		
		// noop: T->T (exact type match required)
		OperationKind noop=new OperationKind("noop", new GenericNoopTypeRule());
		s.registerOperation(noop);
		
		// $select: (bool,T,T)->T (exact type match required)
		TypeFactory fact=Session.getTypeFactory();
		TypeKind boolKind=fact.getTypeKind("bool");
		XlimType boolType=boolKind.createType();
		TypePattern wildcard=new WildCardTypePattern();
		Signature ternarySignature=new Signature(boolKind, wildcard, wildcard);
		
		OperationKind selector=new OperationKind("$selector", 
				new GenericSelectorTypeRule(ternarySignature));
		s.registerOperation(selector);
	}

	/**
	 * Adds taskCall to 's'
	 */

	private void addTaskCall(InstructionSet s) {
		/*
		 * In XLIM spec 1.0 (September 18, 2007)
		 */
		
		// taskCall: void->void
		OperationKind taskCall=new TaskOperationKind("taskCall",
				new VoidTypeRule(null),
				"target");
		s.registerOperation(taskCall);
	}
	
	/**
	 * Adds all boolean operations to 's'
	 * $literal_Integer (of bool type)
	 * $and, $or, $not
	 * $eq, $ne (bool inputs)
	 * noop (of bool type)
	 * $selector (all bool inputs)
	 */
	private void addBooleanOperations(InstructionSet s) {
		TypeFactory fact=Session.getTypeFactory();
		TypeKind boolKind=fact.getTypeKind("bool");
		XlimType boolType=boolKind.createType();
		TypeRule unaryRule=new FixOutputTypeRule(new Signature(boolKind),
				                                 boolType);
		TypeRule binaryRule=new FixOutputTypeRule(new Signature(boolKind, boolKind),
				                                  boolType);
		
		// $literal_Integer: void -> bool
		OperationKind literal=new IntegerAttributeOperationKind("$literal_Integer",
				new FixOutputTypeRule(null,boolType),
				"value");
		s.registerOperation(literal);
		
		// $and: (bool, ...) -> bool
		Signature varArgSignature=new VarArgSignature(boolKind);
		OperationKind and=new OperationKind("$and",
				new FixOutputTypeRule(varArgSignature,boolType));
		s.registerOperation(and);
		
		// $or: (bool,bool) -> bool
		OperationKind or=new OperationKind("$or", binaryRule);
		s.registerOperation(or);
		
		// $not: bool -> bool
		OperationKind not=new OperationKind("$not", unaryRule);
		s.registerOperation(not);
		
		// $eq: (bool,bool) -> bool
		OperationKind eq=new OperationKind("$eq", binaryRule);
		s.registerOperation(eq);
		
		// $ne: (bool,bool) -> bool
		OperationKind ne=new OperationKind("$ne", binaryRule);
		s.registerOperation(ne);
	}
	
	/**
	 * Adds all integer operations to 's'
	 * $literal_Integer (of int type)
	 * $add, $sub, $mul, $div, $negate
	 * lshift, rshift urshift
	 * bitand, bitor, bitxor, bitnot
	 * $eq, $ne, $lt, $le, $gt, $ge (int inputs)
	 * noop (of int type)
	 * $selector (int output)
	 */
	void addIntegerOperations(InstructionSet s) {
		TypeFactory fact=Session.getTypeFactory();
		TypeKind intKind=fact.getTypeKind("int");
		Signature unary=new Signature(intKind);
		Signature binary=new Signature(intKind,intKind);
		TypeKind boolKind=fact.getTypeKind("bool");
		XlimType boolType=boolKind.createType();
		
		// $literal_Integer: void -> int
		OperationKind literal=new IntegerAttributeOperationKind("$literal_Integer",
				new LiteralIntegerTypeRule(),
				"value");
		s.registerOperation(literal);
		
		// $add: (int,int) -> int
		TypeRule addRule=new AddTypeRule(binary, 33);
		OperationKind add=new OperationKind("$add", addRule);
		s.registerOperation(add);
		
		// $sub: (int,int) -> int
		OperationKind sub=new OperationKind("$sub", addRule);
		s.registerOperation(sub);
		
		// $negate: int -> int
		OperationKind negate=new OperationKind("$negate", new AddTypeRule(unary, 33));
		s.registerOperation(negate);
		
		// $mul: (int,int) -> int
		OperationKind mul=new OperationKind("$mul", new MulTypeRule(binary, 33));
		s.registerOperation(mul);
		
		// $div: (int,int) -> int
		// rshift: (int,int) -> int
		// urshift: (int,int) -> int
		TypeRule divRShiftRule=new FirstInputTypeRule(binary);
		String divRShiftOps[]={"$div","rshift","urshift"};
		for (String name: divRShiftOps) {
			OperationKind op=new OperationKind(name, divRShiftRule);
			s.registerOperation(op);
		}
		
		// lshift: (int,int) -> int(33)
		OperationKind lshift=new OperationKind("lshift", 
                new FixIntegerTypeRule(binary,33));
		s.registerOperation(lshift);
		
		// bitand: (int,int) -> int
		// bitor: (int,int) -> int
		// bitxor: (int,int) -> int
		TypeRule bitOpRule=new WidestInputTypeRule(binary);
		String bitOps[]={"bitand", "bitor", "bitxor"};
		for (String name: bitOps) {
			OperationKind op=new OperationKind(name, bitOpRule);
			s.registerOperation(op);
		}
		
		// bitnot: int -> int
		OperationKind bitnot=new OperationKind("bitnot", 
				                               new WidestInputTypeRule(unary));
		s.registerOperation(bitnot);
		
		// $eq: (int,int) -> bool
		// $ge: (int,int) -> bool
		// $gt: (int,int) -> bool
		// $le: (int,int) -> bool
		// $lt: (int,int) -> bool
		// $ne: (int,int) -> bool
		TypeRule relOpRule=new FixOutputTypeRule(binary,boolType);
		String relOps[]={"$eq", "$ge", "$gt", "$le", "$lt", "$ne"};
		for (String name: relOps) {
			OperationKind op=new OperationKind(name, relOpRule);
			s.registerOperation(op);
		}

		// noop: int->int
		OperationKind noop=new OperationKind("noop", 
                new FirstInputTypeRule(unary));
		s.registerOperation(noop);
		
		// $selector: (bool,int,int) -> int
		Signature ternarySignature=new Signature(boolKind, intKind, intKind);
		OperationKind selector=new OperationKind("$selector", 
				new WidestInputTypeRule(ternarySignature));
		s.registerOperation(selector);
	}
}

/**
 * Generic TypeRule for noop: T->T (exact type match required)
 * It is used for bool and real but int, which is parametric in size,
 * uses a more specific type rule.
 */
class GenericNoopTypeRule extends TypeRule {

	public GenericNoopTypeRule() {
		super(new Signature(new WildCardTypePattern()));
	}

	@Override
	public int defaultNumberOfOutputs() {
		return 1;
	}

	@Override
	public XlimType defaultOutputType(List<? extends XlimSource> inputs, int i) {
		assert(i==0);
		return inputs.get(0).getType();
	}
	
	@Override
	public XlimType actualOutputType(XlimOperation op, int i) {
		return op.getInputPort(0).getSource().getType();
	}

	@Override
	public boolean matchesOutputs(List<? extends XlimOutputPort> outputs) {
		return outputs.size()==1;
	}
	
	@Override
	public boolean typecheck(XlimOperation op) {
		XlimType outT=op.getOutputPort(0).getType();
		if (outT!=null) {
			XlimType inT=op.getInputPort(0).getSource().getType();
			return inT==outT;
		}
		else
			return false;
	}
}

/**
 * Generic TypeRule for $select: (bool,T,T)->T (exact type match required)
 * It is used for bool and real but int, which is parametric in size,
 * uses a more specific type rule.
 */
class GenericSelectorTypeRule extends TypeRule {

	public GenericSelectorTypeRule(Signature signature) {
		super(signature);
	}
	
	@Override
	public int defaultNumberOfOutputs() {
		return 1;
	}

	@Override
	public XlimType defaultOutputType(List<? extends XlimSource> inputs, int i) {
		assert(i==0 && inputs.size()==3);
		XlimType t1=inputs.get(1).getType();
		XlimType t2=inputs.get(2).getType();
		return (t1==t2)? t1 : null;
	}
	
	@Override
	public XlimType actualOutputType(XlimOperation op, int i) {
		assert(i==0 && op.getNumInputPorts()==3);
		XlimType t1=op.getInputPort(1).getSource().getType();
		XlimType t2=op.getInputPort(2).getSource().getType();
		return (t1==t2)? t1 : null;
	}

	@Override
	public boolean matchesOutputs(List<? extends XlimOutputPort> outputs) {
		return outputs.size()==1;
	}

	@Override
	public boolean typecheck(XlimOperation op) {
		return actualOutputType(op,0)==op.getOutputPort(0).getType();
	}	
}

class LiteralIntegerTypeRule extends IntegerTypeRule {

	LiteralIntegerTypeRule() {
		super(null);
	}

	@Override
	public XlimType defaultOutputType(List<? extends XlimSource> inputs, int i) {
		return null; // not possible to deduce ("value" attribute needed)
	}

	@Override
	protected int defaultWidth(List<? extends XlimSource> inputs) {
		throw new UnsupportedOperationException("LiteralIntegerTypeRule: defaultWidth");
	}

	@Override
	protected int actualWidth(XlimOperation op) {
		return actualWidth(op.getIntegerValueAttribute());
	}
	
	public static int actualWidth(long value) {
		int width=1;
		while (value>=256 || value<-256) {
			width+=8;
			value>>=8;
		}
		while (value!=0 && value!=-1) {
			width++;
			value>>=1;
		}
		return width;
	}
}

class WidestInputTypeRule extends IntegerTypeRule {
	
	WidestInputTypeRule(Signature signature) {
		super(signature);
	}
	
	
	@Override
	protected int defaultWidth(List<? extends XlimSource> inputs) {
		int widest=0;
		for (XlimSource source: inputs) {
			int w=source.getType().getSize();
			if (w>widest)
				widest=w;
		}
		return widest;
	}

	@Override
	protected int actualWidth(XlimOperation op) {
		int widest=0;
		for (XlimInputPort input: op.getInputPorts()) {
			int w=input.getSource().getType().getSize();
			if (w>widest)
				widest=w;
		}
		return widest;
	}	
}

class AddTypeRule extends WidestInputTypeRule {

	private int mMaxWidth;
	
	AddTypeRule(Signature signature, int maxWidth) {
		super(signature);
		mMaxWidth=maxWidth;
	}
	
	@Override
	protected int defaultWidth(List<? extends XlimSource> inputs) {
		int width=super.defaultWidth(inputs)+1;
		return (width>mMaxWidth)? mMaxWidth : width;
	}
	
	@Override
	protected int actualWidth(XlimOperation op) {
		return super.actualWidth(op)+1;
	}
}

class MulTypeRule extends IntegerTypeRule {

	private int mMaxWidth;
	
	MulTypeRule(Signature signature, int maxWidth) {
		super(signature);
		mMaxWidth=maxWidth;
	}

	
	@Override
	protected int defaultWidth(List<? extends XlimSource> inputs) {
		int width=0;
		for (XlimSource source: inputs)
			width += source.getType().getSize();
		return (width>mMaxWidth)? mMaxWidth : width;
	}


	@Override
	protected int actualWidth(XlimOperation op) {
		int width=0;
		for (XlimInputPort input: op.getInputPorts())
			width += input.getSource().getType().getSize();
		return width;
	}
}

class FirstInputTypeRule extends IntegerTypeRule {

	FirstInputTypeRule(Signature signature) {
		super(signature);
	}

	@Override
	public int defaultNumberOfOutputs() {
		return 1;
	}

	
	@Override
	public XlimType defaultOutputType(List<? extends XlimSource> inputs, int i) {
		return inputs.get(0).getType();
	}

	@Override
	protected int defaultWidth(List<? extends XlimSource> inputs) {
		return defaultOutputType(inputs,0).getSize();
	}

	@Override
	public XlimType actualOutputType(XlimOperation op, int i) {
		assert(i==0);
		return op.getInputPort(0).getSource().getType();
	}

	@Override
	protected int actualWidth(XlimOperation op) {
		return actualOutputType(op,0).getSize();
	}
}
