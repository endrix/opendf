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

package eu.actorsproject.xlim.implementation;

import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.XlimType;
import java.util.Collections;

class TopLevelPort implements XlimTopLevelPort {

	private String mName;
	private Direction mDirection;
	private XlimType mType;
	
	public TopLevelPort(String name, XlimTopLevelPort.Direction dir, XlimType type) {
		mName = name;
		mDirection = dir;
		mType = type;
	}
	
	@Override
	public Direction getDirection() {
		return mDirection;
	}

	@Override
	public String getSourceName() {
		return mName;
	}

	@Override
	public XlimType getType() {
		return mType;
	}

	@Override
	public void setType(XlimType t) {
		mType=t;
	}
	
	@Override
	public String getAttributeDefinitions() {
		String dir;
		if (mDirection==Direction.internal)
			dir = "";
		else
			dir = "dir=\"" + mDirection + "\" ";
		return "name=\"" + mName +"\" " + dir + mType.getAttributeDefinitions();
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public String getTagName() {
		if (mDirection==Direction.internal)
			return "internal-port";
		else
			return "actor-port";
	}
	
	@Override
	public StateVar isStateVar() {
		return null;
	}
	
	@Override
	public TopLevelPort isPort() {
		return this;
	}
}
