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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.util.XmlElement;

public class DominatorTree implements XmlElement {

	private Map<BasicBlock,Node> mNodes=new HashMap<BasicBlock,Node>();
	private Node mArtificialRoot;
	/**
	 * @param cfg  a control-flow graph
	 * @return the dominator tree of the control-flow graph
	 */
	public DominatorTree(ControlFlowGraph cfg) {
		List<Node> rPostOrder=new ArrayList<Node>();
		
		// Create an artificial root, which dominates all nodes
		// In this way we avoid special cases for the degenerate case of flow-graphs 
		// with unreachable basic blocks (they are still dominated by the root)
		mArtificialRoot=new Node(null,-1);
		
		// Create the nodes of the dominator tree and assign rpo-indices
		for (BasicBlock b: cfg.rPostOrder()) {
			Node n=new Node(b,rPostOrder.size());
			
			// Compute an initial estimation of the immediate dominator (might have to move towards root, later)
			Node idom=null;
			for (BasicBlock predBlock: b.getPredecessors()) {
				Node predNode=mNodes.get(predBlock);
				if (predNode!=null)
					idom = (idom==null)? predNode : leastDominator(idom, predNode);
			}
			if (idom==null)
				idom = mArtificialRoot;  // initial node or unreachable node
			
			n.mIdom = idom;
			rPostOrder.add(n);
			mNodes.put(b, n);
		}

		// Iteratively compute immediate dominators until stable
		boolean stable;
		do {
			stable=true;
			for (Node n: rPostOrder) {
				BasicBlock b=n.mBasicBlock;
				Node idom=n.mIdom;
				
				for (BasicBlock predBlock: b.getPredecessors()) {
					idom = leastDominator(idom,mNodes.get(predBlock));
				}
				
				if (idom!=n.mIdom) {
					stable=false;
					n.mIdom=idom;
				}
			}
		} while (!stable);
	}
	
	
	/**
	 * @param b1  a basic block
	 * @param b2  another basic block
	 * @return true if b1 dominates b2 (a block dominates itself by convention)
	 */
	public boolean dominates(BasicBlock b1, BasicBlock b2) {
		Node n1=mNodes.get(b1);
		Node n2=mNodes.get(b2);
		assert(n1!=null && n2!=null);
		
		return n1.dominates(n2);
	}

	Node leastDominator(Node n1, Node n2) {
		while (n1!=n2) {
			while (n2.mRpoIndex>n1.mRpoIndex) { 
				n2=n2.mIdom;
			}
				
			while (n1.mRpoIndex>n2.mRpoIndex) {
				n1=n1.mIdom;
			}
		}
		
		return n1;
	}
	
	/**
	 * @param b1  a basic block
	 * @param b2  another basic block
	 * @return least common dominator of b1 and b2 (a block dominates itself by convention)
	 */
	public BasicBlock leastDominator(BasicBlock b1, BasicBlock b2) {
		Node n1=mNodes.get(b1);
		Node n2=mNodes.get(b2);
		assert(n1!=null && n2!=null);
		
		return leastDominator(mNodes.get(b1), mNodes.get(b2)).mBasicBlock;
	}
	
	/**
	 * @param b  a basic block
	 * @return immediate dominator of b (null if b is the initial basic block)
	 */
	public BasicBlock immediateDominator(BasicBlock b) {
		return mNodes.get(b).mIdom.mBasicBlock;
	}	
	
	
	@Override
	public String getTagName() {
		return "DominatorTree";
	}

	@Override
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		return "";
	}


	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return mArtificialRoot.getChildren();
	}


	/**
	 * @param src  a Basic block
	 * @param dst  a Basic block
	 * @return true if (src,dst) is an irreducible cross edge
	 * 
	 * An irreducible loop (one in which there is no dominating header)
	 * has at least one cross-edge, such that dst goes before src in rPostorder.
	 */
	public boolean isIrreducibleCrossEdge(BasicBlock src, BasicBlock dst) {
		Node n1=mNodes.get(src);
		Node n2=mNodes.get(dst);
		assert(n1!=null && n2!=null);
		
		// If the destination of the edge (dst/n2) goes before the source (src/n1)
		// in rPostOrder, then  (src,dst) is either a back edge or an irreducible
		// cross edge. If a back edge, then dst dominates src.
		return (n1.mRpoIndex > n2.mRpoIndex && n2.dominates(n1)==false);
	}
	
	private class Node implements XmlElement, Iterable<Node> {
		BasicBlock mBasicBlock;
		Node mIdom;
		int mRpoIndex;
		
		Node(BasicBlock b, int rpoIndex) {
			mBasicBlock=b;
			mIdom=null;
			mRpoIndex=rpoIndex;
		}
		
		boolean dominates(Node n) {
			// n1 dominates n2 implies n1.mRpoIndex <= n2.mRpoIndex
			// By iterating n2 through immediate dominates we either end up at
			// a) n1 (in which case n1 dominates n2)
			// b) some node with lower rpo-index than n1 (n1 doesn't dominates n2)
			while (n.mRpoIndex>mRpoIndex) {
				n=n.mIdom;
			}
			return n==this;
		}
		
		@Override
		public String getTagName() {
			return mBasicBlock.getTagName();
		}

		@Override
		public String getAttributeDefinitions(XmlAttributeFormatter formatter) {	
			return "id=\"" + mBasicBlock.getIdentifier() + "\"";
		}


		@Override
		public Iterable<Node> getChildren() {
			return this;
		}
		
		@Override
		public Iterator<Node> iterator() {
			return new ChildIterator(this);
		}
	}
	
	
	class ChildIterator implements Iterator<Node> {

		Node mIdom;
		Iterator<Node> mIterator;
		Node mLookAhead;
		
		ChildIterator(Node idom) {
			mIdom=idom;
			mIterator=mNodes.values().iterator();
			advance();
		}
	
		@Override
		public boolean hasNext() {
			return mLookAhead!=null;
		}

		@Override
		public Node next() {
			Node result=mLookAhead;
			advance();
			return result;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
		void advance() {
			while (mIterator.hasNext()) {
				mLookAhead=mIterator.next();
				if (mLookAhead.mIdom==mIdom)
					return;
			}
			mLookAhead=null;
		}
	}
}

