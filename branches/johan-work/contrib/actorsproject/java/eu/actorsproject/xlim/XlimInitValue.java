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

import eu.actorsproject.util.XmlElement;

/**
 * @author ecarvon
 * An XlimInitValue is either scalar, in which case getScalarType() returns its type
 * and getValueAsInteger() returns its value (interpreted as an integer), or
 * a list of initial values (scalar or aggregates), for which an iterator is provided. 
 */
public interface XlimInitValue extends XmlElement {
	
	
	/**
	 * @return type of scalar value, null for aggregate values
	 */
	XlimType getScalarType();
	
	/**
	 * @return scalar initial value, null for aggregate values
	 */
	Integer getScalarValue();

	
	/**
	 * @return initial values of aggregate, empty list for scalar values
	 */
	List<? extends XlimInitValue> getChildren();
	
	
	/**
	 * @return the unique element type of an scalar/array/matrix kind of initializer,
	 *         that is a multi-dimensional structure with a consistent element type,
	 *         null for structures (records?) with varying element types.
	 */
	XlimType getCommonElementType();
	
	
	/**
	 * @return the total number of scalar elements in the structure
	 */
	int totalNumberOfElements();
	
	/**
	 * @return true if the (possibly multi-dimensional) initializer is zero
	 */
	boolean isZero();
}
