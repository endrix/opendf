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

import java.util.LinkedHashSet;
import java.util.Set;

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimInputPort;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.decision2.Condition;

/**
 * Represents the test that is performed by a DecisionNode
 * in the ControlFlowGraph/ActionSchedule.
 */
public class TestElement implements XmlElement {

	private Condition mCond;
	private Set<XlimOperation> mChildren=new LinkedHashSet<XlimOperation>();
	
	public TestElement(Condition cond) {
		mCond=cond;
		add(cond.getXlimSource());
	}
	
	public Condition getCondition() {
		return mCond;
	}
	
	public XlimSource getXlimSource() {
		return mCond.getXlimSource();
	}
	
	/**
	 * Adds all XlimOperators, which are used when evaluating the condition
	 * 
	 * @param source  XlimSource, which is used in the condition
	 * 
	 * Note: recursion terminates at phi-nodes and references to state variables.
	 * Whereas this works for the side-effect free code that selects action
	 * a more sophisticated machinery would be required in general.
	 */
	private void add(XlimSource source) {
		XlimOutputPort output=source.asOutputPort();
		
		if (output!=null) {
			XlimOperation op=output.getParent().isOperation();
			
			if (op!=null && mChildren.contains(op)==false) {
				// First add inputs
				for (XlimInputPort input: op.getInputPorts())
					add(input.getSource());
				
				// then the operation itself
				mChildren.add(op);
			}
			// else: already there or a phi-node (in loop header)
		}
		// else: a reference to a state variable
	}

	@Override
	public String getTagName() {
		return "test";
	}
	
	@Override
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		XlimSource source=mCond.getXlimSource();
		return formatter.getAttributeDefinition("source", source, source.getUniqueId());
	}

	@Override
	public Iterable<XlimOperation> getChildren() {
		return mChildren;
	}
}
