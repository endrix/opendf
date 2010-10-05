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

import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimType;

/**
 * A TypeRule models the use of types in XLIM operations for the purposes of
 * matching polymorphic operations and performing type checking
 */
public abstract class TypeRule {

	protected Signature mSignature;
	
	/**
	 * @param signature          signature (input type patterns), 
	 *                           null for null-ary operations
	 * @param defaultNumOutputs  Default number of outputs
	 */
	public TypeRule(Signature signature) {
		mSignature=signature;
	}
	
	/**
	 * @param inputs
	 * @return true if this TypeRule is applicable to the given inputs
	 */
	public boolean matches(List<? extends XlimSource> inputs) {
		if (mSignature!=null)
			return mSignature.matches(inputs);
		else
			return inputs.isEmpty();
	}
	
	/**
	 * @param rule
	 * @param inputs
	 * @return true if this TypeRule provides a strictly "better" match than 'rule'
	 *    
	 * Here "better" means this TypeRule matches exactly, whenever 'rule' does and
	 * matches exactly for at least one input, when 'r' does not.
	 */
	public boolean betterMatch(TypeRule rule, List<? extends XlimSource> inputs) {
		if (mSignature!=null)
			return mSignature.betterMatch(rule.mSignature, inputs);
		else
			return false;
	}
	
	/**
	 * @param outputs
	 * @return true if this type rule may produce the given outputs
	 * 
	 * Particularly for literals (which have no inputs) the output type is 
	 * needed to resolve the operation.
	 */
	public abstract boolean matchesOutputs(List<? extends XlimOutputPort> outputs);
		
	/**
	 * @return the default number of outputs of the OperationKind
	 */
	public abstract int defaultNumberOfOutputs();
	
	
	/**
	 * @param inputs
	 * @param i
	 * @return default type of output 'i' (or null if not possible to deduce using
	 *         inputs only).
	 */
	public abstract XlimType defaultOutputType(List<? extends XlimSource> inputs,
			                                   int i);
	
	/**
	 * @param op
	 * @param i
	 * @return Actual type of output 'i' (0,1,2,...) given a completly
	 *         created operation 'op'.
	 *         
	 * For integer operations, the actual type may differ from the declared type
	 * of the output (in which case an implicit type conversion is made). 
	 * Example:
	 * integer(size=16)*integer(size=16)->integer(size=32) (=actual type),
	 * but declared type of output is perhaps integer(size=16).
	 */
	public abstract XlimType actualOutputType(XlimOperation op, int i);
	
	/**
	 * @param fromType  type of actual argument
	 * @param argIndex  index of argument 0,1,2,...
	 * @return          fromType promoted according to the (given argument of the) signature
	 */
	protected XlimType promotedInputType(XlimType fromType, int argIndex) {
		TypePattern pat=mSignature.getPattern(argIndex);
		if (pat.match(fromType)==TypePattern.Match.DoesNotMatch)
			return null;
		
		TypeKind toKind=pat.patternTypeKind();
		if (toKind==null) {
			// No pattern TypeKind means wildcard
			return fromType;
		}
		else {
			return toKind.promote(fromType);
		}
	}
	
	/**
	 * Completes typechecking when a required attribute (e.g. state variable 
	 * or actor port) has been set.
	 * 
	 * @param op
	 * @return true if operation typechecks (assuming TypeRule has matched)
	 */
	public boolean typecheck(XlimOperation op) {
		// By default we assume that matching input/outputs is sufficient
		return true;
	}
	
	protected String outputToString() {
		return "?";
	}
	
	@Override
	public String toString() {
		if (mSignature!=null)
			return mSignature.toString() + " -> " + outputToString();
		else
			return "() -> " + outputToString();
	}
}
