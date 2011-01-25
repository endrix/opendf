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
package eu.actorsproject.xlim.schedule;

import java.util.List;
import java.util.Map;

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.absint.AbstractDomain;
import eu.actorsproject.xlim.absint.Context;
import eu.actorsproject.xlim.absint.DemandContext;
import eu.actorsproject.xlim.absint.Evaluable;
import eu.actorsproject.xlim.absint.LinearExpression;
import eu.actorsproject.xlim.decision2.ActionNode;
import eu.actorsproject.xlim.decision2.DecisionNode;
import eu.actorsproject.xlim.decision2.DecisionTree;
import eu.actorsproject.xlim.decision2.NullNode;

/**
 * Common representation for Loops and BasicBlocks
 *
 */
public abstract class Region implements XmlElement {

	public boolean isLoop() {
		return asLoop()!=null;
	}
	
	public boolean isBasicBlock() {
		return asBasicBlock()!=null;
	}
	
	/**
	 * @return a Loop if this Region is a loop (null otherwise)
	 */
	public Loop asLoop() {
		return null;
	}
	
	/**
	 * @return a BasicBlock if this Region is a BasicBlock (null otherwise)
	 */
	public BasicBlock asBasicBlock() {
		return null;
	}
	
	/**
	 * @return the BasicBlock that dominates the Region:
	 * 	       either the header of a loop or the basic block itself (if the Region is a singleton BasicBlock).
	 */
	public abstract BasicBlock getHeader();
}
