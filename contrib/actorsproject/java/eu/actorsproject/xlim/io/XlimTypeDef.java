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

package eu.actorsproject.xlim.io;

import java.util.Collections;

import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimType;

/**
 * Represents an XLIM typedef element
 * Typedef elements are used only temporarily when reading and writing XLIM files.
 * They are then converted into XlimTypes.
 */
public class XlimTypeDef implements XmlElement {

	private String mName;
	private XlimLocation mLocation;
	private XlimTypeElement mTypeElement;
	private XlimType mType;
	
	private enum State {
		Initial,
		CreatingType,
		TypeCreated
	};
	
	private State mState;
	
	public XlimTypeDef(String name, XlimTypeElement typeElement, XlimLocation location) {
		mName=name;
		mLocation=location;
		mTypeElement=typeElement;
		mState=State.Initial;
	}
	
	public XlimType getType(ReaderPlugIn plugIn, ReaderContext context) {
		if (mState==State.Initial) {
			mState=State.CreatingType;
			if (mTypeElement!=null)
				mType=mTypeElement.createType(plugIn, context);
			mState=State.TypeCreated;
		}
		else if (mState==State.CreatingType) {
			throw new XlimReaderError("Cyclic definition of type "+mName, mLocation);
		}
		// else: type has been created (if erroneous/null it won't help to create it again)
		return mType;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof XlimTypeDef) {
			XlimTypeDef typeDef=(XlimTypeDef) o;
			return (mName.equals(typeDef.mName)
				    && mTypeElement!=null && typeDef.mTypeElement!=null 
				    && mTypeElement.equals(typeDef.mTypeElement));
		}
		else
			return false;
	}
	
	@Override
	public int hashCode() {
		int h=(mTypeElement!=null)? mTypeElement.hashCode() : 0;
		return mName.hashCode() ^ h;
	}
	
	@Override
	public String getTagName() {
		return "typeDef";
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return Collections.singletonList(mTypeElement);
	}

	@Override
	public String getAttributeDefinitions() {
		return "name=\""+mName+"\"";
	}	
}
