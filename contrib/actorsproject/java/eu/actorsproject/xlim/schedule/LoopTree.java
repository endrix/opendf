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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.actorsproject.util.Pair;
import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.util.XmlElement;

public class LoopTree implements XmlElement {

	private Map<BasicBlock,ConcreteLoop> mLoopMap=new HashMap<BasicBlock,ConcreteLoop>();
	private List<Loop> mAllLoops=new ArrayList<Loop>();
	private List<Region> mTopLevelRegions=new ArrayList<Region>();
	
	public Loop getLoop(BasicBlock b) {
		return mLoopMap.get(b);
	}	

	/**
	 * @return the regions (Loops or BasicBlocks) that are not enclosed in any loop
	 */
	public List<Region> getRootRegions() {
		return mTopLevelRegions;
	}
	
	public List<Loop> getAllLoops() {
		return mAllLoops;
	}
	
	static public LoopTree create(ControlFlowGraph cfg, DominatorTree dom) {
		LoopTree t=new LoopTree();
		
		t.create(cfg.rPostOrder(), dom);
		return t;
	}
	
	
	private void create(List<BasicBlock> rPostOrder, DominatorTree dom) {
		// Visit blocks in postorder (inner-loop-headers before headers of outer loops) 
		// extract natural loops, initialize mLoopMap
		for (int i=rPostOrder.size()-1; i>=0; --i) {
			BasicBlock h=rPostOrder.get(i);
			ConcreteLoop loop=null;
			
			for (BasicBlock pred: h.getPredecessors()) {
				if (dom.dominates(h, pred)) {
					// h is a loop header if it dominates one of its predecessors
					if (loop==null) {
						loop=new ConcreteLoop(h);
						mLoopMap.put(h, loop);
					}
					loop.addBackEdge(pred);
					loop.extractLoop(pred);
				}
			}
		}
		
		// Visit blocks in rPostorder (outer-loop-headers before headers of inner loops)
		// insert blocks and inner loops (sorted) in that order
		for (BasicBlock b: rPostOrder) {
			ConcreteLoop inLoop=mLoopMap.get(b);
			if (inLoop!=null) {
				// Add blocks to their loops (sorted in rPostorder)
				inLoop.addBasicBlock(b);
				
				if (inLoop.getHeader()==b) {
					// Add loops to mAllLoops
					mAllLoops.add(inLoop);
					
					// Add inner loops to their surrounding loops
					ConcreteLoop parent=inLoop.getParent();
					if (parent!=null) {
						parent.addInnerLoop(inLoop);
					}
					else {
						mTopLevelRegions.add(inLoop);
					}
				}
			}
			else {
				mTopLevelRegions.add(b);
			}
				
//			// Extract irreducible loops via cross-edges
//			for (BasicBlock pred: b.getPredecessors()) {
//				if (dom.isIrreducibleCrossEdge(pred,b)) {
//					// idom must dominate both b and its predecessor
//					BasicBlock idom=dom.immediateDominator(b);
//					assert(dom.dominates(idom, pred));
//
//					extractIrreducibleLoop(pred, idom);
//				}
//			}
		}
		
		// Identify loop exits
		for (BasicBlock b: rPostOrder) {
			ConcreteLoop bLoop=mLoopMap.get(b);
			
			if (bLoop!=null) {
				for (BasicBlock succ: b.getSuccessors()) {
					ConcreteLoop succLoop=mLoopMap.get(succ);
					ConcreteLoop loop=bLoop;
					
					while (loop!=null && loop.contains(succLoop)==false) {
						loop.addExit(b,succ);
						loop=loop.getParent();
					}
				}
			}
		}
	}

//	void extractIrreducibleLoop(BasicBlock b, BasicBlock dom) {
//		ConcreteLoop inLoop=mLoopMap.get(b);
//		
//		if (inLoop==this) {
//			if (mIrreducibleLoops==null) {
//				mIrreducibleLoops=new HashSet<BasicBlock>();
//			}
//			if (mIrreducibleLoops.add(b)==false)
//				return; // Already visited/extracted
//		}
//		else {
//			// Skip to header of inner loops
//			ConcreteLoop parent=inLoop.getParent();
//			while (parent!=this) {
//				inLoop=parent;
//				parent=parent.getParent();
//			}
//			b=inLoop.getHeader();	
//		}
//		
//		// Visit predecessors of block
//		for (BasicBlock pred: b.getPredecessors()) {
//			if (pred!=dom)
//				extractIrreducibleLoop(pred,dom);
//		}
//	}

	@Override
	public String getTagName() {
		return "LoopTree";
	}

	@Override
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		return "";
	}


	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return mTopLevelRegions;
	}

	private class ConcreteLoop extends Loop {

		private ConcreteLoop mParent;
		private List<Region> mChildren=new ArrayList<Region>();
		private List<ConcreteLoop> mInnerLoops=new ArrayList<ConcreteLoop>();
		private List<BasicBlock> mBackEdges=new ArrayList<BasicBlock>();
		private List<Pair<BasicBlock,BasicBlock>> mExits=new ArrayList<Pair<BasicBlock,BasicBlock>>();
		
		ConcreteLoop(BasicBlock header) {
			super(header);
		}
		
		@Override
		public ConcreteLoop getParent() {
			return mParent;
		}

		private void setParent(ConcreteLoop parent) {
			mParent=parent;
		}
		
		@Override
		public List<Region> getChildren() {
			return mChildren;
		}
		
		void addBasicBlock(BasicBlock b) {
			mChildren.add(b);
		}
		
		void addInnerLoop(ConcreteLoop loop) {
			mChildren.add(loop);
			mInnerLoops.add(loop);
		}

		@Override
		public List<ConcreteLoop> getInnerLoops() {
			return mInnerLoops;
		}
		
		@Override
		public boolean contains(BasicBlock b) {
			return contains(getLoop(b));
		}

		@Override
		public List<BasicBlock> getBackEdges() {
			return mBackEdges;
		}

		void addBackEdge(BasicBlock fromBlock) {
			mBackEdges.add(fromBlock);
		}

		@Override
		public List<Pair<BasicBlock, BasicBlock>> getExits() {
			return mExits;
		}

		void addExit(BasicBlock fromBlock, BasicBlock toBlock) {
			assert(contains(fromBlock));
			mExits.add(new Pair<BasicBlock,BasicBlock>(fromBlock, toBlock)); 
		}

		void extractLoop(BasicBlock b) {
			ConcreteLoop inLoop=mLoopMap.get(b);
			
			if (inLoop==null) {
				// add a new block to this loop
				mLoopMap.put(b, this);
			}
			else {
				// Skip to the header if we're in an inner loop
				do { 
					if (inLoop==this)
						return; // Already visited/extracted
					ConcreteLoop parent=inLoop.getParent();
					if (parent==null) {
						inLoop.setParent(this);
						b=inLoop.getHeader();
					}
					inLoop=parent;
				} while (inLoop!=null);
			}
			
			// Predecessors of b are also in loop
			for (BasicBlock pred: b.getPredecessors()) {
				extractLoop(pred);
			}
		}
	}	
}
