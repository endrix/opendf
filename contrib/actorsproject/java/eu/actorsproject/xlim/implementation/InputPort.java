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


import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimInputPort;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueUsage;

class InputPort implements XlimInputPort {

	private SourceValueUsage mValueAccess;
	
	public InputPort(XlimSource source, Instruction parent) {
		assert(source!=null && parent!=null);
		mValueAccess=new SourceValueUsage(source, parent);
	}
	
	@Override
	public XlimSource getSource() {
		return mValueAccess.getSource();
	}
	
	
	@Override
	public void setSource(XlimOutputPort newSource) {
		mValueAccess.setValue(newSource.getValue());
	}

	@Override
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		return "dir=\"in\" source=\"" + getSource().getUniqueId() + "\"";
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public String getTagName() {
		return "port";
	}
	
	public void removeReference() {
		mValueAccess.setValue(null);
	}
		
	public boolean dependsOnLocation() {
		return (mValueAccess.needsFixup());
	}
	
	@Override
	public ValueNode getValue() {
		return mValueAccess.getValue();
	}
	
	public ValueUsage getValueUsage() {
		return mValueAccess;
	}
}

