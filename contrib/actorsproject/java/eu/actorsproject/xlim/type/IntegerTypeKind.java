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
 * Type kind, which is common to all integer types
 */
class IntegerTypeKind extends ParametricTypeKind {
	
	IntegerTypeKind() {
		super("int");
	}
	
	
	/**
	 * @param size
	 * @return an instance of int with the given size attribute
	 *         
	 * This method supports legacy XLIM, which doesn't deal with parametric types
	 * other than int(size), which has an explicit size attribute in XLIM.
	 */
	
	@Override
	public XlimType createType(int size) {
		Integer typeParameter=size;
		return createAndCache(typeParameter);
	}


	@Override
	protected XlimType create(Object param) {
		if (param instanceof Integer) {
			Integer size=(Integer) param;
			return new IntegerType(this, size);
		}
		else
			throw new IllegalArgumentException("Type \"int\" requires Integer parameter");
	}
	
	@Override
	XlimType createLub(XlimType t1, XlimType t2) {
		XlimType intT1=promote(t1);
		XlimType intT2=promote(t2);
		if (intT1.getSize()>=intT2.getSize())
			return intT1;
		else
			return intT2;
	}


	@Override
	protected Object getParameter(List<XlimTypeArgument> typeArgList) {
		if (typeArgList.size()==1) {
			XlimTypeArgument arg=typeArgList.get(0);
			
			if (arg.getName().equals("size") && arg.isValueParameter()) {
				String size=arg.getValue();
				return Integer.valueOf(size);
			}
		}
		throw new IllegalArgumentException("Type \"int\" requires an Integer \"size\" parameter");
	}	
}
