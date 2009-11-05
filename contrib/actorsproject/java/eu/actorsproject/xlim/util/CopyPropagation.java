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
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimPhiNode;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.dependence.ValueNode;

public class CopyPropagation extends XlimTraversal<Object,Object> {

	protected OperationPlugIn<CopyPropagator> mPlugIn;
	protected boolean mTrace=false; // Debug printouts
	
	public CopyPropagation() {
		CopyPropagator defaultHandler=new CopyPropagator();
		CopyPropagator noopHandler=new NoopHandler();
		mPlugIn=new OperationPlugIn<CopyPropagator>(defaultHandler);
		mPlugIn.registerHandler("noop", noopHandler);
		mPlugIn.registerHandler("cast", noopHandler);  // removes one-input ands
		mPlugIn.registerHandler("$and", noopHandler);  // removes one-input ands
	}
	
	public void copyPropagate(XlimDesign design) {
		traverse(design,null);
	}
	
	public void copyPropagate(XlimTaskModule task) {
		traverse(task,null);
	}

	@Override
	protected Object handleOperation(XlimOperation op, Object dummyArg) {
		CopyPropagator propHandler=mPlugIn.getOperationHandler(op);
		if (propHandler.supports(op))
			propHandler.removeCopy(op);
		
		return null;
	}

	@Override
	protected Object handlePhiNode(XlimPhiNode phi, Object dummyArg) {
		return null; // Phi-nodes are never copy propagated
	}

	protected class CopyPropagator implements OperationHandler {
		public boolean supports(XlimOperation op) {
			return false;
		}

		public void removeCopy(XlimOperation op) {
			throw new UnsupportedOperationException();
		}
	}

	protected class NoopHandler extends CopyPropagator {
		// noop, cast and the weird "unary $and" are copy operations, provided
		// (a) source is an output port (state variables would require us to deal with 
		//     side-effects when propagatibg the source)
		// (b) the result is at least as wide as the source and of the same "kind" of
		//     type (otherwise we deal with a type conversion that alters the value)
		
		public boolean supports(XlimOperation op) {
			return (op.getNumOutputPorts()==1
					&& op.getNumInputPorts()==1
					&& okToPropagate(op.getInputPort(0).getSource(), 
							         op.getOutputPort(0).getValue()));
		}

		private boolean okToPropagate(XlimSource source, ValueNode output) {
			XlimType sourceT=source.getType();
			XlimType resultT=output.getType();
			
			if (sourceT.isList()) {
				assert(resultT.isList());
				assert(source.hasLocation() && output.hasLocation());
				
				// Don't propagate copy if
				// a) source/result types are not the same, or 
				// b) the location of the source is modified, or
				// c) the location of the output is modified
				return sourceT==resultT 
				       && source.getLocation().isModified()==false
				       && output.getLocation().isModified()==false;
			}
			else if (sourceT.isInteger()){
				// Integer source and result,
				// result at least as wide as source
				// not a reference to a state variable,
				return source.asOutputPort()!=null
			       && resultT.isInteger()
			       && sourceT.getSize()<=resultT.getSize();
			}
			else {
				// source and result of same type,
				// not a reference to a state variable
				return source.asOutputPort()!=null
				   && sourceT==resultT;
			}
		}
				
		@Override
		public void removeCopy(XlimOperation op) {
			XlimOutputPort oldPort=op.getOutputPort(0);
			XlimOutputPort newPort=op.getInputPort(0).getSource().asOutputPort();
			oldPort.substitute(newPort);
			if (mTrace)
				System.out.println("// CopyPropagation: substituting " + newPort.getUniqueId()
						          + " for "+oldPort.getUniqueId());
		}
	}
}