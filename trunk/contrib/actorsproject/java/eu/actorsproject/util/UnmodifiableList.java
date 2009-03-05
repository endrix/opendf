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

public class UnmodifiableList<T> implements Iterable<T> {

	protected Linkage<T> mHead;
	
	public UnmodifiableList(Linkage<T> head) {
		mHead = head;
	}
	
	protected UnmodifiableList() {
		mHead = new ListHead();
	}
	
	protected class ListHead extends Linkage<T> {
		public T getElement() {
			return null;
		}
	}
	
	public boolean isEmpty() {
		return mHead.getNext() == mHead;
	}
	
	public T getFirst() {
		Linkage<T> first = mHead.getNext();
		return (first==mHead)? null : first.getElement();
	}

	public T getLast() {
		Linkage<T> last = mHead.getPrevious();
		return (last==mHead)? null : last.getElement();
	}
	
	public Iterator<T> iterator() {
		return new ForwardIterator();
	}

	public Iterator<T> reverseIterator() {
		return new BackwardIterator();
	}
	
	public Iterator<T> iterator(Linkage<T> start) {
		return new ForwardIterator(start, mHead);
	}

	public Iterator<T> reverseIterator(Linkage<T> start) {
		return new BackwardIterator(start, mHead);
	}
	
	public Iterator<T> iterator(Linkage<T> start, Linkage<T> end) {
		return new ForwardIterator(start, end);
	}

	public Iterator<T> reverseIterator(Linkage<T> start, Linkage<T> end) {
		return new BackwardIterator(start, end);
	}
	
	public Iterable<T> getReverseList() {
		return new Iterable<T>() {
			public Iterator<T> iterator() {
				return new BackwardIterator();
			}
		};
	}
		
	protected class ForwardIterator implements Iterator<T> {
		protected Linkage<T> mCurr;
		protected Linkage<T> mEnd;
		
		public ForwardIterator() {
			mCurr = mHead;
			mEnd = mHead;
		}
		
		public ForwardIterator(Linkage<T> start, Linkage<T> end) {
			mCurr=start.getPrevious();
			mEnd=end;
		}
		
		public boolean hasNext() {
			return mCurr.getNext()!=mEnd;
		}
		
		public T next() {
			mCurr = mCurr.getNext();
			return mCurr.getElement();
		}
		
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
		
	protected class BackwardIterator implements Iterator<T> {
		protected Linkage<T> mCurr;
		protected Linkage<T> mEnd;
		
		public BackwardIterator() {
			mCurr = mHead;
			mEnd = mHead;
		}

		public BackwardIterator(Linkage<T> start, Linkage<T> end) {
			mCurr = start.getNext();
			mEnd = end;
		}
		
		public boolean hasNext() {
			return mCurr.getPrevious()!=mEnd;
		}

		public T next() {
			mCurr = mCurr.getPrevious();
			return mCurr.getElement();
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
