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
import java.util.BitSet;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.XlimTypeKind;
import eu.actorsproject.xlim.util.Session;

public abstract class TypeKind implements TypePattern, XlimTypeKind {
	
	private String mTypeName;
	private TypeConversion mDefaultPromotion;
	private ArrayList<TypeConversion> mSpecificPromotions;
	private ArrayList<TypeConversion> mSpecificConversions;
	private BitSet mAncestors;
	
	private static final int DFS_UNINITIALIZED=-2;
	private static final int DFS_INITIALIZING=-1;
	private static final int DFS_MIN=0;
	
	private int mDfsNumber=DFS_UNINITIALIZED;
	
	
	public TypeKind(String typeName) {
		mTypeName=typeName;
		mSpecificPromotions=new ArrayList<TypeConversion>();
		mSpecificConversions=new ArrayList<TypeConversion>();
	}
	
	public String getTypeName() {
		return mTypeName;
	}
	
	@Override
	public abstract XlimType createType();
	
	
	@Override
	public abstract XlimType createType(Object param);
	
	public abstract XlimType createTypeFromAttributes(NamedNodeMap attributes);
	
	public void setDefaultTypePromotion(TypeConversion tp) {
		if (mDefaultPromotion!=null)
			throw new IllegalStateException("Default promotion already set");
		mDefaultPromotion=tp;
	}
	
	public TypeConversion getDefaultTypePromotion() {
		return mDefaultPromotion;
	}
	
	public void addSpecificTypePromotion(TypeConversion tp) {
		mSpecificPromotions.add(tp);
	}
		
	public void addTypeConversion(TypeConversion tc) {
		mSpecificConversions.add(tc);
	}
	
	public boolean hasPromotionFrom(XlimType t) {
		XlimTypeKind sourceKind=t.getTypeKind();
		return sourceKind.hasPromotionFrom(this);
	}

	@Override
	public boolean hasPromotionFrom(XlimTypeKind tk) {
		if (tk instanceof TypeKind) {
			TypeKind kind=(TypeKind) tk;
			return kind.hasPromotionTo(this);
		}
		else
			return false;
	}
	
	private boolean hasPromotionTo(TypeKind kind) {
		assert(kind.mDfsNumber>=DFS_MIN);
		return mAncestors.get(kind.mDfsNumber);
	}
	
	public TypeConversion getTypePromotion(TypeKind kind) {
		for (TypeConversion tp: mSpecificPromotions)
			if (tp.getTargetTypeKind()==kind)
				return tp;
		return mDefaultPromotion;
	}
	
	@Override
	public XlimType promote(XlimType t) {
		XlimTypeKind tk=t.getTypeKind();
		if (tk instanceof TypeKind) {
			TypeKind sourceKind=(TypeKind) tk;
			while (sourceKind!=null && sourceKind!=this) {
				TypeConversion tp=sourceKind.getTypePromotion(this);
				t=tp.apply(t);
				sourceKind=tp.getTargetTypeKind();
			}
			return t;
		}
		else
			return null;
	}
	
	public boolean hasConversionTo(TypeKind kind) {
		if (hasPromotionTo(kind))
			return true;
		else {
			for (TypeConversion tp: mSpecificConversions)
				if (tp.getTargetTypeKind()==kind)
					return true;
			return false;
		}	
	}
	
	public boolean hasConversionFrom(XlimType t) {
		XlimTypeKind tk=t.getTypeKind();
		if (tk instanceof TypeKind) {
			TypeKind sourceKind=(TypeKind) tk;
			return sourceKind.hasConversionTo(this);
		}
		else
			return false;
	}
	
	public TypeConversion getTypeConversion(TypeKind kind) {
		// First, all type promotions qualify as conversions
		if (kind.hasPromotionTo(this))
			return getTypePromotion(kind);
		
		// Then, try to find an exact type conversion
		for (TypeConversion tp: mSpecificConversions)
			if (tp.getTargetTypeKind()==kind)
				return tp;
		return null;
	}
	
	public XlimType convert(XlimType t) {
		XlimTypeKind tk=t.getTypeKind();
		if (tk instanceof TypeKind) {
			TypeKind sourceKind=(TypeKind) tk;
			while (sourceKind!=null && sourceKind!=this) {
				TypeConversion tp=sourceKind.getTypeConversion(this);
				t=tp.apply(t);
				sourceKind=tp.getTargetTypeKind();
			}
			return t;
		}
		else
			return null;
	}
	
	/**
	 * @param t
	 * @return result of matching type 't'
	 */
	@Override
	public Match match(XlimType t) {
		XlimTypeKind sourceKind=t.getTypeKind();
		if (sourceKind==this)
			return Match.ExactTypeKind;
		else if (hasPromotionFrom(sourceKind))
			return Match.PromotedType;
		else
			return Match.DoesNotMatch;
	}
	
	/**
	 * @return maximal TypeKind that is matched by the pattern
	 *         (null if no such TypeKind exists: e.g. wildcard patttern)
	 *         
	 * Used to order 'PromotedType' matches (check if one Pattern is a 
	 * sub-pattern of the other).
	 */
	@Override
	public TypeKind patternTypeKind() {
		return this;
	}
	
	/**
	 * @param dfsNumber
	 * @return dfsNumber to use for next, smaller (or unrelated) TypeKind
	 */
	int topSort(int dfsNumber) {
		assert(mDfsNumber!=DFS_INITIALIZING); // Check for cyclic type promotion
		
		if (mDfsNumber==DFS_UNINITIALIZED) {
			mDfsNumber=DFS_INITIALIZING;
			for (TypeConversion tp: mSpecificPromotions)
				dfsNumber=tp.getTargetTypeKind().topSort(dfsNumber);
			if (mDefaultPromotion!=null)
				dfsNumber=mDefaultPromotion.getTargetTypeKind().topSort(dfsNumber);
			
			assert(dfsNumber>=DFS_MIN);
			mDfsNumber=dfsNumber-1;
			return mDfsNumber;
		}
		else
			return dfsNumber;
	}
	
	BitSet computeAncestors(int numTypeKinds) {
		
		if (mAncestors==null) {
			mAncestors=new BitSet(numTypeKinds);

			if (mDefaultPromotion!=null) {
				// Transitive type promotions via "default" promotion
				TypeKind targetKind=mDefaultPromotion.getTargetTypeKind();
				mAncestors.or(targetKind.computeAncestors(numTypeKinds));
			}
			for (TypeConversion tp: mSpecificPromotions) {
				// For "specific" promotions just include the target type
				TypeKind targetKind=tp.getTargetTypeKind();
				mAncestors.set(targetKind.mDfsNumber);
			}
			mAncestors.set(mDfsNumber);  // Also include myself
		}
		
		return mAncestors;
	}
	
	protected String getAttribute(String name, NamedNodeMap attributes) {
		Node node=attributes.getNamedItem(name);
		if (node==null)
			return null;
		else
			return node.getNodeValue();
	}
	
	protected Integer getIntegerAttribute(String name, NamedNodeMap attributes) {
		String value=getAttribute(name,attributes);
		if (value!=null)
			return Integer.valueOf(value);
		else
			return null;
	}
	
	@Override
	public String toString() {
		return mTypeName;
	}
}
