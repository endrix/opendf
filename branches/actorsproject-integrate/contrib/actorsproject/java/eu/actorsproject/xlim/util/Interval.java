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

import java.math.BigInteger;


import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.absint.AbstractValue;

/**
 * Interval represents intervals of (long) integers.
 *
 * An Interval is at set of integers -the methods isEmpty(), contains(), overlap() and equals()
 * take the view of an Interval being a set (equals() returns true if its argument equals "this Interval").
 * 
 * An Interval can also be used as the abstract set, in which the values computed by a program can be represented.
 * An array of methods that provides useful abstract functions in this domain is provided by the class: add(), 
 * subtract(), multiply(), divide(), equalsOperator() etc. (unlike equals() equalsOperator() returns either of
 * the "boolean" Intervals [0,1], [0,0] or [1,1]).
 * 
 * The collection of all intervals under the subset inclusion operator, contains(), forms a partially ordered set
 * known as a bounded lattice, with top-element 'universe' and the empty set as bottom-element. Taking this view,
 * union yields the least upper bound and intersection yields the greatest lower bound of two elements (Intervals) 
 * of the lattice.
 */
public class Interval implements Cloneable, AbstractValue<Interval> {

	protected long mLo, mHi;

	// Normalized representation of the empty set
	protected static final long sEmptyLo=Long.MAX_VALUE;
	protected static final long sEmptyHi=Long.MIN_VALUE;
		
	public static final Interval empty=new Interval(sEmptyLo,sEmptyHi);
    public static final Interval universe=new Interval(Long.MIN_VALUE, Long.MAX_VALUE);
    public static final Interval zero=new Interval(0,0);
    public static final Interval one=new Interval(1,1);
    public static final Interval zeroOrOne=new Interval(0,1);
    
	public Interval(long lo, long hi) {
		if (lo>hi) {
			lo=sEmptyLo;
			hi=sEmptyHi;
		}
		mLo=lo;
		mHi=hi;
	}
	
	@Override
	public Interval clone() {
		return new Interval(mLo,mHi);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Interval) {
			return equals((Interval) obj);		
		}
		else
			return false;
	}
	
	public boolean equals(Interval i) {
		return mLo==i.mLo && mHi==i.mHi;
	}
	
	@Override
	public int hashCode() {
		int i1=(int) (mLo & 0xffff);
		int i2=(int) (mLo >> 32);
		int i3=(int) (mHi & 0xffff);
		int i4=(int) (mHi >> 32);
		return i1 + 1031*i2 + 4789*i3 + 7253*i4;
	}

	@Override
	public Interval getAbstractValue() {
		return this;
	}
	
	@Override
	public Interval getNullValue() {
		return empty;
	}

	@Override
	public Interval getUniverse(XlimType type) {
		return new Interval(type.minValue(), type.maxValue());
	}
	
	@Override
	public Interval getAbstractValue(long constant) {
		return new Interval(constant, constant);
	}
	
	@Override
	public Interval union(Interval i) {
		return create(Math.min(mLo,i.mLo), Math.max(mHi,i.mHi));
	}

	public long getLo() {
		return mLo;
	}

	public long getHi() {
		return mHi;
	}
	
	@Override
	public String toString() {
		if (isEmpty())
			return "[empty]";
		else
			return "["+mLo+","+mHi+"]";
	}
	
	public boolean isEmpty() {
		return mHi<mLo;
	}	
	
	public boolean contains(long l) {
		return mLo<=l && l<=mHi;  // emptyInterval -> false
	}
	
	public boolean contains(Interval i) {
		// All Intervals contain the emptyInterval
		// emptyInterval contains only itself
		return mLo<=i.mLo && i.mHi<=mHi;  
	}

	public boolean overlap(Interval i) {
		// emptyInterval overlaps no interval (not even itself)
		return isEmpty()==false && mLo<=i.mHi && i.mLo<=mHi; 
	}	
	
	public Interval intersection(Interval i) {
		return create(Math.min(mLo,i.mLo), Math.max(mHi,i.mHi));
	}
	
	public Interval create(long lo, long hi) {
		if (lo>hi)
			return empty;
		else if (lo!=mLo || hi!=mHi)
			return new Interval(lo,hi);
		else
			return this;
	}
		
	/**
	 * @param i
	 * @return The Interval that is the image of this + i
	 */
	public Interval add(Interval i) {
		if (isEmpty() || i.isEmpty())
			return empty;
		else if (addOverflow(mLo,i.getLo()) || addOverflow(mHi,i.getHi()))
			return universe; 
		else
			return create(mLo+i.getLo(),mHi+i.getHi());
	}

    private static final long halfMax=4611686018427387904L;
	
	private static boolean sameSign(long x, long y) {
		return (x^y)>=0;
	}
	
	private static boolean addOverflow(long x, long y) {
		if (sameSign(x,y)==false
			|| -halfMax<=x && x<halfMax && -halfMax<=y && y<halfMax)
			return false;
		else {
			BigInteger bigX=BigInteger.valueOf(x);
			BigInteger bigY=BigInteger.valueOf(y);
			return bigX.add(bigY).bitLength()>63;			
		}
	}
	
	/**
	 * @param i
	 * @return The Interval that is the image of this - i
	 */
	
	public Interval subtract(Interval i) {
		if (isEmpty() || i.isEmpty())
			return empty;
		else if (subtractOverflow(mLo,i.getHi()) || subtractOverflow(mHi,i.getLo()))
			return universe; 
		else
			return create(mLo-i.getHi(),mHi-i.getLo());
	}

	private static boolean subtractOverflow(long x, long y) {
		if (sameSign(x,y)
			|| -halfMax<=x && x<halfMax && -halfMax<y && y<=halfMax)
			return false;
		else {
			BigInteger bigX=BigInteger.valueOf(x);
			BigInteger bigY=BigInteger.valueOf(y);
			return bigX.subtract(bigY).bitLength()>63;			
		}
	}

	/**
	 * @param i
	 * @return The Interval that is the image of this * i
	 */
	
	public Interval multiply(Interval i) {
		long lo2=i.getLo();
		long hi2=i.getHi();
		if (isEmpty() || i.isEmpty()) {
			return Interval.empty;
		}
		else if (multiplyOverflow(mLo,lo2) || multiplyOverflow(mLo,hi2)
			|| multiplyOverflow(mHi,lo2) || multiplyOverflow(mHi,hi2)) {
			return universe; 
		}
		else {
			return createExtreme(mLo*lo2,mLo*hi2,mHi*lo2,mHi*hi2);
		}
	}
	
	private static final long sqrtMax=3037000499L; /* sqrt(2^63) */

	private static boolean multiplyOverflow(long x, long y) {
		if (-sqrtMax<=x && x<=sqrtMax && -sqrtMax<=y && y<=sqrtMax)
			return false;
		else {
			BigInteger bigX=BigInteger.valueOf(x);
			BigInteger bigY=BigInteger.valueOf(y);
			return bigX.multiply(bigY).bitLength()>63;
		}
	}

	private Interval createExtreme(long x1, long x2, long x3, long x4) {
		long min12,max12;
		if (x1<x2) {
			min12=x1;
			max12=x2;
		}
		else {
			min12=x2;
			max12=x1;
		}
		
		long min34,max34;
		if (x3<x4) {
			min34=x3;
			max34=x4;
		}
		else {
			min34=x4;
			max34=x3;
		}
		return create(Math.min(min12, min34), Math.max(max12, max34));
	}

	/**
	 * @param i
	 * @return The Interval that is the image of this/i
	 */
	public Interval divide(Interval i) {
		// Assuming division by zero is undefined
		long lo2=i.getLo();
		long hi2=i.getHi();
		if (isEmpty() || i.isEmpty() || lo2==0 && hi2==0) {
			// TODO: what is the Cal semantics for division by zero
			// Here, we say that it is "undefined" (empty range)
			return empty;
		}
		if (this.contains(Long.MIN_VALUE) && i.contains(-1)) {
			// Only case of overflow
			return universe;
		}
		else if (lo2>=0) {
			if (lo2==0) {
				// get away with the undefined point at zero
				lo2=1;
				if (hi2==0) hi2=1; // undefined, could return empty interval
			}
			return create(Math.min(mLo/lo2, mLo/hi2), Math.max(mHi/lo2, mHi/hi2));
		}
		else if (hi2<=0) {
			// get away with the undefined point at zero
			if (hi2==0) hi2=-1;
			return create(Math.min(mHi/lo2, mHi/hi2), Math.max(mLo/lo2, mLo/hi2));
		}
		else {
			// lo2<0<hi2, we have +/-1 in the interval i
			return create(Math.min(mLo,-mHi), Math.max(-mLo, mHi));
		}
	}
	
	

	/**
	 * @param i
	 * @return The Interval that is the image of this<<i. Shift-count (in i) is clipped to the range
	 *         [0,63]. In particular this means that shift by negative values is treated as shift by zero
	 *         (identity). Other semantics can be modeled using e.g. shiftRight and mod32. 
	 */
	public Interval shiftLeft(Interval i) {
		if (isEmpty() || i.isEmpty())
			return Interval.empty;
		else {
			// TODO: how does Cal define shifts outside range [0,63]? Or, should it be [0,31]?
			// modulus into the range? Right-shift for negative values?
			int minShift,maxShift;
			if (i.getLo()<0 || i.getHi()>63) {
				minShift=0;
				maxShift=63;
			}
			else {
				minShift=(int) i.getLo();
				maxShift=(int) i.getHi();
			}
			
			long signBit=1L<<(63-maxShift);
			if (isSignExtension(mLo,signBit) && isSignExtension(mHi,signBit))
				return create(Math.min(mLo<<minShift, mLo<<maxShift), Math.max(mHi<<minShift, mHi<<maxShift));
			else
				return universe; // overflow
		}
	}
	
	private static boolean isSignExtension(long value, long signBit) {
		long mask=~(signBit-1);
		value&=mask;
		return (value==0 || value==mask);
	}
	
	
	/**
	 * @param i
	 * @return The Interval that is the image of this>>i. Shift-count (in i) is clipped to the range
	 *         [0,63]. In particular this means that shift by negative values is treated as shift by zero
	 *         (identity). Other semantics can be modeled using e.g. shiftLeft and mod32. 
	 */

	public Interval shiftRight(Interval i) {
		if (isEmpty() || i.isEmpty())
			return empty;
		else {
			// TODO: how does Cal define shifts outside range [0,63]? Or, should it be [0,31]?
			// modulus into the range? Left-shift for negative values?
			int minShift,maxShift;
			if (i.getLo()<0 || i.getHi()>63) {
				minShift=0;
				maxShift=63;
			}
			else {
				minShift=(int) i.getLo();
				maxShift=(int) i.getHi();
			}
			
			return create(Math.min(mLo>>minShift, mLo>>maxShift), Math.max(mHi>>minShift, mHi>>maxShift));
		}
	}
	
	/**
	 * @return The Interval that is the image of this mod {32} a sub-interval of [0,31]
	 *         (useful for Cal shift operators, whose shift-count is modulo 32).
	 */
	public Interval mod32() {
		return zeroExtend(5);
	}
	
	/**
	 * @return The Interval that is the image of -this
	 */
	public Interval negate() {
		if (isEmpty())
			return empty;
		else if (mLo==Long.MIN_VALUE)
			return universe; // Only case of overflow
		else
			return create(-mHi,-mLo);
	}
	
	/**
	 * @return The Interval that is the image of ~this (bitwise complement)
	 */
	public Interval not() {
		if (isEmpty())
			return empty;
		else // ~x = -x-1
			return create(-mHi-1,-mLo-1);
	}
	
	/**
	 * @param i
	 * @return The Interval that is the image of this | i (bitwise or)
	 */
	public Interval or(Interval i) {
		// Here, we do the following trick: the bounds of the resulting interval can be determined
		// as the lower (upper) bound of one of the input intervals plus a value that has been
		// derived from the other input interval. We go from OR to ADD by removing bits that are
		// already present in the lower (upper) bound. For this reason, we determine the set:
		// { x - (x & bound) | xlo <= x <= xhi } and take the smallest (largest) element from that set.
		if (isEmpty() || i.isEmpty())
			return empty;
		else
			return create(orLowerBound(i), orUpperBound(i));
	}
	
	private long orLowerBound(Interval i) {
		Interval temp1=this.clone();
		Interval temp2=i.clone();
		temp1.clearBits(i.getLo());
		temp2.clearBits(mLo);
		return Math.min(mLo+temp2.getLo(), i.getLo()+temp1.getLo());
	}
	
	private long orUpperBound(Interval i) {
		Interval temp1=this.clone();
		Interval temp2=i.clone();
		temp1.clearBits(i.getHi());
		temp2.clearBits(mHi);
		return Math.max(mHi+temp2.getHi(), i.getHi()+temp1.getHi());
	}

	private void clearBits(long bits) {
		// The sign-bit must be dealt with separately (clearBit assumes unsigned ranges)
		// We also take the opportunity of processing (possibly long) sequences of ones/zeros 
		// (i.e. sign-extensions) in one go (not necessary -for efficiency only).
		if (bits==-1) {
			// This is a trivial case
			mLo=0;
			mHi=0;
			bits=0; // we're done
		}
		else if (bits<0) {
			// how long the sequence of ones?
			long b=Long.highestOneBit(~bits); // highest zero (we know there is at least one)
			
			// in the same way for bounds mLo and mHi, which is the highest bit 
			// that breaks the sequence of zeros/ones (the sign extension)?
			b=Math.max(b, Long.highestOneBit((mLo<0)? ~mLo : mLo));
			b=Math.max(b, Long.highestOneBit((mHi<0)? ~mHi : mHi));
			
			// After clearing all bits higher than b (they are set in 'bits')
			// we end up with the following maximum value
			long maxValue=2*b-1;
			
			if (mLo<0)
				if (mHi<0) {
					// Bounds are simply mLo (mHi) "less the sign-extension"
					mLo &= maxValue;
					mHi &= maxValue;
				}
				else {
					// We have zero (and -1) in the interval, which gives us
					mLo=0;
					mHi=maxValue;
				}
			// away with the sign-bits
			bits &= maxValue;
		}

		// The rest of the business concerns lower-order bits only
		// (where signedness is not an issue).
		if (bits!=0) {
			long b=Long.highestOneBit(bits);
			clearBit(b);
			bits &= ~b;
			while (bits!=0) {
				// Find next bit
				b>>>= 1;
				while ((b & bits)==0)
					b>>>=1;
			
				clearBit(b);
				bits &= ~b;
			}
		}
	}
	
	/*
	 * Computes the interval of x & ~bit, where x is in this interval 
	 * 'bit' is a single-bit value, the bit to clear, and must not be the sign-bit
	 */
	private void clearBit(long bit) {
		long lo=mLo;
		
		// Compute lower bound
		if ((lo & bit)==bit)
			lo &= ~bit; // the smallest element we can find is mLo "less the bit"
		else {
			// Clearly, lo is in the resulting interval, but is it the lower bound?
			// We may also have the opportunity to "step up" the bit and then remove it
			// This results in a smaller value than mLo, since we also clear all lower order bits
			long evenLower=lo & ~(bit-1);
			if ((evenLower | bit)<=mHi) // Yes, it is in the Interval
				lo=evenLower;
			/* else: mLo is already the lower bound of the resulting interval */
		}
		
		// Compute upper bound
		if ((mHi & bit)==bit) {
			// Clearly, mHi "less the bit" is in the resulting interval, but is it the upper bound?
			// We may also have the opportunity to "step down" and end up with all one:s in lower
			// order bits.
			long evenHigher=(mHi & ~(bit-1))-1;
			if (evenHigher>=mLo)
				mHi=evenHigher;
			else
				mHi &= ~bit;
		}
		/* else: mHi is already the upper bound of the resulting interval */
		
		mLo=lo;
	}

	/**
	 * @param i
	 * @return The Interval that is the image of this & i (bitwise and)
	 */
	public Interval and(Interval i) {
		if (isEmpty() || i.isEmpty())
			return empty;
		else {
			// x and y = ~(~x or ~y)
			Interval temp1=this.not();
			Interval temp2=i.not();
			
			temp1=temp1.or(temp2);
			return temp1.not();
		}
	}
	
	/**
	 * @param i
	 * @return The Interval that is the image of this ^ i (bitwise xor)
	 */
	public Interval xor(Interval i) {
		if (isEmpty() || i.isEmpty())
			return empty;
		else if (mLo<0 && mHi>=0 && i.getLo()<0 && i.getHi()>=0) {
			// Here the sign-bit messes things up
			Interval positive=new Interval(0,mHi);
			Interval negative=new Interval(mLo,-1);
			positive=positive.xor(i);
			negative=negative.xor(i);
			return create(Math.min(positive.getLo(), negative.getLo()),
				          Math.max(positive.getHi(), negative.getHi()));
		}
		else {
			// x xor y = (x and ~y) or (~x and y) = (~(~x or y)) or ~(x or ~y)
			Interval temp1=this.not();
			temp1=temp1.or(i);
			temp1=temp1.not();
			
			Interval temp2=i.not();
			temp2=temp2.or(this);
			temp2=temp2.not();
			
			return temp1.or(temp2);
		}
	}
	
	/**
	 * Sign extends interval from a given bit number, by which the interval is
	 * effectively restricted to [-2^(fromBit-1), 2^(fromBit-1) - 1].
	 * @param fromBit sign-bit to extend into higher-order bits
	 * @return true if interval changed
	 */

	public Interval signExtend(int fromBit) {
		if (isEmpty() || fromBit>=63)
			return this;
		else {
		    long signBit=1L<<fromBit;
		    long lo,hi;
		    
		    // Compute the multiple of 2*signBit, to which mLo (mHi) belongs
		    // that is mLo = 2*signBit*loM + signedRemainder
		    // loM is in the range [-2^(N-2-fromBit),+2^(N-2-fromBit)]
		    long loM=(mLo<0)? (mLo+signBit)>>(fromBit+1) : (mLo+signBit)>>>(fromBit+1);
			long hiM=(mHi<0)? (mHi+signBit)>>(fromBit+1) : (mHi+signBit)>>>(fromBit+1);
			if (loM<hiM) {
				// range spans at least one "discontinuity", so we have the entire range
				hi=signBit-1; // a value like 0..07fff..f
				lo=~hi;       // a value like f..f8000..0
			}
			else {
				// mLo and mHi are in the same multiple
			    long mask=2*signBit-1; // a value like 0..0ffff..f
				lo=((mLo & signBit)!=0)? (mLo | ~mask) : (mLo & mask);
				hi=((mHi & signBit)!=0)? (mHi | ~mask) : (mHi & mask);
			}
			return create(lo,hi);
		}
	}

	/**
	 * Zero extends interval from a given width (clears bit 'fromWidth' and 
	 * higher-order bits, by which the interval is restricted to [0, 2^fromWidth - 1].
	 * @param fromWidth width of unsigned integer, that is lowest bit to clear
	 * @return true if interval changed
	 */
	
	
	public Interval zeroExtend(int fromWidth) {
		if (isEmpty() || fromWidth>=64)
			return this;
		else {
		    long maxValue=(1L<<fromWidth)-1; // a value like 0..0ffff...
		    
		    if ((mLo & ~maxValue)<(mHi & ~maxValue)) {
		    	// range spans at least one "discontinuity", so we have the entire range
		    	return create(0,maxValue);
		    }
		    else {
		    	// end points in same multiple of (maxValue+1)
		    	return create(mLo & maxValue, mHi & maxValue);
		    }
		}
	}
	
	/**
	 * @return The Interval that is the image of !this (same as this == {0})
	 * 	       Returns a "boolean" interval: [0,1], [0,0] or [1,1]
	 */
	public Interval logicalComplement() {
		return equalsOperator(zero);
	}
	
	public Interval equalsOperator(Interval i) {
		if (isEmpty() || i.isEmpty())
			return empty;
		else if (this.overlap(i)) { 
			// The intervals overlap, they may be equal
			// If they are the same singleton interval, they are always equal
			if (mLo==mHi && i.getLo()==i.getHi())
				return one;       // true
			else
				return zeroOrOne; // false or true
		}
		else
			return zero; // false
	}
	
	public Interval lessThanOperator(Interval i) {
		if (isEmpty() || i.isEmpty())
			return empty;
		else if (mLo<i.getHi()) {
			// True (1) is in the output interval
			// Are all values in this interval smaller?
			if (mHi<i.getLo())
				return one;       // true
			else
				return zeroOrOne; // false or true
		}
		else
			return zero; // false
	}
}
