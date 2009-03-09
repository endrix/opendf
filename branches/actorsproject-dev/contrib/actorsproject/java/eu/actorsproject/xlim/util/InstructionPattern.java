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

import java.util.List;

import eu.actorsproject.xlim.XlimInstruction;

public class InstructionPattern extends WildcardInstructionPattern {

	private String mKind;
	
	/**
	 * Pattern that matches instructions of the given kind
	 * (any number of input ports matches)
	 * @param kind
	 */
	public InstructionPattern(String kind) {
		super();
		mKind=kind;
	}
	
	/**
	 * Pattern that matches instructions of the given kind
	 * and exactly one input port that matches subPattern
	 * @param kind
	 * @param subPattern
	 */
	public InstructionPattern(String kind, XlimTreePattern subPattern) {
		super(subPattern);
		mKind=kind;
	}
	
	/**
	 * Pattern that matches instructions of the given kind
	 * and exactly two input ports that matches their corresponding subPatterns
	 * @param kind
	 * @param subPattern1
	 * @param subPattern2
	 */
	public InstructionPattern(String kind, XlimTreePattern subPattern1, XlimTreePattern subPattern2) {
		super(subPattern1, subPattern2);
		mKind=kind;
	}
	
	/**
	 * Pattern that matches instructions of the given kind and, whose input ports 
	 * match the corresponding patterns in subPatterns
	 * @param kind
	 * @param subPatterns
	 */
	public InstructionPattern(String kind, List<XlimTreePattern> subPatterns) {
		super(subPatterns);
		mKind=kind;
	}
	
	@Override
	protected boolean matchesAtRoot(XlimInstruction instr) {
		return instr.getKind().equals(mKind);
	}
}
