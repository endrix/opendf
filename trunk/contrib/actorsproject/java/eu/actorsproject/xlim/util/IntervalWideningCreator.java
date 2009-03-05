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

package eu.actorsproject.xlim.util;

import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.XlimInstruction;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimPhiNode;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimType;

/**
 * An interval widening operator is needed to make range analysis practical (otherwise
 * the lattice would (essentially) be of infinite height and cyclic dependences could 
 * take forever to resolve).
 * 
 * This is a simple/stupid way of arriving at a widening operator:
 * (a) use the same widening operator for all cyclic dependences
 * (b) use the constants that appear in the program to partition the intervals
 * (c) also add the constants that represents the end points of types involved in type casts
 * (d) in addition, add a number of strategic constants (such as MAX_INT)
 * 
 * In this way we at least get useful results for propagation of constants, removal of casts
 * and tests (=,< etc) involving a constant. 
 */
public class IntervalWideningCreator extends XlimTraversal<Object,IntervalWidening>{

	protected OperationPlugIn<Handler> mOperationPlugIn;
	
	public IntervalWideningCreator() {
		mOperationPlugIn=new OperationPlugIn<Handler>(new Handler()); // Add noop default handler
		mOperationPlugIn.registerHandler("$literal_Integer", new LiteralHandler());
		mOperationPlugIn.registerHandler("cast", new CastHandler());
	}
	
	/**
	 * By default we start with the following partitioning:
	 * (-infinity,Integer.MIN_VALUE-1], [Integer.MIN_VALUE,-1] [0,0] [1,1]
	 * [2,Integer.MAX_VALUE] [Integer.MAX_VALUE+1,UINT32_MAX] [UINT32_MAX,+infinity)
	 */
	protected void addDefaultEndPoints(IntervalWidening arg) {
		arg.addStartPoint(Integer.MIN_VALUE);
		arg.addStartPoint(0);
		arg.addStartPoint(1);
		arg.addStartPoint(2);
		arg.addEndPoint(Integer.MAX_VALUE);
		arg.addStartPoint(1L<<32); // UINT32_MAX+1
	}

	private void debugPrintOut(IntervalWidening arg) {
		System.out.println("IntervalWideningCreator: resulting partitions");
		System.out.println(arg.toString());
	}
	
	public IntervalWidening createWideningOpeator(XlimDesign design, IntervalWidening arg) {
		traverse(design, arg);
		addDefaultEndPoints(arg);
		debugPrintOut(arg);
		return arg;
	}

	public IntervalWidening createWideningOperator(XlimTaskModule task, IntervalWidening arg) {
		super.traverse(task, arg);
		addDefaultEndPoints(arg);
		debugPrintOut(arg);
		return arg;
	}

	@Override
	protected Object handleOperation(XlimOperation op, IntervalWidening arg) {
		mOperationPlugIn.getOperationHandler(op).handleOperation(op, arg);
		return null;
	}

	@Override
	protected Object handlePhiNode(XlimPhiNode phi, IntervalWidening arg) {
		return null;
	}	
	
	
	protected static class Handler implements OperationHandler {

		@Override
		public boolean supports(XlimOperation op) {
			return true;
		}
		
		public void handleOperation(XlimOperation op, IntervalWidening arg) {
			// Default handler does nothing
		}
	}
	
	protected static class LiteralHandler extends Handler {
		@Override
		public void handleOperation(XlimOperation op, IntervalWidening arg) {
			System.out.println("IntervalWideningCreator: "+op.toString()+" adds "+
			                   op.getIntegerValueAttribute());
			arg.addConstant(op.getIntegerValueAttribute());
		}
	}
	
	protected static class CastHandler extends Handler {
		@Override
		public void handleOperation(XlimOperation op, IntervalWidening arg) {			
			XlimOutputPort port=op.getOutputPort(0);
			XlimType type=port.getType();
			System.out.println("IntervalWideningCreator: "+op.toString()+" adds ["+
					           type.minValue()+","+type.maxValue()+"]");
			arg.addStartPoint(type.minValue());
			arg.addEndPoint(type.maxValue());
		}
	}
	
	protected static class SignExtendHandler extends Handler {
		@Override
		public void handleOperation(XlimOperation op, IntervalWidening arg) {
			int fromWidth=(int)(long) op.getIntegerValueAttribute();
			long signBit=1L<<(fromWidth-1);
			long minValue=-signBit;
			long maxValue=~minValue;
			System.out.println("IntervalWideningCreator: "+op.toString()+" adds ["+
					           minValue+","+maxValue+"]");
			arg.addStartPoint(minValue);
			arg.addEndPoint(maxValue);
		}
	}
}
