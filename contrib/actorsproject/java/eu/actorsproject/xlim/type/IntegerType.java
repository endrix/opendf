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

import java.util.Collections;

import eu.actorsproject.xlim.XlimType;

class IntegerType implements XlimType {
	private TypeKind mIntegerTypeKind;
	private int mSize;
	private String mTypeDefName;
	
	IntegerType(TypeKind integerTypeKind, int size) {
		mIntegerTypeKind=integerTypeKind;
		mSize=size;
	}
	
	@Override
	public TypeKind getTypeKind() {
		return mIntegerTypeKind;
	}
	
	@Override
	public int getSize() { 
		return mSize; 
	}
	
	@Override
	public String getTypeName() { 
		return "int"; 
	}
	
	@Override
	public String getAttributeDefinitions() {
		return "typeName=\"int\" size=\"" + mSize + "\"";
	}
	
	@Override
	public String toString() {
		return "int(size="+mSize+")";
	}
	
	@Override
	public long minValue() {
		return ((long) -1)<<(mSize-1);
	}
	
	@Override
	public long maxValue() {
		return ~minValue();
	}
	
	@Override
	public boolean isZero(String s) {
		try {
			return Long.valueOf(s)==0;
		} catch (NumberFormatException ex) {
			return false;
		}
	}
	
	@Override
	public String getZero() {
		return "0";
	}

	@Override
	public boolean isBoolean() {
		return false;
	}
	
	@Override
	public boolean isInteger() {
		return true;
	}
	
	@Override
	public boolean isList() {
		return false;
	}
	
	public Iterable<TypeArgument> getTypeArguments() {
		return Collections.singletonList(new TypeArgument("size", String.valueOf(mSize)));
	}
	
	@Override
	public XlimType getTypeParameter(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getValueParameter(String name) {
		if (name.equals("size"))
			return Integer.toString(mSize);
		else
			throw new IllegalArgumentException("no such value parameter: "+name);
	}
	
	@Override
	public int getIntegerParameter(String name) {
		if (name.equals("size"))
			return mSize;
		else
			throw new IllegalArgumentException("no such value parameter: "+name);
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