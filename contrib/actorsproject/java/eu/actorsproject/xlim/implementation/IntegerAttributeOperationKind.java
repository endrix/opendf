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

import java.util.Collection;
import java.util.List;


import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.io.XlimAttributeList;
import eu.actorsproject.xlim.io.ReaderContext;
import eu.actorsproject.xlim.type.TypeRule;

/**
 * OperationKind of IntegerAttributeOperations ($literal_Integer, $signExtend)
 */
public class IntegerAttributeOperationKind extends OperationKind {
	private String mAttributeName;
	
	public IntegerAttributeOperationKind(String kind, 
			                             TypeRule typeRule,
			                             String attributeName) {
		super(kind,typeRule);
		mAttributeName=attributeName;
	}
	
	@Override
	public Operation create(List<? extends XlimSource> inputs,
			                    List<? extends XlimOutputPort> outputs,
			                    ContainerModule parent) {
		return new IntegerAttributeOperation(this,inputs,outputs,parent);
	}
	
	@Override
	public String getAttributeDefinitions(XlimOperation op, XmlAttributeFormatter formatter) {
		Long value=op.getIntegerValueAttribute();
		if (value!=null)
			return super.getAttributeDefinitions(op, formatter)+" "+mAttributeName+"=\""+value+"\"";
		else
			return super.getAttributeDefinitions(op, formatter);
	}
	
	@Override
	public void setAttributes(XlimOperation op,
			                  XlimAttributeList attributes, 
			                  ReaderContext context) {
		Long value=getRequiredIntegerAttribute(mAttributeName,attributes);
		if (value!=null)
			op.setIntegerValueAttribute(value);
	}		
}

class IntegerAttributeOperation extends Operation {
	private Long mValue;
	
	public IntegerAttributeOperation(OperationKind kind,
			Collection<? extends XlimSource> inputs,
			Collection<? extends XlimOutputPort> outputs,
			ContainerModule parent) {
		super(kind,inputs,outputs,parent);
	}
	
	@Override
	public Long getIntegerValueAttribute() {
		return mValue;
	}
	
	@Override
	public String getValueAttribute() {
		if (mValue!=null)
			return mValue.toString();
		else
			return null;
	}
	
	@Override
	public boolean setIntegerValueAttribute(long value) {
		mValue=value;
		mKind.doDeferredTypecheck(this); // set width (if not done already)
		return true;
	}
	
	@Override
	public boolean setValueAttribute(String value) {
		return setIntegerValueAttribute(Long.valueOf(value));
	}
	
	@Override
	public String attributesToString() {
		return Long.toString(mValue);
	}
}