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

package eu.actorsproject.xlim.type;

import java.util.ArrayList;
import java.util.List;

import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimType;

/**
 * Represents the signature of an operation: type patterns of input ports
 */
public class Signature {

	protected List<TypePattern> mPatterns;
	
	public Signature(TypePattern p) {
		mPatterns=new ArrayList<TypePattern>();
		mPatterns.add(p);
	}
	
	public Signature(TypePattern p1, TypePattern p2) {
		mPatterns=new ArrayList<TypePattern>();
		mPatterns.add(p1);
		mPatterns.add(p2);
	}
	
	public Signature(TypePattern p1, TypePattern p2, TypePattern p3) {
		mPatterns=new ArrayList<TypePattern>();
		mPatterns.add(p1);
		mPatterns.add(p2);
		mPatterns.add(p3);
	}
	
	public Signature(List<TypePattern> patterns) {
		mPatterns=patterns;
	}

	public boolean matches(List<? extends XlimSource> inputs) {
		if (matchesArity(inputs.size())) {
			int N=inputs.size();
			for (int i=0; i<N; ++i) {
				TypePattern p=getPattern(i);
				XlimType t=inputs.get(i).getType();
				if (p.match(t).matches()==false)
					return false; // mismatch
			}
			return true; // All patterns are matched
		}
		else
			return false;
	}
	
	public boolean betterMatch(Signature s, List<? extends XlimSource> inputs) {
		assert(matches(inputs) && s.matches(inputs));
		
		// A "better" match is signified by having at least one
		// more specific match than the other one.
		boolean thisWinsOne=false;

		int N=inputs.size();
		for (int i=0; i<N; ++i) {
			XlimType t=inputs.get(i).getType();
			TypePattern p1=getPattern(i);
			TypePattern p2=s.getPattern(i);
			int comp=compareMatches(p1, p2, t);
			if (comp>0)
				return false; // p2 matches better than p1
			else if (comp<0)
				thisWinsOne=true;
		}
		return thisWinsOne;
	}
	
	private int compareMatches(TypePattern p1, TypePattern p2, XlimType t) {
		TypePattern.Match m1=p1.match(t);
		TypePattern.Match m2=p2.match(t);
		
		if (m1==m2 && m1==TypePattern.Match.PromotedType) {
			TypeKind kind1=p1.patternTypeKind();
			TypeKind kind2=p2.patternTypeKind();
			
			if (kind1==null || kind2==null) {
				// null means "no maximal TypeKind matched" (e.g. wildcard)
				if (kind2!=null)
					return 1; // m2 is the better match
				else if (kind1!=null)
					return -1; // m1 is the better match
				else
					return 0; // equally good/unordered
			}
			else if (kind1!=kind2) {
				if (kind1.hasPromotionFrom(kind2))
					return 1; // m2 is the petter match
				else if (kind2.hasPromotionFrom(kind1))
					return -1; // m1 is the better match
				else
					return 0; // equally good/unordered
			}
			else
				return 0; // equally good/unordered
		}
		else
			return m1.compareTo(m2);
	}
	
	protected boolean matchesArity(int arity) {
		return mPatterns.size()==arity;
	}
	
	protected TypePattern getPattern(int i) {
		return mPatterns.get(i);
	}	
	
	@Override
	public String toString() {
		if (mPatterns.size()==1)
			return mPatterns.get(0).toString();
		else {
			String result="(";
			String delimiter="";
			for (TypePattern p: mPatterns) {
				result += delimiter + p.toString();
				delimiter=",";
			}
			result += ")";
			return result;
		}
	}
}
