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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Constructs and represents a "slice" of a DataDependenceGraph:
 * all the ValueOperators and ValueNodes required to compute a
 * set of outputs
 */
public class DependenceSlice {

	private String mName;
	private Map<CallNode,DependenceSlice> mCalleeSlices;
	private HashSet<ValueNode> mInputValues;
	private HashSet<ValueOperator> mValueOperators;
	private SliceExtractor mSliceExtractor;
	
	/**
	 * @param name          name of the slice (for printout)
	 * @param calleeSlices  mapping from call node to dependence slice:
	 *                      an entry for each call node, which might be encountered
	 *                      in the slice, is required.
	 */
	public DependenceSlice(String name, Map<CallNode,DependenceSlice> calleeSlices) {
		mName=name;
		mCalleeSlices=calleeSlices;
		mInputValues=new HashSet<ValueNode>();
		mValueOperators=new HashSet<ValueOperator>();
		mSliceExtractor=new SliceExtractor();
	}
	
	/**
	 * @return the name of the slice (for printout)
	 */
	public String getName() {
		return mName;
	}

	/**
	 * @return the input values of this slice
	 */
	public Set<ValueNode> getInputValues() {
		return mInputValues;
	}

	/**
	 * @return the value operators of this slice
	 */
	public Set<ValueOperator> getValueOperators() {
		return mValueOperators;
	}
	
	/**
	 * @param value  a value node that is to be added to the slice
	 */
	public void add(ValueNode value) {
		mSliceExtractor.visitPredecessor(value);
	}

	/**
	 * @param value  a value node
	 * @return       true iff the value node belongs to the slice
	 */
	public boolean contains(ValueNode value) {
		ValueOperator def=value.getDefinition();
		return (def!=null && contains(def) || mInputValues.contains(value));
	}
	
	/**
	 * @param op  a value operator
	 * @return    true iff the value operator belongs to the slice
	 */
	public boolean contains(ValueOperator op) {
		return mValueOperators.contains(op);
	}
	
	
	protected boolean addInput(ValueNode value) {
		return mInputValues.add(value);
	}
	
	/**
	 * @param def  a ValueOperator that is a predecessor to the slice
	 * @return     true if 'def' should be added to the slice
	 *             false if 'def' is to be considered an input of the slice
	 *             
	 * By overriding this method, the extension of the slice can be limited
	 * (for instance to the "scope" of a particular module)
	 */
	protected boolean includeInSlice(ValueOperator def) {
		return (def!=null);
	}
	
	/**
	 * @param call  a call node
	 * @return      the DependenceSlice, which is associated with the call node
	 */
	protected DependenceSlice getCalleeSlice(CallNode call) {
		return mCalleeSlices.get(call);
	}
	
	/**
	 * Provides traversal of predecessors, in particular the (non-trivial)
	 * traversal over slices of call nodes is implemented.
	 */
	private class SliceExtractor implements ValueNode.Visitor<Object,Object> {

		public void visitPredecessor(ValueNode value) {
			ValueOperator def=value.getDefinition();

			if (includeInSlice(def)) {
				if (mValueOperators.add(def)) {
					// Visit the predecessors of def
					value.accept(this, null);
				}
			}
			else {
				// mark as 'input value'
				addInput(value);
			}
		}
		
		@Override
		public Object visitInitial(ValueNode node, CallNode inCallNode, Object dummyArg) {
			return null;
		}
		
		@Override
		public Object visitAssign(ValueNode node, ValueNode old, boolean killsOld, Object dummyArg) {
			ValueNode killed=(killsOld)? old : null;
			for (ValueNode pred: node.getDefinition().getInputValues())
				if (pred!=killed) {
					visitPredecessor(pred);
				}
			return null;
		}

		@Override
		public Object visitOther(ValueNode node, Object dummyArg) {
			for (ValueNode pred: node.getDefinition().getInputValues())
				visitPredecessor(pred);
			return null;
		}
		
		@Override
		public Object visitCall(ValueNode node, CallSite callSite, Object dummyArg) {
			CallNode callee=callSite.getCallee();
			DependenceSlice calleeSlice=getCalleeSlice(callee);
			assert(calleeSlice!=null);
			
			// Visit all predecessors of the call
			// -provided that they are in the slice of the callee
			Set<ValueNode> calleeSliceInputs=calleeSlice.getInputValues();
			DataDependenceGraph ddg=callee.getDataDependenceGraph();
			for (ValueNode actualInput: callSite.getInputValues()) {
				ValueNode formalInput=ddg.getInputValue(actualInput.getStateCarrier());
				
				if (calleeSliceInputs.contains(formalInput)) {
					visitPredecessor(actualInput);
				}
			}
			return null;
		}
	}
}
