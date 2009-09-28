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

import org.w3c.dom.NamedNodeMap;

import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.io.ReaderContext;
import eu.actorsproject.xlim.type.FixOutputTypeRule;
import eu.actorsproject.xlim.type.Signature;
import eu.actorsproject.xlim.type.TypeFactory;
import eu.actorsproject.xlim.type.TypeKind;
import eu.actorsproject.xlim.type.TypeRule;
import eu.actorsproject.xlim.util.Session;
import eu.actorsproject.xlim.util.XlimFeature;

/**
 * Packages operation kind on the "real" type
 *
 */
public class RealOperations extends XlimFeature {

	@Override
	public void initialize(InstructionSet s) {
		TypeFactory fact=Session.getTypeFactory();
		TypeKind realKind=fact.getTypeKind("real");
		XlimType realType=realKind.createType();
		TypeKind boolKind=fact.getTypeKind("bool");
		XlimType boolType=boolKind.createType();
		
		// $literal_Integer: void -> real
		OperationKind literal=new RealAttributeOperationKind("$literal_Integer",
				new FixOutputTypeRule(null,realType),
				"value");
		s.registerOperation(literal);
		
		// Binary operations:
		// $add: (real,real) -> real
		// $sub: (real,real) -> real
		// $mul: (real,real) -> real
		// $div: (real,real) -> real
		Signature binary=new Signature(realKind, realKind);
		TypeRule binaryRule=new FixOutputTypeRule(binary,realType);
		String binOps[]={"$add", "$sub", "$mul", "$div"};
		for (String name: binOps) {
		    OperationKind op=new OperationKind(name, binaryRule);
		    s.registerOperation(op);
		}
		
		// Relational operators:
		// $eq: (real,real) -> bool
		// $ge: (real,real) -> bool
		// $gt: (real,real) -> bool
		// $le: (real,real) -> bool
		// $lt: (real,real) -> bool
		// $ne: (real,real) -> bool
		TypeRule relOpRule=new FixOutputTypeRule(binary,boolType);
		String relOps[]={"$eq", "$ge", "$gt", "$le", "$lt", "$ne"};
		for (String name: relOps) {
			OperationKind op=new OperationKind(name, relOpRule);
			s.registerOperation(op);
		}
		
		// noop: real -> real
		TypeRule unaryRule=new FixOutputTypeRule(new Signature(realKind),realType);
		OperationKind noop=new OperationKind("noop", unaryRule);
		s.registerOperation(noop);
		
		// $negate: real -> real
		OperationKind negate=new OperationKind("$negate", unaryRule);
		s.registerOperation(negate);
		
		// $selector: (bool,real,real) -> real
		Signature ternarySignature=new Signature(boolKind, realKind, realKind);
		OperationKind selector=new OperationKind("$selector", 
				new FixOutputTypeRule(ternarySignature, realType));
		s.registerOperation(selector);
	}
}

/**
 * OperationKind of IntegerAttributeOperations ($literal_Integer, $signExtend)
 */
class RealAttributeOperationKind extends OperationKind {
	private String mAttributeName;
	
	public RealAttributeOperationKind(String kind, 
			                          TypeRule typeRule,
			                          String attributeName) {
		super(kind,typeRule);
		mAttributeName=attributeName;
	}
	
	@Override
	public Operation create(List<? extends XlimSource> inputs,
			                List<? extends XlimOutputPort> outputs,
			                ContainerModule parent) {
		return new RealAttributeOperation(this,inputs,outputs,parent);
	}
	
	@Override
	public String getAttributeDefinitions(XlimOperation op) {
		String value=op.getValueAttribute();
		if (value!=null)
			return super.getAttributeDefinitions(op)+" "+mAttributeName+"=\""+value+"\"";
		else
			return super.getAttributeDefinitions(op);
	}
	
	@Override
	public void setAttributes(XlimOperation op,
			                  NamedNodeMap attributes, 
			                  ReaderContext context) {
		String value=getAttribute(mAttributeName,attributes);
		if (value!=null)
			op.setValueAttribute(value);
	}		
}

class RealAttributeOperation extends Operation {
	private Double mValue;
	
	public RealAttributeOperation(OperationKind kind,
			Collection<? extends XlimSource> inputs,
			Collection<? extends XlimOutputPort> outputs,
			ContainerModule parent) {
		super(kind,inputs,outputs,parent);
	}
	
	@Override
	public String getValueAttribute() {
		return mValue.toString();
	}
	
	@Override
	public Long getIntegerValueAttribute() {
		throw new UnsupportedOperationException("RealAttributeOperation: getIntegerValueAttribute");
	}
	
	@Override
	public boolean setValueAttribute(String value) {
		mValue=Double.valueOf(value);
		return true;
	}
	
	@Override 
	public boolean setIntegerValueAttribute(long value) {
		throw new UnsupportedOperationException("RealAttributeOperation: setIntegerValueAttribute");
	}
	
	@Override
	public String attributesToString() {
		return Double.toString(mValue);
	}
}
