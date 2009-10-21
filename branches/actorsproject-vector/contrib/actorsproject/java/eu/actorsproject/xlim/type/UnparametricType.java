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


import java.util.List;

import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.XlimTypeArgument;

/**
 * An unparamteric type is both a TypeKind (type constructor etc.) and
 * an XlimType.
 */
public abstract class UnparametricType extends TypeKind implements XlimType {

	public UnparametricType(String typeName) {
		super(typeName);
	}
	
	@Override
	public XlimType createType() {
		return this;
	}
	
	@Override
	public XlimType createType(int size) {
		if (size==getSize())
			return createType();
		else
			throw new IllegalArgumentException("Type "+getTypeName()+" has size=\""+getSize()
					                           +"\" inconsisten attribute size=\""+size+"\" provided");
	}

	@Override
	public XlimType createType(List<XlimTypeArgument> typeArg) {
		if (typeArg.isEmpty())
			return createType();
		else
			throw new IllegalArgumentException("Type "+getTypeName()+" takes no parameter");
	}
		
	@Override
	public TypeKind getTypeKind() {
		return this;
	}
			
	@Override
	public String getAttributeDefinitions() {
		return "typeName=\"" + getTypeName() + "\"";
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
		return false;
	}
	
	@Override
	public XlimType getTypeParameter(String name) {
		return null;
	}

	@Override
	public String getValueParameter(String name) {
		return null;
	}

	@Override
	XlimType createLub(XlimType t1, XlimType t2) {
		assert(hasPromotionFrom(t1.getTypeKind()) 
			   && hasPromotionFrom(t2.getTypeKind()));
		return this;
	}
}
