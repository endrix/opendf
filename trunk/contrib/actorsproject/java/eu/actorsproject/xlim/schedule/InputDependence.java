/* 
 * Copyright (c) Ericsson AB, 2010
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

package eu.actorsproject.xlim.schedule;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.decision2.PortSignature;
import eu.actorsproject.xlim.dependence.DependenceSlice;
import eu.actorsproject.xlim.dependence.PhiOperator;
import eu.actorsproject.xlim.dependence.TestOperator;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueOperator;
import eu.actorsproject.xlim.util.InstructionPattern;

/**
 * Identifies the input dependence of a decision tree:
 * Each ValueNode in the slice is associated with a PortSignature, which
 * signifies the availability tests that are required (in order to peek at inputs).
 */
public class InputDependence {

	private OperatorVisitor mVisitor=new OperatorVisitor();
	private InstructionPattern mPinPeekPattern=new InstructionPattern("pinPeek", 
			                                                          new InstructionPattern("$literal_Integer")
			                                                         );
	public InputDependence() {
	}
	
	public Map<ValueNode,PortSignature> inputDependence(DependenceSlice slice) {
		Map<ValueNode,PortSignature> requiredInput=new HashMap<ValueNode,PortSignature>();
		for (ValueOperator op: slice.topDownOrder()) {
			update(requiredInput, op);
		}
		return requiredInput;
	}
	
	private void update(Map<ValueNode,PortSignature> requiredInput, ValueOperator op) {
		op.accept(mVisitor, requiredInput);
	}
	
	/**
	 * Identifies an operator's (eventually a condition's) dependence on input availability.
	 * The pinPeek operators, which do token look-ahead, creates this dependence: they have to
	 * be guarded with an appropraite token availability test.
	 */
	private class OperatorVisitor implements ValueOperator.Visitor<Object, Map<ValueNode,PortSignature>> {

		public Object visitOperation(XlimOperation xlimOp, Map<ValueNode, PortSignature> requiredInput) {
			if (mPinPeekPattern.matches(xlimOp)) {
				// pinPeek($literalInteger)
				XlimTopLevelPort port=xlimOp.getPortAttribute();
				XlimOperation literal=(XlimOperation) mPinPeekPattern.getOperand(0, xlimOp);
				long index=literal.getIntegerValueAttribute();
				int requiredPortRate=(int) index + 1;
				PortSignature s=new PortSignature(Collections.singletonMap(port,requiredPortRate));
				
				requiredInput.put(xlimOp.getOutputPort(0).getValue(), s);
			}
			else {
				// all other operations
				visitDefault(xlimOp.getValueOperator(), requiredInput);
			}
			return null;
		}

		public Object visitPhi(PhiOperator phi, Map<ValueNode, PortSignature> requiredInput) {
			visitDefault(phi, requiredInput);
			return null;
		}

		public Object visitTest(TestOperator test, Map<ValueNode, PortSignature> requiredInput) {
			visitDefault(test, requiredInput);
			return null;
		}
		
		void visitDefault(ValueOperator valueOp, Map<ValueNode, PortSignature> requiredInput) {
			PortSignature result=null;
			
			for (ValueNode input: valueOp.getInputValues()) {
				PortSignature s=requiredInput.get(input);
				if (s!=null)
					if (result!=null)
						result=PortSignature.union(result, s);
					else
						result=s;
			}
			
			if (result!=null) {
				for (ValueNode output: valueOp.getOutputValues()) {
					requiredInput.put(output, result);
				}
			}
		}
		
	}
}
