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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimTopLevelPort;

public class ParallelNode extends DecisionTree {

	ArrayList<DecisionTree> mChildren;
	
	public static DecisionTree create(DecisionTree t1, DecisionTree t2) {
		if (t1==null)
			return t2;
		else if (t2==null)
			return t1;
		else {
			ParallelNode p1;
			if (t1 instanceof ParallelNode) {
				p1=(ParallelNode) t1;
			}
			else {
				p1=new ParallelNode(t1);
			}
			
			p1.add(t2);
			return p1;
		}
	}
	
	private ParallelNode(DecisionTree t1) {
		mChildren=new ArrayList<DecisionTree>();
		mChildren.add(t1);
	}

	private void add(DecisionTree t) {
		if (t instanceof ParallelNode)
			mChildren.addAll(((ParallelNode) t).mChildren);
		else
			mChildren.add(t);
	}
	
	
	@Override
    protected  void decorateNullNodes(Map<XlimTopLevelPort,Integer> ports) {
		for (DecisionTree child: mChildren)
		  child.decorateNullNodes(ports);
	}
	
	@Override
    public void generateBlockingWait() {
		// We have not implemented any scheme for pinWait generation from "mutex" modules
		// (which imply parallel evaluation of guard conditions). Use the modified SSA-
		// generator instead (which uses nested if-then-else constructs instead)
		throw new UnsupportedOperationException("\"mutex\" modules not supported");
    }
	
	/* XmlElement interface */

	@Override
	public String getTagName() {
		return "parallelNode";
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return mChildren;
	}
}
