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

package eu.actorsproject.xlim.type;

import java.util.ArrayList;

import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.XlimTypeKind;


public class ListType implements XlimType {

	private TypeKind mListTypeCtor;
	private XlimType mElementType;
	private int mNumElements;
	private String mTypeDefName;
	
	public ListType(TypeKind listTypeCtor, XlimType elementType, int numElements) {
		mListTypeCtor=listTypeCtor;
		mElementType=elementType;
		mNumElements=numElements;
	}

	@Override
	public String getTypeName() {
		return "List";
	}

	@Override
	public XlimTypeKind getTypeKind() {
		return mListTypeCtor;
	}
	
	@Override
	public String getAttributeDefinitions() {
		if (mTypeDefName!=null)
			return "typeName=\"" + mTypeDefName + "\"";
		else
			return "typeName=\"List\"";
	}

	@Override
	public boolean isBoolean() {
		return false;
	}

	@Override
	public boolean isInteger() {
		return false;
	}

	@Override
	public boolean isList() {
		return true;
	}
	
	// TODO: this method doesn't make sense, can it be removed?
	// In particular it doesn't mean the same thing as getValueParameter("size") 
	@Override
	public int getSize() {
		throw new UnsupportedOperationException("ListType.getSize()");
	}	

	// TODO: this method doesn't make sense, can it be removed?
	@Override
	public boolean isZero(String value) {
		throw new UnsupportedOperationException("ListType.isZero()");
	}

	@Override
	public String getZero() {
		throw new UnsupportedOperationException("ListType.isZero()");
	}
	
	// TODO: this method doesn't make sense, can it be removed?
	public long maxValue() {
		throw new UnsupportedOperationException("ListType.maxValue()");
	}

	// TODO: this method doesn't make sense, can it be removed?
	public long minValue() {
		throw new UnsupportedOperationException("ListType.minValue()");
	}	
	
	public Iterable<TypeArgument> getTypeArguments() {
		ArrayList<TypeArgument> args=new ArrayList<TypeArgument>(2);
		args.add(new TypeArgument("type", mElementType));
		args.add(new TypeArgument("size", String.valueOf(mNumElements)));
		return args;
	}
	
	@Override
	public XlimType getTypeParameter(String name) {
		if (name.equals("type"))
			return mElementType;
		else
			throw new IllegalArgumentException("No such type parameter: "+name);
	}

	@Override
	public String getValueParameter(String name) {
		if (name.equals("size"))
			return Integer.toString(mNumElements);
		else
			throw new IllegalArgumentException("No such value parameter: "+name);
	}
	
	@Override
	public int getIntegerParameter(String name) {
		if (name.equals("size"))
			return mNumElements;
		else
			throw new IllegalArgumentException("No such value parameter: "+name);
	}
	
	@Override
	public String toString() {
		return "List(type:"+mElementType+",size="+mNumElements+")";
	}
	
	@Override
	public String getTypeDefName() {
		return mTypeDefName;
	}

	@Override
	public void setTypeDefName(String name) {
		mTypeDefName=name;
	}
}
