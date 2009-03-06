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

import java.util.Collections;
import java.util.List;

import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.type.TypeFactory;

public abstract class TypeRule {

	protected int mNumInputs;
	protected int mNumOutputs;
	
	public TypeRule(int numInputs, int numOutputs) {
		mNumInputs=numInputs;
		mNumOutputs=numOutputs;
	}
	
	public abstract List<XlimType> defaultOutputTypes(List<? extends XlimSource> inputs);
	
	public void checkInputs(List<? extends XlimSource> inputs, String kind) {
		if (inputs.size()!=mNumInputs) {
			throw new IllegalArgumentException(kind+": Unexpected number of input ports: "+inputs.size());
		}
	}
	
	public void checkOutputs(List<? extends XlimOutputPort> outputs, String kind) {
		if (outputs.size()!=mNumOutputs) {
			throw new IllegalArgumentException(kind+": Unexpected number of output ports: "+outputs.size());
		}
	}
	
	protected void failedTypeCheck(XlimType t, String kind) {
		throw new IllegalArgumentException(kind+": Unexpected type: "+t.getTypeName());
	}
	
	protected void checkThatBoolean(XlimType t, String kind) {
		if (!t.isBoolean())
			failedTypeCheck(t, kind);
	}
	
	protected void checkThatInteger(XlimType t, String kind) {
		if (!t.isInteger())
			failedTypeCheck(t, kind);
	}
	
	protected void checkThatIntegerOrBoolean(XlimType t1, XlimType t2, String kind) {
		if (t1.isBoolean())
			checkThatBoolean(t2, kind);
		else if (t1.isInteger())
			checkThatInteger(t2, kind);
		else
			failedTypeCheck(t1, kind);
	}
}
