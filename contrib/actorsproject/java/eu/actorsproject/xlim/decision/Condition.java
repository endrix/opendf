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

package eu.actorsproject.xlim.decision;

import java.util.Map;

import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimTestModule;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.absint.AbstractValue;
import eu.actorsproject.xlim.absint.Context;
import eu.actorsproject.xlim.dependence.ValueNode;

/**
 * A Condition, which is associated with a DecisionNode.
 * Two subclasses represent relevant special cases:
 * AtomicTest   a test that consists of a single CNF term
 * Conjunction  a conjunction: c1 && c2 && ... && cn of AtomicTests
 *              (all of which are asserted on the "true" branch of a decision node)
 */
/**
 * @author ecarvon
 *
 */
/**
 * @author ecarvon
 *
 */
public abstract class Condition implements XmlElement {

	private XlimSource mSource;
	private ValueNode mValue;
	
	public Condition(XlimSource source, ValueNode value) {
		mSource=source;
		mValue=value;
	}
	
	public XlimSource getXlimSource() {
		return mSource;
	}
	
	public ValueNode getValue() {
		return mValue;
	}
	
	public void addTo(Conjunction conjunction) {
		conjunction.add(this);
	}

	/**
	 * @return true iff this condition is the constant 'true'
	 */
	protected abstract boolean alwaysTrue(); 
		
	public enum Evaluation {
		ALWAYS_FALSE,
		ALWAYS_TRUE,
		TRUE_OR_FALSE
	};
	
	/**
	 * @param context       a mapping from value nodes to abstract values
	 * @param optSignature  an optional port signature (may be null). If given it
	 *                      expresses a token-availability assertion .
	 * @return ALWAYS_TRUE if this condition is 'true' given context/token availability
	 *         ALWAYS_FALSE if this condition is 'false' given context/token availability
	 *         TRUE_OR_FALSE if the outcome cannot be deduced
	 */
	public <T extends AbstractValue<T>> Evaluation evaluate(Context<T> context,
			                                                PortSignature optSignature) {
		T aValue=context.get(mValue);
		if (aValue!=null) {
			if (aValue.mayContain(1)) {
				if (aValue.mayContain(0)==false)
					return Evaluation.ALWAYS_TRUE;
			}
			else
				return Evaluation.ALWAYS_FALSE;
		}
		
		if (optSignature!=null && isImpliedBy(optSignature))
			return Evaluation.ALWAYS_TRUE;
		
		return Evaluation.TRUE_OR_FALSE;
	}
	
	/**
	 * @param optSignature  asserted token availability (may be null)
	 * 
	 * @return true if the condition contains a AvailabilityTest
	 *         that is not implied by the asserted token availability
	 */
	
	public boolean dependsOnInput(PortSignature optSignature) {
		return false;
	}
	
	/**
	 * @param avaliableTokens  asserted token availability
	 * @return true if the condition is true given the asserted token availability
	 */
	public boolean isImpliedBy(PortSignature availableTokens) {
		// In the general case, only the (true) condition is implied
		return alwaysTrue();
	}
	
	
	/**
	 * Creates a port-to-pinAvail look-up by traversing AvailabilityTests.
	 * An action scheduler must have a unique evaluation of pinAvail for each port
	 * -not to take incorrect scheduling decisions for timing-dependent actors.
	 * The look-up helps maintaining this property when creating new tests.
	 * 
	 * @param pinAvailMap  port-to-pinAvail look-up
	 */
	protected abstract void findPinAvail(Map<XlimTopLevelPort,XlimOperation> pinAvailMap);
	
	/**
	 * Adds the port rate of TokenAvailabilityTests in the condition.
	 * 
	 * @param testsInAncestors  TokenAvailabilityTests made in ancestors
	 */
	protected abstract void updateDominatingTests(Map<XlimTopLevelPort,Integer> testsInAncestors);

	/**
	 * Removes tests that are redundant (given the port signature) from a Conjunction
	 * 
	 * @param portSignature  token availability that has been successfully established
	 * @param testModule     TestModule of the condition (patch point)
	 * 
	 * @return simplified condition
	 */
	protected abstract Condition removeRedundantTests(PortSignature portSignature, 
			                                          XlimTestModule testModule);
}
