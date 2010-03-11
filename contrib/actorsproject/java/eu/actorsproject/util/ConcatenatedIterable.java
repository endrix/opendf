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

package eu.actorsproject.util;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * An Iteratable object, which iterates over several other iterable objects
 */
public class ConcatenatedIterable<T> implements Iterable<T> {

	private Iterable<Iterable<? extends T>> mConcatenation;
	
	public ConcatenatedIterable(Iterable<Iterable<? extends T>> concatenation) {
		mConcatenation=concatenation;
	}

	public ConcatenatedIterable(Iterable<? extends T> c1, Iterable<? extends T> c2) {
		ArrayList<Iterable<? extends T>> list=new ArrayList<Iterable<? extends T>>(2);
		list.add(c1);
		list.add(c2);
		mConcatenation=list;
	}
	
	public ConcatenatedIterable(Iterable<? extends T> c1, 
			                    Iterable<? extends T> c2,
			                    Iterable<? extends T> c3) {
		ArrayList<Iterable<? extends T>> list=new ArrayList<Iterable<? extends T>>(2);
		list.add(c1);
		list.add(c2);
		list.add(c3);
		mConcatenation=list;
	}

	public Iterator<T> iterator() {
		return new ConcatIterator();
	}
	
	class ConcatIterator implements Iterator<T> {

		private Iterator<Iterable<? extends T>> mOuter=mConcatenation.iterator();
		private Iterator<? extends T> mInner;
		
		@Override
		public boolean hasNext() {
			if (mInner!=null && mInner.hasNext())
				return true;
			else {
				mInner=advance();
				return mInner!=null;
			}
		}

		public T next() {
			if (mInner.hasNext()==false)
				mInner=advance();
			return mInner.next();
		}

		private Iterator<? extends T> advance() {
			while (mOuter.hasNext()) {
				Iterator<? extends T> inner=mOuter.next().iterator();
				if (inner.hasNext())
					return inner;
			}
			return null;
		}
		
		public void remove() {
			mInner.remove();
		}
	}
}
