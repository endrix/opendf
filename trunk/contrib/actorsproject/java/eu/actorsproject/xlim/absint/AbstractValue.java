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

package eu.actorsproject.xlim.absint;

import eu.actorsproject.util.XmlElement;

/**
 * AbstractValue is the requirements placed on an abstract value by OperationEvaluator
 * -the primitives needed to do abstract interpretation of CAL operations.
 * @param <T> is intended to be the abstract value itself
 */
public interface AbstractValue<T> extends XmlElement {

	/**
	 * @return the actual abstract value (i.e. an instance of T)
	 * 
	 * This allows the abstract methods to be declared as taking T arguments
	 * (rather than AbstractValue<T> which would require any other implementation
	 * to be handled as well). Note that there is no similar technicality associated
	 * with return values, since an overridden method may have a *more* specific
	 * return type (e.g. T in this case -provided T implements AbstractValue<T>).
	 */
	T getAbstractValue();
	
	/**
	 * @param type
	 * @return the abstract value that represents all values of the given type
	 */
	// AbstractValue<T> getUniverse(XlimType type);
	
	/**
	 * @param constant
	 * @param type     the type of the constant
	 * @return the abstract value that represents the constant
	 */
	// AbstractValue<T> getAbstractValue(String constant, XlimType type);	

	/**
	 * @param constant
	 * @return true if the abstract value may represent ("contain") the constant
	 */
	boolean mayContain(long constant);

	/**
	 * @param obj
	 * @return true if this abstract value is equal to obj
	 * (Context<T> relies on a proper implementation of equals)
	 */
	boolean equals(Object obj);
	
	/**
	 * @return hash code of this abstract value
	 * (Context<T> relies on a proper implementation of hashCode)
	 */
	int hashCode();
	
	/**
	 * @param aValue
	 * @return the union of this abstract value and aValue
	 * union() is used as confluence operator at joining flow paths,
	 * that is the result of two joining values is aValue1.union(aValue2)
	 */
	AbstractValue<T> union(T aValue);
	
	/**
	 * @param aValue
	 * @return the intersection of this abstract value and aValue
	 */
	AbstractValue<T> intersect(T aValue);
	
	/**
	 * @return true if the abstract value is "empty" 
	 *         (corresponds to no concrete values)
	 */
	boolean isEmpty();
	
	/*
	 * Effect of arithmetic operators:
	 */
	
	AbstractValue<T> add(T aValue);
	AbstractValue<T> subtract(T aValue);
	AbstractValue<T> multiply(T aValue);
	AbstractValue<T> divide(T aValue);
	AbstractValue<T> shiftLeft(T aValue);
	AbstractValue<T> shiftRight(T aValue);
	AbstractValue<T> and(T aValue);
	AbstractValue<T> or(T aValue);
	AbstractValue<T> xor(T aValue);
	AbstractValue<T> equalsOperator(T aValue);
	AbstractValue<T> lessThanOperator(T aValue);
	
	AbstractValue<T> negate();
	AbstractValue<T> not();
	AbstractValue<T> logicalComplement();
	AbstractValue<T> signExtend(int fromBit);
	AbstractValue<T> zeroExtend(int fromWidth);
}
