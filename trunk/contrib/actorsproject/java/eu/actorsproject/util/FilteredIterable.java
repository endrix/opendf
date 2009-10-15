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

/**
 * An Iterable object, which is based on another Iterable
 * with an added filter
 */
public abstract class FilteredIterable<T> implements Iterable<T> {

	Iterable<? extends T> mUnfiltered;
	
	public FilteredIterable(Iterable<? extends T> unfiltered) {
		mUnfiltered=unfiltered;
	}
	
	public Iterator<T> iterator() {
		return new FilteredIterator();
	}
	
	/**
	 * @param element  an element, which is returned from the iterator
	 * @return         true if the element is to be included in the
	 *                 filtered iteration
	 */
	protected abstract boolean include(T element);
	
	private class FilteredIterator implements Iterator<T> {
	
		private Iterator<? extends T> mUnfilteredIterator;
		private T mLookAhead;
		
		FilteredIterator() {
			mUnfilteredIterator=mUnfiltered.iterator();
			mLookAhead=lookAhead();
		}
		
		T lookAhead() {
			while (mUnfilteredIterator.hasNext()) {
				T next=mUnfilteredIterator.next();
				if (include(next))
					return next;
			}
			return null;
		}
		
		public boolean hasNext() {
			return mLookAhead!=null;
		}

		public T next() {
			T result=mLookAhead;
			mLookAhead=lookAhead();
			return result;
		}

		public void remove() {
			// last "unfiltered" next() is not the last "filtered" next
			throw new UnsupportedOperationException();
		}
	}
}
