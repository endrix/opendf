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
import java.util.HashMap;
import java.util.List;

import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.XlimTypeArgument;
import eu.actorsproject.xlim.XlimTypeKind;

/**
 * Container for all type kinds, types and type conversions
 */
public class TypeSystem implements TypeFactory {
	private HashMap<String,TypeKind> mTypeMap = new HashMap<String,TypeKind>();
	private ListTypeConstructor mListConstructor = new ListTypeConstructor();
	
	/**
	 * Add a TypeKind, by which type instances (XlimType) 
	 * can be created 
	 * @param kind
	 */
	public void addTypeKind(TypeKind kind) {
		mTypeMap.put(kind.getTypeName(), kind);
	}

	/**
	 * A list type is needed -even if not available to the programmer-
	 * since we rely on List-types to annotate aggragate state variables with
	 * a type.
	 * This method makes the type constructor, List(type,size), available
	 * for other uses aswell. 
	 */
	public void addListType() {
		addTypeKind(mListConstructor);
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
		
		if (mTypeMap.containsKey(mListConstructor.getTypeName())) {
			// It is not certain that we have added the List constructor, but
			// if we have it should be the same one as is used in createList
			assert(mTypeMap.get(mListConstructor.getTypeName())==mListConstructor);
		}
		else {
			// We have to initialize the List constructor anyway, since it is
			// used for aggregate state variables, but it won't be returned by getTypeKind(). 
			numTypeKinds++;
			mListConstructor.topSort(dfsNumber);
		}
		
		for (TypeKind kind: mTypeMap.values())
			dfsNumber=kind.topSort(dfsNumber);
		
		for (TypeKind kind: mTypeMap.values())
			kind.computeAncestors(numTypeKinds);
		// Also sort the List-type (in case it is not registered)
		mListConstructor.computeAncestors(numTypeKinds);
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
	
	
	public TypeKind leastUpperBound(XlimTypeKind xlimKind1, XlimTypeKind xlimKind2) {
		if (xlimKind1 instanceof TypeKind 
		    && xlimKind2 instanceof TypeKind) {
			TypeKind kind1=(TypeKind) xlimKind1;
			TypeKind kind2=(TypeKind) xlimKind2;
			if (kind1==kind2 || kind1.hasPromotionFrom(kind2))
				return kind1;
			else if (kind2.hasPromotionFrom(kind1))
				return kind2;
			else {
				// TODO: there may also be a common LUB, which is
				// neither kind1 nor kind2
				return null;
			}
		}
		else
			return null;
	}
	
	@Override
	public XlimType leastUpperBound(XlimType t1, XlimType t2) {
		TypeKind kind=leastUpperBound(t1.getTypeKind(), t2.getTypeKind());
		if (kind!=null)
			return kind.createLub(t1, t2);
		else
			return null;
	}

	// TODO: replace by create w parameter
	@Override
	public XlimType createInteger(int size) {
		TypeKind kind=getTypeKind("int");
		return kind.createType(size);
	}
	
	// TODO: replace by "plain" create
	@Override
	public XlimType createBoolean() {
		return create("bool");
	}
	
	// TODO: replace by getTypeKind()/TypeKind.create
	@Override
	public XlimType create(String typeName) {
		TypeKind kind=getTypeKind(typeName);
		return kind.createType();
	}

	public XlimType createList(XlimType elementType, int size) {
		List<XlimTypeArgument> typeArgs=new ArrayList<XlimTypeArgument>(2);
		typeArgs.add(new TypeArgument("type", elementType));
		typeArgs.add(new TypeArgument("size", Integer.toString(size)));
		return mListConstructor.createType(typeArgs);
	}
}
