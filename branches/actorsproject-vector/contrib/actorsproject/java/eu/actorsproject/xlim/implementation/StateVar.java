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

import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimInitValue;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimType;

import java.util.Collections;

class StateVar implements XlimStateVar {

	private static int sNextUniqueId;
	
	private String mSourceName;
	private XlimInitValue mInitValue;
	private int mUniqueId;
	
	public StateVar(String sourceName, XlimInitValue initValue) {
		mSourceName=sourceName;
		mInitValue=initValue;
		mUniqueId=sNextUniqueId++;
	}

	@Override
	public XlimInitValue getInitValue() {
		return mInitValue;
	}

	@Override
	public String getSourceName() {
		return mSourceName;
	}

	@Override
	public OutputPort isOutputPort() {
		return null; // not an OutputPort (see XlimSource)
	}

	@Override
	public StateVar isStateVar() {
		return this; // yes, it's a StateVar (see XlimSource)
	}

	@Override
	public TopLevelPort isPort() {
		return null;
	}

	@Override
	public XlimType getType() {
		return mInitValue.getType();
	}
	
	@Override
	public String getTagName() {
		return "stateVar";
	}

	@Override
	public String getUniqueId() {
		return "s"+mUniqueId;
	}

	@Override
	public String getAttributeDefinitions() {
		String attributes="name=\"" + getUniqueId() + "\"";
		if (mSourceName!=null)
			attributes+=" sourceName=\"" + mSourceName + "\"";
		return attributes;
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		if (mInitValue.getScalarValue()!=null)
			return Collections.singletonList(mInitValue);
		else
			return mInitValue.getChildren();
	}	
}
