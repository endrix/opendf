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

package eu.actorsproject.xlim.dependence;

import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.absint.AbstractDomain;
import eu.actorsproject.xlim.absint.Context;

public interface ValueOperator extends DependenceComponent {
	/**
	 * @return the ValueUsages of this operator
	 * 
	 * Differs from getInputValues() in that ValueUsages are returned,
	 * This  method is required when building the data dependence graph,
	 * since the values of stateful resources (ports and state variables) are
	 * not yet set (i.e. getInputValues() would return null ValueNodes)
	 */
	Iterable<? extends ValueUsage> getUsedValues();
	
	/**
	 * @return the ValueNodes, which are used by this operator
	 */
	Iterable<? extends ValueNode> getInputValues();
	
	/**
	 * @return the ValueNodes, which are produced by this operator
	 */
	Iterable<? extends ValueNode> getOutputValues();
	
	/**
	 * @return module, to which this operator belongs
	 */
	XlimModule getParentModule();	
	
	/**
	 * @param usage  One of the values, which is used by the operator
	 * @return       the module, to which the usage is attributed.
	 * 
	 * With the exception of PhiOperators, all usages are simply attributed
	 * to the parent module of the operator. For phi-nodes the usage is 
	 * attributed to the predecessor that corresponds to the usage
	 * (this not only models what is actually going on, but it is also required to
	 * maintain the property that a value can only be used in a context that is
	 * dominated by the definition of the value).
	 */
	XlimModule usedInModule(ValueUsage usage);
	
	/**
	 * Removes all references this operator makes to ValueNodes
	 */
	public abstract void removeReferences();

	interface Visitor<Result,Arg> {
		Result visitOperation(XlimOperation xlimOp, Arg arg);
		Result visitPhi(PhiOperator phi, Arg arg);
		Result visitTest(TestOperator test, Arg arg);
	}
	
	<Result,Arg> Result accept(Visitor<Result,Arg> evaluator, Arg arg);

	/**
	 * @param context  a mapping from value nodes to abstract values
	 * @param domain   a domain, in which XlimOperations and phi-nodes
	 *                 can be evaluated
	 * @return true iff context was updated
	 */
	@Override
	<T> boolean evaluate(Context<T> context, AbstractDomain<T> domain);
	
	/**
	 * @return textual representation of operator (for printouts)
	 */
	String getOperatorName();
	
	/**
	 * @return textual representation of attributes associated with operator 
	 *         (things other than input/output value nodes -for printouts)
	 */
	String attributesToString();
}
