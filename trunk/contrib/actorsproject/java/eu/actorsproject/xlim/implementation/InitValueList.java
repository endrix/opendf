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

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.xlim.XlimInitValue;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.util.Session;

class InitValueList implements XlimInitValue {

	private ArrayList<XlimInitValue> mChildren;
	private XlimType mType;
	private XlimType mCommonElementType;
	private int mNumElements;
	private boolean mIsZero;
	
	public InitValueList(Collection<? extends XlimInitValue> children) {
		mChildren = new ArrayList<XlimInitValue>(children);
		XlimType childT=null;
		
		// Find total number of elements and a possible common element type
		if (!mChildren.isEmpty()) {
			XlimInitValue firstChild=mChildren.get(0);
			mCommonElementType=firstChild.getCommonElementType();
			childT=firstChild.getType();
		}
		
		// Comupute the following properties
		// mIsZero            = true iff all elements are zero
		// mCommonElementType = possible common element type (a scalar type)
		// mNumElements       = total number of elements
		// childT             = common type of children (possibly a List-type)
		mIsZero=true;
		for (XlimInitValue initValue: mChildren) {
			XlimType t=initValue.getCommonElementType();
			int n=initValue.totalNumberOfElements();
			
			if (t!=mCommonElementType && n!=0)
				mCommonElementType=null;
			if (initValue.isZero()==false)
				mIsZero=false;
			mNumElements+=n;
			if (initValue.getType()!=childT)
				childT=null;
		}
		
		// TODO: we might not be able to assert this property
		// if (childT!=null) ...
		assert(childT!=null);
		mType=Session.getTypeFactory().createList(childT, mChildren.size());
	}
	
	@Override
	public List<? extends XlimInitValue> getChildren() {
		return mChildren;
	}

	
	public XlimType getType() {
		return mType;
	}

	@Override
	public XlimType getScalarType() {
		return null;
	}

	@Override
	public String getScalarValue() {
		return null;
	}

	@Override
	public XlimType getCommonElementType() {
		return mCommonElementType;
	}

	@Override
	public XlimType setCommonElementType(XlimType t) {
		XlimType childT=null;
		for (XlimInitValue initValue: mChildren) {
			XlimType newChildT=initValue.setCommonElementType(t);
			if (childT==null)
				childT=newChildT;
			else {
				assert(childT==newChildT);
			}
		}
		assert(childT!=null);
		
		mCommonElementType=t;
		mType=Session.getTypeFactory().createList(childT, mChildren.size());
		return mType;
	}
	
	@Override
	public int totalNumberOfElements() {
		return mNumElements;
	}

	
	@Override
	public boolean isZero() {
		return mIsZero;
	}

	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		return "typeName=\"List\"";
	}

	public String getTagName() {
		return "initValue";
	}

}
