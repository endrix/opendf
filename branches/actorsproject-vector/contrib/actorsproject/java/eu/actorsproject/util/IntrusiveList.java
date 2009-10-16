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

import java.util.Iterator;

public class IntrusiveList<T> extends UnmodifiableList<T> {

	public void clear() {
		mHead.out();
	}
	
	public void addFirst(Linkage<T> link) {
		link.follow(mHead);
	}
	
	public void addLast(Linkage<T> link) {
		link.precede(mHead);
	}

	public void moveAll(IntrusiveList<T> list) {
		mHead.moveAll(list.mHead.getNext(), list.mHead);	
	}
		
	@Override
	public Iterator<T> iterator() {
		return new ForwardIteratorWithRemove();
	}

	@Override
	public Iterator<T> reverseIterator() {
		return new BackwardIteratorWithRemove();
	}
	
	@Override
	public Iterator<T> iterator(Linkage<T> start) {
		return new ForwardIteratorWithRemove(start, mHead);
	}

	@Override
	public Iterator<T> reverseIterator(Linkage<T> start) {
		return new BackwardIteratorWithRemove(start, mHead);
	}
	
	@Override
	public Iterator<T> iterator(Linkage<T> start, Linkage<T> end) {
		return new ForwardIteratorWithRemove(start, end);
	}

	@Override
	public Iterator<T> reverseIterator(Linkage<T> start, Linkage<T> end) {
		return new BackwardIteratorWithRemove(start, end);
	}
	
	@Override
	public Iterable<T> getReverseList() {
		return new Iterable<T>() {
			public Iterator<T> iterator() {
				return new BackwardIteratorWithRemove();
			}
		};
	}
	
	public UnmodifiableList<T> asUnmodifiableList() {
		return new UnmodifiableList<T>(mHead);
	}
	
	protected class ForwardIteratorWithRemove extends ForwardIterator {
		
		public ForwardIteratorWithRemove() {
			super();
		}
		
		public ForwardIteratorWithRemove(Linkage<T> start, Linkage<T> end) {
			super(start,end);
		}
		
		@Override
		public void remove() {
			if (mCurr==mHead)
				throw new IllegalStateException("next() not called");
			else {
				mCurr = mCurr.out();
			}
		}
	}
		
	protected class BackwardIteratorWithRemove extends BackwardIterator {
		public BackwardIteratorWithRemove() {
			super();
		}
		
		public BackwardIteratorWithRemove(Linkage<T> start, Linkage<T> end) {
			super(start,end);
		}
		
		@Override
		public void remove() {
			if (mCurr==mHead)
				throw new IllegalStateException("next() not called");
			else {
				mCurr = mCurr.out();
				mCurr = mCurr.getNext();
			}
		}
	}
}

