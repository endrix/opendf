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

import eu.actorsproject.xlim.XlimInputPort;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.dependence.PhiOperator;
import eu.actorsproject.xlim.dependence.TestOperator;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueOperator;
import eu.actorsproject.xlim.util.OperationHandler;
import eu.actorsproject.xlim.util.OperationPlugIn;

/**
 * Evaluates constraints bottom-up
 */
public abstract class ConstraintEvaluator<T extends AbstractValue<T>> {

	private OperationPlugIn<ConstraintHandler> mConstraintHandlers;
	private ValueOperatorVisitor mVisitor = new ValueOperatorVisitor();
	
	public ConstraintEvaluator() {
		ConstraintHandler defaultHandler=new ConstraintHandler();
		mConstraintHandlers=new OperationPlugIn<ConstraintHandler>(defaultHandler);
		register("$and", new AndHandler());
		register("$or", new OrHandler());
		register("$not", new NotHandler());
		register("noop", new NoopHandler());
	}

	/**
	 * @param assertedValue  a concrete boolean value
	 * @return               corresponding abstract value
	 */
	public abstract T getAbstractValue(boolean assertedValue);
	
	/**
	 * @param node           a value node
	 * @param assertedValue  asserted  value of 'node'
	 * @param bag            bag of constraints
	 * @return               true if the constraint of 'node' to 'assertedValue'
	 *                       was successful, false if it lead to a contradiction
	 */
	public boolean evaluate(ValueNode node,
                            T assertedValue,
                            BagOfConstraints<T> bag) {
		ValueOperator def=node.getDefinition();
		XlimOperation xlimOp=def.accept(mVisitor,null);
		if (xlimOp!=null) {
			ConstraintHandler handler=mConstraintHandlers.getOperationHandler(xlimOp);
			return handler.evaluate(xlimOp, node, assertedValue, bag);
		}
		else
			return true; // do nothing...
	}

	protected void register(String opKind, ConstraintHandler handler) {
		mConstraintHandlers.registerHandler(opKind, handler);
	}
	
	
	private class ValueOperatorVisitor 
		implements ValueOperator.Visitor<XlimOperation, Object> {

		@Override
		public XlimOperation visitOperation(XlimOperation xlimOp, 
				                            Object dummy) {
			return xlimOp;
		}

		@Override
		public XlimOperation visitPhi(PhiOperator phi, 
				                      Object dummy) {
			return null;
		}

		@Override
		public XlimOperation visitTest(TestOperator test, 
				                       Object dummy) {
			// tests don't produce any values and shouldn't be reached
			throw new UnsupportedOperationException();
		}		
	}
	
	/*
	 * Handlers, which create the constraints
	 */
	
	protected class ConstraintHandler implements OperationHandler {

		@Override
		public boolean supports(XlimOperation xlimOp) {
			return true;
		}
		
		/**
		 * @param xlimOp  an XLIM operation
		 * @param node    one of the outputs of 'xlimOp'
		 * @param aValue  asserted value of 'node'
		 * @param bag     collection of constraints
		 * @return true if 'node' can be constrained to 'aValue'
		 *         false if bag of constraints inconsistent is inconsistent
		 *         (no value possible for 'node'). 
		 */
		public boolean evaluate(XlimOperation xlimOp,
				                ValueNode node,
				                T aValue,
				                BagOfConstraints<T> bag) {
			// Default handler does nothing 
			return true;
		}
	}

	protected class NoopHandler extends ConstraintHandler {

		@Override
		public boolean supports(XlimOperation xlimOp) {
			XlimInputPort input=xlimOp.getInputPort(0);
			XlimType inputT=input.getSource().getSourceType();
			XlimType outputT=xlimOp.getOutputPort(0).getType();
			
			// Supports "pure" copies and sign extension, 
			// but neither truncation nor other type conversion 
			return inputT==outputT 
			       || inputT.isInteger() && outputT.isInteger() 
			          && inputT.getSize()<=outputT.getSize();
		}
		
		@Override
		public boolean evaluate(XlimOperation xlimOp, 
				                ValueNode node, 
				                T aValue, 
				                BagOfConstraints<T> bag) {
			ValueNode inputNode=xlimOp.getInputPort(0).getValue();
			return bag.putConstraint(inputNode, aValue);
		}
	}
	
	protected class AndHandler extends ConstraintHandler {

		@Override
		public boolean evaluate(XlimOperation xlimOp, 
				                ValueNode node, 
				                T aValue, 
				                BagOfConstraints<T> bag) {
			if (aValue.mayContain(0)==false) {
				// t1 and t2 and ... = true => t1=true, t2=true, ...
				for (XlimInputPort input: xlimOp.getInputPorts()) {
					ValueNode inputNode=input.getValue();
					if (bag.putConstraint(inputNode,aValue)==false)
						return false;
				}
			}
			return true;
		}
	}

	protected class OrHandler extends ConstraintHandler {

		@Override
		public boolean evaluate(XlimOperation xlimOp, 
				                ValueNode node, 
				                T aValue, 
				                BagOfConstraints<T> bag) {
			if (aValue.mayContain(1)==false) {
				// t1 or t2 = false => t1=false, t2=false
				for (XlimInputPort input: xlimOp.getInputPorts()) {
					ValueNode inputNode=input.getValue();
					if (bag.putConstraint(inputNode,aValue)==false)
						return false;
				}
			}
			return true;
		}
	}

	protected class NotHandler extends ConstraintHandler {

		@Override
		public boolean evaluate(XlimOperation xlimOp, 
				                ValueNode node, 
				                T aValue, 
				                BagOfConstraints<T> bag) {
			ValueNode inputNode=xlimOp.getInputPort(0).getValue();
			aValue=aValue.logicalComplement().getAbstractValue();
			return bag.putConstraint(inputNode, aValue);
		}
	}	
}
