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

package eu.actorsproject.xlim;

import java.util.List;

/**
 * XlimTypeKind groups the type constructor (createType methods) and
 * type conversions of a set of XlimTypes. 
 */
public interface XlimTypeKind {

	/**
	 * @return (unparametric) type (=result of a nullary type constructor).
	 * 
	 * This method fails for parametric type kind (arg->type).
	 */
	XlimType createType();
	
	/**
	 * @param param
	 * @return an instance of a parametric type 
	 *         (=result of a type constructor applied to 'typeArg').
	 *         
	 * Unless 'typeArg' is the empty list, this method fails for unparametric types. 
	 */
	XlimType createType(List<XlimTypeArgument> typeArg);
	
	
	/**
	 * @param size
	 * @return an instance of a type that is either unparameteric or parametric
	 *         in an int-valued parameter only.
	 *         
	 * This method supports legacy XLIM, which doesn't deal with parametric types
	 * other than int(size), in which also the (unparametric) bool type may have a 
	 * size attribute in XLIM.
	 */
	XlimType createType(int size);
	
	/**
	 * @param kind
	 * @return true iff 'kind' promotes (converts implicitly and losslessly)
	 *         to this type kind.
	 */
	boolean hasPromotionFrom(XlimTypeKind kind);
	
	/**
	 * @param t
	 * @return type (f this type kind, which is the result of promotion from 't'
	 */
	XlimType promote(XlimType t);
}
