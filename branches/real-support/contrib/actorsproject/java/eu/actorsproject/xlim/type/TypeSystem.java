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

import org.w3c.dom.NamedNodeMap;

import eu.actorsproject.xlim.XlimType;

/**
 * Container for all type kinds, types and type conversions
 */
public class TypeSystem implements TypeFactory {
	private HashMap<String,TypeKind> mTypeMap = new HashMap<String,TypeKind>();
			
	/**
	 * Add a TypeKind, by which type instances (XlimType) 
	 * can be created 
	 * @param kind
	 */
	public void addTypeKind(TypeKind kind) {
		mTypeMap.put(kind.getTypeName(), kind);
	}

	/**
	 * Adds a "specific" type promotion, which is used when it matches
	 * (source type, target type) exactly. 
	 * @param tp - type promotion
	 */
	public void addSpecificTypePromotion(TypeConversion tp) {
		TypeKind sourceKind=tp.getSourceTypeKind();
		sourceKind.addSpecificTypePromotion(tp);
	}
	
	/**
	 * Sets the "default" type promotion of a particular source type,
	 * which is used in the event that no exact match (source type, target type)
	 * is found.
	 * @param tp - type promotion
	 */
	public void addDefaultTypePromotion(TypeConversion tp) {
		TypeKind sourceKind=tp.getSourceTypeKind();
		sourceKind.setDefaultTypePromotion(tp);
	}
	
	/**
	 * Adds an (explicit) type conversion.
	 * @param tc - TypeConversion
	 */
	public void addTypeConversion(TypeConversion tc) {
		TypeKind sourceKind=tc.getSourceTypeKind();
		sourceKind.addTypeConversion(tc);
	}
	
	/**
	 * Determine the relation between the types
	 */
	public void completeInitialization() {
		int numTypeKinds=mTypeMap.size();
		int dfsNumber=numTypeKinds;
		for (TypeKind kind: mTypeMap.values())
			dfsNumber=kind.topSort(dfsNumber);
		
		for (TypeKind kind: mTypeMap.values())
			kind.computeAncestors(numTypeKinds);
	}
	
	/*
	 * Implementation of TypeFactory
	 */
	
	@Override
	public TypeKind getTypeKind(String typeName) {
		TypeKind kind=mTypeMap.get(typeName);
		if (kind!=null)
			return kind;
		else
			throw new RuntimeException("Unsupported typename: "+typeName);
	}
	
	// TODO: replace by create w parameter
	@Override
	public XlimType createInteger(int size) {
		return create("int", size);
	}
	
	// TODO: replace by "plain" create
	@Override
	public XlimType createBoolean() {
		return create("bool");
	}
	
	@Override
	public XlimType create(String typeName) {
		TypeKind kind=getTypeKind(typeName);
		return kind.createType();
	}
	
	@Override
	public XlimType create(String typeName, Object param) {
		TypeKind kind=getTypeKind(typeName);
		return kind.createType(param);
	}
	
	@Override
	public XlimType create(String typeName, NamedNodeMap attributes) {
		TypeKind kind=getTypeKind(typeName);
		return kind.createTypeFromAttributes(attributes);
	}
}
