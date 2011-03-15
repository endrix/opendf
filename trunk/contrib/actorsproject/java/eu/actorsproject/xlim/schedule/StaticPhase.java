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

import java.util.ArrayList;
import java.util.Collections;

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.decision2.PortSignature;

/**
 * A collection of actions with identical consumption/production rates ("a phase")
 */
public class StaticPhase implements StaticSubSchedule {

	private PortSignature mPortSignature;
	private BasicBlock mActionSelection;
	
	public StaticPhase(PortSignature portSignature, BasicBlock actionSelection) {
		assert(portSignature!=null);
		mPortSignature=portSignature;
		mActionSelection=actionSelection;
	}
	
	public PortSignature getPortSignature() {
		return mPortSignature;
	}
	
	public BasicBlock getActionSelection() {
		return mActionSelection;
	}

	/*
	 * Implementation of StaticSubSchedule
	 */
	
	@Override
	public int getNumberOfPhases() {
		return 1;
	}

	@Override
	public Iterable<StaticPhase> getPhases() {
		return Collections.singleton(this);
	}

	@Override
	public int getRepeatCount() {
		return 1;
	}
	
	@Override
	public int getNumberOfSubSchedules() {
		return 0;
	}

	@Override
	public Iterable<StaticSubSchedule> getSubSchedules() {
		return Collections.emptyList();
	}


	/*
	 * Implementation of XmlElement
	 */
	
	@Override
	public String getTagName() {
		return "staticPhase";
	}

	@Override
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		return "";
	}

	@Override
	public Iterable<XmlElement> getChildren() {
		ArrayList<XmlElement> children=new ArrayList<XmlElement>();
		for (XmlElement child: mPortSignature.asXmlElements())
			children.add(child);
		children.add(mActionSelection);
		return children;
	}
}
