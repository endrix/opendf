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

import java.util.AbstractList;
import java.util.List;

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.xlim.XlimInitValue;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.util.Session;

/**
 * Represents an initial value (of a state variable) that
 * is a List of N identical elements. Useful for a compact
 * representation of certain large initializers, zero
 * initializers in particular.
 */
public class RepeatedInitValue implements XlimInitValue {

	private XlimInitValue mElement;
	private int mRepeatFactor;
	private XlimType mType;
	
	public RepeatedInitValue(XlimInitValue element, int repeatFactor) {
		mElement=element;
		mRepeatFactor=repeatFactor;
		mType=Session.getTypeFactory().createList(element.getType(), repeatFactor);
	}

	@Override
	public XlimType getType() {
		return mType;
	}

	@Override
	public XlimType getScalarType() {
		return null; // not a scalar type
	}

	@Override
	public String getScalarValue() {
		return null; // not a scalar value
	}

	@Override
	public boolean isZero() {
		return mElement.isZero();
	}

	@Override
	public int totalNumberOfElements() {
		return mRepeatFactor*mElement.totalNumberOfElements();
	}

	@Override
	public XlimType getCommonElementType() {
		return mElement.getCommonElementType();
	}

	@Override
	public XlimType setCommonElementType(XlimType t) {
		XlimType childT=mElement.setCommonElementType(t);
		mType=Session.getTypeFactory().createList(childT, mRepeatFactor);
		return mType;
	}
	
	@Override
	public String getTagName() {
		return "initValue";
	}
	
	@Override
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		return "typeName=\"List\"";
	}
	
	@Override
	public List<XlimInitValue> getChildren() {
		return new RepeatList();
	}
	
	class RepeatList extends AbstractList<XlimInitValue> {
		@Override
		public XlimInitValue get(int index) {
			if (index<0 || index>=mRepeatFactor)
				throw new IndexOutOfBoundsException();
			return mElement;
		}

		@Override
		public int size() {
			return mRepeatFactor;
		}
	}
}
