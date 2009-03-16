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

package eu.actorsproject.xlim.decision;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimTopLevelPort;

public abstract class DecisionTree implements XmlElement {

	private DecisionTree mParent;
	private int mIdentifier;
	private static int mNextIdentifier;
	
	public DecisionTree() {
		mIdentifier=mNextIdentifier++;
	}
	
	public DecisionTree getParent() {
		return mParent;
	}
	
	public void setParent(DecisionTree parent) {
		mParent=parent;
	}
	
	protected int getIdentifier() {
		return mIdentifier;
	}
	
    /**
     * Decorate the NullNodes of a decision tree with the set of ports that may
     * have been tested for availability tokens (input ports) or space (output ports)
     * and the outcome of that test was failure.
     * This set can be used to formulate blocking conditions (to avoid busy waiting),
     * since the scheduling decision (the path leading to a NullNode) will not change
     * before the availability tests change (more tokens/space become available). 
     */
    public void decorateNullNodes() {
    	Map<XlimTopLevelPort,Integer> ports = new HashMap<XlimTopLevelPort,Integer>();
        decorateNullNodes(ports);
    }
    
    /**
     * Decorates the NullNodes of a decision tree with the set of ports that may
     * have been tested for availability tokens (input ports) or space (output ports)
     * and the outcome of that test was failure.
     * @param ports        ports that have been tested (and failed) on *some* path
     *                     from the root to the decision tree to this node.
     *                     the associated integer is the minimum number of tokens 
     *                     (or empty slots) required for a different outcome of the
     *                     scheduling decision
     */
    protected abstract void decorateNullNodes(Map<XlimTopLevelPort,Integer> ports);
    
    
    /**
     * Alters the underlying XLIM-representation so that it uses blocking wait
     */
    public abstract void generateBlockingWait();
    
	/* XmlElement interface */
	
	@Override
	public String getAttributeDefinitions() {
		return "ident=\""+getIdentifier()+"\"";
	}
}
