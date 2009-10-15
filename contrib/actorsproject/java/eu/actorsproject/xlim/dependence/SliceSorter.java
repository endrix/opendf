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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import eu.actorsproject.xlim.XlimOperation;

/**
 * Sorts  a DependenceSlice topologically (so that definitions precede uses) 
 * and forms strongly-connected components (SCCs) for cyclic dependences 
 * (caused by loops).
 */
public class SliceSorter {

	private int mNextDepth;
	private HashMap<ValueOperator,Integer> mIndexMap;
	private ArrayList<DependenceComponent> mSorted;
	private DependenceSlice mTheSlice;
	
	private HashMap<CallNode,DependenceComponent> mCalleeComponents=
	        new HashMap<CallNode,DependenceComponent>();
	
	private CyclicPhiTest mCyclicPhiTest=new CyclicPhiTest();
	private ComponentCreator mComponentCreator=new ComponentCreator();
	
	private boolean mTrace=false;
	
	/**
	 * Creates a DependenceComponent, which represents a slice
	 * of a call node and associates it with the call node
	 * 
	 * @param callNode  a node in the call graph
	 * @param slice     a slice of that call node
	 * @return          the dependence component, which represents the slice
	 */
	public DependenceComponent topSort(CallNode callNode,
			                           DependenceSlice slice) {
		assert(mCalleeComponents.containsKey(callNode)==false);
		
		DependenceComponent result=topSort(slice);
		mCalleeComponents.put(callNode, result);
		return result;
	}
	
	/**
	 * @param slice  a dependence slice
	 * @return       the dependence component, which represents the slice
	 */
	public DependenceComponent topSort(DependenceSlice slice) {
		mNextDepth=0;
		mIndexMap=new HashMap<ValueOperator,Integer>();
		mSorted=new ArrayList<DependenceComponent>();
		mTheSlice=slice;

		if (mTrace) {
			System.out.println("Sorting slice "+slice.getName());
		}		

		// Visit all the value operators of the slice
		for (ValueOperator op: mTheSlice.getValueOperators())
			sortComponent(op);
		
		return new SequenceComponent(mSorted, slice.getName());
	}
	
	/**
	 * Adds ancesors to slice and computes the "depth" of a value node
	 * 
	 * @param node  a ValueNode
	 * @return      minimum "depth" of the node's ancestors 
	 *              (mNextDepth if they are already extracted)
	 * 
	 * The concept of "depth" is used to detect cyclic dependences
	 * (strongly-connected components): it is the level in the call stack,
	 * at which the definition (ValueOperator) of a value was first encountered.
	 * 
	 * Depths are kept in the "index map" and the following convention is used:
	 * a) ValueOperators that are currently on the call stack are tagged with
	 *    integers 0,1,2,....
	 * b) ValueOperators that belong to already extracted components (including
	 *    singleton ValueOperators that are not involved in any cycle) are tagged
	 *    with "null".
	 * c) Not yet visited ValueOperators have no entry in the index map
	 * 
	 * Cyclic dependences are detected when a predecessor has a (non-null) index
	 * that is less than that of its successor (=the predecessor is still on the 
	 * call stack and was encountered before its successor). This makes for the
	 * following extraction strategy:
	 * a) If the minimum (non-null) index of any ancestor, which is reachable
	 *    from a node is no less than that of the "current" ValueOperator then
	 *    the "current" ValueOperator is the "leader" of an SCC (extraction
	 *    starts at the leader)
	 * b) When the minimum index is less than that of the "current" ValueOperator,
	 *    it is part of an SCC that is "lead" by the ValueOperator with that index
	 *    (or one with even lower index). In this case extraction is postponed
	 *    until returning to the "leader".
	 *    
	 * An important special case of (a) is when the minimum index is higher than 
	 * that of the "current" ValueOperator, in which case it is not involved in any
	 * cycle and extraction is simply a matter of adding the ValueOperator to the
	 * list of topologically sorted components
	 * 
	 * Also note the convention of using mNextDepth (which is higher than any
	 * index, currently in the "index map") to signal that a predecessor is
	 * "already extracted".  
	 */
	private int sortComponent(ValueOperator def) {
		Integer index=mIndexMap.get(def);
		if (index!=null) {
			return index; // 'def' is still on call stack
		}
		else if (mIndexMap.containsKey(def)
				 || mTheSlice.contains(def)==false) {
			// 'def' is "already extracted" or it is not part of the slice
			// in neither case it should be processed (again)
			return mNextDepth; 
		}
		
		// 'def' not yet visited: assign depth and visit predecessors
		int myIndex=mNextDepth++;
		mIndexMap.put(def, myIndex);
		
		// compute min depth of ancestors 
		int minDepth=mNextDepth;
		for (ValueNode input: def.getInputValues()) {
			ValueOperator pred=input.getDefinition();
			if (pred!=null) {
				int temp=sortComponent(pred);
				if (temp<minDepth)
					minDepth=temp;
			}
		}
		
		if (minDepth>=myIndex) {
			// 'def' is not in the same component as any of the elements that 
			// were visited before it (def is the "leader" of its component)
			if (myIndex==mNextDepth-1) {
				// def is a singleton component, extract it directly to mSorted
				extract(def, mSorted);
			}
			else {
				// def is the representer of an SCC
				DependenceComponent scc=extractSCC(def);
				mSorted.add(scc);
			}
				
			// Restore mNextDepth (as if "popping" component)
			mNextDepth=myIndex;
			return myIndex;
		}
		else {
			// 'def' is in an SCC. We handle it (at the earliest) when we're back 
			// at depth 'minDepth'
			return minDepth;
		}
	}
	
	/*
	 * Used for tracing
	 */
	private String outputNames(ValueOperator op) {
		String result="";
		String delimiter="";
		
		for (ValueNode output: op.getOutputValues()) {
			result += delimiter + output.getUniqueId();
			delimiter=",";
		}
		return result;
	}
	
	/**
	 * Extracts a ValueOperator to a sequence of DependenceComponents
	 * 
	 * @param op           a ValueOperator to be extracted
	 * @param sequence     sequence, to which component (op) is added
	 */
	private void extract(ValueOperator op, 
                         List<DependenceComponent> sequence) {
		assert(mIndexMap.get(op)!=null);  // should still be "unextracted"

		// add the component
		sequence.add(createComponent(op));
		
		// mark it "extracted"
		mIndexMap.put(op,null);
		
		if (mTrace) {
			System.out.println("Extracting: "+outputNames(op));
		}
	}

	private DependenceComponent extractSCC(ValueOperator leader) {
		List<DependenceComponent> scc=new ArrayList<DependenceComponent>();
		ArrayDeque<PhiOperator> cyclicPhis=new ArrayDeque<PhiOperator>();
		
		if (mTrace) {
			System.out.println("Extracting SCC");
		}
		extractAncestors(leader,scc,cyclicPhis);
		
		// Handle nodes that were deferred to break the cycle(s)
		while (!cyclicPhis.isEmpty()) {
			PhiOperator phi=cyclicPhis.remove();
			
			extractAncestors(phi.getInputValue(1).getDefinition(), 
					         scc, cyclicPhis);
			extractAncestors(phi.getControlDependence().getDefinition(), 
					         scc, cyclicPhis);
		}
		
		if (mTrace) {
			System.out.println("SCC extraction complete");
		}
		// result should neither be empty nor a singleton 
		assert(scc.size()>1);  
		return new StronglyConnectedComponent(scc);
	}
		
	private void extractAncestors(ValueOperator op,
			                      List<DependenceComponent> scc,
			                      ArrayDeque<PhiOperator> deferred) {
		if (op!=null && mIndexMap.get(op)!=null) {
			// op is still waiting to be extracted
			PhiOperator cyclicPhi=isCyclicPhi(op);
		
			if (mTrace) {
				System.out.println("Extracting ancestors of: "+outputNames(op));
			}
			
			if (cyclicPhi!=null) {
				if (mTrace) {
					System.out.println("Deferring back-edge: "
					           +cyclicPhi.getInputValue(0).getUniqueId());
					System.out.println("Deferring control-dependence: "
					           +cyclicPhi.getInputValue(0).getUniqueId());
				}
				// We have to deal with the cycles (caused by phi-nodes of loops)
				// Break the cycle by visiting the dominating predessor only
				//
				// In this way we also order the SCC so that the first ValueOperator
				// (a cyclic phi) has inputs outside the SCC
				ValueNode pred=cyclicPhi.getInputValue(0);
				extractAncestors(pred.getDefinition(), scc, deferred);

				// Defer the rest of the traversal to avoid an infinite loop
				deferred.add(cyclicPhi);
			}
			else {
				// In this case we are OK: predecessors can be traversed
				// without looping back
				for (ValueNode pred: op.getInputValues())
					extractAncestors(pred.getDefinition(), scc, deferred);
			}
		
			// Extract the ValueNode to sequence
			extract(op, scc);
		}
	}
	
	/**
	 * @param op  a value operator
	 * @return    a "cyclic" PhiOperator if op is a phi in a loop (null otherwise)
	 */
	private PhiOperator isCyclicPhi(ValueOperator op) {
		return op.accept(mCyclicPhiTest, null);
	}
	
	/**
	 * @param op  a value operator
	 * @return    a component that represent the value operator
	 *            (op itself or a special CallComponent for task calls)
	 */
	private DependenceComponent createComponent(ValueOperator op) {
		return op.accept(mComponentCreator, null);
	}
	
	/**
	 * Creates a CallComponent
	 * 
	 * @param callSite  a call site
	 * @return          a dependence component representing the call
	 * 
	 * Specific dependence components are needed for taskCalls to
	 * (1) Associate the call with the proper (sliced) component
	 * (2) Associate inputs and outputs of callee with values in caller 
	 */
	private DependenceComponent createCallComponent(CallSite callSite) {
		CallNode callee=callSite.getCallee();
		DependenceSlice calleeSlice=mTheSlice.getCalleeSlice(callee);
		assert(calleeSlice!=null);
		
		DependenceComponent calleeComponent=getCalleeComponent(callee);
		assert(calleeComponent!=null);

		// create the list of inputs in the caller
		Set<ValueNode> inputsInCalleeSlice=calleeSlice.getInputValues();
		DataDependenceGraph ddg=callee.getDataDependenceGraph();
		ArrayList<ValueNode> inputsInCaller=new ArrayList<ValueNode>();
		for (ValueNode input: callSite.getInputValues()) {
			ValueNode inputInCallee=ddg.getInputValue(input.getStateCarrier());
			
			if (inputsInCalleeSlice.contains(inputInCallee)) {
				inputsInCaller.add(input);
			}
		}
		
		// create the list of outputs in the caller
		ArrayList<ValueNode> outputsInCaller=new ArrayList<ValueNode>();
		for (ValueNode output: callSite.getOutputValues()) {
			ValueNode outputInCallee=ddg.getOutputValue(output.getStateCarrier());
			
			if (calleeSlice.contains(outputInCallee)) {
				outputsInCaller.add(output);
			}
		}
		
		return new CallComponent(calleeComponent,inputsInCaller,outputsInCaller,callee);
	}
	
	/**
	 * @param callNode  a node in the call graph
	 * @return          the dependence component, which is associated
	 *                  with the call node.
	 */
	private DependenceComponent getCalleeComponent(CallNode callNode) {
		return mCalleeComponents.get(callNode);
	}
	
	/**
	 * Identifies phi-nodes that cause cycles
	 * (removing their back edges makes the SCC acyclic)
	 */
	private class CyclicPhiTest implements ValueOperator.Visitor<PhiOperator, Object> {
		@Override
		public PhiOperator visitPhi(PhiOperator phi, Object dummyArg) {
			return phi.inLoop()? phi : null;
		}
		
		@Override
		public PhiOperator visitOperation(XlimOperation xlimOp, Object dummyArg) {
			return null;
		}

		@Override
		public PhiOperator visitTest(TestOperator test, Object dummyArg) {
			return null;
		}
	}
	
	/**
	 * Helper to create CallComponents
	 * (the value operator itself is used for everything but task calls)
	 */
	private class ComponentCreator implements 
	              ValueOperator.Visitor<DependenceComponent, Object> {
		@Override
		public DependenceComponent visitPhi(PhiOperator phi, Object dummyArg) {
			return phi; // the value operator itself
		}
		
		@Override
		public DependenceComponent visitOperation(XlimOperation xlimOp, Object dummyArg) {
			CallSite callSite=xlimOp.getCallSite();
			if (callSite!=null)
				return createCallComponent(callSite);
			else
				return xlimOp.getValueOperator(); // the value operator itself 
		}

		@Override
		public DependenceComponent visitTest(TestOperator test, Object dummyArg) {
			return test; // the value operator itself
		}
	}	
}
