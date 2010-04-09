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

package eu.actorsproject.xlim.decision2;

import java.util.Set;

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.util.XmlElement;


/**
 * A DecisionTree models the scheduling decision of an action scheduler.
 * There are four kinds of nodes (known sub-classes):
 * DecisionNode a two-way decision, depending on a side-effect-free condition.
 * ActionNode   a leaf, which groups side effects (an action)
 * NullNode     an empty leaf (which needs blocking to avoid busy wait)
 */
public abstract class DecisionTree implements XmlElement {

	private int mIdentifier;  // Identifier for print-outs
	
	private static int mNextIdentifier;
	
	public DecisionTree() {
		mIdentifier=mNextIdentifier++;
	}


	public interface Visitor<Result,Arg> {
		Result visitDecisionNode(DecisionNode decision, Arg arg);
		Result visitActionNode(ActionNode action, Arg arg);
		Result visitNullNode(NullNode node, Arg arg);
	}
	
	public abstract <Result,Arg> Result accept(Visitor<Result,Arg> visitor, Arg arg);
	
	
	/**
	 * @return the port signature (consumption/production rates) that is common
	 *         to all leaf-nodes of the tree.
	 *         
	 * The port signature indicates the port (and number of tokens) that can be
	 * read/written using blocking operations, since all action nodes in the tree
	 * require them). A possible NullNode that indicates termination of the actor
	 * makes the common port signature empty.
	 */
	public abstract PortSignature requiredPortSignature();
	
	/**
	 * @return the port signature (consumption/production rates) that is shared
	 *         by all leaf-node of the tree (or null if port signature differ).
	 *         
	 * A non-null mode indicates that the port signature of all possible "next" 
	 * actions, which are reachable from this tree, have identical signatures.
	 */
	public abstract PortSignature getMode();
	
	/**
	 * @return the set of action nodes, which are reachable from this node
	 */
	public abstract Set<ActionNode> reachableActionNodes();
		

	/**
	 * @return true if no actions are reachable from this node
	 */
	public boolean isNullNode() {
		return reachableActionNodes().isEmpty();
	}
	
	/* XmlElement interface */

	public int getIdentifier() {
		return mIdentifier;
	}

	@Override
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		return "ident=\""+getIdentifier()+"\"";
	}
}
