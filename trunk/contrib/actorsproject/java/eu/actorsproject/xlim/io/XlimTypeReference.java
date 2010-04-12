/* 
 * Copyright (c) Ericsson AB, 2010
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

package eu.actorsproject.xlim.io;

import java.util.Collections;

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimType;

/**
 * An XlimTypeElement, which is a reference to a type definition
 */

public class XlimTypeReference extends XlimTypeElement {
	private String mName;
	private XlimLocation mLocation;
	private XlimTypeDef mTypeDef;
	
	/**
	 * Create an unresolved XlimTypeElement   
	 * @param name      Name of (built-in) type or typeDef
	 * @param location  Location in XLIM file
	 */
	public XlimTypeReference(String name, XlimLocation location) {
		mName=name;
		mLocation=location;
	}
	
	/**
	 * Create a XlimTypeElement, which is a reference to a typeDef
	 * @param typeDef
	 */
	public XlimTypeReference(XlimTypeDef typeDef) {
		mName=typeDef.getName();
		mLocation=null;
		mTypeDef=typeDef;
	}
	
	@Override
	public XlimType createType(ReaderPlugIn plugIn, ReaderContext context) {
		if (mTypeDef==null) {
			mTypeDef=context.getTypeDef(mName);
		
			if (mTypeDef==null)
				throw new XlimReaderError("Unsupported Type: "+mName, mLocation);
		}
		
		return mTypeDef.getType();
	}

	@Override
	public XlimType getType() {
		assert(mTypeDef!=null);
		return mTypeDef.getType();
	}
	
	public XlimTypeDef getTypeDef() {
		assert(mTypeDef!=null);
		return mTypeDef;
	}
	
	@Override
	public String getTagName() {
		return "type";
	}
		
	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		return formatter.getAttributeDefinition("name",getType(),mName);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof XlimTypeReference) {
			XlimTypeReference other=(XlimTypeReference) o;
			
			return (mName.equals(other.mName));
		}
		else
			return false;
	}
	
	@Override 
	public int hashCode() {
		return mName.hashCode();
	}
}
