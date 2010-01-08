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

package eu.actorsproject.xlim.dependence;

import java.util.Collections;

import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimInitValue;
import eu.actorsproject.xlim.XlimStateCarrier;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.XlimType;

/**
 * StateValueNode node is the common base of value nodes that represent state
 * (ports and state variables)
 *  
 * StateValueNodes model data dependence that is caused by operations on
 * ports and state variables, both true dependence ("read-after-write") and
 * artificial dependence ("write-after-write" and "write-after-read").
 */
public abstract class StateValueNode extends ValueNode {

	private static int mNextId;
	private int mUniqueId;
	
	public StateValueNode() {
		mUniqueId=mNextId++;
	}
	
	@Override
	public String getUniqueId() {
		return "v"+mUniqueId; 
	}
	
	@Override
	public XlimType getScalarType() {
		XlimStateCarrier carrier=getStateCarrier();
		XlimTopLevelPort port=carrier.isPort();
		if (port!=null)
			return port.getType();
		else {
			XlimStateVar stateVar=carrier.isStateVar();
			XlimInitValue initValue=stateVar.getInitValue();
			return initValue.getScalarType();
		}
	}
	
	@Override
	public XlimType getCommonElementType() {
		XlimStateCarrier carrier=getStateCarrier();
		XlimTopLevelPort port=carrier.isPort();
		if (port!=null)
			return port.getType();
		else {
			XlimStateVar stateVar=carrier.isStateVar();
			XlimInitValue initValue=stateVar.getInitValue();
			return initValue.getCommonElementType();
		}
	}

	@Override
	public String getTagName() {
		return "StateValueNode";
	}
	
	@Override
	public String getAttributeDefinitions() {
		XlimStateCarrier carrier=getStateCarrier();
		String name=carrier.getSourceName();
		String source="";
		
		if (name!=null)
			name = " name=\"" + name + "\"";
		else
			name="";
		
		XlimStateVar stateVar=carrier.isStateVar();
		if (stateVar!=null)
			source = " source=\"" + stateVar.getUniqueId() + "\"";
		
		return "valueId=\"" + getUniqueId() + "\"" + name + source;
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return Collections.emptyList();
	}
}
