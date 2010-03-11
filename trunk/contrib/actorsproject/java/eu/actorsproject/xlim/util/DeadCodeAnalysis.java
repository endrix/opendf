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

import java.util.ArrayDeque;
import java.util.HashSet;

import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimPhiNode;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.dependence.CallNode;
import eu.actorsproject.xlim.dependence.CallSite;
import eu.actorsproject.xlim.dependence.DataDependenceGraph;
import eu.actorsproject.xlim.dependence.Location;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.StateLocation;


public class DeadCodeAnalysis {
		
	public DeadCodePlugIn findDeadCode(XlimDesign design) {
		UsefulCodeResult result=new UsefulCodeResult();
		UsefulCodeWorkList workList=new UsefulCodeWorkList(result);
		
		/* 
		 * Traverse the dependence relation "bottom-up" from the output values,
		 * which correspond to actor ports, and any "non-removable" operations.
		 * All values that are reached by this traversal are useful and,
		 * conversely, unreached values corresponds to dead code.
		 */
		for (XlimTaskModule task: design.getTasks()) {
			workList.addCriticalSideEffects(task);
		}
		workList.formClosure();
		return result;
	}
}


class UsefulCodeResult extends DeadCodePlugIn {
	private HashSet<ValueNode> mUsefulValues=new HashSet<ValueNode>();

	@Override
	public boolean isDead(ValueNode value) {
		return (mUsefulValues.contains(value)==false);
	}
	
	public boolean markUseful(ValueNode value) {
		return mUsefulValues.add(value);
	}	
}

class UsefulCodeWorkList  { 

	private boolean mTrace=false;  // Debug printouts

	private UsefulCodeResult mResult;
	private ArrayDeque<ValueNode> mWorkList;
	private ValueVisitor mValueVisitor;
	private TaskTraversal mTaskTraversal;
	
	public UsefulCodeWorkList(UsefulCodeResult result) {
		mResult=result;
		mWorkList=new ArrayDeque<ValueNode>();
		mValueVisitor=new ValueVisitor();
		mTaskTraversal=new TaskTraversal();
	}
	
	public void markUseful(ValueNode v) {
		if (mResult.markUseful(v)) {
			if (mTrace)
				System.out.println("// DeadCodeAnalysis, Added useful value: "+v.getUniqueId());
			mWorkList.push(v);
		}
	}
	
	public void addCriticalSideEffects(XlimTaskModule task) {
		// Add output values of all actor ports
		CallNode callNode=task.getCallNode();
		DataDependenceGraph ddg=callNode.getDataDependenceGraph();
		for (StateLocation carrier: ddg.getModifiedState())
			if (carrier.asActorPort()!=null)
				markUseful(ddg.getOutputValue(carrier));
		// Add operations with removable="no"
		mTaskTraversal.addNonRemovableOperations(task);
	}

	public void formClosure() {
		while (mWorkList.isEmpty()==false) {
			ValueNode value=mWorkList.pop();
			if (mTrace)
				System.out.println("// DeadCodeAnalysis, Following value: "+value.getUniqueId());
			value.accept(mValueVisitor,null);
		}
	}
	
	class ValueVisitor implements ValueNode.Visitor<Object, Object> {
		@Override
		public Object visitInitial(ValueNode v, CallNode callNode, Object dummyArg) {
			// Initial values (of tasks): mark corresponding input of all callers
			Location loc=v.getLocation();
			assert(loc!=null && loc.isStateLocation());
			StateLocation stateLoc=loc.asStateLocation();
			
			for (CallSite callSite: callNode.getCallers()) {
				ValueNode input=callSite.getInputValue(stateLoc);
				markUseful(input);
			}
			return null;
		}

		@Override
		public Object visitCall(ValueNode v, CallSite callSite, Object dummyArg) {
			Location loc=v.getLocation();
			assert(loc!=null && loc.isStateLocation());
			
			// For taskCalls: mark corresponding output value of the called task and its task module
			CallNode callee=callSite.getCallee();
			DataDependenceGraph g=callee.getDataDependenceGraph();
			
			markUseful(g.getOutputValue(loc.asStateLocation()));
			return null;
		}

		@Override
		public Object visitAssign(ValueNode v, ValueNode old, boolean killsOld, Object dummyArg) {
			ValueNode killed=(killsOld)? old : null;
			for (ValueNode pred: v.getDefinition().getInputValues())
				if (pred!=killed)
					markUseful(pred);
			return null;
		}

		@Override
		public Object visitOther(ValueNode v, Object dummyArg) {
			for (ValueNode pred: v.getDefinition().getInputValues())
				markUseful(pred);
			return null;
		}
	}

	class TaskTraversal extends XlimTraversal<Object, Object> {
		
		public void addNonRemovableOperations(XlimTaskModule task) {
			traverse(task,null);
		}
		
		@Override
		protected Object handleOperation(XlimOperation op, Object dummyArg) {
			// mark all inputs of "non-removable" operations
			if (op.isRemovable()==false) {
				if (mTrace)
					System.out.println("// DeadCodeAnalysis, non-removable operation: "+op.toString());
				for (ValueNode input: op.getValueOperator().getInputValues())
					markUseful(input);
			}
			return null;
		}
		

		@Override
		protected Object handlePhiNode(XlimPhiNode phi, Object arg) {
			return null;
		}
	}
}