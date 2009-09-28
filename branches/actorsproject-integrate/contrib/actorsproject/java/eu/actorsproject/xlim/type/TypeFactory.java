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

import java.util.HashMap;
import java.util.TreeMap;

import org.w3c.dom.NamedNodeMap;

import eu.actorsproject.xlim.XlimType;

public class TypeFactory {
	
	private static TypeFactory sSingletonInstance=new TypeFactory();
	
	private HashMap<String,TypeKindPlugIn> mTypeMap;
	private IntegerKind mIntegerKind; 
	private SingletonTypeKind mBooleanKind;
	
	private TypeFactory() {
		mTypeMap=new HashMap<String,TypeKindPlugIn>();
		mIntegerKind=new IntegerKind();
		mBooleanKind=new SingletonTypeKind(new BooleanType());
		registerTypeKind(mIntegerKind);
		registerTypeKind(mBooleanKind);
	}
	
	public static TypeFactory getInstance() {
		return sSingletonInstance;
	}
	
	public TypeKindPlugIn getTypeKindPlugIn(String typeName) {
		return mTypeMap.get(typeName);
	}
	
	public void registerTypeKind(TypeKindPlugIn plugIn) {
		mTypeMap.put(plugIn.getTypeName(), plugIn);
	}
	
	public XlimType createInteger(int size) {
		return mIntegerKind.getType(size);
	}
	
	public XlimType createBoolean() {
		return mBooleanKind.getType();
	}
	
	private static class IntegerKind extends ParametricTypeKind<Integer> {
		private TreeMap<Integer,IntegerType> mIntegerTypes;
		
		IntegerKind() {
			super("int");
			mIntegerTypes=new TreeMap<Integer,IntegerType>();
		}
		
		protected Integer getParameter(NamedNodeMap attributes) {
			return getIntegerAttribute("size",attributes);
		}
		
		protected XlimType create(Integer size) {
			IntegerType type=mIntegerTypes.get(size);
			if (type==null) {
				type=new IntegerType(size);
				mIntegerTypes.put(size, type);
			}
			return type;
		}
	}
	
	private static class IntegerType implements XlimType {
		private int mSize;
		IntegerType(int size) {
			mSize=size;
		}
		public int getSize() { 
			return mSize; 
		}
		public String getTypeName() { 
			return "int"; 
		}
		public String getAttributeDefinitions() {
			return "typeName=\"int\" size=\"" + mSize + "\"";
		}
		public String toString() {
			return "int(size="+mSize+")";
		}
		public long minValue() {
			return ((long) -1)<<(mSize-1);
		}
		public long maxValue() {
			return ~minValue();
		}
		
		public boolean isBoolean() {
			return false;
		}
		
		public boolean isInteger() {
			return true;
		}
	}
	
	private static class BooleanType implements XlimType {
		public int getSize() { 
			return 1; 
		}
		public String getTypeName() { 
			return "bool"; 
		}
		public String getAttributeDefinitions() {
			return "typeName=\"bool\"";
		}
		public String toString() {
			return "bool";
		}
		public long minValue() {
			return 0;
		}
		public long maxValue() {
			return 1;
		}
		
		public boolean isBoolean() {
			return true;
		}
		
		public boolean isInteger() {
			return false;
		}
	}
}
