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

package eu.actorsproject.xlim.io;

import org.w3c.dom.NamedNodeMap;

import eu.actorsproject.xlim.XlimFactory;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimType;

public interface ReaderPlugIn {
	/**
	 * @return factory object for the creation of Xlim elements
	 */
	XlimFactory getFactory();
	
	/**
	 * Sets the attributes of an operation that are given by "attributes" and that are
	 * representable in the particular implementation of XlimOperation
	 * @param op          Operation
	 * @param attributes  DOM representation of attributes
	 * @param context     Reader context, which provides look-up of ports, tasks and sources
	 */
	void setAttributes(XlimOperation op, NamedNodeMap attributes, ReaderContext context);
	
	/**
	 * @param typeName     typeName attribute
	 * @param attributes   DOM representation of possible additional attributes (e.g. "size").
	 * @return             XlimType corresponding to "typeName" and additional attributes
	 */
	XlimType getType(String typeName, NamedNodeMap attributes);
}
