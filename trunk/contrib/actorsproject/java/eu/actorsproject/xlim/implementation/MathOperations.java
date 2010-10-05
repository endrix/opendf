/* 
 * Copyright (c) Ericsson AB, 2010
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

import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.XlimTypeKind;
import eu.actorsproject.xlim.type.FixOutputTypeRule;
import eu.actorsproject.xlim.type.IntegerTypeRule;
import eu.actorsproject.xlim.type.Signature;
import eu.actorsproject.xlim.type.TypeFactory;
import eu.actorsproject.xlim.type.TypeKind;
import eu.actorsproject.xlim.type.TypeRule;
import eu.actorsproject.xlim.util.Session;
import eu.actorsproject.xlim.util.XlimFeature;

public class MathOperations extends XlimFeature {

	@Override
	public void addOperations(InstructionSet s) {
		TypeFactory fact=Session.getTypeFactory();
		TypeKind intKind=fact.getTypeKind("int");
		TypeKind realKind=fact.getTypeKind("real");
		XlimType realType=realKind.createType();
		
		// Unary operations: real->real
		String mathApi[]={"abs",    "acos",  "asin",  "atan", "ceil",  "cos",   "cosh",  "exp",
		                  "floor",  "log",  "log10", "sin",   "sinh",  "sqrt",  "tan",   "tanh"};
		
		registerAll(mathApi, new FixOutputTypeRule(new Signature(realKind),realType), s);
	
		// atan2: real x real -> real
		// $mod:  real x real -> real (there's also int x int -> int)
		TypeRule typeRule=new FixOutputTypeRule(new Signature(realKind,realKind), realType);
		s.registerOperation(new OperationKind("atan2",typeRule)); 
		s.registerOperation(new OperationKind("$mod",typeRule));
		
		// abs: int -> int (there's also real->real)
		OperationKind abs=new OperationKind("abs", new FirstInputTypeRule(new Signature(intKind),intKind));
		s.registerOperation(abs);
		
		// mod: int x int -> int (there's also real x real -> real)
		OperationKind mod=new OperationKind("$mod", new ModTypeRule(new Signature(intKind, intKind),intKind));
		s.registerOperation(mod);
	}
	
	private void registerAll(String[] xlimOps, TypeRule typeRule, InstructionSet s) {
		for (String name: xlimOps) {
		    OperationKind op=new OperationKind(name, typeRule);
		    s.registerOperation(op);
		}
	}
}


/**
 * Type rule used for the (integer) $mod operator.
 * 
 * int(w1)  % int(w2)  --> int(w2)
 * int(w1)  % uint(w2) --> int(w2+1)   (second operand first promoted to int)
 * uint(w1) % int(w2)  --> int(w2) 
 * uint(w1) % uint(w2) --> uint(w2)
 */
class ModTypeRule extends IntegerTypeRule {

	ModTypeRule(Signature signature, XlimTypeKind outputTypeKind) {
		super(signature, outputTypeKind);
	}

	@Override
	public int defaultNumberOfOutputs() {
		return 1;
	}

	@Override
	protected int defaultWidth(List<? extends XlimSource> inputs) {
		return promotedInputType(inputs.get(1).getType(), 1).getSize();
	}

	@Override
	protected int actualWidth(XlimOperation op) {
		return promotedInputType(op.getInputPort(1).getSource().getType(), 1).getSize();
	}
}
