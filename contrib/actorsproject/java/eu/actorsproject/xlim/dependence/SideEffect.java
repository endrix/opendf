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

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimType;

/**
 * SideEffect node is the common base of value nodes that represent side effects
 * (which act on actor ports, state variables or local aggregates)
 *  
 * StateValueNodes model data dependence that is caused by operations on
 * ports and state variables, both true dependence ("read-after-write") and
 * artificial dependence ("write-after-write" and "write-after-read").
 */
public abstract class SideEffect extends ValueNode {

	private static int mNextId;
	private int mUniqueId;
	
	public SideEffect() {
		mUniqueId=mNextId++;
	}
	
	@Override
	public boolean hasLocation() {
		return true;
	}

	@Override
	public String getUniqueId() {
		return "v"+mUniqueId; 
	}
	
	@Override
	public String getTagName() {
		return "SideEffect";
	}
	
	@Override
	public XlimType getType() {
		Location location=getLocation();
		return location.getType();
	}
	
	@Override
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		Location location=getLocation();
		String name=location.getDebugName();
		String attributes="valueId=\"" + getUniqueId() + "\" name=\"" + name + "\"";
		
		if (location.hasSource()) {
			XlimSource source=location.getSource();
			return formatter.addAttributeDefinition(attributes,"source",source,source.getUniqueId());
		}
		else
			return attributes;
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return Collections.emptyList();
	}
}
