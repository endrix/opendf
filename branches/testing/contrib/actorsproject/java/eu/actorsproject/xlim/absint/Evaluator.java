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
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimStateCarrier;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueOperator;
import eu.actorsproject.xlim.util.OperationHandler;
import eu.actorsproject.xlim.util.OperationPlugIn;

/**
 * Evaluator evaluates XlimOperators in some abstract domain.
 * It provides functionality, particular to the instruction set (XlimOperators),
 * but independent of abstract domain.
 */
public class Evaluator {

	private OperationPlugIn<Handler> mAbstractOperators;
		
	public Evaluator() {
		mAbstractOperators=new OperationPlugIn<Handler>(new DefaultHandler());
		registerDefaultHandlers();
	}
	
	private void registerDefaultHandlers() {
		register("pinRead", new PinReadHandler(true /* modifies port */));
		register("pinPeek", new PinReadHandler(false /* doesn't */));
		register("pinWrite", new PinWriteHandler());
		register("pinStatus", new PinStatusHandler());
		register("assign", new AssignHandler());
		register("var_ref", new VarRefHandler());
		register("$literal_Integer", new LiteralHandler());
		register("$add", new AddHandler());
		Handler andHandler=new AndHandler();
		register("$and", andHandler);
		register("bitand", andHandler);
		register("$div", new DivHandler());
		register("$mul", new MulHandler());
		Handler orHandler=new OrHandler();
		register("$or", orHandler);
		register("bitor", orHandler);
		register("$sub", new SubHandler());
		register("bitxor", new XorHandler());
		register("lshift", new LShiftHandler());
		register("rshift", new RShiftHandler());
		register("$eq", new EqHandler());
		register("$ge", new GeHandler());
		register("$gt", new GtHandler());
		register("$le", new LeHandler());
		register("$lt", new LtHandler());
		register("$ne", new NeHandler());
		register("$negate", new NegHandler());
		register("$not", new LogicalNotHandler());
		register("bitnot", new BitNotHandler());
		register("urshift", new URShiftHandler());
		Handler copyAndCast=new CopyAndCastHandler();
		register("noop", copyAndCast);
		register("cast", copyAndCast);
		register("$selector", new SelectorHandler());
		register("pinAvail", new PinAvailHandler());
	}
	
	/**
	 * Evaluates 'op' in 'context' for abstract values of 'domain'
	 * 
	 * @param op        XlimOperation
	 * @param context   a mapping from value nodes to abstract values
	 * @param domain    an abstract domain
	 * @return          true iff the context was updated
	 */
	public <T extends AbstractValue<T>> boolean evaluate(XlimOperation op,
                                                         Context<T> context,
                                                         AbstractDomain<T> domain) {
		Handler handler=mAbstractOperators.getOperationHandler(op);
		return handler.evaluate(op,context,domain);
	}	
	
	protected void register(String opKind, Handler handler) {
		mAbstractOperators.registerHandler(opKind, handler);
	}
	
	/*
	 * Handy support routines
	 */
	
	protected ValueNode findValueNode(XlimStateCarrier carrier,
			                          Iterable<? extends ValueNode> valueNodes) {
		for (ValueNode value: valueNodes) {
			if (value.getStateCarrier()==carrier)
				return value;
		}
		throw new IllegalArgumentException("No such state access: "+carrier.getSourceName());
	}
	
	
	protected interface Handler extends OperationHandler {
		
		public <T extends AbstractValue<T>> boolean evaluate(XlimOperation op,
                                                             Context<T> context,
                                                             AbstractDomain<T> domain);
	}
	
	/**
	 * DefaultHandler sets all outputs to "universe" (all possible values of type)
	 */
	protected class DefaultHandler implements Handler {
		
		@Override
		public boolean supports(XlimOperation op) {
			return true;
		}
		
		@Override
		public <T extends AbstractValue<T>> boolean evaluate(XlimOperation op,
                                                             Context<T> context,
                                                             AbstractDomain<T> domain) {
			boolean changed=false;
			ValueOperator valueOp=op.getValueOperator();
			
			for (ValueNode outputValue: valueOp.getOutputValues()) {
				XlimType type=outputValue.getCommonElementType();
				T universe=domain.getUniverse(type);
				if (context.put(outputValue, universe))
					changed=true;
			}
			return changed;
		}
	}
	
	protected abstract class ScalarHandler implements Handler {
		
		@Override
		public boolean supports(XlimOperation op) {
			return true;
		}
		
		@Override
		public <T extends AbstractValue<T>> boolean evaluate(XlimOperation op,
                                                             Context<T> context,
                                                             AbstractDomain<T> domain) {
			AbstractValue<T> aValue=evaluateScalar(op,context,domain);
			XlimOutputPort out=op.getOutputPort(0);
			ValueNode node=out.getValue();
			XlimType type=out.getType();
			
			//	if evaluateScalar returned null, this means "top" (no information)
			T result=(aValue!=null)? domain.restrict(aValue.getAbstractValue(),type) : null;
			
			return context.put(node, result);
		}
		
		protected abstract <T extends AbstractValue<T>> 
		    AbstractValue<T> evaluateScalar(XlimOperation op,
                                            Context<T> context,
                                            AbstractDomain<T> domain);  
	}
	
	protected abstract class UnaryHandler extends ScalarHandler {
		@Override
		protected<T extends AbstractValue<T>> 
		    AbstractValue<T> evaluateScalar(XlimOperation op,
                                            Context<T> context,
                                            AbstractDomain<T> domain) {
			ValueNode node=op.getInputPort(0).getValue();
			T aValue=context.get(node);
			
			if (aValue!=null)
				return evaluate(aValue);
			else {
				// if null, that value must have been explicitly set
				// otherwise, there is a problem with the evaluation order
				assert(context.hasValue(node));
				return null;
			}
		}
		
		protected abstract<T extends AbstractValue<T>> AbstractValue<T> evaluate(T aValue);
	}
	
	protected abstract class BinaryHandler extends ScalarHandler {
		@Override
		protected<T extends AbstractValue<T>> 
		    AbstractValue<T> evaluateScalar(XlimOperation op,
                                            Context<T> context,
                                            AbstractDomain<T> domain) {
			ValueNode node1=op.getInputPort(0).getValue();
			T aValue1=context.get(node1);
			ValueNode node2=op.getInputPort(1).getValue();
			T aValue2=context.get(node2);
			
			if (aValue1!=null && aValue2!=null) 
				return evaluate(aValue1,aValue2);
			else {
				// if null, that value must have been explicitly set
				// otherwise, there is a problem with the evaluation order
				assert(context.hasValue(node1) && context.hasValue(node2));
				return null;
			}
		}
		
		protected abstract<T extends AbstractValue<T>> AbstractValue<T> evaluate(T aValue1, T aValue2);
	}
	
	/**
	 * Implements the zero-padding that is stipulated for the shorter
	 * operand of bitand, bitor, bitxor
	 */
	protected abstract class BitwiseHandler extends BinaryHandler {
		
		@Override
		protected<T extends AbstractValue<T>> 
		    AbstractValue<T> evaluateScalar(XlimOperation op,
                                            Context<T> context,
                                            AbstractDomain<T> domain) {
			XlimInputPort in1=op.getInputPort(0);
			int width1=in1.getSource().getSourceType().getSize();
			T aValue1=context.get(in1.getValue());
			
			XlimInputPort in2=op.getInputPort(1);
			int width2=in1.getSource().getSourceType().getSize();
			T aValue2=context.get(in2.getValue());
			
			if (aValue1!=null && aValue2!=null) {
				// zero extend inputs, if necessary
				if (width1<width2)
					aValue1=aValue1.zeroExtend(width1).getAbstractValue();
				else if (width2<width1)
					aValue2=aValue2.zeroExtend(width2).getAbstractValue();
			
				return evaluate(aValue1,aValue2);
			}
			else {
				// if null, that value must have been explicitly set
				// otherwise, there is a problem with the evaluation order
				assert(context.hasValue(in1.getValue()) 
					   && context.hasValue(in2.getValue()));
				return null;
			}
		}
	}
	
	protected class LiteralHandler extends ScalarHandler {
	
		@Override
		protected<T extends AbstractValue<T>> T evaluateScalar(XlimOperation op,
                                      Context<T> context,
                                      AbstractDomain<T> domain) {
			XlimType type=op.getOutputPort(0).getType();
			return domain.getAbstractValue(op.getValueAttribute(), type);
		}
	}
	
	protected class AddHandler extends BinaryHandler {
		@Override
		protected <T extends AbstractValue<T>> AbstractValue<T> evaluate(T x, T y)	{
			return x.add(y);
		}
	}
	
	protected class SubHandler extends BinaryHandler {
		@Override
		protected <T extends AbstractValue<T>> AbstractValue<T> evaluate(T x, T y)	{
			return x.subtract(y);
		}	
	}
	
	protected class MulHandler extends BinaryHandler {
		@Override
		protected <T extends AbstractValue<T>> AbstractValue<T> evaluate(T x, T y)	{
			return x.multiply(y);
		}	
	}
	
	protected class DivHandler extends BinaryHandler {
		@Override
		protected <T extends AbstractValue<T>> AbstractValue<T> evaluate(T x, T y)	{
			return x.divide(y);
		}	
	}
	
	protected class AndHandler extends BitwiseHandler {
		@Override
		protected <T extends AbstractValue<T>> AbstractValue<T> evaluate(T x, T y)	{
			return x.and(y);
		}	
	}
	
	protected class OrHandler extends BitwiseHandler {
		@Override
		protected <T extends AbstractValue<T>> AbstractValue<T> evaluate(T x, T y)	{
			return x.or(y);
		}	
	}
	
	protected class XorHandler extends BitwiseHandler {
		@Override
		protected <T extends AbstractValue<T>> AbstractValue<T> evaluate(T x, T y)	{
			return x.xor(y);
		}	
	}
	
	protected class LShiftHandler extends BinaryHandler {
		@Override
		protected <T extends AbstractValue<T>> AbstractValue<T> evaluate(T x, T y)	{
			return x.shiftLeft(y);
		}
	}
	
	protected class RShiftHandler extends BinaryHandler {
		@Override
		protected <T extends AbstractValue<T>> AbstractValue<T> evaluate(T x, T y)	{
			return x.shiftRight(y);
		}
	}
	
	protected class URShiftHandler extends RShiftHandler {
		@Override
		protected<T extends AbstractValue<T>> 
		    AbstractValue<T> evaluateScalar(XlimOperation op,
                                            Context<T> context,
                                            AbstractDomain<T> domain) {
			XlimInputPort in1=op.getInputPort(0);
			int width1=in1.getSource().getSourceType().getSize();
			T aValue1=context.get(in1.getValue());
			T aValue2=context.get(op.getInputPort(1).getValue());
			
			// zero extend left operand
			aValue1=aValue1.zeroExtend(width1).getAbstractValue();
			return evaluate(aValue1,aValue2);
		}		
	}
	
	protected class NegHandler extends UnaryHandler {
		@Override
		protected <T extends AbstractValue<T>> AbstractValue<T> evaluate(T x)	{
			return x.negate();
		}
	}
	
	protected class BitNotHandler extends UnaryHandler {
		@Override
		protected <T extends AbstractValue<T>> AbstractValue<T> evaluate(T x)	{
			return x.not();
		}
	}
	
	protected class LogicalNotHandler extends UnaryHandler {
		@Override
		protected <T extends AbstractValue<T>> AbstractValue<T> evaluate(T x)	{
			return x.logicalComplement();
		}
	}
	
	protected class CopyAndCastHandler extends UnaryHandler {
		@Override
		protected <T extends AbstractValue<T>> AbstractValue<T> evaluate(T x)	{
			return x;
		}
	}
	
	protected class SignExtendHandler extends ScalarHandler {
		@Override
		protected  <T extends AbstractValue<T>> 
	        AbstractValue<T> evaluateScalar(XlimOperation op,
                                            Context<T> context,
                                            AbstractDomain<T> domain) {  
			int fromBit=(int)((long) op.getIntegerValueAttribute())-1;
			T x=context.get(op.getInputPort(0).getValue());
			return x.signExtend(fromBit);
		}
	}
	
	protected class EqHandler extends BinaryHandler {
		@Override
		protected <T extends AbstractValue<T>> AbstractValue<T> evaluate(T x, T y)	{
			return x.equalsOperator(y);
		}	
	}
	
	protected class NeHandler extends BinaryHandler {
		@Override
		protected <T extends AbstractValue<T>> AbstractValue<T> evaluate(T x, T y)	{
			return x.equalsOperator(y).logicalComplement();
		}	
	}

	protected class LtHandler extends BinaryHandler {
		@Override
		protected <T extends AbstractValue<T>> AbstractValue<T> evaluate(T x, T y)	{
			return x.lessThanOperator(y);
		}	
	}

	protected class GtHandler extends BinaryHandler {
		@Override
		protected <T extends AbstractValue<T>> AbstractValue<T> evaluate(T x, T y)	{
			return y.lessThanOperator(x);
		}	
	}
	
	protected class GeHandler extends BinaryHandler {
		@Override
		protected <T extends AbstractValue<T>> AbstractValue<T> evaluate(T x, T y)	{
			return x.lessThanOperator(y).logicalComplement();
		}	
	}
	
	protected class LeHandler extends BinaryHandler {
		@Override
		protected <T extends AbstractValue<T>> AbstractValue<T> evaluate(T x, T y)	{
			return y.lessThanOperator(x).logicalComplement();
		}	
	}
	
	protected class SelectorHandler extends ScalarHandler {
		@Override
		protected  <T extends AbstractValue<T>> 
        AbstractValue<T> evaluateScalar(XlimOperation op,
                                        Context<T> context,
                                        AbstractDomain<T> domain) {
			ValueNode conditionNode=op.getInputPort(0).getValue();
			T condition=context.get(conditionNode);
			assert(condition!=null || context.hasValue(conditionNode));

			XlimType type=op.getOutputPort(0).getType();
			
			// restrict the ifTrue/ifFalse expressions to the resulting type
			// before joining them
			ValueNode trueNode=op.getInputPort(1).getValue();
			T ifTrue=domain.restrict(context.get(trueNode), type);
			assert(ifTrue!=null || context.hasValue(trueNode));
			
			ValueNode falseNode=op.getInputPort(2).getValue();
			T ifFalse=domain.restrict(context.get(falseNode), type);
			assert(ifFalse!=null || context.hasValue(falseNode));

			if (condition==null || condition.mayContain(1)) {
				if (condition==null || condition.mayContain(0)) {
					// true or false
					if (ifTrue!=null && ifFalse!=null)
						return ifTrue.union(ifFalse);  
					else
						return null;  // null is "top element" (no information)
				}
				else
					return ifTrue; // True only
			}
			else
				return ifFalse;    // False only
		}
	}
	
	protected class AssignHandler implements Handler {
			
		@Override
		public boolean supports(XlimOperation op) {
			return true;
		}
		
		@Override
		public <T extends AbstractValue<T>> boolean evaluate(XlimOperation op,
                Context<T> context,
                AbstractDomain<T> domain) {
			XlimStateVar stateVar=op.getStateVarAttribute();
			ValueNode newValueNode=findValueNode(stateVar,op.getValueOperator().getOutputValues());
			XlimType elementType=stateVar.getInitValue().getCommonElementType();
			T result;
			
			if (op.getNumInputPorts()==1) {
				// Unary variant of operator is assignment of scalar variable: v:=op0
				// Value of variable becomes value of right-hand-side
				ValueNode rhsNode=op.getInputPort(0).getValue();
				result=domain.restrict(context.get(rhsNode), elementType);
				assert(result!=null || context.hasValue(rhsNode));
			}
			else {
				// Binary variant is assignment of aggregate: v[op0]:=op1
				// Value of aggregate is join(oldValue,newValue)
				// that is: the value represents any/all elements of the aggregate
				ValueNode oldValueNode=findValueNode(stateVar,op.getValueOperator().getInputValues());
				ValueNode rhsNode=op.getInputPort(1).getValue();
				T oldValue=context.get(oldValueNode);
				T rhsValue=context.get(rhsNode);
				if (oldValue!=null && rhsValue!=null) {
					rhsValue=domain.restrict(rhsValue,elementType);
					result=oldValue.union(rhsValue).getAbstractValue();
				}
				else {
					// if null, that value must have been explicitly set
					// otherwise, there is a problem with the evaluation order
					assert(context.hasValue(oldValueNode) && context.hasValue(rhsNode));
					result=null;
				}
			}
			
			return context.put(newValueNode, result);
		}
	}
	
	protected class VarRefHandler extends ScalarHandler {
		
		@Override
		protected <T extends AbstractValue<T>> AbstractValue<T> 
		evaluateScalar(XlimOperation op, Context<T> context, AbstractDomain<T> domain) {
			// Implements indexed access of aggregate: v[op0]
			// Current value of aggregate represents any/all elements
			XlimStateCarrier carrier=op.getStateVarAttribute();
			ValueNode oldValueNode=findValueNode(carrier,op.getValueOperator().getInputValues());
			return context.get(oldValueNode);
		}		
	}
	
	protected class PinReadHandler implements Handler {

		private boolean mModifiesPort;
		
		public PinReadHandler(boolean modifiesPort) {
			mModifiesPort = modifiesPort;
		}

		@Override
		public boolean supports(XlimOperation op) {
			return true;
		}
		
		@Override
		public <T extends AbstractValue<T>> boolean evaluate(XlimOperation op, 
				                                             Context<T> context, 
				                                             AbstractDomain<T> domain) {
			boolean changed=false;
			XlimStateCarrier port=op.getPortAttribute();
			ValueNode portNodeIn=findValueNode(port, op.getValueOperator().getInputValues());
			T aValue=context.get(portNodeIn);
			
			ValueNode resultNode=op.getOutputPort(0).getValue();
			
			// Here, we assume that the value of an input port represents
			// all values that can be read from the port
			XlimType type=op.getOutputPort(0).getType();
			if (context.put(resultNode, domain.restrict(aValue,type)))
				changed=true;

			if (mModifiesPort) {
				// We recycle the same value to represent the updated
				// port value (i.e. rest of the stream readable from the port)
				ValueNode portNodeOut=findValueNode(port, op.getValueOperator().getOutputValues());
				if (context.put(portNodeOut, aValue))
					changed=true;
			}
			
			return changed;
		}
	}
	
	protected class PinWriteHandler implements Handler {
		
		@Override
		public boolean supports(XlimOperation op) {
			return true;
		}
		
		@Override
		public <T extends AbstractValue<T>> boolean evaluate(XlimOperation op, 
				                                             Context<T> context, 
				                                             AbstractDomain<T> domain) {
			// Here, we assume that the value of an output port represents
			// all values that can be written to the port
			XlimTopLevelPort port=op.getPortAttribute();
			XlimType portType=port.getType();
			ValueNode valueNodeIn=findValueNode(port, op.getValueOperator().getInputValues());
			ValueNode valueNodeOut=findValueNode(port, op.getValueOperator().getOutputValues());
			T oldValue=context.get(valueNodeIn);
			T newValue=context.get(op.getInputPort(0).getValue());
			
			newValue=domain.restrict(newValue,portType);
			newValue.union(oldValue);
			return context.put(valueNodeOut, newValue);
		}
	}
	
	protected class PinStatusHandler extends ScalarHandler {
		
		@Override
		protected <T extends AbstractValue<T>> AbstractValue<T> 
		evaluateScalar(XlimOperation op, Context<T> context, AbstractDomain<T> domain) {
			// Returns a value representing "false" or "true"
			return domain.getUniverse(op.getOutputPort(0).getType());
		}
	}
	
	protected class PinAvailHandler extends ScalarHandler {
		@Override
		protected <T extends AbstractValue<T>> AbstractValue<T> 
		evaluateScalar(XlimOperation op, Context<T> context, AbstractDomain<T> domain) {
			// Returns a value representing 0..MAX_INT
			XlimType type=op.getOutputPort(0).getType();
			T maxValue=domain.getAbstractValue(Long.toString(type.maxValue()), type);
			T universe=domain.getUniverse(type);
			if (maxValue!=null && universe!=null)
				return maxValue.and(universe);
			else
				return null;
		}
	}
}
