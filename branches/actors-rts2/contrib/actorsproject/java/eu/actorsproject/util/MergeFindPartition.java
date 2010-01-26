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

package eu.actorsproject.util;

import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Implements a partition of disjunct sets. In addition to the usual
 * Set methods the merge and find methods are provided.
 * Note that element removal (remove, removeAll, retainAll, clear)
 * is unsupported.
 */
public class MergeFindPartition<T> extends AbstractSet<T> {

	private Map<T,Block> mBlockMap=new HashMap<T,Block>();
	private IntrusiveList<Block> mBlockList=new IntrusiveList<Block>();
	
	/**
	 * @param element
	 * @return the block of the partition, which 'element' belongs to
	 *         (null if 'element' is not part of the partition)
	 */
	public Block find(T element) {
		Block b=mBlockMap.get(element);
		if (b!=null) {
			Block leader=b.getClassLeader();
			if (leader!=b) {
				// Path compression/allow redundant blocks to be collected
				mBlockMap.put(element, leader);
			}
			return leader;
		}
		else
			return null;
	}
	
	/**
	 * Merges the blocks, to which 'element1' and 'element2' belong
	 * 
	 * @param element1 
	 * @param element2
	 * @return true if the partition was modified
	 */
	public boolean merge(T element1, T element2) {
		Block b1=find(element1);
		Block b2=find(element2);
		mergeClassLeaders(b1,b2);
		return (b1!=b2);
	}
	
	public boolean merge(Block b1, Block b2) {
		b1=b1.getClassLeader();
		b2=b2.getClassLeader();
		mergeClassLeaders(b1,b2);
		return (b1!=b2);
	}
	
	public boolean mergeAll(Iterable<T> elements) {
		boolean changed=false;
		Iterator<T> pElement=elements.iterator();
		
		if (pElement.hasNext()) {
			Block b=find(pElement.next());
			while (pElement.hasNext()) {
				Block b2=find(pElement.next());
				if (b!=b2)
					changed=true;
				b=mergeClassLeaders(b,b2);
			}
		}
		return changed;
	}
	
	/**
	 * @return the blocks (disjunct sets) of the partition
	 */
	public Iterable<Block> getBlocks() {
		return mBlockList.asUnmodifiableList();
	}
	
	@Override
	public boolean add(T element) {
		if (mBlockMap.containsKey(element))
			return false;
		else {
			createBlock(element);
			return true;
		}
	}

	/**
	 * Adds all elements into a single block (adds and merges them)
	 * 
	 * @param elements
	 * @return true if the partition was changed
	 */
	public boolean addBlock(Iterable<T> elements) {
		boolean changed=false;
		Iterator<T> pElement=elements.iterator();
		
		if (pElement.hasNext()) {
			Block b=addBlock(pElement.next());
			while (pElement.hasNext()) {
				Block b2=addBlock(pElement.next());
				if (b!=b2)
					changed=true;
				b=mergeClassLeaders(b,b2);
			}
		}
		return changed;
	}
	
	private Block addBlock(T element) {
		if (mBlockMap.containsKey(element))
			return find(element);
		else
			return createBlock(element);
	}
	
	private Block createBlock(T element) {
		assert(mBlockMap.containsKey(element)==false);
		Block b=new Block(element);
		mBlockMap.put(element, b);
		mBlockList.addLast(b.mLinkage);
		return b;
	}
	
	@Override
	public boolean contains(Object element) {
		return mBlockMap.containsKey(element);
	}

	@Override
	public Iterator<T> iterator() {
		return mBlockMap.keySet().iterator();
	}

	@Override
	public int size() {
		return mBlockMap.size();
	}

	private Block mergeClassLeaders(Block b1, Block b2) {
		assert(b1!=null && b2!=null && b1.mParent==null && b2.mParent==null);
		
		if (b1!=b2) {
			// Let b1 be the larger of the two blocks
			if (b1.size()<b2.size()) {
				Block temp=b1;
				b1=b2;
				b2=temp;
			}

			// Merge b2 into b1
			b1.mergeClassLeaders(b2);
		}
		return b1;
	}
	
	public class Block extends AbstractSet<T> {
		
		private BlockLinkage mLinkage;
		private IntrusiveList<T> mElements;
		private Block mParent;
		private int mSize;
		private int mHashCode;
		
		private Block(T element) {
			mLinkage=new BlockLinkage();
			mParent=null;
			mSize=1;
			mHashCode=element.hashCode();
			mElements=new IntrusiveList<T>();
			mElements.addLast(new ElementLinkage(element));
		}
				
		@Override
		public boolean contains(Object obj) {
			Block b=mBlockMap.get(obj);
			return (b!=null && b.getClassLeader()==getClassLeader());
		}

		@Override
		public boolean equals(Object obj) {
			if (obj==this)
				return true;
			else if (obj instanceof MergeFindPartition<?>.Block) {
				// If comparing to a Block in the same partition, compare the class leaders
				MergeFindPartition.Block b=(MergeFindPartition.Block) obj;
				if (b.myPartition()==myPartition()) {
					return b.getClassLeader()==getClassLeader();
				}
				else if (b.hashCode()!=hashCode()) {
					// We can also leverage on the fast hashCode() of Block
					return false;
				}
			}
			// Otherwise use the default implementation in AbstractSet
			return super.equals(obj);
		}
		
		@Override
		public int hashCode() {
			return getClassLeader().mHashCode;
		}
		
		private MergeFindPartition<T> myPartition() {
			return MergeFindPartition.this;
		}
		
		@Override
		public Iterator<T> iterator() {
			Block leader=getClassLeader();
			return leader.mElements.asUnmodifiableList().iterator();
		}

		@Override
		public int size() {
			return getClassLeader().mSize;
		}

		private Block getClassLeader() {
			if (mParent==null)
				return this;
			else {
				// Path compression
				mParent=mParent.getClassLeader();
				return mParent;
			}
		}
		
		private void mergeClassLeaders(Block b) {
			assert(b.mParent==null && mParent==null && b!=this);
			b.mParent=this;
			// Move all elements of the other block to this block
			mElements.moveAll(b.mElements);
			mSize+=b.mSize;
			// hash code is the sum of the element's hash codes
			mHashCode+=b.mHashCode;
			// Remove the other block from the list of blocks
			b.mLinkage.out();
		}
		
		private class BlockLinkage extends Linkage<Block> {
			
			@Override
			public Block getElement() {
				return Block.this;
			}
		}
	}
	
	
	private class ElementLinkage extends Linkage<T> {
		private T mElement;
		
		ElementLinkage(T element) {
			mElement=element;
		}
		
		@Override
		public T getElement() {
			return mElement;
		}
	}
}
