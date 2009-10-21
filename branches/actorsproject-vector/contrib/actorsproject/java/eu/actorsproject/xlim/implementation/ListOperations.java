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
import eu.actorsproject.xlim.type.TypeFactory;
import eu.actorsproject.xlim.type.TypeRule;
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
	public void initialize(InstructionSet s) {
		//TypeFactory fact=Session.getTypeFactory();
		//TypeKind listKind=fact.getTypeKind("list");

		// $vcons(T,...)->List(type:T,size=N)
		OperationKind and=new OperationKind("$vcons",new VConsTypeRule());
		s.registerOperation(and);
	}
}

class VConsTypeRule extends TypeRule {

	public VConsTypeRule() {
		super(new VarArgSignature(new WildCardTypePattern()));
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
