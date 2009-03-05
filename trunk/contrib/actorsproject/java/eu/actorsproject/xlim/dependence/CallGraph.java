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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.XlimStateCarrier;
import eu.actorsproject.xlim.XlimTaskModule;

public class CallGraph {
	
	private XlimDesign mDesign;
	private CallNode mAutoStartNode;
	private ArrayList<CallNode> mPostOrder;
	
	protected CallGraph(XlimDesign design) {
		mDesign=design;
	}
	
	/**
	 * Builds the representation of dependence within the given design by 
	 * traversing its XLIM structure. In particular, the data dependence graph 
	 * of each call nodes is constructed.
	 * @param design
	 */
	public static CallGraph create(XlimDesign design) {
		CallGraph callGraph=new CallGraph(design);
		callGraph.build();
		return callGraph;
	}
	
	public XlimDesign getDesign() {
		return mDesign;
	}
	
	public CallNode getAutoStartNode() {
		return mAutoStartNode;
	}

	/**
	 * @return topologically ordered callNodes (called node/task before its callers)
	 */
	public ArrayList<CallNode> postOrder() {
		if (isModified()) {
			HashSet<CallNode> visited=new HashSet<CallNode>();
			HashSet<CallNode> onStack=new HashSet<CallNode>();
			mPostOrder=new ArrayList<CallNode>();
			for (XlimTaskModule task: mDesign.getTasks()) { 
				sortCallNodes(task.getCallNode(),visited,onStack,mPostOrder);
			}
		}
		return mPostOrder;
	}
	
	boolean isModified() {
		if (mAutoStartNode!=null)
			return mAutoStartNode.isModified();
		else {
			for (XlimTaskModule task: mDesign.getTasks()) { 
				if (task.getCallNode().isModified())
					return true;
			}
		}
		return false;
	}
	
	private void setAutoStartNode(CallNode root) {
		if (mAutoStartNode!=null)
			throw new IllegalStateException("Multiple \"autostart\" tasks");
		mAutoStartNode=root;
	}
	
	private void build() {
		// Fix-up each node/task in topologic/postorder:
		for (CallNode callNode: postOrder()) {
			XlimTaskModule task=callNode.getTask();
			task.completePatchAndFixup();
			
			// update callers (we haven't fixed them up yet), 
			// since we traverse the call graph from callee-to-caller
			DataDependenceGraph ddg=callNode.getDataDependenceGraph();
			Set<XlimStateCarrier> inputs=ddg.getAccessedState();
			Set<XlimStateCarrier> outputs=ddg.getModifiedState();
			
			for (CallSite callSite: callNode.getCallers()) {
				callSite.createInputValues(inputs);
				callSite.createOutputValues(outputs);
			}
			
			if (task.isAutostart())
				setAutoStartNode(callNode);
		}
	}
	
	/**
	 * Topological sort of call nodes, so that called nodes/tasks appears before their callers 
	 * @param callNode         one of the callNodes (iterate over all callNodes)
	 * @param visited          set of already visited callNodes
	 * @param onStack          set of call nodes that is being processed at this time
	 * @param sortedCallNodes  result, list of sorted callNodes
	 */
	private static void sortCallNodes(CallNode callNode, 
							   Set<CallNode> visited, 
							   Set<CallNode> onStack,
							   List<CallNode> sortedCallNodes) {
		// TODO: Can an XLIM call graph be cyclic?
		if (onStack.contains(callNode))
			throw new IllegalStateException("Recursive taskCalls not supported (should they?)");

		if (visited.add(callNode)) {
			onStack.add(callNode);
			for (CallSite callSite: callNode.getCallSites())
				sortCallNodes(callSite.getCallee(), visited, onStack, sortedCallNodes);
			onStack.remove(callNode);
			sortedCallNodes.add(callNode);
		}
	}	
}
