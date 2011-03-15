/* 
 * Copyright (c) Ericsson AB, 2010
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

package eu.actorsproject.xlim.schedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.util.XmlElement;
import eu.actorsproject.util.XmlPrinter;
import eu.actorsproject.xlim.XlimBlockElement;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.decision2.ActionNode;
import eu.actorsproject.xlim.decision2.Condition;
import eu.actorsproject.xlim.decision2.DecisionNode;
import eu.actorsproject.xlim.decision2.DecisionTree;
import eu.actorsproject.xlim.decision2.NullNode;

/**
 * The control-flow graph of an actor. 
 * Nodes/Basic Blocks correspond to DecisionNodes, ActionNodes and NullNodes.
 */
public class ControlFlowGraph implements XmlElement {

	private BasicBlock mInitialBlock;
	private TerminalBlock mTerminalBlock;
	private NullNode mBlockingNullNode=new NullNode();
	private Map<DecisionNode,DecisionBlock> mDecisions=
		new HashMap<DecisionNode,DecisionBlock>();
	private Map<ActionNode,ActionBlock> mActions=
		new HashMap<ActionNode,ActionBlock>();
	private List<BasicBlock> mRPostOrder;
	
	/**
	 * Sets the entry point of the control-flow graph
	 * 
	 * @param initialNode
	 */
	public void setInitialNode(BasicBlock initialBlock) {
		// Assume that we do this only once 
		// (perhaps we could allow for changing the initial node?)
		assert(mInitialBlock==null);
		mInitialBlock=initialBlock;
	}
	
	/**
	 * @return the entry point of the control-flow graph
	 */
	public BasicBlock getInitialNode() {
		return mInitialBlock;
	}
	
	/**
	 * @return the node, which signifies actor termination (or null if no such
	 *         node is reachable from the initial node).
	 */
	public BasicBlock getTerminalNode() {
		return mTerminalBlock;
	}
	
	/**
	 * @param t  a decision tree
	 * @return   the basic block, which corresponds to the root of the decision tree
	 *           (or null if t has no correspondance in the control-flow graph)
	 */
	public BasicBlock getNode(DecisionTree t) {
		BasicBlock b=mDecisions.get(t);
		if (b==null) {
			if (mTerminalBlock!=null && t==mTerminalBlock.getDecisionTree())
				b=mTerminalBlock;
			else
				b=mActions.get(t);
		}
		return b;
	}

	/**
	 * @return the ActionNodes that are part of this control-flow graph
	 */
	public Iterable<ActionNode> getActions() {
		return mActions.keySet();
	}
	
	/**
	 * @param action  an action
	 * @return the unique successor of the action
	 */
	public BasicBlock getSuccessor(ActionNode action) {
		ActionBlock actionBlock=mActions.get(action);
		return (actionBlock!=null)? actionBlock.getSuccessor() : null;
	}
	
	/**
	 * Set the successor of an ActionNode
	 * 
	 * @param action     an ActionNode
	 * @param successor  Node in the graph, which follows 'action'
	 */
	public void setSuccessor(ActionNode action, BasicBlock successor) {
		ActionBlock actionBlock=mActions.get(action);
		if (actionBlock==null) {
			actionBlock=new ActionBlock(action);
			mActions.put(action,actionBlock);
		}
		actionBlock.setSuccessor(successor);
		successor.addPredecessor(actionBlock);
		
		// Invalidate rPostOrder, if already computed
		mRPostOrder=null;
	}
	
	/**
	 * @param action  an Action
	 * @return        the block representing 'action'
	 */
	public BasicBlock addAction(ActionNode action) {
		ActionBlock actionBlock=mActions.get(action);
		if (actionBlock==null) {
			actionBlock=new ActionBlock(action);
			mActions.put(action, actionBlock);
			
			// Invalidate rPostOrder, if already computed
			mRPostOrder=null;
		}
		return actionBlock;
	}
	
	/**
	 * Adds a block, which represents a DecisionNode
	 * @param cond        condition
	 * @param trueBlock   block to execute if condition is true (null if blocking)
	 * @param falseBlock  block to execute if condition is false (null if blocking)
	 * @return the block representing the decision (or null if blocking is the outcome in either case)
	 */
	public BasicBlock addDecision(Condition cond, BasicBlock trueBlock, BasicBlock falseBlock) {
		// First check that there are any actions (or the terminal node) reachable from the decision
		if (trueBlock==null && falseBlock==null)
			return null; 
		
		// Use a unique NullNode to represent blocking leaves (null Blocks)
		DecisionTree trueTree=(trueBlock!=null)? trueBlock.getDecisionTree() : mBlockingNullNode;
		DecisionTree falseTree=(falseBlock!=null)? falseBlock.getDecisionTree() : mBlockingNullNode;
		DecisionNode key=new DecisionNode(cond,trueTree,falseTree);
		DecisionBlock result=mDecisions.get(key);
		
		if (result==null) {
			result=new DecisionBlock(key);
			mDecisions.put(key, result);
			if (trueBlock!=null)
				trueBlock.addPredecessor(result);
			if (falseBlock!=null)
				falseBlock.addPredecessor(result);
			
			// Invalidate rPostOrder, if already computed
			mRPostOrder=null;
		}
		
		return result;
	}
	
	/**
	 * @return the unique block, which represents termination of the actor
	 */
	public BasicBlock addTerminalNode() {
		if (mTerminalBlock==null) {
			mTerminalBlock=new TerminalBlock(new NullNode());
			
			// Invalidate rPostOrder, if already computed
			mRPostOrder=null;
		}
		return mTerminalBlock;
	}
	
	/**
	 * @return the nodes of the graph sorted in reverse postorder
	 */
	public List<BasicBlock> rPostOrder() {
		if (mRPostOrder==null) {
			RPostOrderVisitor visitor=new RPostOrderVisitor();
			mRPostOrder=visitor.sort();
		}
		return mRPostOrder;
	}
	
	private int numberOfBasicBlocks() {
		return mActions.size() + mDecisions.size() + (mTerminalBlock!=null? 1 : 0);
	}
	
	private class RPostOrderVisitor {
	
		private List<BasicBlock> mRPostOrder;
		int mLast;
		Set<BasicBlock> mVisited;
		
		List<BasicBlock> sort() {
			mLast=numberOfBasicBlocks();
			mRPostOrder=new ArrayList<BasicBlock>(mLast);
			mVisited=new HashSet<BasicBlock>(mLast);
		
			for (int i=0; i<mLast; ++i)
				mRPostOrder.add(null);
			visit(mInitialBlock);
			
			assert(mLast==0); // All nodes inserted
			return mRPostOrder;
		}
		
		void visit(BasicBlock block) {
			if (mVisited.add(block)) {
				// Visit successors
				for (BasicBlock successor: block.getSuccessors())
					visit(successor);
				mRPostOrder.set(--mLast,block);
				// Use RPO-numbers as block identifiers
				block.setIdentifier(mLast);
			}
		}
	}
	
	@Override
	public String getTagName() {
		return "control-flow-graph";
	}
	
	@Override
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		String result="entry=\""+mInitialBlock.getIdentifier()+"\"";
		if (mTerminalBlock!=null) {
			result += " exit=\""+mTerminalBlock.getIdentifier()+"\"";
		}
		return result;
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return rPostOrder();
	}
	
	protected class DecisionBlock extends BasicBlock {

		private DecisionNode mDecision;
		private TestElement mTestElement;
		
		DecisionBlock(DecisionNode decision) {
			mDecision=decision;
		}
		
		@Override
		public DecisionNode getDecisionTree() {
			return mDecision;
		}

		@Override
		public Kind getKind() {
			return Kind.decisionNode;
		}

		@Override
		public Iterable<BasicBlock> getSuccessors() {
			List<BasicBlock> successors=new ArrayList<BasicBlock>(2);
			BasicBlock s=getNode(mDecision.getChild(true));
			if (s!=null)
				successors.add(s);
			s=getNode(mDecision.getChild(false));
			if (s!=null)
				successors.add(s);
			return successors;
		}

		@Override
		public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
			String result=super.getAttributeDefinitions(formatter);
			BasicBlock s=getNode(mDecision.getChild(true));
			if (s!=null)
				result+=" whenTrue=\""+s.getIdentifier()+"\"";
			s=getNode(mDecision.getChild(false));
			if (s!=null)
				result+=" whenFalse=\""+s.getIdentifier()+"\"";
			return result;
		}
		
		@Override
		public Iterable<? extends XmlElement> getChildren() {
			if (mTestElement==null)
				mTestElement=new TestElement(mDecision.getCondition());
			return Collections.singleton(mTestElement);
		}

		@Override
		void printPhase(XlimPhasePrinterPlugIn blockPrinter) {
			BasicBlock whenTrue=getNode(mDecision.getChild(true));
			BasicBlock whenFalse=getNode(mDecision.getChild(false));
			
			if (whenFalse==null) {
				// No leaves on false branch (this happens when condition is a token-availability test)
				whenTrue.printPhase(blockPrinter);
			}
			else if (whenTrue==null) {
				// No leaves on true branch (this happens when condition is a token-availability test)
				whenFalse.printPhase(blockPrinter);
			}
			else {
				// Proper decision node: leaves on both branches
				
				// Evaluate the condition
				XlimSource source=mDecision.getCondition().getXlimSource();
				blockPrinter.printSource(source);

				// Start of if-module <module kind="if">
				XmlPrinter xmlPrinter=blockPrinter.getPrinter();
				xmlPrinter.println("<module kind=\"if\">");
				xmlPrinter.increaseIndentation();

				// Test-module <module kind="test" decision="$source"/>
				// TODO: source should be renamed
				XmlAttributeFormatter formatter=xmlPrinter.getAttributeFormatter();
				String decisionDef=formatter.getAttributeDefinition("decision", source, source.getUniqueId());
				xmlPrinter.println("<module kind=\"test\" "+decisionDef+"/>");

				// Then-module
				xmlPrinter.println("<module kind=\"then\">");
				xmlPrinter.increaseIndentation();
				blockPrinter.enterScope();
				whenTrue.printPhase(blockPrinter);
				blockPrinter.leaveScope();
				xmlPrinter.decreaseIndentation();
				xmlPrinter.println("</module>");

				// Else-module
				xmlPrinter.println("<module kind=\"else\">");
				xmlPrinter.increaseIndentation();
				blockPrinter.enterScope();
				getNode(mDecision.getChild(false)).printPhase(blockPrinter);
				blockPrinter.leaveScope();
				xmlPrinter.decreaseIndentation();
				xmlPrinter.println("</module>");

				// End of if-module </module>
				xmlPrinter.decreaseIndentation();
				xmlPrinter.println("</module>");
			}
		}
	}
	
	protected class ActionBlock extends BasicBlock {
		
		private ActionNode mAction;
		private BasicBlock mSuccessor;
		
		ActionBlock(ActionNode action) {
			mAction=action;
		}
		
		
		@Override
		public ActionNode getDecisionTree() {
			return mAction;
		}

		@Override
		public Kind getKind() {
			return Kind.actionNode;
		}

		void setSuccessor(BasicBlock successor) {
			// Assume that we do this once
			// (perhaps we can allow for changing successors?)
			assert(mSuccessor==null);
			mSuccessor=successor;
		}
		
		BasicBlock getSuccessor() {
			assert(mSuccessor!=null);
			return mSuccessor;
		}
		
		@Override
		public Iterable<? extends BasicBlock> getSuccessors() {
			assert(mSuccessor!=null);
			return Collections.singleton(mSuccessor);
		}

		@Override
		public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
			String result=super.getAttributeDefinitions(formatter);
			if (mSuccessor!=null)
				result+=" next=\""+mSuccessor.getIdentifier()+"\"";
			return result;
		}
		
		@Override
		public Iterable<? extends XlimBlockElement> getChildren() {
			return mAction.getChildren();
		}	
		
		@Override
		void printPhase(XlimPhasePrinterPlugIn printer) {
			for (XlimBlockElement element: getChildren()) {
				printer.printBlockElement(element);
			}
		}
	}
	
	protected class TerminalBlock extends BasicBlock {
		
		private NullNode mNullNode;
		
		TerminalBlock(NullNode nullNode) {
			mNullNode=nullNode;
		}

		@Override
		public NullNode getDecisionTree() {
			return mNullNode;
		}

		@Override
		public Iterable<? extends BasicBlock> getSuccessors() {
			return Collections.emptyList();
		}
		
		@Override
		public Kind getKind() {
			return Kind.terminalNode;
		}

		public Iterable<? extends XmlElement> getChildren() {
			return Collections.emptyList();
		}
		
		@Override
		void printPhase(XlimPhasePrinterPlugIn printer) {
			// TODO: the actual output goes here!
			printer.getPrinter().printComment("Here should be the XLIM of a TerminalNode");
		}
	}
}
