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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.io.ReaderContext;
import eu.actorsproject.xlim.io.XlimAttributeList;
import eu.actorsproject.xlim.type.FixIntegerTypeRule;
import eu.actorsproject.xlim.type.FixOutputTypeRule;
import eu.actorsproject.xlim.type.Signature;
import eu.actorsproject.xlim.type.TypeFactory;
import eu.actorsproject.xlim.type.TypeKind;
import eu.actorsproject.xlim.type.TypePattern;
import eu.actorsproject.xlim.type.TypeRule;
import eu.actorsproject.xlim.util.Session;
import eu.actorsproject.xlim.util.XlimFeature;

/**
 * Packages ETSI fixed-point operations 
 *
 */
public class EtsiOperations extends XlimFeature {

	@Override
	public void initialize(InstructionSet s) {
		TypeFactory fact=Session.getTypeFactory();
		TypeKind intKind=fact.getTypeKind("int");
		Signature unary=new Signature(intKind);
		Signature binary=new Signature(intKind,intKind);
		Signature ternary=new Signature(intKind,intKind,intKind);
		
		// Unary operations: int->int16
		String unaryOps16[]={"ETSI_norm_s", "ETSI_abs_s", "ETSI_negate",
				             "ETSI_saturate", "ETSI_extract_h", "ETSI_extract_l"};
		registerAll(unaryOps16, new FixIntegerTypeRule(unary,16), s);
		
		// Unary operations: int->int32
		String unaryOps32[]={"ETSI_norm_l", "ETSI_L_abs", "ETSI_L_negate",
				             "ETSI_round",  "ETSI_L_deposit_h", "ETSI_L_deposit_l",
				             "ETSI_typecast16_32"};
		registerAll(unaryOps32, new FixIntegerTypeRule(unary,32), s);
		
		// Binary operations: (int,int)->int16
		String binaryOps16[]={"ETSI_add", "ETSI_sub", "ETSI_mult", "ETSI_div_s",
				              "ETSI_shr", "ETSI_shl", "ETSI_shr_r", "ETSI_mult_r"};
		registerAll(binaryOps16, new FixIntegerTypeRule(binary,16), s);
		
		// Binary operations: (int,int)->int32
		String binaryOps32[]={"ETSI_L_add", "ETSI_L_sub", "ETSI_L_mult", 
				              "ETSI_L_shr", "ETSI_L_shl", "ETSI_L_shr_r"};
		registerAll(binaryOps32, new FixIntegerTypeRule(binary,16), s);
		
		// Ternary operations: (int,int,int)->int32
		String ternaryOps32[]={"ETSI_L_mac","ETSI_L_msu","ETSI_Mpy_32_16","ETSI_Div_32"};
		registerAll(ternaryOps32, new FixIntegerTypeRule(ternary,32), s);
		
		// Quaternary: (int,int,int,int)->int32
		List<TypePattern> args4=new ArrayList<TypePattern>();
		args4.add(intKind); args4.add(intKind); args4.add(intKind); args4.add(intKind); 
		Signature quaternary=new Signature(args4);
		s.registerOperation(new OperationKind("ETSI_Mpy_32", 
				                              new FixIntegerTypeRule(quaternary,32)));
	}
	
	private void registerAll(String[] xlimOps, TypeRule typeRule, InstructionSet s) {
		for (String name: xlimOps) {
		    OperationKind op=new OperationKind(name, typeRule);
		    s.registerOperation(op);
		}
	}
}

