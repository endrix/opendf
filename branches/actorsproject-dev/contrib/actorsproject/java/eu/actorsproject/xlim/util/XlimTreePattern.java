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

import eu.actorsproject.xlim.XlimInstruction;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimStateVar;

public abstract class XlimTreePattern {

	/**
	 * @param root
	 * @return true if this pattern matches at 'root'
	 */
	public boolean matches(XlimSource root) {
		XlimOutputPort output=root.isOutputPort();
		
		if (output!=null) {
			XlimInstruction parent=output.getParent();
			return matchesAtRoot(parent) && matchesSubPatterns(parent);
		}
		else {
			XlimStateVar stateVar=output.isStateVar();
			assert(stateVar!=null);
			
			return matchesStateVar(stateVar);
		}
	}
	
	/**
	 * @param i
	 * @param matchedRoot
	 * @return instruction that matches the i:th subpattern
	 */
	public abstract XlimInstruction getOperand(int i, XlimSource matchedRoot);

	protected abstract XlimInstruction matchedInstruction(XlimSource matchedRoot);
	
	protected abstract boolean matchesAtRoot(XlimInstruction instr);
	protected abstract boolean matchesSubPatterns(XlimInstruction instr);
	protected abstract boolean matchesStateVar(XlimStateVar stateVar);
	
}