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

package eu.actorsproject.xlim.absint;

import java.util.HashMap;

import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.XlimStateCarrier;
import eu.actorsproject.xlim.dependence.CallGraph;
import eu.actorsproject.xlim.dependence.CallNode;
import eu.actorsproject.xlim.dependence.CallSite;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueUsage;

public class ContextInsensitiveEvaluator<T> extends AbstractEvaluator<T> {

	public ContextInsensitiveEvaluator(AbstractDomain<T> domain) {
		super(domain);
	}

	@Override
	public Context<T> createContext(XlimDesign design) {
		CallGraph g=design.getCallGraph();
		return new SingletonContext(g.getAutoStartNode());
	}

	@Override
	protected EvaluationGraph createEvaluationGraph() {
		return new ContextInsensitiveEvaluationGraph();
	}

	@Override
	protected StronglyConnectedComponent createSCC(EvaluationContext context,
                                                   ValueNode value) {
		return new ContextInsensitiveSCC(context);
	}

	/**
	 * In this (context insensitive) evaluator, we just have a single
	 * evaluation context for all the code of an actor
	 */
	private class SingletonContext extends EvaluationContext {

		
		public SingletonContext(CallNode actionScheduler) {
			super(actionScheduler);
		}
		
		@Override
		public EvaluationContext getCalleeContext(CallSite callSite) {
			return this;
		}


		@Override
		protected T reEvaluateInitial(ValueNode initValue, CallNode callNode) {
			XlimStateCarrier carrier=initValue.getStateCarrier();

			T result=mValueMap.get(initValue);
			for (CallSite callSite: callNode.getCallers()) {
				ValueNode input=callSite.getInputValue(carrier);
				result=mDomain.join(result,mValueMap.get(input));
			}
			return result;
		}
	}
	
	private class ContextInsensitiveEvaluationGraph extends EvaluationGraph {

		private HashMap<ValueNode,Integer> mDepthMap=
			new HashMap<ValueNode,Integer>();
		
		@Override
		protected boolean addNode(EvaluationContext context,
				                  ValueNode node,
				                  int nextIndex) {
			if (mDepthMap.containsKey(node))
				return false;
			else {
				mDepthMap.put(node, nextIndex);
				return true;
			}
		}

		@Override
		protected int getIndex(EvaluationContext context, 
				               ValueNode node) {
			return mDepthMap.get(node);
		}

		@Override
		protected int visitCallers(EvaluationContext context,
				                   ValueNode node,
				                   CallNode callNode) {
			XlimStateCarrier carrier=node.getStateCarrier();
			int minIndex=Integer.MAX_VALUE;
			for (CallSite callSite: callNode.getCallers()) {
				ValueNode input=callSite.getInputValue(carrier);
				minIndex=Math.min(minIndex,visit(context,input));
			}
			return minIndex;
		}
	}
	
	private class ContextInsensitiveSCC extends StronglyConnectedComponent {

		private WorkItem mWorkItem;
		
		public ContextInsensitiveSCC(EvaluationContext context) {
			mWorkItem=new WorkItem(context);
		}
		
		@Override
		protected void enqueue(EvaluationContext context,
				               ValueNode node) {
			mWorkItem.enqueue(node);
		}

		@Override
		protected WorkItem getNextWorkItem() {
			return mWorkItem;
		}

		@Override
		protected boolean moreWorkItems() {
			return mWorkItem!=null && mWorkItem.isEmpty()==false;
		}
		

		@Override
		protected void initializeCallers(ValueNode initValue,
				                         CallNode inCallNode,
				                         EvaluationContext context) {
			XlimStateCarrier carrier=initValue.getStateCarrier();
			for (CallSite callSite: inCallNode.getCallers()) {
				ValueNode input=callSite.getInputValue(carrier);
				initialize(context,input);
			}
		}

		@Override
		protected void propagateToCallers(ValueUsage finalUse,
				                          CallNode fromCallNode,
				                          EvaluationContext context) {
			XlimStateCarrier carrier=finalUse.getStateCarrier();
			
			for (CallSite callSite: fromCallNode.getCallers()) {
				ValueNode out=callSite.getOutputValue(carrier);
				propagateToSuccessor(out,context);
			}
		}
	}
}
