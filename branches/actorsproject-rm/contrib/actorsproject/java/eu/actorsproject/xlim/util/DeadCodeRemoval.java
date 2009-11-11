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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;


import eu.actorsproject.xlim.XlimBlockElement;
import eu.actorsproject.xlim.XlimBlockModule;
import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.XlimIfModule;
import eu.actorsproject.xlim.XlimInstruction;
import eu.actorsproject.xlim.XlimLoopModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimPhiNode;
import eu.actorsproject.xlim.XlimStateCarrier;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.dependence.CallNode;
import eu.actorsproject.xlim.dependence.DataDependenceGraph;
import eu.actorsproject.xlim.dependence.StatePhiOperator;
import eu.actorsproject.xlim.dependence.ValueNode;

public class DeadCodeRemoval {
	
	private DeadCodeTraversal deadCodeTraversal=new DeadCodeTraversal();
	private boolean mTrace=false;  // debug printouts
	
	public void deadCodeRemoval(XlimDesign design, DeadCodePlugIn plugIn) {
		ArrayList<CallNode> postorder=design.getCallGraph().postOrder();
		bottomUpPass(postorder,plugIn);
		topDownPass(design,postorder,plugIn);
	}
	
	/**
	 * Performs "cheap" dead-code removal, by which (most?) unreferenced, side-effect free operations
	 * are removed. Loops, if-modules and block modules, which become empty as a result, are also
	 * removed. Unlike "real" dead-code removal, which is based on the concept of "useful" operations,
	 * "cheap" dead-code removal may leave dead code behind.
	 * @param task Task to transform
	 */
	public void cheapDeadCodeRemoval(XlimDesign design) {
		deadCodeRemoval(design, new CheapDeadCodePlugIn());
	}
	
	private class CheapDeadCodePlugIn extends DeadCodePlugIn {
		@Override
		public boolean isDead(XlimInstruction instr) {
			return (instr.isReferenced()==false 
					&& instr.mayModifyState()==false
					&& instr.isRemovable());
		}
		
		@Override
		public boolean isDead(ValueNode value) {
			return value.getUses().iterator().hasNext()==false;
		}
	}

	
	/**
	 * Removes dead operations by traversing the call graph "bottom-up"
	 * 
	 * @param postorder  call nodes sorted in "postorder" (called task before caller)
	 * @param plugIn     plugIn to query the liveness of operators
	 */
	private void bottomUpPass(ArrayList<CallNode> postorder, DeadCodePlugIn plugIn) {		
		// Traverse called tasks before callers
		for (CallNode callNode: postorder) {
			XlimTaskModule task=callNode.getTask();
			boolean empty=deadCodeTraversal.traverse(task, plugIn);
			if (empty) {
				// Calls to this task has no effect -remove all calls!
				// (and substitute inputs for outputs at all call sites)
				callNode.removeAllCallers();
			}
		}
	}

	/**
	 * Removes redundant tasks, state variables and (internal) ports. Call graph is
	 * traversed "top-down".
	 * 
	 * @param design     actor/XLIM design
	 * @param postorder  call nodes sorted in "postorder" (called task before caller)
	 * @param plugIn     plugIn to query the liveness of operators
	 */
	private void topDownPass(XlimDesign design, ArrayList<CallNode> postorder, DeadCodePlugIn plugIn) {
		// Start by assuming that all state variables and (internal) actor ports are unref:ed
		HashSet<XlimStateCarrier> unreferenced=new HashSet<XlimStateCarrier>();
		unreferenced.addAll(design.getStateVars());
		unreferenced.addAll(design.getInternalPorts());
		
		// Look for unreferenced (non-autostart) tasks in reverse postorder
		// (caller before called node/task)
		for (int i=postorder.size()-1; i>=0; --i) {
			CallNode callNode=postorder.get(i);
			XlimTaskModule task=callNode.getTask();
			if (task.isAutostart()==false
				&& callNode.isReferenced()==false) {
				
				// Unreferenced task remove it from call graph
				callNode.removeAllCallSites();
				if (mTrace)
					System.out.println("// DeadCodeRemoval: removing task "+task.getName());
				design.removeTask(task);
			}
			else {
				// If a state variable/port is useful in at least one task,
				// we need to keep it!
				DataDependenceGraph ddg=callNode.getDataDependenceGraph();
				unreferenced.removeAll(ddg.getAccessedState());
			}
		}
		
		for (XlimStateCarrier carrier: unreferenced) {
			XlimStateVar stateVar=carrier.isStateVar();
			if (stateVar!=null) {
				if (mTrace) {
					String name=stateVar.getSourceName();
					name=(name!=null)? " ("+name+")" : "";
					System.out.println("// DeadCodeRemoval: removing state variable "+stateVar.getUniqueId()+name);
				}
				design.removeStateVar(stateVar);
			}
			else {
				XlimTopLevelPort port=carrier.isPort();
				if (mTrace)
					System.out.println("// DeadCodeRemoval: removing port "+port.getSourceName());
				design.removeTopLevelPort(port);
			}
		}
	}	


	class DeadCodeTraversal extends XlimTraversal<Boolean,DeadCodePlugIn> {
		@Override
		public Boolean traverse(XlimDesign design, DeadCodePlugIn plugIn) {
			// DeadCodeRemoval.topDownPass does this instead!
			throw new UnsupportedOperationException();
		}

		@Override
		public Boolean traverse(XlimTaskModule task, DeadCodePlugIn plugIn) {
			boolean empty=super.traverse(task, plugIn);
			DataDependenceGraph ddg=task.getCallNode().getDataDependenceGraph();
			ArrayList<XlimStateCarrier> unused=null;

			// check for state access that has become dead:
			// input unused, no modification within task.
			for (XlimStateCarrier carrier: ddg.getAccessedState()) {
				ValueNode input=ddg.getInputValue(carrier);
				ValueNode output=ddg.getOutputValue(carrier);
				if (plugIn.isDead(input) && input==output) {
					if (unused==null)
						unused=new ArrayList<XlimStateCarrier>();
					unused.add(carrier);
				}
			}

			// Remove input/outputs of dead state access
			// This affects callSites in the callers of this task
			if (unused!=null) {
				CallNode callNode=task.getCallNode();
				for (XlimStateCarrier carrier: unused) {
					callNode.removeStateAccess(carrier);
				}
			}
			return empty;
		}

		@Override
		protected Boolean traverseContainerModule(XlimContainerModule m, DeadCodePlugIn arg) {
			Iterator<? extends XlimBlockElement> pElement=m.getChildren().iterator();
			boolean empty=true;
			while (pElement.hasNext()) {
				XlimBlockElement child=pElement.next();
				if (child.accept(mVisitor, arg)) {
					if (mTrace)
						System.out.println("// DeadCodeRemoval: removing "+child.toString());
					pElement.remove();
				}
				else
					empty=false;
			}

			return empty;
		}

		@Override
		protected Boolean traverseBlockModule(XlimBlockModule m, DeadCodePlugIn arg) {
			return traverseContainerModule(m,arg);
		}

		@Override
		protected Boolean traverseIfModule(XlimIfModule m, DeadCodePlugIn arg) {
			boolean empty=traverseTestModule(m.getTestModule(),arg);
			if (traverseContainerModule(m.getThenModule(),arg)==false)
				empty=false;
			if (traverseContainerModule(m.getElseModule(),arg)==false)
				empty=false;
			if (traversePhiNodes(m.getPhiNodes(),arg)==false)
				empty=false;
			if (traverseStatePhiNodes(m.getStatePhiOperators())==false)
				empty=false;

			return empty;
		}

		@Override
		protected Boolean traverseLoopModule(XlimLoopModule m, DeadCodePlugIn arg) {
			boolean empty=traverseTestModule(m.getTestModule(),arg);			
			if (traverseContainerModule(m.getBodyModule(),arg)==false)
				empty=false;
			if (traversePhiNodes(m.getPhiNodes(),arg)==false)
				empty=false;
			if (traverseStatePhiNodes(m.getStatePhiOperators())==false)
				empty=false;

			return empty;
		}

		@Override
		protected Boolean traversePhiNodes(Iterable<? extends XlimPhiNode> phiNodes, DeadCodePlugIn arg) {
			Iterator<? extends XlimPhiNode> pPhi=phiNodes.iterator();
			boolean empty=true;
			while (pPhi.hasNext()) {
				XlimPhiNode phi=pPhi.next();
				if (handlePhiNode(phi,arg)) {
					if (mTrace)
						System.out.println("// DeadCodeRemoval: removing "+phi.toString());
					pPhi.remove();
				}
				else
					empty=false;
			}
			return empty;
		}

		boolean traverseStatePhiNodes(Iterable<? extends StatePhiOperator> statePhis) {
			Iterator<? extends StatePhiOperator> pPhi=statePhis.iterator();
			boolean empty=true;
			while (pPhi.hasNext()) {
				StatePhiOperator phi=pPhi.next();
				ValueNode in0=phi.getInputValue(0);
				ValueNode in1=phi.getInputValue(1);
				// A state-phi is removed when it represents "no modification":
				// a) both inputs represents the "dominating definition"
				// b) a loop-phi, which feeds-back itself along the back edge
				//    (in which case it is actually the "dominating definition"
				//     coming from the pre-header of the loop)
				if (in0==in1 || in1==phi.getOutput()) {
					if (mTrace)
						System.out.println("// DeadCodeRemoval: removing "+phi.toString());
					pPhi.remove();
				}
				else
					empty=false;
			}
			return empty;
		}

		@Override
		protected Boolean handleOperation(XlimOperation op, DeadCodePlugIn arg) {
			return arg.isDead(op) && op.isRemovable();
		}

		@Override
		protected Boolean handlePhiNode(XlimPhiNode phi, DeadCodePlugIn arg) {
			return arg.isDead(phi);
		}
	}
}