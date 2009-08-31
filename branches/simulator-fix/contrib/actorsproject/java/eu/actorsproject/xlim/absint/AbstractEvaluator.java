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
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.dependence.CallNode;
import eu.actorsproject.xlim.dependence.CallSite;
import eu.actorsproject.xlim.dependence.PhiOperator;
import eu.actorsproject.xlim.dependence.TestOperator;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueOperator;
import eu.actorsproject.xlim.dependence.ValueUsage;

public abstract class AbstractEvaluator<T> {

	protected AbstractDomain<T> mDomain;
	
	protected AbstractEvaluator(AbstractDomain<T> domain) {
		mDomain=domain;
	}
	
	/**
	 * @param design an XlimDesign (i.e. an actor)
	 * @return the evaluation context of design's action scheduler 
	 */
	public abstract Context<T> createContext(XlimDesign design);
	
	/**
	 * @return a new evaluation graph
	 */
	protected abstract EvaluationGraph createEvaluationGraph();
	
	/**
	 * @return a new strongly connected component
	 */
	protected abstract StronglyConnectedComponent createSCC(EvaluationContext context,
                                                            ValueNode value);
	
	
	/**
	 * EvaluationContext provides the storage for memoized evaluations
	 * It also factors out code that is common for the context sensitive/
	 * insensitive cases while the specifics of each case is handled in subclasses. 
	 */
	protected abstract class EvaluationContext implements Context<T>, 
	                                                      ValueNode.Visitor<T, Object>,
	                                                      ValueOperator.Visitor<T, ValueNode>{
		private CallNode mActionScheduler;
		protected HashMap<ValueNode,T> mValueMap;
			
		
		public EvaluationContext(CallNode actionScheduler) {
			mActionScheduler=actionScheduler;
			mValueMap=new HashMap<ValueNode,T>();
		}
		
		@Override
		public T demand(ValueNode node) {
			T result=mValueMap.get(node);
			if (result==null) {
				EvaluationGraph g=createEvaluationGraph();
				g.visit(this,node);
				result=mValueMap.get(node);
			}
			return result;
		}
		
		@Override
		public abstract EvaluationContext getCalleeContext(CallSite callSite);
		
		public boolean hasBeenEvaluated(ValueNode node) {
			return mValueMap.containsKey(node);
		}

		/**
		 * Initializes node (sets its value to the null-value of the domain).
		 * @param node
		 * @return true if the node was initialized (false if it already was)
		 */
		public boolean initialize(ValueNode node) {
			if (mValueMap.containsKey(node)==false) {
				mValueMap.put(node, mDomain.getNullValue());
				return true;
			}
			else
				return false;
		}
		
		/**
		 * Re-evaluates initValue given new values of at least one of its
		 * predecessors in the callers.
		 * @param initValue  an initial ValueNode of a CallNode
		 * @param inCallNode CallNode, which initValue belongs to
		 * @return the resulting abstract value
		 */
		protected abstract T reEvaluateInitial(ValueNode initValue, CallNode inCallNode);
		
		/**
		 * Re-evaluates node given new values of at least one of its 
		 * predecessors.
		 * @param node
		 * @return true, if the abstract value of node was updated
		 */
		public boolean reEvaluate(ValueNode node) {
			T result=node.accept(this,null /*dummyArg*/);		
			T oldValue=mValueMap.get(node);
			if (oldValue.equals(result))
				return false;
			else {
				mValueMap.put(node, result);
				return true;
			}
		}

		/*
		 * ValueNode.Visitor methods are used in the re-evaluation of nodes
		 */
		
		@Override
		public T visitInitial(ValueNode node, CallNode inCallNode, Object dummyArg) {
			if (inCallNode==mActionScheduler)
				return mDomain.initialState(node.getStateCarrier());
			else
				return reEvaluateInitial(node,inCallNode);
		}
		
		@Override
		public T visitCall(ValueNode node, CallSite call, Object dummyArg) {
			// The output of the call site is the output of the callee
			EvaluationContext calleeContext=getCalleeContext(call);
			ValueNode out=call.getCalleeOutput(node);
			return calleeContext.mValueMap.get(out);
		}

		@Override
		public T visitAssign(ValueNode node, 
				             ValueNode old,
				             boolean killsOld, 
				             Object dummyArg) {
			ValueOperator def=node.getDefinition();
			return def.accept(this, node);
		}

		@Override
		public T visitOther(ValueNode node, Object dummyArg) {
			ValueOperator def=node.getDefinition();
			return def.accept(this, node);
		}
		
		/*
		 * ValueOperator.Visitor methods are used in the re-evaluation of nodes
		 */
		@Override
		public T visitOperation(XlimOperation op, ValueNode node) {
			return mDomain.evaluate(node, op, this);
		}
		
		@Override
		public T visitPhi(PhiOperator phi, ValueNode node) {
			return mDomain.evaluate(phi, this);
		}
		
		@Override 
		public T visitTest(TestOperator test, ValueNode node) {
			// Since the test operator produces no value, we should never end up here!
			throw new UnsupportedOperationException();
		}
	}
	
	/**
	 * EvaluationGraph organizes the evaluation of value nodes, possibly
	 * spanning over multiple contexts. It sees to that predecessors are
	 * evaluated before their successors. In particular, cyclic dependences
	 * (strongly-connected components) are resolved before proceeding
	 * to nodes that depends on the values that take part in the cyclic
	 * dependence.
	 * It also factors out code that is independent of the context sensitive/
	 * insensitive cases while the specifics of each case is handled in subclasses.
	 */
	protected abstract class EvaluationGraph implements ValueNode.Visitor<Integer, EvaluationContext>{
		
		private int mNextIndex;
		
		/**
		 * Adds context/node to the evaluation graph
		 * @param context    the evaluation context
		 * @param node       the value node
		 * @param nextIndex  next preorder index (to be used if adding context/node)
		 * @return           true if context/node was added (false if already present)
		 */
		protected abstract boolean addNode(EvaluationContext context,
				                           ValueNode node,
				                           int nextIndex);
		
		/**
		 * @param context    the evaluation context
		 * @param node       the value node
		 * @return the preorder index, at which context/node was added to the evaluation graph
		 */
		protected abstract int getIndex(EvaluationContext context,
				                        ValueNode node);
		
		/**
		 * Adds all values, which are passed from callers to the initial value node
		 * @param context    the evaluation context
		 * @param initValue  an initial ValueNode of a CallNode
		 * @param inCallNode CallNode, to which initValue
		 * @return the minimum preorder index of any predecessor node visited via the callers 
		 */
		protected abstract int visitCallers(EvaluationContext context,
				                            ValueNode initValue,
				                            CallNode inCallNode);
		
		/**
		 * Adds context/node and all predecessors that have not yet been evaluated
		 * @param context    the evaluation context
		 * @param valueNode  a value node
		 * @return the minimum preorder index over the visited nodes:
		 *         < index of caller, if a node already on the stack was visited, 
		 *           in which case a cyclic dependence has been identified and evaluation of that 
		 *           SCC is deferred.
		 *         = index of caller, if the context/node itself is the representer of a SCC (including
		 *           the case when there is no cyclic dependence). In this case the entire SCC is
		 *           evaluated before returning.
		 */
		public int visit(EvaluationContext context, 
				         ValueNode valueNode) {
			int myIndex=mNextIndex;
			if (context.hasBeenEvaluated(valueNode)) {
				// already evaluated -not part of the same strongly-connected component (SCC) as the caller
				return myIndex; // greater than index of caller 
			}
			else if (addNode(context,valueNode,myIndex)) {
				// this is the first time this node is visited
				// it may or may not be in the same SCC as the caller/successor node
				mNextIndex++;
				int minIndex=valueNode.accept(this,context);
								
				if (minIndex>=myIndex) {
					// There is no ancestor that is still on the stack
					// (visitPredecessors would then have returned a lower index),
					// which means that we are ready to evaluate the SCC entered via this node
			
					StronglyConnectedComponent scc=createSCC(context,valueNode);
					scc.evaluate(context,valueNode);
					return myIndex;
				}
				else {
					// the evaluation is deferred (at least) until we're back 
					// to the ancestor that is indicated by minDepth
					return minIndex;
				}
			}
			else {
				// this node is still on the stack, which means that it is
				// in the same SCC as the caller/successor node
				return getIndex(context,valueNode);
			}
		}


		@Override
		public Integer visitInitial(ValueNode node, 
				                    CallNode inCallNode,
				                    EvaluationContext context) {
			return visitCallers(context,node,inCallNode);
		}

		@Override
		public Integer visitCall(ValueNode node, 
				                 CallSite call,
				                 EvaluationContext context) {
			// visit the corresponding output of the callee
			EvaluationContext calleeContext=context.getCalleeContext(call);
			ValueNode out=call.getCalleeOutput(node);
			return visit(calleeContext,out);
		}

		
		@Override
		public Integer visitAssign(ValueNode node, 
				                   ValueNode old,
				                   boolean killsOld, 
				                   EvaluationContext context) {
			// Visit all predecessors that are not killed by the assignment
			ValueNode killedPred=(killsOld)? old : null;
			ValueOperator def=node.getDefinition();
			int minIndex=Integer.MAX_VALUE;
			
			for (ValueNode pred: def.getInputValues()) {
				if (pred!=killedPred) {
					minIndex=Math.min(minIndex, visit(context,pred));
				}
			}
			return minIndex;

		}

		@Override
		public Integer visitOther(ValueNode node, EvaluationContext context) {
			ValueOperator def=node.getDefinition();
			int minIndex=Integer.MAX_VALUE;
			
			for (ValueNode pred: def.getInputValues()) {
				minIndex=Math.min(minIndex, visit(context,pred));
			}
			return minIndex;
		}
	}
	
	/**
	 * WorkItem provides a work list, consisting of value nodes in the
	 * same EvaluationContext, which needs to be reEvaluated
	 */
	protected class WorkItem extends WorkList<ValueNode> {
		private EvaluationContext mContext;
		
		public WorkItem(EvaluationContext context) {
			mContext=context;
		}
		
		public EvaluationContext getContext() {
			return mContext;
		}
	}
	
	/**
	 * StronglyConnectedComponent identifies the contexts/value nodes that take part in a 
	 * cyclic dependence and evaluates/re-evaluates these nodes until a fix-point is reached
	 * 
	 * It also factors out code that is independent of the context sensitive/insensitive 
	 * cases while the specifics of each case is handled in subclasses.
	 */
	protected abstract class StronglyConnectedComponent implements ValueUsage.Visitor<Object, EvaluationContext>,
	                                                               ValueNode.Visitor<Object, EvaluationContext> {
		
		/**
		 * Queues context/node for re-evaluation
		 * @param context
		 * @param node
		 */
		protected abstract void enqueue(EvaluationContext context, ValueNode node);
		
		/**
		 * @return true if there is more work to do
		 */
		protected abstract boolean moreWorkItems();
		
		/**
		 * @return a work item to chew on
		 */
		protected abstract WorkItem getNextWorkItem();
				
		protected void evaluate(EvaluationContext context, ValueNode node) {
			// Initialize the entire SCC starting from context/node
			initialize(context,node);
			
			while (moreWorkItems()) {
				WorkItem item=getNextWorkItem();
				context=item.getContext();
				while (!item.isEmpty()) {
					node=item.dequeue();
					if (context.reEvaluate(node)) {
						// the value associated with node changed,
						// propagate change to the successors that are in the same SCC
						for (ValueUsage use: node.getUses()) {
							// Depending on the kind of use, propagation to successors
							// is handled by either of the ValueUsage.Visitor methods:
							// visitAssign, visitCall, visitFinal or visitOther
							use.accept(this,context);
						}
					}
				}
			}
		}

		/*
		 * Initialization of SCC
		 * =====================
		 */

		/**
		 * Initialize node and all its not-yet-initialized predecessors (which belong to
		 * the same SCC): (a) set an initial (null) value in the given context (b) add
		 * context/node to the SCC worklist
		 * @param node    initial value node
		 * @param context Evaluation context of node
		 */

		protected void initialize(EvaluationContext context, ValueNode node) {
			if (context.initialize(node)) {
				enqueue(context,node);
				node.accept(this,context);
				// Depending on the kind of use, initialization of predecessors
				// is handled by either of the ValueNode.Visitor methods:
				//  visitInitial, visitCall, visitAssign or visitOther
			}
			// else: node already initialized (may not even be in this SCC) 
		}

		/**
		 * Initialize the nodes corresponding to the values passed from caller to initValue
		 * @param initValue  initial value node
		 * @param inCallNode CallNode, to which initValue belongs
		 * @param context    EvaluationContext of initValue
		 */

		protected abstract void initializeCallers(ValueNode initValue, 
				                                  CallNode inCallNode, 
				                                  EvaluationContext context);

		@Override
		public Object visitInitial(ValueNode node, CallNode inCallNode, EvaluationContext context) {
			initializeCallers(node,inCallNode,context);
			return null;
		}

		@Override
		public Object visitCall(ValueNode node, CallSite call, EvaluationContext context) {
			// CallSite is a special case: connect the output 
			// of the call site with that of the callee
			EvaluationContext calleeContext=context.getCalleeContext(call);
			ValueNode out=call.getCalleeOutput(node);
			initialize(calleeContext,out);
			return null;
		}

		@Override
		public Object visitAssign(ValueNode node, ValueNode old, boolean killsOld, EvaluationContext context) {
			ValueOperator def=node.getDefinition();
			ValueNode killed=(killsOld)? old : null;
			for (ValueNode pred: def.getInputValues()) {
				if (pred!=killed) {
					initialize(context,pred);
				}
			}
			return null;
		}
		
		@Override
		public Object visitOther(ValueNode node, EvaluationContext context) {
			ValueOperator def=node.getDefinition();
			for (ValueNode pred: def.getInputValues()) {
				initialize(context,pred);
			}
			return null;
		}

		/*
		 *  Propagation of re-evaluated ValueNodes to successors
		 *  ====================================================
		 */
		
		protected void propagateToSuccessor(ValueNode succ, EvaluationContext context) {
			if (context.hasBeenEvaluated(succ)) {
				// successor is in same SCC, since it has been evaluated
				enqueue(context,succ);
			}
		}
		
		protected void propagateToSuccessors(Iterable<? extends ValueNode> successors,
				                             EvaluationContext context) {
			for (ValueNode succ: successors) {
				if (context.hasBeenEvaluated(succ)) {
					// successor is in same SCC, since it has been evaluated
					enqueue(context,succ);
				}
			}
		}
		
		/**
		 * Propagates a changed value from "final" value in a CallNode to its successors.
		 * Different ValueNodes constitute successors in context sensitive/insensitive
		 * analyses.
		 * @param use          Usage of value that has been re-evaluated and updated
		 * @param fromCallNode Usage corresponds to "final" usage of stateful resource
		 *                     (state variable/port) in fromCallNode
		 * @param context      Context, in which evaluation was performed
		 */
		protected abstract void propagateToCallers(ValueUsage use, 
                                                   CallNode fromCallNode,
                                                   EvaluationContext context);
		
		@Override
		public Object visitAssign(ValueUsage use, 
				                  ValueNode newValue,
				                  boolean killsUse, 
				                  EvaluationContext context) {
			// Propagate to successors if the assignment doesn't kill the old value
			if (killsUse==false) {
				ValueOperator op=use.usedByOperator();	
				propagateToSuccessors(op.getOutputValues(),context);
			}
			return null;
		}

		@Override
		public Object visitCall(ValueUsage use, 
				                CallSite call,
				                EvaluationContext context) {
			// CallSite is a special case: propagate the input 
			// of the call site to initial value of the callee
			ValueNode nodeInCaller=use.getValue();
			EvaluationContext calleeContext=context.getCalleeContext(call);
			ValueNode nodeInCallee=call.getCalleeInput(nodeInCaller);
			propagateToSuccessor(nodeInCallee,calleeContext);
			return null;
		}

		
		@Override
		public Object visitFinal(ValueUsage use, 
				                 CallNode inCallNode,
				                 EvaluationContext context) {
			propagateToCallers(use,inCallNode,context);
			return null;
		}

		@Override
		public Object visitOther(ValueUsage use, EvaluationContext context) {
			// Normal case
			ValueOperator op=use.usedByOperator();	
			propagateToSuccessors(op.getOutputValues(),context);
			return null;
		}
	}	
}
