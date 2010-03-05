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

/**
 * 
 */
package eu.actorsproject.xlim;

import eu.actorsproject.xlim.type.TypeArgument;

/**
 * @author ecarvon
 *
 */
public interface XlimType {
	/**
	 * @return the name of the type/type constructor
	 * 
	 * For parametric types the name of the type constructor (e.g. int, List)
	 * is returned. For unparametric types the type and the type constructor
	 * have the same name (e.g. bool, real). 
	 */
	String getTypeName();
	
	/**
	 * @return the XlimTypeKind of the type, by which new instances can
	 *         be created.
	 * TODO: Confusing name? the TypeKind is actually the type constructor
	 */
	XlimTypeKind getTypeKind();
	
	/**
	 * @return true for the bool type, false for all other types
	 */
	boolean isBoolean();
	
	/**
	 * @return true for instances of int, false for all other types
	 */
	boolean isInteger();
	
	
	/**
	 * @return true for instances of List, false for all other types
	 */
	boolean isList();

	/**
	 * @param value
	 * @return true if 'value' represents zero in this type
	 * 
	 * Only applicable to scalar types, will return false for aggregates
	 */
	boolean isZero(String value);
	
	
	/**
	 * @return a representation of the zero in this type (there might be several)
	 */
	String getZero();

	/**
	 * @return collection of type arguments (possibly empty)
	 */
	Iterable<TypeArgument> getTypeArguments();
	
	/**
	 * @param name
	 * @return the type parameter of the given name
	 * 
	 * Only applicable to parameteric types that have a type parameter 'name'.
	 */
	XlimType getTypeParameter(String name);
	
	/**
	 * @param name
	 * @return the value parameter of the given name
	 * 
	 * Only applicable to parameteric types that have a value parameter 'name'.
	 */
	String getValueParameter(String name);
	
	/**
	 * @param name
	 * @return the integer value parameter of the given name
	 * 
	 * Only applicable to parameteric types that have an integer value 
	 * parameter 'name'.
	 */
	int getIntegerParameter(String name);

	/**
	 * @return the name of (a unique) typeDef that has been associated with this type
	 *         (null if no such name)
	 */
	String getTypeDefName();
	
	/**
	 * @param name of typeDef that is to be associated with this type
	 */
	void setTypeDefName(String name);
	
	// TODO: should be support these methods?
	int getSize();
	long minValue();
	long maxValue();
	String getAttributeDefinitions();
}
