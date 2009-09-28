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

import eu.actorsproject.xlim.XlimInitValue;
import eu.actorsproject.xlim.XlimInputPort;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimStateCarrier;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.dependence.PhiOperator;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueOperator;

public class GenericDomain<T extends AbstractValue<T>> 
             extends OperationEvaluator<T>
             implements AbstractDomain<T> {

	private T mNullValue;
	private WideningOperator<T> mWideningOperator;

	/**
	 * @param nullValue the abstract value that represents "no information"
	 *        (used for initialization and to get a first value, from which new ones can be created) 
	 *        The nullValue must have the property: union(x,nullValue) == x, for all abstract values x.
	 */
	public GenericDomain(T nullValue) {
		super(null /* no default handler */);
		mNullValue=nullValue;
		registerDefaultHandlers();
	}

	/**
	 * @param nullValue the abstract value that represents "no information"
	 *        (used for initialization and to get a first value, from which new ones can be created) 
	 *        The nullValue must have the property: union(x,nullValue) == x, for all abstract values x.
	 * @param defaultHandler, the handler to use if no other handler matches an operation 
	 *        (an exception is otherwise thrown if/when this happens)
	 */
	public GenericDomain(T nullValue, Handler defaultHandler) {
		super(defaultHandler);
		mNullValue=nullValue;
		registerDefaultHandlers();
	}
	
	protected void registerDefaultHandlers() {
		Handler pinRead=new PinReadHandler();
		register("pinRead", pinRead);
		register("pinPeek", pinRead);
		register("pinWrite", new PinWriteHandler());
		register("pinStatus", new PinStatusHandler());
		register("assign", new AssignHandler());
		register("var_ref", new VarRefHandler());
		register("$literal_Integer", new LiteralIntegerHandler());
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
		Handler notHandler=new NotHandler();
		register("$not", notHandler);
		register("bitnot", notHandler);
		register("urshift", new URShiftHandler());
		Handler copyAndCast=new CopyAndCastHandler();
		register("noop", copyAndCast);
		register("cast", copyAndCast);
		register("$selector", new SelectorHandler());
		register("pinAvail", new PinAvailHandler());
	}

	/**
	 * Sets the widening operator, which is applied to abstract values
	 * that are the result of evaulation of phi-operators of loops.
	 * @param w Widening operator (null signifies no widening)
	 */
	public void setWideningOperator(WideningOperator<T> w) {
		mWideningOperator=w;
	}
	
	/*
	 * Implementation of AbstractDomain<T>
	 */
	
	@Override
	public T getNullValue() {
		return mNullValue;
	}


	@Override
	public T join(T in1, T in2) {
		return in1.union(in2);
	}
	
	@Override
	public T initialState(XlimStateCarrier carrier) {
		XlimStateVar stateVar=carrier.isStateVar();
		if (stateVar!=null) {
			return initialValue(stateVar.getInitValue());
		}
		else {
			// All possible values of the port type
			return getUniverse(carrier.isPort().getType());
		}
	}
	
	private T initialValue(XlimInitValue initValue) {
		if (initValue.getScalarType()!=null)
			return getAbstractValue(initValue.getScalarValue());
		else {
			T result=getNullValue();
			for (XlimInitValue element: initValue.getChildren()) {
				result=result.union(initialValue(element));
			}
			return result;
		}
	}
	
	@Override
	public T evaluate(PhiOperator phi, Context<T> context) {
		T condition=context.demand(phi.getControlDependence());
		if (phi.inLoop()) {
			T result;
			if (mayBeTrue(condition)==false) {
				// Use only the initial definition
				result=context.demand(phi.getInputValue(0));
			}
			else {
				// use both paths
				result=join(context.demand(phi.getInputValue(0)), 
						    context.demand(phi.getInputValue(1)));
			}
			return widen(result, phi);
		}
		else if (mayBeTrue(condition)==false) {
			// Use only "else" path
			return context.demand(phi.getInputValue(1));
		}
		else if (mayBeFalse(condition)==false) {
			// Use only "then" path
			return context.demand(phi.getInputValue(0));
		}
		else {
			return join(context.demand(phi.getInputValue(0)), 
				        context.demand(phi.getInputValue(1)));
		}
	}
	
	private T widen(T abstractValue, PhiOperator phi) {
		if (mWideningOperator!=null) {
			System.out.println("GenericDomain: widening "+phi+" w("+abstractValue+") = "+
					           mWideningOperator.widen(abstractValue, phi));
			return mWideningOperator.widen(abstractValue, phi);
		}
		else
			return abstractValue;
	}
	
	/*
	 * Some handy support methods
	 */
		
	protected T getAbstractValue(long constant) {
		return mNullValue.getAbstractValue(constant);
	}
	
	protected T getUniverse(XlimType type) {
		return mNullValue.getUniverse(type);
	}
	
	protected boolean mayBeTrue(T condition) {
		return condition.contains(1);
	}
	
	protected boolean mayBeFalse(T condition) {
		return condition.contains(0);
	}
		
	protected T restrict(T value, XlimType type) {
		// TODO: find a nice way of plugging in new types
		if (type.isInteger()) {
			return value.signExtend(type.getSize()-1);
		}
		else if (type.isBoolean()) {
			return value.zeroExtend(1);
		}
		else
			throw new IllegalArgumentException("cannot restrict values of type "+type.getTypeName());
	}
	
	protected T getInput(XlimInputPort input, Context<T> context) {
		return context.demand(input.getValue());
	}
	
	protected T getInput(XlimStateCarrier carrier, XlimOperation xlimOp, Context<T> context) {
		ValueOperator valueOp=xlimOp.getValueOperator();
		for (ValueNode value: valueOp.getInputValues()) {
			if (value.getStateCarrier()==carrier)
				return context.demand(value);
		}
		throw new IllegalArgumentException("No such state access");
	}
	
	/*
	 * Here comes all the default handlers
	 */
	
	protected class LiteralIntegerHandler extends Handler {

		@Override
		public T evaluate(ValueNode output, 
				          XlimOperation op,
				          Context<T> context) {
			return getAbstractValue(op.getIntegerValueAttribute());
		}		
	}
	
	
	protected abstract class GenericHandler extends Handler {
		@Override
		public T evaluate(ValueNode output, 
				          XlimOperation op,
				          Context<T> context) {
			T result=getInput(op.getInputPort(0), context);
			for (int i=1; i<op.getNumInputPorts(); ++i) {
				result = operator(result, getInput(op.getInputPort(i), context));
			}
			return restrict(result, op.getOutputPort(0).getType());
		}
		
		protected abstract T operator(T x, T y);
	}
	
	protected class AddHandler extends GenericHandler {
		@Override
		protected T operator(T x, T y)	{
			return x.add(y);
		}
	}
	
	protected class SubHandler extends GenericHandler {
		@Override
		protected T operator(T x, T y)	{
			return x.subtract(y);
		}	
	}
	
	protected class MulHandler extends GenericHandler {
		@Override
		protected T operator(T x, T y)	{
			return x.multiply(y);
		}	
	}
	
	protected class DivHandler extends GenericHandler {
		@Override
		protected T operator(T x, T y)	{
			return x.divide(y);
		}	
	}
	
	protected class AndHandler extends GenericHandler {
		@Override
		protected T operator(T x, T y)	{
			return x.and(y);
		}	
	}
	
	protected class OrHandler extends GenericHandler {
		@Override
		protected T operator(T x, T y)	{
			return x.or(y);
		}	
	}
	
	protected class XorHandler extends GenericHandler {
		@Override
		protected T operator(T x, T y)	{
			return x.xor(y);
		}	
	}
	
	protected abstract class ShiftHandler extends GenericHandler {
		@Override
		public T evaluate(ValueNode output, 
				          XlimOperation op,
				          Context<T> context) {
			// shift count modulo 32
			T shiftCount=getInput(op.getInputPort(1), context).zeroExtend(5);
			T x=getInput(op.getInputPort(0), context);
			T result=operator(x,shiftCount);
			return restrict(result, op.getOutputPort(0).getType());
		}
	}
	
	protected class LShiftHandler extends ShiftHandler {
		@Override
		protected T operator(T x, T y)	{
			return x.shiftLeft(y);
		}
	}
	
	protected class RShiftHandler extends ShiftHandler {
		@Override
		protected T operator(T x, T y)	{
			return x.shiftRight(y);
		}
	}
	
	protected class URShiftHandler extends ShiftHandler {
		@Override
		protected T operator(T x, T y)	{
			return x.zeroExtend(32).shiftLeft(y);
		}
	}
	
	protected abstract class UnaryHandler extends Handler {
		@Override
		public T evaluate(ValueNode output, 
				          XlimOperation op,
				          Context<T> context) {
			T result = operator(getInput(op.getInputPort(0), context));
			return restrict(result, op.getOutputPort(0).getType());
		}
		
		protected abstract T operator(T x);
	}
	
	protected class NegHandler extends UnaryHandler {
		@Override
		protected T operator(T x) {
			return x.negate();
		}
	}
	
	protected class NotHandler extends UnaryHandler {
		@Override
		protected T operator(T x) {
			return x.not();
		}
	}
	
	protected class CopyAndCastHandler extends UnaryHandler {
		@Override
		protected T operator(T x) {
			return x;
		}
	}
	
	protected class SignExtendHandler extends Handler {
		@Override
		public T evaluate(ValueNode output, 
		          XlimOperation op,
		          Context<T> context) {
			int fromBit=(int)((long) op.getIntegerValueAttribute())-1;
			T x=getInput(op.getInputPort(0), context);
			return x.signExtend(fromBit);
		}
	}
	
	protected class EqHandler extends GenericHandler {
		@Override
		protected T operator(T x, T y)	{
			return x.equalsOperator(y);
		}	
	}
	
	protected class NeHandler extends GenericHandler {
		@Override
		protected T operator(T x, T y)	{
			return x.equalsOperator(y).not();
		}	
	}

	protected class LtHandler extends GenericHandler {
		@Override
		protected T operator(T x, T y)	{
			return x.lessThanOperator(y);
		}	
	}

	protected class GtHandler extends GenericHandler {
		@Override
		protected T operator(T x, T y)	{
			return y.lessThanOperator(x);
		}	
	}
	
	protected class GeHandler extends GenericHandler {
		@Override
		protected T operator(T x, T y)	{
			return x.lessThanOperator(y).not();
		}	
	}
	
	protected class LeHandler extends GenericHandler {
		@Override
		protected T operator(T x, T y)	{
			return y.lessThanOperator(x).not();
		}	
	}
	
	protected class SelectorHandler extends Handler {
		@Override
		public T evaluate(ValueNode output, 
				          XlimOperation op,
				          Context<T> context) {
			T condition=getInput(op.getInputPort(0), context);
			T result;
			// TODO: We should handle phis in the same way (If) and 
			// similarly for Loops (input 0 always, 1 if mayBeTrue)
			if (mayBeTrue(condition))
				result=getInput(op.getInputPort(1), context);
			else
				result=getNullValue();
			if (mayBeFalse(condition))
				result=join(result, getInput(op.getInputPort(1), context));
			return restrict(result, op.getOutputPort(0).getType());
		}
	}
	
	protected class AssignHandler extends Handler {
			
		@Override
		public T evaluate(ValueNode output,
				          XlimOperation op,
				          Context<T> context) {
			if (op.getNumInputPorts()==1) {
				// Unary variant of operator is assignment of scalar variable
				// Value of variable becomes value of right-hand-side
				return getInput(op.getInputPort(0), context);
			}
			else {
				// Binary variant is assignment of aggregate: v[op0]=op1
				// Value of aggregate is join(oldValue,newValue)
				// that is: the value represents any/all elements of the aggregate
				T oldValue=getInput(op.getStateVarAttribute(),op,context);
				T newValue=getInput(op.getInputPort(1), context);
				return join(oldValue, newValue);
			}
				
		}
	}
	
	protected class VarRefHandler extends Handler {
		@Override
		public T evaluate(ValueNode output,
                          XlimOperation op,
                          Context<T> context) {
			// Implements indexed access of aggregate: v[op0]
			// Current value of aggregate represents any/all elements
			return getInput(op.getStateVarAttribute(),op,context);
		}		
	}
	
	protected class PinReadHandler extends Handler {
		@Override
		public T evaluate(ValueNode output,
				          XlimOperation op,
				          Context<T> context) {
			if (output==op.getOutputPort(0)) {
				// Here, we assume that the value of an input port represents
				// all values that can be read from the port
				return getInput(op.getPortAttribute(),op,context); 
			}
			else {
				// We recycle the same value to represent the updated
				// port value (i.e. rest of the stream readable from the port)
				return getInput(op.getPortAttribute(),op,context);
			}
		}
	}
	
	protected class PinWriteHandler extends Handler {
		@Override
		public T evaluate(ValueNode output,
				          XlimOperation op,
				          Context<T> context) {
			// Here, we assume that the value of an output port represents
			// all values that can be written to the port
			T oldValue=getInput(op.getPortAttribute(),op,context);
			T newValue=getInput(op.getInputPort(0), context);
			return join(oldValue, newValue);
		}
	}
	
	protected class PinStatusHandler extends Handler {
		@Override
		public T evaluate(ValueNode output,
                          XlimOperation op,
                          Context<T> context) {
			// Returns a value representing "false" or "true"
			return getUniverse(op.getOutputPort(0).getType());
		}
	}
	
	protected class PinAvailHandler extends Handler {
		@Override
		public T evaluate(ValueNode output,
                          XlimOperation op,
                          Context<T> context) {
			// Returns a value representing 0..MAX_INT
			XlimType type=op.getOutputPort(0).getType();
			T maxValue=getAbstractValue(type.maxValue());
			return getUniverse(type).and(maxValue);
		}
	}
}
