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

import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimTopLevelPort;

/**
 * Represents the condition: c1 && c2 && ... && cn
 */
public class Conjunction extends Condition {

	private ArrayList<Condition> mConditions;
	
	public Conjunction(XlimContainerModule container, XlimSource xlimSource) {
		super(container,xlimSource);
		mConditions=new ArrayList<Condition>();
	}
	
	private Conjunction(XlimContainerModule container, 
						XlimSource xlimSource,
						ArrayList<Condition> conditions) {
		super(container,xlimSource);
		mConditions=conditions;
	}
	
	@Override
	public void addTo(Conjunction conjunction) {
		conjunction.mConditions.addAll(mConditions);
	}
	
	protected void add(Condition condition) {
		mConditions.add(condition);
	}
	
	
	@Override
	protected int assertedTokenCount(XlimTopLevelPort port) {
		int result=0;
		for (Condition cond: mConditions) {
			int tokenCount=cond.assertedTokenCount(port);
			if (tokenCount>result)
				result=tokenCount;
		}
		return result;
	}

	@Override
	protected PortMap updateFailedTests(PortMap failedTests) {
		for (Condition cond: mConditions)
			failedTests=cond.updateFailedTests(failedTests);
		return failedTests;
	}
	
	
	@Override
	protected PortMap updateSuccessfulTests(PortMap successfulTests) {	
		for (Condition cond: mConditions)
			successfulTests=cond.updateSuccessfulTests(successfulTests);
		return successfulTests;
	}

	@Override
	public Condition updateCondition(PortMap successfulTests) {
		ArrayList<Condition> updated=new ArrayList<Condition>();
		boolean changed=false;
		
		for (Condition cond: mConditions) {
			// 
			Condition newCondition=cond.updateCondition(successfulTests);
			if (newCondition.alwaysTrue())
				changed=true; // skip "cond"
			else {
				updated.add(newCondition);
				if (newCondition!=cond)
					changed=true;
			}
		}
		
		if (changed) {
			if (updated.size()>=2) {
				// Update conjunction: use simpler condition
				ArrayList<XlimSource> inputs=new ArrayList<XlimSource>(updated.size());
				for (Condition cond: updated)
					inputs.add(cond.getXlimSource());

				XlimContainerModule container=getXlimContainer();
				container.startPatchAtEnd();
				XlimOperation op=container.addOperation("$and", inputs);
			    XlimSource decision=op.getOutputPort(0);
			    container.completePatchAndFixup();
			    
			    return new Conjunction(container,decision,updated);
			}
			else if (updated.isEmpty()) {
				// Create an always true condition
				return makeAlwaysTrue();
			}
			else {
				// A single condition remains
				return updated.get(0);
			}
		}
		else
			return this;
	}
    
	@Override
	public String getTagName() {
		return "conjunction";
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return mConditions;
	}
}
