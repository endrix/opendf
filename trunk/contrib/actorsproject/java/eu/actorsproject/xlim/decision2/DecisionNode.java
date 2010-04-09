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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import eu.actorsproject.util.XmlElement;

/**
 * A DecisionNode selects either of two children, depending on the
 * outcome of an associated (side-effect free) condition.
 */

public class DecisionNode extends DecisionTree {
	private Condition mCondition;
	private DecisionTree mIfTrue, mIfFalse;
	private PortSignature mPortSignature, mMode;
	private Set<ActionNode> mReachableActions;
	
	public DecisionNode(Condition condition,
			            DecisionTree ifTrue,
			            DecisionTree ifFalse) {
		mCondition=condition;
		mIfTrue=ifTrue;
		mIfFalse=ifFalse;
		determinePortSignature();
		mReachableActions=computeReachableActions();
	}
	
	
	/**
	 * @return the condition of the decision node
	 */
	public Condition getCondition() {
		return mCondition;
	}

	/**
	 * @param path the path (true/false) from the test
	 * @return the node in the decision diagram, which corresponds to path
	 */
	public DecisionTree getChild(boolean path) {
		return (path)? mIfTrue : mIfFalse;
	}

	
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof DecisionNode) {
			DecisionNode entry=(DecisionNode) o;
			return mCondition==entry.getCondition()
			       && mIfTrue==entry.getChild(true)
			       && mIfFalse==entry.getChild(false);
		}
		else
			return false;
	}


	@Override
	public int hashCode() {
		int h=mIfTrue.hashCode();
		return (((h<<16) | (h>>>16)) + mIfFalse.hashCode()) ^ mCondition.hashCode();
	}


	@Override
	public <Result, Arg> Result accept(Visitor<Result, Arg> visitor, Arg arg) {
		return visitor.visitDecisionNode(this, arg);
	}


	@Override
	public Set<ActionNode> reachableActionNodes() {
		return mReachableActions;
	}


	@Override
	public PortSignature requiredPortSignature() {
		return mPortSignature;
	}

	
	@Override
	public PortSignature getMode() {
		return mMode;
	}


	private Set<ActionNode> computeReachableActions() {
		// Form the union of true/false branches
		Set<ActionNode> s1=mIfTrue.reachableActionNodes();
		Set<ActionNode> s2=mIfFalse.reachableActionNodes();
		if (s2.containsAll(s1))
			return s2;
		else if (s1.containsAll(s2))
			return s1;
		else {
			Set<ActionNode> s=new LinkedHashSet<ActionNode>();
			s.addAll(s1);
			s.addAll(s2);
			return Collections.unmodifiableSet(s);
		}
	}
	
	private void determinePortSignature() {
		PortSignature s1=mIfTrue.requiredPortSignature();
		
		if (mIfFalse.isNullNode()) {
			// Although this decision might do more than just tests 
			// the availability of tokens (required on true path),
			// it is safe to use the mode/port signature of the true path.
			// The NullNode either means blocking or termination and it
			// does no harm blocking -rather than identifying termination
			// as soon as possible. We do so either when receiving the
			// input(s) or when the actor becomes dead due to it blocking.
			mPortSignature=s1;
			mMode=mIfTrue.getMode();
		}
		else {
			// Mode is a port signature that is shared by both paths
			PortSignature mode1=mIfTrue.getMode();
			PortSignature mode2=mIfFalse.getMode();
			if (mode1!=null && mode2!=null && mode1.equals(mode2))
				mMode=mode1;
			else
				mMode=null;
			
			// Port signature is the "intersection" of both paths
			// (the minimum port rate)
			PortSignature s2=mIfFalse.requiredPortSignature();
			mPortSignature=PortSignature.intersect(s1,s2);
		}
	}
	
	
	/* XmlElement interface */

	@Override
	public String getTagName() {
		return "decisionNode";
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		ArrayList<XmlElement> children=new ArrayList<XmlElement>(3);
		children.add(mCondition);
		children.add(mIfTrue);
		children.add(mIfFalse);
		return children;
	}
}
