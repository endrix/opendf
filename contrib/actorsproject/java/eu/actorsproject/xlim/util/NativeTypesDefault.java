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

package eu.actorsproject.xlim.util;

import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.XlimTypeKind;
import eu.actorsproject.xlim.type.TypeFactory;

public class NativeTypesDefault implements NativeTypePlugIn {
	protected XlimTypeKind mIntTypeKind;
	protected XlimType mInt8, mInt16, mInt32, mInt64;
	
	public NativeTypesDefault() {
		TypeFactory typeFact=Session.getTypeFactory();
		mIntTypeKind=typeFact.getTypeKind("int");
		mInt8=typeFact.createInteger(8);
		mInt16=typeFact.createInteger(16);
		mInt32=typeFact.createInteger(32);
		mInt64=typeFact.createInteger(64);
	}

	@Override
	public XlimType nativeType(XlimType type) {
		return nativeType(type, mInt32);
	}

	@Override
	public XlimType nativeElementType(XlimType type) {
		// TODO: Support small integer types for aggregates
		// return nativeType(type, mInt8);
		return nativeType(type, mInt32);
	}

	@Override
	public XlimType nativePortType(XlimType type) {
		// TODO: Support small integer types for aggregates
		// return nativeType(type, mInt8);
		return nativeType(type, mInt32);
	}

	protected XlimType nativeType(XlimType type, XlimType smallestInt) {
		if (type.getTypeKind()==mIntTypeKind) {
			assert(type.isInteger());
			int width=type.getSize();
			assert(width<=64);

			if (width<=smallestInt.getSize())
				return smallestInt;
			else if (width<=16)
				if (width<=8)
					return mInt8;
				else
					return mInt16;
			else if (width<=32)
				return mInt32;
			else
				return mInt64;
		}
		else
			return type;
	}
}
