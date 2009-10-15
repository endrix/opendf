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

import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimPhiNode;
import eu.actorsproject.xlim.XlimStateCarrier;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.dependence.StatePhiOperator;

public interface AbstractDomain<T> {
	
	/**
	 * @param type
	 * @return the abstract respresentation of all possible values of 'type'
	 */
	T getUniverse(XlimType type);
	
	/**
	 * @param constant A constant value (as String)
	 * @param type     A type
	 * @return         The abstraction of the constant in this domain
	 */
	T getAbstractValue(String constant, XlimType type);
		
	/**
	 * @param aValue   An abstract value of the domain
	 * @param type     a type
	 * @return         the restriction of 'aValue' to 'type'
	 *                 (e.g. a narrower integer type)
	 *                 
	 * This method is used to ensure that the output of an operation/phi-node
	 * is restricted to the type of its output port
	 */
	T restrict(T aValue, XlimType type);
	
	/**
	 * Evaluates an XLIM operation in the given context
	 * 
	 * @param operation
	 * @param context    a mapping from value nodes to abstract values
	 * @return true iff the context was updated
	 */
	boolean evaluate(XlimOperation operation, Context<T> context);
	
	/**
	 * Evaluates a phi-operator in the given context
	 * 
	 * @param phi        a phi-operator
	 * @param context    a mapping from value nodes to abstract values
	 * @return true iff the context was updated
	 */
	boolean evaluate(StatePhiOperator phi, Context<T> context);
	
	/**
	 * Evaluates a phi-operator in the given context
	 * 
	 * @param phi        a phi-operator
	 * @param context    a mapping from value nodes to abstract values
	 * @return true iff the context was updated
	 */
	boolean evaluate(XlimPhiNode phi, Context<T> context);
		
	/**
	 * @param carrier  a state variable or an actor port
	 * @return         the abstraction of the initial value of 'carrier'
	 */
	T initialState(XlimStateCarrier carrier);
}
