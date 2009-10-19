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
import java.util.List;

import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.XlimTypeArgument;

public abstract class ParametricTypeKind extends TypeKind {
	
	private HashMap<Object,XlimType> mTypeMap;
		
	public ParametricTypeKind(String name) {
		super(name);
		mTypeMap=new HashMap<Object,XlimType>();
	}

	@Override
	public XlimType createType() {
		throw new UnsupportedOperationException("Type "+getTypeName()+" requires parameter(s)");
	}
	
	@Override
	public XlimType createType(int size) {
		throw new UnsupportedOperationException("Type "+getTypeName()+" requires parameter(s)");
	}

	@Override
	public XlimType createType(List<XlimTypeArgument> typeArg) {
		if (typeArg.isEmpty())
			return createType();
		else {
			Object typeParameter=getParameter(typeArg);
			if (typeParameter!=null)
				return createAndCache(typeParameter);
			else
				return null;
		}
	}
	
	/**
	 * @param typeParameter
	 * @return instanced type, either a fresh instance or a cached one
	 *         with the same type parameter.
	 *         
	 * Updates the map of instantiated types.
	 */
	protected XlimType createAndCache(Object typeParameter) {
		assert(typeParameter!=null);
		XlimType type=mTypeMap.get(typeParameter);
		if (type==null) {
			type=create(typeParameter);
			mTypeMap.put(typeParameter, type);
		}
		return type;
	}
	
	/**
	 * @param typeArg  list of type arguments
	 * @return         encoding of type argument record
	 *                 
	 * The number of type arguments, their names and values/types is checked
	 * and an object representing the complete record of arguments is returned.
	 * The returned object is used in two ways:
	 * (1) As a look-up in the map of instantiated types (the object should
	 *     implement equals and hashCode properly -to avoid duplicates)
	 * (2) As the argument of the create() method, which instantiates the type.
	 */
	protected abstract Object getParameter(List<XlimTypeArgument> typeArg);
	
	/**
	 * @param typeParameter
	 * @return an instantiation of the type, with the given type parameter
	 * 
	 * The type parameter is the object that is returned by getParameter
	 */
	protected abstract XlimType create(Object typeParameter);
}
