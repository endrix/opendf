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

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.io.ReaderContext;

abstract class OperationKind {

	protected String mKindAttribute;
	
	public OperationKind(String kindAttribute) {
		mKindAttribute=kindAttribute;
	}
	
	public String toString() {
		return mKindAttribute;
	}
		
	public boolean mayAccessState(Operation op) {
		for (int i=0; i<op.getNumInputPorts(); ++i)
			if (op.getInputPort(i).getSource().isStateVar()!=null)
				return true;
		return false;
	}
	
	public abstract boolean mayModifyState(Operation op);
	
	protected String getAttributeDefinitions(XlimOperation operation) {
		return "kind=\"" + toString() + "\"";
	}
	
	protected String getAttribute(String name, NamedNodeMap attributes) {
		Node node=attributes.getNamedItem(name);
		if (node==null)
			return null;
		else
			return node.getNodeValue();
	}
	
	protected Integer getIntegerAttribute(String name, NamedNodeMap attributes) {
		String value=getAttribute(name,attributes);
		if (value!=null)
			return Integer.valueOf(value);
		else
			return null;
	}
	
	public void setAttributes(XlimOperation op,
			                  NamedNodeMap attributes, 
			                  ReaderContext context) {
		// default implementation does nothing...
	}
		
	public abstract Operation create(List<? extends XlimSource> inputs,
            						 List<? extends XlimOutputPort> outputs,
            						 ContainerModule parent);
	
	public abstract List<XlimType> defaultOutputTypes(List<? extends XlimSource> inputs);
	
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
}
