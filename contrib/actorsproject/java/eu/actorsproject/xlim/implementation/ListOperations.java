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
import eu.actorsproject.xlim.type.Signature;
import eu.actorsproject.xlim.type.TypeFactory;
import eu.actorsproject.xlim.type.TypeKind;
import eu.actorsproject.xlim.type.TypePattern;
import eu.actorsproject.xlim.type.TypeRule;
import eu.actorsproject.xlim.type.TypeSystem;
import eu.actorsproject.xlim.type.VarArgSignature;
import eu.actorsproject.xlim.type.WildCardTypePattern;
import eu.actorsproject.xlim.util.Session;
import eu.actorsproject.xlim.util.XlimFeature;

/**
 * Packages operation kind on "List" types
 *
 */
public class ListOperations extends XlimFeature {

	
	@Override
	public void addTypes(TypeSystem typeSystem) {
		typeSystem.addListType();
	}

	@Override
	public void addOperations(InstructionSet s) {
		TypeFactory fact=Session.getTypeFactory();
		TypeKind intKind=fact.getTypeKind("int");
		TypeKind listKind=fact.getTypeKind("List");
        Signature unaryList=new Signature(listKind);
        
		// $vcons(T,...)->List(type:T,size=N)
		TypePattern wildcard=new WildCardTypePattern();
		OperationKind vconsGen=new OperationKind("$vcons",new GenericVConsTypeRule(wildcard));
		s.registerOperation(vconsGen);
		
		// $vcons(int,...)->List(type:int,size=N)
		OperationKind vconsInt=new OperationKind("$vcons",new IntVConsTypeRule(intKind));
		s.registerOperation(vconsInt);
		
		// cast: List(type:S,size=N)->List(type:T,size=N)
		OperationKind cast=new OperationKind("cast", new ListCastTypeRule(unaryList));
		s.registerOperation(cast);
		
		// noop: List(type:S,size=N)->List(type:T,size=N)
		// TODO: Unlike the GenericNoopTypeRule, this rule allows for type conversion
		// Is this really a good idea? Alternatively use cast/fix the XLIM generation...
		OperationKind noop=new OperationKind("noop", new SloppyListNoopTypeRule(unaryList));
		s.registerOperation(noop);
	}
}

/**
 * Type rule that demands that
 * a) The N inputs are of the same type T
 * b) The output is of type List(type:T, size=N)
 */
class GenericVConsTypeRule extends TypeRule {

	public GenericVConsTypeRule(TypePattern typePattern) {
		super(new VarArgSignature(typePattern));
	}

	@Override
	public int defaultNumberOfOutputs() {
		return 1;
	}

	@Override
	public boolean matchesOutputs(List<? extends XlimOutputPort> outputs) {
		return outputs.size()==1;
	}

	@Override
	public XlimType defaultOutputType(List<? extends XlimSource> inputs, int i) {
		assert(i==0);
		if (!inputs.isEmpty()) {
			XlimType elementT=inputs.get(0).getType();
			for (XlimSource in: inputs)
				if (in.getType()!=elementT)
					return null;
			
			TypeFactory fact=Session.getTypeFactory();
			return fact.createList(elementT,inputs.size());
		}
		else
			return null;
	}

	
	@Override
	public XlimType actualOutputType(XlimOperation op, int i) {
		assert(i==0);
		int size=op.getNumInputPorts();
		if (size!=0) {
			XlimType elementT=op.getInputPort(0).getSource().getType();
			for (XlimInputPort in: op.getInputPorts())
				if (in.getSource().getType()!=elementT)
					return null;
			
			TypeFactory fact=Session.getTypeFactory();
			return fact.createList(elementT,size);
		}
		return null;
	}

	
	@Override
	public boolean typecheck(XlimOperation op) {
		XlimType outT=op.getOutputPort(0).getType();
		return outT!=null && outT==actualOutputType(op,0);
	}
}


/**
 * Relaxed type rule for integer vector constructor, $vcons:
 * a) The N inputs are of integer type
 * b) The output is of type List(type:T, size=N), for some integer type T
 * 
 * Default output type has T=the widest input type
 */
class IntVConsTypeRule extends GenericVConsTypeRule {

	public IntVConsTypeRule(TypePattern typePattern) {
		super(typePattern);
	}
	
	@Override
	public XlimType defaultOutputType(List<? extends XlimSource> inputs, int i) {
		assert(i==0);
		XlimType elementT=widestInput(inputs);
		if (elementT!=null) {
			TypeFactory fact=Session.getTypeFactory();
			return fact.createList(elementT,inputs.size());
		}
		else
			return null;
	}

	@Override
	public XlimType actualOutputType(XlimOperation op, int i) {
		assert(i==0);
		return op.getOutputPort(0).getType();
	}
	
	@Override
	public boolean typecheck(XlimOperation op) {
		XlimType type=op.getOutputPort(0).getType();
		if (type.isList()) {
			XlimType elementT=type.getTypeParameter("type");
			return elementT.isInteger() 
			       && type.getIntegerParameter("size")==op.getNumInputPorts();
		}
		else
			return false;
	}
	
	protected XlimType widestInput(List<? extends XlimSource> inputs) {
		int widest=0;
		XlimType widestType=null;
		
		for (XlimSource source: inputs) {
			XlimType type=source.getType();
			int w=type.getSize();
			if (w>widest) {
				widest=w;
				widestType=type;
			}
		}
		return widestType;
	}
}

class ListCastTypeRule extends CastTypeRule {

	public ListCastTypeRule(Signature signature) {
		super(signature);
	}
	
	@Override
	protected boolean typecheck(XlimType tIn, XlimType tOut) {
		assert(tIn.isList());
		// Cast from list-type typechecks if:
		// a) Output is list-type with same number of elements, and
		// b) there is a conversion between element types
		while (tIn.isList() && tOut.isList()
			   && tIn.getIntegerParameter("size")==tOut.getIntegerParameter("size")) {
			tIn=tIn.getTypeParameter("type");
			tOut=tOut.getTypeParameter("type");
		}
		return super.typecheck(tIn, tOut);
	}
}

class SloppyListNoopTypeRule extends TypeRule {
	
	public SloppyListNoopTypeRule(Signature signature) {
		super(signature);
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
		XlimType tOut=op.getOutputPort(0).getType();
		if (tOut!=null) {
			XlimType tIn=op.getInputPort(0).getSource().getType();
			assert(tIn.isList());
			// Noop from list-type typechecks if:
			// a) Output is list-type with same number of elements, and
			// b) element types same (or integers -possibly of different sizes)
			while (tIn.isList() && tOut.isList()
				   && tIn.getIntegerParameter("size")==tOut.getIntegerParameter("size")) {
				tIn=tIn.getTypeParameter("type");
				tOut=tOut.getTypeParameter("type");
			}
			return tIn==tOut || (tIn.isInteger() && tOut.isInteger());
		}
		else
			return false;
	}
}