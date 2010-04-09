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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.actorsproject.xlim.XlimInstruction;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimStateVar;

public class WildcardInstructionPattern extends WildcardPattern {

	private List<XlimTreePattern> mSubPatterns;

	/**
	 * Pattern that matches instructions with any number of input ports
	 * Note: differs from a WildcardPattern in that a state-variable source
	 * is not matched by a WildcardInstructionPattern
	 */
	public WildcardInstructionPattern() {
	}

	/**
	 * Pattern that matches instructions with exactly one input port 
	 * that matches subPattern
	 * @param subPattern
	 */
	public WildcardInstructionPattern(XlimTreePattern subPattern) {
		mSubPatterns=Collections.singletonList(subPattern);
	}
	
	/**
	 * Pattern that matches instructions with exactly two input ports 
	 * that match their corresponding subPatterns
	 * @param subPattern1
	 * @param subPattern2
	 */
	public WildcardInstructionPattern(XlimTreePattern subPattern1, XlimTreePattern subPattern2) {
		mSubPatterns=new ArrayList<XlimTreePattern>(2);
		mSubPatterns.add(subPattern1);
		mSubPatterns.add(subPattern2);
	}
	
	/**
	 * Pattern that matches instructions, whose input ports match the corresponding
	 * patterns in subPatterns
	 * @param subPatterns
	 */
	public WildcardInstructionPattern(List<XlimTreePattern> subPatterns) {
		mSubPatterns=subPatterns;
	}
	
	
	public boolean matches(XlimInstruction instr) {
		return matchesAtRoot(instr) && matchesSubPatterns(instr);
	}
	
	@Override
	protected boolean matchesSubPatterns(XlimInstruction instr) {
		if (mSubPatterns==null)
			return true;
		else {
			if (instr.getNumInputPorts()!=mSubPatterns.size())
				return false; // arity mismatch
			int i=0;
			for (XlimTreePattern subPattern: mSubPatterns) {
				if (!subPattern.matches(instr.getInputPort(i).getSource()))
					return false;
				i++;
			}
			return true;
		}
	}
	
	/**
	 * @param i
	 * @param matchedRoot
	 * @return instruction that matches the i:th subpattern
	 */
	@Override
	public XlimInstruction getOperand(int i, XlimSource matchedRoot) {
		if (mSubPatterns==null)
			throw new UnsupportedOperationException("Pattern has no subpatterns");
		else {
			XlimInstruction instr=matchedInstruction(matchedRoot);
			return mSubPatterns.get(i).matchedInstruction(instr.getInputPort(i).getSource());
		}
	}
	
	public XlimInstruction getOperand(int i, XlimInstruction matchedInstruction) {
		if (mSubPatterns==null)
			throw new UnsupportedOperationException("Pattern has no subpatterns");
		else {
			return mSubPatterns.get(i).matchedInstruction(matchedInstruction.getInputPort(i).getSource());
		}
	}
	
	protected XlimInstruction matchedInstruction(XlimSource matchedRoot) {
		return matchedRoot.asOutputPort().getParent();
	}
	
	@Override
	protected boolean matchesStateVar(XlimStateVar stateVar) {
		return false;
	}
}
