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

import java.util.List;

import eu.actorsproject.util.XmlPrinter;
import eu.actorsproject.xlim.XlimInitValue;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimPhiNode;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.dependence.PhiOperator;
import eu.actorsproject.xlim.dependence.SideEffectPhiOperator;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.StateLocation;

/**
 * Intended as a base class of abstract domains: factors out reasonable
 * default behavior:
 *   1) XlimOperators are evaluated using an evaluator plug-in
 *   2) PhiOperators are evaluated using the join() method of AbstractValue<T>
 *   3) Initial state of state variables is created by traversing the initializers,
 *      using the abstract method getAbstractValue() and, for aggregates, the join() method of
 *      AbstractValue<T> to summarize all element values in a single abstract value.
 *   4) Initial state of actor ports is null (not subject to abstract interpretation)
 *   
 * Abstract methods getUniverse and getAbstractValue need to be implemented.
 */
public abstract class GenericDomain<T extends AbstractValue<T>> implements AbstractDomain<T> {

	private Evaluator mEvaluatorPlugIn;
	private boolean mTrace=false;
	private static XmlPrinter sTracePrinter=new XmlPrinter(System.out);
	
	public GenericDomain(Evaluator evaluator) {
		mEvaluatorPlugIn=evaluator;
	}
	
	public void setTrace(boolean tracingOn) {
		mTrace=tracingOn;
	}
	
	@Override
	public abstract T getUniverse(XlimType type);
	
	@Override
	public abstract T getAbstractValue(String constant, XlimType type);
	
	@Override
	public T restrict(T aValue, XlimType type) {
		// TODO: find a nice way of plugging in new types
		
		// Get the scalar element type of Lists
		while (type.isList()) {
			type = type.getTypeParameter("type");
		}
		
		if (aValue==null)
			return getUniverse(type);
		else if (type.isInteger())
			if (type.minValue()<0)
				return aValue.signExtend(type.getSize()-1).getAbstractValue();
			else
				return aValue.zeroExtend(type.getSize()).getAbstractValue();
		else
			return aValue;
	}
	
	@Override
	public boolean evaluate(XlimOperation op, Context<T> context) {
		boolean changed=mEvaluatorPlugIn.evaluate(op,context,this);
		
		if (mTrace) {
			sTracePrinter.printElement(op);
			for (XlimOutputPort output: op.getOutputPorts()) {
				String id=output.getUniqueId();
				T aValue=context.get(output.getValue());
				
				sTracePrinter.println("<!-- Abstract value of "+ id +" -->");
				printValue(aValue);
			}
		}
		
		return changed;
	}
	
	private void printValue(T aValue) {
		if (aValue!=null)
			sTracePrinter.printElement(aValue);
		else
			sTracePrinter.println("<top/>  <!-- null -->");
	}
	
	@Override
	public boolean evaluate(SideEffectPhiOperator phi, Context<T> context) {
		T aValue=evaluatePhi(phi,context);
		
		if (mTrace) {
			sTracePrinter.printElement(phi);
			sTracePrinter.println("<!-- Abstract value of "+phi.getOutput().getUniqueId()+" -->");
			printValue(aValue);
		}
		
		return context.put(phi.getOutput(), aValue);
	}

	@Override
	public boolean evaluate(XlimPhiNode phi, Context<T> context) {
		PhiOperator phiOperator=phi.getValueOperator();
		T aValue=evaluatePhi(phiOperator,context);
		
		// restrict the result to the output type
		if (aValue!=null) {
			XlimType type=phi.getOutputPort(0).getType();
			aValue=restrict(aValue,type);
		}
		
		if (mTrace) {
			sTracePrinter.printElement(phi);
			sTracePrinter.println("<!-- Abstract value of "+phi.getOutputPort(0).getUniqueId()+" -->");
			printValue(aValue);
		}
		
		return context.put(phiOperator.getOutput(), aValue);
	}

	protected T evaluatePhi(PhiOperator phi, Context<T> context) {
		ValueNode node1=phi.getInputValue(0);
		T aValue1=context.get(node1);
		ValueNode node2=phi.getInputValue(1);
		T aValue2=context.get(node2);
		ValueNode conditionNode=phi.getControlDependence();
		T condition=context.get(conditionNode);
		T result;

		// Predecessors should have been evaluated
		if (aValue1==null)
			assert(context.hasValue(node1));
		
		if (aValue2==null && context.hasValue(node2)==false
			|| condition==null && context.hasValue(conditionNode)==false) {
			// Values that propagate back from a loop body
			// haven't necessarily been evaluated yet
			assert(phi.inLoop());
			result=aValue1;
		}
		else if (condition==null || condition.mayContain(1)) {
			if (condition==null || condition.mayContain(0)) {
				// either true or false
				if (aValue1!=null && aValue2!=null)
					result=aValue1.union(aValue2).getAbstractValue();
				else
					result=null; // join(x,null)=null "top element"
			}
			else {
				// condition true only
				result=aValue1;
			}
		}
		else {
			// Condition false only
			result=aValue2;
		}
		
		return result;
	}

	@Override
	public T initialState(StateLocation carrier) {
		XlimStateVar stateVar=carrier.asStateVar();
		if (stateVar!=null)
			return initialState(stateVar);
		else {
			XlimTopLevelPort port=carrier.asActorPort();
			assert(port!=null);
			return initialState(port);
		}
	}
	
	/**
	 * @param stateVar  a state variable
	 * @return the initial abstract value of 'stateVar'
	 */
	protected T initialState(XlimStateVar stateVar) {
		return getAbstractValue(stateVar.getInitValue());
	}
	
	protected T getAbstractValue(XlimInitValue initValue) {
		XlimType scalarType=initValue.getScalarType();
		if (scalarType!=null)
			return getAbstractValue(initValue.getScalarValue(), scalarType);
		else {
			List<? extends XlimInitValue> children=initValue.getChildren();
			AbstractValue<T> result=getAbstractValue(children.get(0));
			
			if (initValue.isZero()==false) {
				for (int i=1; i<children.size() && result!=null; ++i)
					result=result.union(getAbstractValue(children.get(i)));
			}
			// else: all elements the same as first one
			return result.getAbstractValue();
		}
	}

	/**
	 * @param port  an actor port
	 * @return the initial abstract value of 'port'
	 * 
	 * This implementation returns null to indicate that no attempt is
	 * made to interpret the port values.
	 */
	protected T initialState(XlimTopLevelPort port) {
		return null;
	}
}
