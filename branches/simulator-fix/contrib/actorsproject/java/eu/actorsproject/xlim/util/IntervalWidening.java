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

package eu.actorsproject.xlim.util;

import java.util.TreeSet;

import eu.actorsproject.xlim.absint.WideningOperator;
import eu.actorsproject.xlim.dependence.PhiOperator;


public class IntervalWidening implements WideningOperator<Interval> {

	private TreeSet<Long> mPoints;
	
	public IntervalWidening() {
		mPoints=new TreeSet<Long>();
		mPoints.add(Long.MIN_VALUE);
	}
	
	@Override
	public Interval widen(Interval abstractValue, PhiOperator phi) {
		if (abstractValue.isEmpty())
			return Interval.empty;
		else {
			long lo=abstractValue.getLo();
			long hi=abstractValue.getHi();
			if (lo==hi) {
				// Here, we avoid widening of singleton intervals [x,x],
				// by which we essentially do constant folding/constant propagation
				// If proven wrong (e.g. [x,x+1] next iteration) we will widen then.
				return abstractValue;
			}
			else {
				Long wHi=mPoints.higher(hi);
				if (wHi==null)
					hi=Long.MAX_VALUE;
				else
					hi=wHi-1;
				lo=mPoints.floor(lo);
				
				// No need to widen past the bounds of the type
				/*
				XlimOutputPort port=phi.getOutputPort(0);
				XlimType type=port.getType();
				lo=Math.max(lo,type.minValue());
				hi=Math.min(hi,type.maxValue());
				*/
				return abstractValue.create(lo,hi);
			}
		}
	}

	/**
	 * Adds two points: x and x+1 so that at least the intervals
	 * (-infinity,x-1], [x,x] and [x+1, infinity) are distinguishable after
	 * widening.
	 * @param x
	 */
	public void addConstant(long x) {
		addStartPoint(x);
		addEndPoint(x);
	}
	
	/**
	 * Adds one point (x) so that at least the intervals (-infinity,x-1] and 
	 * [x,+infinity) are distinguishable after widening.
	 * @param x
	 */
	public void addStartPoint(long x) {
		mPoints.add(x);
	}
	
	/**
	 * Adds one point (x+1) so that at least the intervals (-infinity,x] and
	 * [x+1,+infinity) are distinguishable after widening.
	 * @param x
	 */
	public void addEndPoint(long x) {
		if (x!=Long.MAX_VALUE)
			mPoints.add(x+1);
	}
	
	public String toString() {
		String result="(-Inf";
		for (long x: mPoints) {
			if (x!=Long.MIN_VALUE) {
				result += "," + (x-1) + "] ";
				result += "[" + x;
			}
		}
		return result + ",+Inf)";
	}
}
