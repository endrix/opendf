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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.XlimStateCarrier;

public class DataDependenceGraph {

	private Map<XlimStateCarrier,ValueNode> mInputValues;
	private Map<XlimStateCarrier,ValueUsage> mOutputValues;
	private CallNode mCallNode;
	
	public DataDependenceGraph(CallNode callNode) {
		mCallNode=callNode;
	}

	/**
	 * @return the port/state variables that are accessed (used and/or modified) 
	 *         in this graph
	 */
	public Set<XlimStateCarrier> getAccessedState() {
		return mInputValues.keySet();
	}
	
	/**
	 * @return the ports/state variables that are modified in this graph
	 *         (this is a subset of the accessed ports/state variables) 
	 */
	public Set<XlimStateCarrier> getModifiedState() {
		return mOutputValues.keySet();
	}
	
	/**
	 * @param carrier, a port or a state variable
	 * @return the node in this graph that represents the initial value
	 *         of carrier (null if not accessed in this graph)
	 */
	public ValueNode getInputValue(XlimStateCarrier carrier) {
		return mInputValues.get(carrier);
	}

	/**
	 * @param carrier, a port or a state variable
	 * @return the node in this graph that represents the final value
	 *         of carrier (null if not accessed in this graph, the initial
	 *         value if not modified in this graph)
	 */
	public ValueNode getOutputValue(XlimStateCarrier carrier) {
		ValueUsage output=mOutputValues.get(carrier);
		if (output!=null)
			return output.getValue();
		else
			return mInputValues.get(carrier);
	}
		
	public void resolveExposedUses(FixupContext context) {
		if (mInputValues==null) {
			// First time: allocate initial values and resolve exposed uses
			mInputValues=new HashMap<XlimStateCarrier,ValueNode>();
			// We have to copy the exposed uses (we are modifying the underlying map)
			ArrayList<XlimStateCarrier> exposedUses=
				new ArrayList<XlimStateCarrier>(context.getExposedUses());
			for (XlimStateCarrier carrier: exposedUses) {
				ValueNode initialValue=new InitialValueNode(carrier);
				mInputValues.put(carrier, initialValue);
				context.resolveExposedUses(initialValue);
			}
		}
		else {
			// Following times: use existing initial values to resolve exposed uses
			ArrayList<XlimStateCarrier> exposedUses=
				new ArrayList<XlimStateCarrier>(context.getExposedUses());
			for (XlimStateCarrier carrier: exposedUses) {
				ValueNode initialValue=mInputValues.get(carrier);
				if (initialValue!=null)
					context.resolveExposedUses(initialValue);
				else {
					// Currently we do not support patches that access new stateful resources.
					// Doing so doesn't seem terribly useful. Support is complicated in that
					// it affects the callers of the task (each call site needs to be updated 
					// and the resulting exposed uses need to be resolved in the caller tasks).
					throw new UnsupportedOperationException("Patch adds access to new state carrier: "
							+carrier.getSourceName());
				}
			}
		}
	}
	
	public void propagateNewValues(FixupContext context) {
		if (mOutputValues==null) {
			// First time: allocate final value uses and fix them up
			mOutputValues=new HashMap<XlimStateCarrier,ValueUsage>();
			for (XlimStateCarrier carrier: context.getNewValues()) {
				ValueUsage finalUse=new FinalStateUsage(carrier);
				mOutputValues.put(carrier, finalUse);
				context.propagateNewValue(finalUse);
			}
		}
		else {
			// Following times: use existing final value uses, but set new values
			for (XlimStateCarrier carrier: context.getNewValues()) {
				ValueUsage finalUse=mOutputValues.get(carrier);
				if (finalUse!=null)
					context.propagateNewValue(finalUse);
				else {
					// Currently we do not support patches that access new stateful resources.
					// Doing so does not seem terribly useful. Support is complicated in that
					// it affects the callers of the task (each call site needs to be updated 
					// and the resulting new values need to be propagated in the caller tasks).
					throw new UnsupportedOperationException("Patch adds access to new state carrier: "
							+carrier.getSourceName());
				}
			}
		}
	}
	
	/**
	 * Removes an input/output value (used by CallNode, which also updates callers)
	 * @param carrier
	 */
	void remove(XlimStateCarrier carrier) {
		// First check that we are not doing anything stupid
		ValueNode input=mInputValues.get(carrier);
		ValueUsage output=mOutputValues.get(carrier);
		Iterator<ValueUsage> p=input.getUses().iterator();
		assert(p.hasNext()==false || p.next()==output && p.hasNext()==false);
		
		mInputValues.remove(carrier);
		mOutputValues.remove(carrier);
	}
	
	
	private class InitialValueNode extends StateValueNode {

		private XlimStateCarrier mStateCarrier;
		
		public InitialValueNode(XlimStateCarrier carrier) {
			mStateCarrier=carrier;
		}
				
		@Override
		public XlimStateCarrier getStateCarrier() {
			return mStateCarrier;
		}
		
		@Override
		public ValueOperator getDefinition() {
			return null;
		}	

		
		@Override
		public ValueNode getDominatingDefinition() {
			// By convention initial values have no previous definition
			// though they actually have one for each caller
			return null;  
		}

		@Override
		public <Result,Arg> Result accept(Visitor<Result,Arg> visitor, Arg arg) {
			return visitor.visitInitial(this,mCallNode,arg);
		}
	}
	
	// TODO: setting a FinalStateUsage to the same value as the input value means no modification is made
	// Perhaps let the CallNode check, remove output values and update callers?
	private class FinalStateUsage extends ValueUsage {

		private XlimStateCarrier mStateCarrier;
		
		public FinalStateUsage(XlimStateCarrier carrier) {
			super(null);
			mStateCarrier=carrier;
		}
		
		@Override
		public XlimStateCarrier getStateCarrier() {
			return mStateCarrier;
		}
		
		@Override
		public ValueOperator usedByOperator() {
			return null; // Final usage has no ValueOperator
		}
		
		@Override
		public XlimModule getModule() {
			return mCallNode.getTask();
		}
		
		@Override
		public <Result,Arg> Result accept(Visitor<Result,Arg> visitor, Arg arg) {
			return visitor.visitFinal(this,mCallNode,arg);
		}
	}
}
