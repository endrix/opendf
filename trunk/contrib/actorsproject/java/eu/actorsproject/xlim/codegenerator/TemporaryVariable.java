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

package eu.actorsproject.xlim.codegenerator;

import eu.actorsproject.util.Linkage;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimType;

public class TemporaryVariable extends Linkage<TemporaryVariable>{

	private XlimOutputPort mCreatedFrom;
	private TemporaryVariable mMergedWith;
	private int mLiveRangeStart;
	private int mLiveRangeEnd;
	private int mFirstMutation;
	private int mLastMutation;
	
	public TemporaryVariable(XlimOutputPort port) {
		mCreatedFrom=port;
		mLiveRangeStart=Integer.MAX_VALUE;
		mLiveRangeEnd=Integer.MIN_VALUE;
		mFirstMutation=Integer.MAX_VALUE;
		mLastMutation=Integer.MIN_VALUE;
	}
	
	public TemporaryVariable getElement() {
		return this;
	}
	
	public XlimOutputPort getOutputPort() {
		return mCreatedFrom;
	}
	
	/**
	 * @return the type of the TemporaryVariable
	 */
	public XlimType getType() {
		return mCreatedFrom.getType();
	}

	/**
	 * @return for aggregate (List-) type: the scalar element type,
	 *         for scalars: same as getType()
	 *         
	 * Generalizes to List of List of ... of scalar type
	 */
	public XlimType getElementType() {
		XlimType type=mCreatedFrom.getType();
		while (type.isList()) {
			type=type.getTypeParameter("type");
		}
		return type;
	}
	
	/**
	 * @return for aggregate (List-) type: the total number of scalar elements
	 *         for scalars: one
	 */
	public int getNumberOfElements() {
		int numElements=1;
		XlimType type=mCreatedFrom.getType();
		while (type.isList()) {
			numElements *= type.getIntegerParameter("size");
			type=type.getTypeParameter("type");
		}
		return numElements;
	}
	
	public TemporaryVariable getClassLeader() {
		TemporaryVariable temp=this;
		while (temp.mMergedWith!=null)
			temp=temp.mMergedWith;
		return temp;
	}
	
	public void mergeWith(TemporaryVariable temp) {
		assert(mMergedWith==null);
		temp=temp.getClassLeader();
		
		if (temp!=this) {
			mMergedWith=temp;
		
			// Update live range
			temp.mergeLiveRange(this);
		
			// remove from scope
			out();
		}
		// else: already in same equivalence class
	}
	
	private void mergeLiveRange(TemporaryVariable temp) {
		if (temp.mLiveRangeStart < mLiveRangeStart)
			mLiveRangeStart=temp.mLiveRangeStart;
		
		if (mLiveRangeEnd < temp.mLiveRangeEnd)
			mLiveRangeEnd =temp.mLiveRangeEnd;
		
		if (temp.isMutated()) {
			if (temp.mFirstMutation < mFirstMutation)
				mFirstMutation=temp.mFirstMutation;
			
			if (mLastMutation < temp.mLastMutation)
				mLastMutation =temp.mLastMutation;
		}
	}
	
	/**
	 * @param lrStart  definition point
	 * 
	 * Creates an initial live range [lrStart,lrStart), which can later
	 * be extended (using extendLiveRange) so that the end-point is further away.
	 */
	public void createLiveRange(int lrStart) {
		assert(mLiveRangeStart==Integer.MAX_VALUE);
		mLiveRangeStart=lrStart;
		mLiveRangeEnd=lrStart;
	}
	
	/**
	 * @param newLrEnd
	 * 
	 * Extends the live range to newLrEnd (non-inclusive)
	 * If we already have a later end-point, there's no effect.
	 */
	public void extendLiveRange(int newLrEnd) {
		assert(newLrEnd>=mLiveRangeStart);
		if (newLrEnd>mLiveRangeEnd)
			mLiveRangeEnd=newLrEnd;
	}

	public boolean hasLiveRange() {
		return mLiveRangeStart!=Integer.MAX_VALUE;
	}
	
	public int getLiveRangeStart() {
		return mLiveRangeStart;
	}
	
	public int getLiveRangeEnd() {
		return mLiveRangeEnd;
	}
	public void registerMutation(int atEvent) {
		extendLiveRange(atEvent);
		if (mLastMutation<=atEvent) {
			mLastMutation=atEvent+1;
			if (mFirstMutation==Integer.MAX_VALUE)
				mFirstMutation=atEvent;
		}
	}
	
	public boolean isMutated() {
		return mFirstMutation!=Integer.MAX_VALUE;
	}
	
	public int getFirstMutation() {
		return mFirstMutation;
	}
	
	public int getLastMutation() {
		return mLastMutation;
	}
	
	public boolean mutationOverlaps(TemporaryVariable temp) {
		return isMutated() 
		       && temp.getLiveRangeStart() < mLastMutation 
		       && mFirstMutation < temp.getLiveRangeEnd();
	}
}
