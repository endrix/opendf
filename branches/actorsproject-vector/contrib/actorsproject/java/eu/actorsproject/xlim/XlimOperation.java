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

package eu.actorsproject.xlim;

import eu.actorsproject.xlim.dependence.CallSite;

/**
 * @author ecarvon
 *
 */
public interface XlimOperation extends XlimBlockElement, XlimInstruction {		
	
	@Override
	XlimContainerModule getParentModule();
	
	XlimSource getStateVarAttribute();     // target of assign, name of var_ref
	XlimTaskModule getTaskAttribute();   // target of taskCall
	XlimTopLevelPort getPortAttribute(); // portName of pinRead, pinWrite, pinPeek, pinStatus
	Long getIntegerValueAttribute();     // value of $literal_Integer (interpreted as integer)
	String getValueAttribute();          // value of $literal_Integer
	boolean hasBlockingStyle();          // "style" attribute is "blocking"
	Object getGenericAttribute();        // Common/generic getter of "other" attributes
	
	/**
	 * @return DdgCallSite that corresponds to a taskCall 
	 */
	CallSite getCallSite();
	
	/**
	 * @param "target" attribute of assign, "name" attribute of var_ref
	 * @return true if attribute was set (if internal state was updated)
	 */
	boolean setStateVarAttribute(XlimSource stateVar);
	boolean setTaskAttribute(XlimTaskModule task);
	boolean setPortAttribute(XlimTopLevelPort port);
	boolean setIntegerValueAttribute(long value);
	boolean setValueAttribute(String value);
	boolean setBlockingStyle();
	boolean setGenericAttribute(Object value);
}
