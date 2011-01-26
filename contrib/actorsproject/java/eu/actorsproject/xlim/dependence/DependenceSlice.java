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
import java.util.Iterator;
import java.util.LinkedHashSet;
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
	private LinkedHashSet<ValueOperator> mValueOperators;
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
		mValueOperators=new LinkedHashSet<ValueOperator>();
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
	 * @return the value operators of this slice in a top-up partial order
	 *         (ordered by data-dependence)
	 */
	public Iterable<ValueOperator> topDownOrder() {
		return new Iterable<ValueOperator>() {
			public Iterator<ValueOperator> iterator() {
				return new TopDownIterator();
			}
		};
	}
	
	private class TopDownIterator implements Iterator<ValueOperator> {
		
		private ValueOperator mOperators[];
		private int mNext;
		
		TopDownIterator() {
			mOperators=new ValueOperator[mValueOperators.size()];
			mValueOperators.toArray(mOperators);
			mNext=mOperators.length;
		}

		@Override
		public boolean hasNext() {
			return mNext>0;
		}

		@Override
		public ValueOperator next() {
			return mOperators[--mNext];
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	/**
	 * @param value  a value node that is to be added to the slice
	 * @return true iff adding 'value' caused an additional inputValue to be added to the slice
	 */
	public boolean add(ValueNode value) {
		return mSliceExtractor.visitPredecessor(value);
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
	private class SliceExtractor implements ValueNode.Visitor<Boolean,Object> {

		public boolean visitPredecessor(ValueNode value) {
			ValueOperator def=value.getDefinition();
            
			if (includeInSlice(def)) {
				boolean inputAdded=false;
				
				if (mValueOperators.add(def)) {
					// Visit the predecessors of def
					if (value.accept(this, null))
						inputAdded=true;
				}
				
				return inputAdded;
			}
			else {
				// mark as 'input value'
				addInput(value);
				return true;
			}
		}
		
		@Override
		public Boolean visitInitial(ValueNode node, CallNode inCallNode, Object dummyArg) {
			assert(false); // Should never be called (includeInSlice should be false)
			addInput(node);
			return true;
		}
		
		@Override
		public Boolean visitAssign(ValueNode node, ValueNode old, boolean killsOld, Object dummyArg) {
			ValueNode killed=(killsOld)? old : null;
			boolean inputAdded=false;
			
			for (ValueNode pred: node.getDefinition().getInputValues())
				if (pred!=killed) {
					if (visitPredecessor(pred))
						inputAdded=true;
				}
			return inputAdded;
		}

		@Override
		public Boolean visitOther(ValueNode node, Object dummyArg) {
			boolean inputAdded=false;
			
			for (ValueNode pred: node.getDefinition().getInputValues()) {
				if (visitPredecessor(pred))
					inputAdded=true;
			}
			return inputAdded;
		}
		
		@Override
		public Boolean visitCall(ValueNode node, CallSite callSite, Object dummyArg) {
			boolean inputAdded=false;
			CallNode callee=callSite.getCallee();
			DependenceSlice calleeSlice=getCalleeSlice(callee);
			assert(calleeSlice!=null);
			
			// Visit all predecessors of the call
			// -provided that they are in the slice of the callee
			Set<ValueNode> calleeSliceInputs=calleeSlice.getInputValues();
			DataDependenceGraph ddg=callee.getDataDependenceGraph();
			for (ValueNode actualInput: callSite.getInputValues()) {
				Location loc=actualInput.getLocation();
				assert(loc!=null && loc.isStateLocation());
				ValueNode formalInput=ddg.getInputValue(loc.asStateLocation());
				
				if (calleeSliceInputs.contains(formalInput)) {
					if (visitPredecessor(actualInput))
						inputAdded=true;
				}
			}
			return inputAdded;
		}
	}
}
