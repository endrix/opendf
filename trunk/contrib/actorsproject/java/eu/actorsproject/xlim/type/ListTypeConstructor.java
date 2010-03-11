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

import eu.actorsproject.util.Pair;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.XlimTypeArgument;

/**
 * @author ecarvon
 *
 */
public class ListTypeConstructor extends ParametricTypeKind {

	public ListTypeConstructor() {
		super("List");
	}

	@Override
	protected Object getParameter(List<XlimTypeArgument> typeArgList) {
		if (typeArgList.size()==2) {
			XlimType type=null;
			Integer size=null;
			for (XlimTypeArgument arg: typeArgList) {
				String name=arg.getName();
				if (name.equals("type")) {
					type=arg.getType();
				}
				else if (name.equals("size")) {
					size=Integer.valueOf(arg.getValue());
				}
				else
					throw new IllegalArgumentException("Unexpected parameter \""+name+"\" to type List");
			}
			
			if (type!=null && size!=null)
				return new Pair<XlimType,Integer>(type,size);
		}
		
		throw new IllegalArgumentException("Type \"List\" requires parameters \"type\" and \"size\"");
	}

	@Override
	protected XlimType create(Object typeParameter) {
		Pair pair=(Pair) typeParameter; 
		XlimType type=(XlimType) pair.getFirst();
		Integer size=(Integer) pair.getSecond();
		
		return new ListType(this, type, size);
	}
	
	@Override
	XlimType createLub(XlimType t1, XlimType t2) {
		assert(t1==t2);
		return t1;
	}
}
