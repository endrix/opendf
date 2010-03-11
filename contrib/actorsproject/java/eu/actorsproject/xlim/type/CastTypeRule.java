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

package eu.actorsproject.xlim.type;

import java.util.List;

import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.XlimTypeKind;


/**
 * @author ecarvon
 *
 */
public class CastTypeRule extends TypeRule {

	public CastTypeRule() {
		super(new Signature(new WildCardTypePattern()));
	}

	protected CastTypeRule(Signature signature) {
		super(signature);
	}
	
	@Override
	public int defaultNumberOfOutputs() {
		return 1;
	}

	@Override
	public boolean matchesOutputs(List<? extends XlimOutputPort> outputs) {
		return outputs.size()==1;
	}
	
	@Override
	public XlimType actualOutputType(XlimOperation op, int i) {
		// Not possible to deduce the type, it simply is the type of the output
		return op.getOutputPort(i).getType();
	}

	
	@Override
	public XlimType defaultOutputType(List<? extends XlimSource> inputs, int i) {
		throw new UnsupportedOperationException("CastTypeRule: defaultOutputType");
	}

	@Override
	public boolean typecheck(XlimOperation op) {
		XlimType tOut=op.getOutputPort(0).getType();
		if (tOut!=null) {
			XlimType tIn=op.getInputPort(0).getSource().getType();
			if (tIn!=null)
				return typecheck(tIn, tOut);
			// else: input type required -doesn't typecheck
		}
		// else: output type required -there is no default
		return false;
	}
	
	protected boolean typecheck(XlimType tIn, XlimType tOut) {
		XlimTypeKind kindOut=tOut.getTypeKind();
		// TODO: we should add the necessessary stuff to XlimTypeKind!
		assert(kindOut instanceof TypeKind);
		return ((TypeKind) kindOut).hasConversionFrom(tIn);
	}
}
