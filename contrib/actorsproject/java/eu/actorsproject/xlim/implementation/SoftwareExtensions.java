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
import java.util.Map;

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.XlimTypeKind;
import eu.actorsproject.xlim.decision.PortSignature;
import eu.actorsproject.xlim.io.ReaderContext;
import eu.actorsproject.xlim.io.XlimAttributeList;
import eu.actorsproject.xlim.type.FixIntegerTypeRule;
import eu.actorsproject.xlim.type.IntegerTypeRule;
import eu.actorsproject.xlim.type.Signature;
import eu.actorsproject.xlim.type.TypeFactory;
import eu.actorsproject.xlim.type.TypeKind;
import eu.actorsproject.xlim.type.TypeRule;
import eu.actorsproject.xlim.type.VoidTypeRule;
import eu.actorsproject.xlim.util.Session;
import eu.actorsproject.xlim.util.XlimFeature;

/**
 * Packages the extensions of XLIM, which are required by Xlim2c
 */
public class SoftwareExtensions extends XlimFeature {
	@Override
	public void addOperations(InstructionSet s) {
		TypeFactory fact=Session.getTypeFactory();
		TypeKind intKind=fact.getTypeKind("int");
		Signature unary=new Signature(intKind);

		// signExtend: int->int
		OperationKind signExtend=new IntegerAttributeOperationKind("signExtend",
				new SignExtendTypeRule(unary,intKind),
				"size");
		s.registerOperation(signExtend);

		// pinAvail: void -> integer(32)
		OperationKind pinAvail=new PortOperationKind("pinAvail",
				new FixIntegerTypeRule(null,intKind,32),
				"portName", 
				false /* doesn't modify port */, 
				null /* no size */);
		s.registerOperation(pinAvail);

		// yield: void -> void
		OperationKind yield=new YieldOperationKind("yield", new VoidTypeRule(null));
		s.registerOperation(yield);
		
		// valloc void -> T
		OperationKind valloc=new OperationKind("$valloc", new VAllocTypeRule());
		s.registerOperation(valloc);
	}
}

class SignExtendTypeRule extends IntegerTypeRule {

	SignExtendTypeRule(Signature signature, XlimTypeKind outputTypeKind) {
		super(signature, outputTypeKind);
	}
	
	@Override
	public XlimType defaultOutputType(List<? extends XlimSource> inputs, int i) {
		return null; // not possible to deduce 
	}

	@Override
	protected int defaultWidth(List<? extends XlimSource> inputs) {
		throw new UnsupportedOperationException("SignExtendTypeRule: defaultWidth");
	}
	
	@Override
	protected int actualWidth(XlimOperation op) {
		XlimType t=op.getOutputPort(0).getType();
		if (t!=null)
			return promotedInputType(t,0).getSize();
		else
			throw new IllegalArgumentException("signExtend: not possible to deduce width");
	}
}

/**
 * Checks: void -> T (any type T)
 * $valloc is used for (undefined) initial values
 */
class VAllocTypeRule extends TypeRule {

	public VAllocTypeRule() {
		super(null);  // null-ary/no inputs
	}

	@Override
	public int defaultNumberOfOutputs() {
		return 1;
	}

	@Override
	public XlimType defaultOutputType(List<? extends XlimSource> inputs, int i) {
		return null;  // Can't say, must be provided
	}

	@Override
	public boolean matchesOutputs(List<? extends XlimOutputPort> outputs) {
		return outputs.size()==1; // Only requirement is that there be a single output
	}
	
	@Override
	public XlimType actualOutputType(XlimOperation op, int i) {
		return op.getOutputPort(i).getType(); // Actual type is "declared type"
	}
}

class YieldOperationKind extends OperationKind {
	
	YieldOperationKind(String kind, TypeRule typeRule) {
		super(kind,typeRule);
	}
	
	
	@Override
	public Operation create(List<? extends XlimSource> inputs,
			                    List<? extends XlimOutputPort> outputs,
			                    ContainerModule parent) {
		return new YieldOperation(this,inputs,outputs,parent);
	}
	
	@Override
	public String getAttributeDefinitions(XlimOperation op, XmlAttributeFormatter formatter) {
		PortSignature portSignature=(PortSignature) op.getGenericAttribute();
		String portNames="";
		String rates="";
		String delimiter="";
		
		for (XlimTopLevelPort port: portSignature.getPorts()) {
			int rate=portSignature.getPortRate(port);
			portNames += delimiter + port.getName();
			rates += delimiter + rate;
			delimiter="|";
		}
		
		return super.getAttributeDefinitions(op, formatter) 
		       + " portName=\"" + portNames + "\" value=\"" +rates + "\" removable=\"no\"";
	}
	
	@Override
	public void setAttributes(XlimOperation op,
			                  XlimAttributeList attributes, 
			                  ReaderContext context) {
		// Set port attribute
		String portName=getRequiredAttribute("portName",attributes);
		long value=getRequiredIntegerAttribute("value",attributes);
		
		XlimTopLevelPort port=context.getTopLevelPort(portName);
		if (port!=null) {
			Map<XlimTopLevelPort,Integer> portMap=Collections.singletonMap(port,(int) value);
			PortSignature portSignature=new PortSignature(portMap);
			op.setGenericAttribute(portSignature);
		}
		else {
			throw new RuntimeException("No such port: \""+portName+"\"");
		}
	}
}

class YieldOperation extends Operation {
	
	public Object mValue;
	
	public YieldOperation(OperationKind kind,
                            Collection<? extends XlimSource> inputs,
                            Collection<? extends XlimOutputPort> outputs,
                            ContainerModule parent) {
		super(kind,inputs,outputs,parent);
	}
	
	@Override
	public Object getGenericAttribute() {
		return mValue;
	}

	@Override
	public boolean setGenericAttribute(Object value) {
		mValue=value;
		return true;
	}
	
	@Override
	public boolean isRemovable() {
		return false;
	}

	
	/**
	 * @return additional attributes to show up in debug printouts
	 */
	@Override
	public String attributesToString() {
		return mValue.toString();
	}
}
