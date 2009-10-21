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

import java.util.Collections;


import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimInstruction;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimStateCarrier;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueOperator;

class OutputPort extends ValueNode implements XlimOutputPort {

	private static int sNextUniqueId;
	
	private XlimType mType;
	private Instruction mParent;
	private int mUniqueId;
	
	public OutputPort(XlimType type) {
		mType=type;
		mUniqueId=sNextUniqueId++;
		mParent=null;
	}
	
	@Override
	public ValueOperator getDefinition() {
		return mParent.getValueOperator();
	}
		
	@Override
	public XlimStateCarrier getStateCarrier() {
		return null; // null for OutputPorts
	}

	@Override
	public ValueNode getDominatingDefinition() {
		return null; // null for OutputPorts
	}

	@Override
	public XlimInstruction getParent() {
		return mParent;
	}

	public void setParent(Instruction parent) {
		assert(mParent==null);  // Only to be done once!
		mParent=parent;
	}
	
	@Override
	public void substitute(XlimOutputPort newXlimPort) {
		if (newXlimPort!=this) {
			if ((newXlimPort instanceof OutputPort)==false)
				throw new IllegalArgumentException();
			OutputPort newPort=(OutputPort) newXlimPort;
			super.substitute(newPort);
		}
	}
		
	@Override
	public XlimType getType() {
		return mType;
	}
	
	@Override
	public void setType(XlimType type) {
		mType=type;
	}
	
	@Override
	public XlimType actualOutputType() {
		Operation op=mParent.isOperation();
		if (op==null) {
			// Phi-node: actual type and declared type the same
			return mType;
		}
		else {
			for (int i=0; i<op.getNumOutputPorts(); ++i)
				if (op.getOutputPort(i)==this) {
					OperationKind kind=op.getOperationKind();
					return kind.actualOutputType(op, i);
				}
			throw new IllegalStateException("OutputPort not found in its parent");
		}
	}

	@Override
	public OutputPort isOutputPort() {
		return this; // yes, it's an OutputPort (see XlimSource)
	}

	@Override
	public StateVar isStateVar() {
		return null; // not a StateVar (see XlimSource)
	}

	@Override
	public String getUniqueId() {
		return "t"+mUniqueId;
	}
	
	@Override
	public String getAttributeDefinitions() {
		return "dir=\"out\" source=\"" + getUniqueId() + "\" " + mType.getAttributeDefinitions();
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public String getTagName() {
		return "port";
	}
	
	@Override
	public ValueNode getValue() {
		return this;
	}	
}
