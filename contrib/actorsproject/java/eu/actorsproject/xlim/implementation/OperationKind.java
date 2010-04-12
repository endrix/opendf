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

import java.util.ArrayList;
import java.util.List;

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.xlim.XlimInputPort;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.io.XlimAttributeList;
import eu.actorsproject.xlim.io.ReaderContext;
import eu.actorsproject.xlim.type.TypeRule;

public class OperationKind {

	protected String mKindAttribute;
	private TypeRule mTypeRule;
	
	public OperationKind(String kindAttribute, TypeRule typeRule) {
		mKindAttribute=kindAttribute;
		mTypeRule=typeRule;
	}
	
	public String getKindAttribute() {
		return mKindAttribute;
	}
	
	public boolean dependsOnLocation(Operation op) {
		for (int i=0; i<op.getNumInputPorts(); ++i)
			if (op.getInputPort(i).dependsOnLocation())
				return true;
		return false;
	}
	
	public boolean modifiesLocation(Operation op) {
		// The default implementation just checks if 
		// a local aggreagate (with a location) is defined
		for (int i=0; i<op.getNumOutputPorts(); ++i)
			if (op.getOutputPort(i).hasLocation())
				return true;
		return false;
	}
	
	public boolean mayModifyState(Operation op) {
		// This is the default implementation
		// -appropriate for most operations.
		// pinRead/Write, assign, taskCall etc. must override
		return false;
	}
	
	protected String getAttributeDefinitions(XlimOperation operation, XmlAttributeFormatter formatter) {
		return "kind=\"" + mKindAttribute + "\"";
	}
	
	protected String getRequiredAttribute(String name, XlimAttributeList attributes) {
		String value=attributes.getAttributeValue(name);
		if (value!=null)
			return value;
		else
			throw new RuntimeException("Operation kind=\""+mKindAttribute
					                   +"\" missing attribute \""+name+"\"");
	}
	
	protected Long getRequiredIntegerAttribute(String name, XlimAttributeList attributes) {
		String value=getRequiredAttribute(name, attributes);
		try {
			return Long.valueOf(value);
		} catch (NumberFormatException ex) {
			throw new RuntimeException("Expecting integer attribute, found: "+name
					                   +"=\""+value+"\"");
		}
	}
	
	public void setAttributes(XlimOperation op,
			                  XlimAttributeList attributes, 
			                  ReaderContext context) {
		// default implementation does nothing...
	}
	
	/**
	 * @param outputs
	 * @return true if this OperationKind may produce the given outputs
	 * 
	 * Particularly for literals (which have no inputs) the output type is 
	 * needed to resolve the operation.
	 */
	public boolean matchesOutputs(List<? extends XlimOutputPort> outputs) {
		return mTypeRule.matchesOutputs(outputs);
	}
	
	/**
	 * @param inputs
	 * @return true if this OperationKind is applicable to the given inputs
	 */
	public boolean matches(List<? extends XlimSource> inputs) {
		return mTypeRule.matches(inputs);
	}
	
	/**
	 * @param kind
	 * @param inputs
	 * @return true if this OperationKind provides a strictly "better" match than 'kind'
	 *    
	 * Here "better" means the inputs match exactly, whenever those of 'kind' do and
	 * for at least one input there is an exact match, when that of 'kind' does not.
	 */
	public boolean betterMatch(OperationKind kind, List<? extends XlimSource> inputs) {
		return mTypeRule.betterMatch(kind.mTypeRule, inputs);
	}
	
	public Operation create(List<? extends XlimSource> inputs,
            				List<? extends XlimOutputPort> outputs,
            				ContainerModule parent)  {
		Operation op=new Operation(this,inputs,outputs,parent);
		typecheck(op);
		return op;
	}
	
	public Operation create(List<? extends XlimSource> inputs,
			                ContainerModule parent)  {
		int numOutputs=mTypeRule.defaultNumberOfOutputs();
		ArrayList<XlimOutputPort> outputs=new ArrayList<XlimOutputPort>();
		for (int i=0; i<numOutputs; ++i) {
			// Determine default output type, possibly null
			XlimType t=mTypeRule.defaultOutputType(inputs, i);
			outputs.add(new OutputPort(t));
		}
		return create(inputs,outputs,parent);
	}
	
	private void typecheck(Operation op) {
		if (mTypeRule.typecheck(op)==false) {
			System.out.println(getSignature(op)+" doesn't match "+getSignature());
			throw new RuntimeException("Operation \""+mKindAttribute+"\" doesn't typecheck");
		}
	}
	
	/**
	 * Completes any deferred typechecking -when a required attribute is set
	 * @param op
	 */
	public void doDeferredTypecheck(Operation op) {
		// First fill in any missing output types
		for (int i=0; i<op.getNumOutputPorts(); ++i) {
			OutputPort output=op.getOutputPort(i);
			if (output.getType()==null)
				output.setType(actualOutputType(op,i));
		}
		
		typecheck(op);
	}

	/**
	 * @param inputs
	 * @param i
	 * @return default type of output 'i' (or null if not possible to deduce using
	 *         inputs only).
	 */

	public XlimType defaultOutputType(Operation op, int outputPort) {
		return mTypeRule.actualOutputType(op, outputPort);
	}
	
	
	/**
	 * @param op
	 * @param i
	 * @return Actual type of output 'i' (0,1,2,...) given a completly
	 *         created operation 'op'.
	 *         
	 * For integer operations, the actual type may differ from the declared type
	 * of the output (in which case an implicit type conversion is made). 
	 * Example:
	 * integer(size=16)*integer(size=16)->integer(size=32) (=actual type),
	 * but declared type of output is perhaps integer(size=16).
	 */
	public XlimType actualOutputType(XlimOperation op, int i) {
		return mTypeRule.actualOutputType(op, i);
	}
	
	protected String lhsToString(Operation op) {
		if (op.getNumOutputPorts()!=0)
			if (op.getNumOutputPorts()==1)
				return op.getOutputPort(0).getUniqueId()+"=";
			else {
				String lhs="("+op.getOutputPort(0).getUniqueId();
				for (int i=2; i<op.getNumOutputPorts(); ++i)
					lhs += ","+op.getOutputPort(i).getUniqueId();
				lhs+=")=";
				return lhs;
			}
		else
			return "";
	}

	protected String inputsToString(Operation op) {
		if (op.getNumInputPorts()!=0) {
			String inputs=op.getInputPort(0).getSource().getUniqueId();
			for (int i=1; i<op.getNumInputPorts(); ++i)
				inputs += ","+op.getInputPort(i).getSource().getUniqueId();
			return inputs;
		}
		else
			return "";
	}
	
	protected String rhsToString(Operation op) {
		String attributes=op.attributesToString();
		if (!attributes.equals("") && op.getNumInputPorts()!=0)
			attributes+=",";
		return mKindAttribute+"("+attributes+inputsToString(op)+")";
	}
	
	public String textRepresentation(Operation op) {
		return lhsToString(op)+rhsToString(op);
	}
	
	public String getSignature() {
		return mKindAttribute+": "+mTypeRule.toString();
	}
	
	@Override
	public String toString() {
		return getSignature();
	}
	
	private String getSignature(XlimOperation op) {
		String result=op.getKind()+": ";
		
		if (op.getNumInputPorts()==1)
			result += op.getInputPort(0).getSource().getType();
		else {
			String delimiter="";
			result += "(";
			for (XlimInputPort input: op.getInputPorts()) {
				result += delimiter+input.getSource().getType();
				delimiter=",";
			}
			result += ")";
		}
		result += " -> ";
		
		if (op.getNumOutputPorts()==1)
			result += op.getOutputPort(0).getType();
		else {
			String delimiter="";
			result += "(";
			for (XlimOutputPort port: op.getOutputPorts()) {
				result+=delimiter+port.getType();
				delimiter=",";
			}
			result += ")";
		}
		return result;
	}
}
