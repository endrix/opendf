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

import java.util.Collections;
import java.util.Map;

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimTestModule;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.util.LiteralPattern;

/**
 * A Condition, which is associated with a DecisionNode.
 * The subclass AvailabilityTest represents the special case
 * of token/space availability on in-/out-port
 */
public class AtomicCondition extends Condition {

    private static LiteralPattern sTruePattern = new LiteralPattern(1);

	public AtomicCondition(XlimSource xlimSource, ValueNode value) {
		super(xlimSource, value);
	}
	    
	@Override
    public boolean alwaysTrue() {
    	return sTruePattern.matches(getXlimSource());
    }
    
	/* XmlElement interface */
	
	@Override
	public String getTagName() {
		return "condition";
	}

	@Override
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		XlimSource source=getXlimSource();
		return formatter.getAttributeDefinition("decision",source,source.getUniqueId());
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return Collections.emptyList();
	}

	@Override
	protected void findPinAvail(Map<XlimTopLevelPort, XlimOperation> pinAvailMap) {
		// Nothing to do here...		
	}

	@Override
	protected void updateDominatingTests(Map<XlimTopLevelPort, Integer> testsInAncestors) {
		// Nothing to do here (overridden in AvailabilityTest)
	}

	@Override
	protected Condition removeRedundantTests(PortSignature portSignature, XlimTestModule testModule) {
		if (isImpliedBy(portSignature))
			return null;
		else
			return this;
	}
}