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
import eu.actorsproject.xlim.XlimTypeArgument;


/**
 * Represents an XLIM <type> element (part of a typeDef element)
 * XlimTypeElements are used only temporarily when reading and writing XLIM files.
 * They are then converted into XlimTypes.
 */
public abstract class XlimTypeElement implements XmlElement {

	/**
	 * @param readerPlugIn  plug-in used to create types
	 * @param context       context, which is used to resolve typeDefs
	 * @return the type that corresponds to this element
	 */
	public abstract XlimType createType(ReaderPlugIn readerPlugIn, ReaderContext context);

	/**
	 * @return type of this element (null if there are unresolved references)
	 */
	public abstract XlimType getType();
}

abstract class TypeArgElement implements XmlElement {
	
	protected String mName;
	
	TypeArgElement(String name) {
		mName=name;
	}
	
	String getName() {
		return mName;
	}
	
	abstract XlimTypeArgument createTypeArgument(ReaderPlugIn readerPlugIn, ReaderContext context);
	
	@Override
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		return "name=\"" + mName + "\"";
	}
}

class TypeParElement extends TypeArgElement {

	private XlimTypeElement mTypeElement;
	
	TypeParElement(String name, XlimTypeElement typeElement) {
		super(name);
		mTypeElement=typeElement;
	}
	
	@Override
	XlimTypeArgument createTypeArgument(ReaderPlugIn readerPlugIn, ReaderContext context) {
		XlimType type=mTypeElement.createType(readerPlugIn, context);
		if (type!=null)
			return readerPlugIn.createTypeArgument(mName,type);
		else
			return null;
	}
	
	@Override
	public String getTagName() {
		return "typePar";
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return Collections.singletonList(mTypeElement);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof TypeParElement) {
			TypeParElement t=(TypeParElement) o;
			return mName.equals(t.mName) && mTypeElement.equals(t.mTypeElement);
		}
		else
			return false;
	}
	
	@Override
	public int hashCode() {
		return mName.hashCode() ^ mTypeElement.hashCode();
	}
}

class ValueParElement extends TypeArgElement {

	private String mValue;
	
	ValueParElement(String name, String value) {
		super(name);
		mValue=value;
	}
	
	@Override
	XlimTypeArgument createTypeArgument(ReaderPlugIn readerPlugIn, ReaderContext context) {
		return readerPlugIn.createTypeArgument(mName, mValue);
	}
	
	@Override
	public String getTagName() {
		return "valuePar";
	}
		
	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		return "name=\"" + mName + "\" value=\"" + mValue + "\"";
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof ValueParElement) {
			ValueParElement v=(ValueParElement) o;
			return mName.equals(v.mName) && mValue.equals(v.mValue);
		}
		else
			return false;
	}
	
	@Override
	public int hashCode() {
		return mName.hashCode() ^ mValue.hashCode();
	}
}