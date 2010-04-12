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

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimInitValue;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.dependence.Location;
import eu.actorsproject.xlim.dependence.StateLocation;

import java.util.Collections;

class StateVar implements XlimStateVar, StateLocation {

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
	public boolean isStateLocation() {
		return true;  // yes, it's a StateLocation (see Location)
	}
	
	@Override
	public StateLocation asStateLocation() {
		return this;  // yes, it's a StateLocation (see Location)
	}

	@Override
	public boolean hasSource() {
		return true; // yes, it has a source (see Location)
	}

	@Override
	public XlimSource getSource() {
		return this; // yes, it has a source (see Location)
	}

	@Override
	public boolean hasLocation() {
		return true; // yes, it has a location (see XlimSource)
	}
	
	@Override
	public Location getLocation() {
		return this; // yes, it has a location (see XlimSource)
	}

	@Override
	public StateVar asStateVar() {
		return this; // yes, it's a StateVar (see XlimSource)
	}

	@Override
	public OutputPort asOutputPort() {
		return null; // not an OutputPort (see XlimSource)
	}

	@Override
	public TopLevelPort asActorPort() {
		return null; // not an actor port (see StateLocation)
	}

	@Override
	public boolean isModified() {
		// For now assume that all statevars are modified at some point
		// TODO: identify immutable statevars
		return true;
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
	public String getSourceName() {
		return mSourceName;
	}

	@Override
	public String getDebugName() {
		return (mSourceName!=null)? mSourceName : getUniqueId();
	}

	@Override
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		String attributes=formatter.getAttributeDefinition("name",this,getUniqueId());
		if (mSourceName!=null)
			attributes+=" sourceName=\"" + mSourceName + "\"";
		attributes += " " + getType().getAttributeDefinitions();
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
