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


/**
 * @author ecarvon
 *
 * @param <T> Element type
 * 
 * Linkage is used to create intrusive doubly-linked lists and multi-lists
 * (=lists that embed linkage in the list elements). Such lists have advantages 
 * for specific applications. In particular, it is possible to insert/remove
 * given only a reference to the embedding element object.
 * 
 * For other applications non-intrusive containers (as provided by Java Collections) 
 * are preferable.
 * 
 * Use Linkage as base class of the element class T or as a mix-in.
 * 
 * @see IntrusiveList
 */

public abstract class Linkage<T> {

	private Linkage<T> mPrev, mNext;

	public Linkage() {
		mPrev = mNext = this;
	}

	public Linkage<T> getPrevious()   { return mPrev; }
	public Linkage<T> getNext()       { return mNext; }
	public abstract T getElement();

	public final Linkage<T> out() {
		Linkage<T> prev = mPrev;
		mNext.mPrev = mPrev;
		mPrev.mNext = mNext;
		return prev;
	}

	public void precede(Linkage<T> link) {
		out();
		mPrev = link.mPrev;
		mNext = link;
		mPrev.mNext = this;
		mNext.mPrev = this;
	}

	public void follow(Linkage<T> link) {
		precede(link.mNext);
	}
}
