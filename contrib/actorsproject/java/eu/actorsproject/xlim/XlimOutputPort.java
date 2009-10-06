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

/**
 * 
 */
package eu.actorsproject.xlim;

import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.dependence.ValueNode;

/**
 * @author ecarvon
 *
 */
public interface XlimOutputPort extends XmlElement, XlimSource {
	XlimType getType();
	void setType(XlimType type);
	XlimInstruction getParent();
	
	/**
	 * @return true if collection of references is non-empty
	 */
	boolean isReferenced();
		
	/**
	 * Substitutes "newPort" for this port in all the references of this port
	 * (after calling this method this port is unreferenced).
	 * @param newPort
	 */
	void substitute(XlimOutputPort newPort);
	
	/**
	 * @return The value that is defined by this output port
	 */
	ValueNode getValue();
	
	/**
	 * @return "Actual" type of this output port.
	 *         
	 * The actual type depends on the operation/phi-node, to which the output 
	 * port belongs, the inputs and attributes of the operation (such as state
	 * variable and actor port). 
	 * 
	 * For integer operations, the actual type may differ from the declared type
	 * of the output (in which case an implicit type conversion is made). 
	 * Example:
	 * integer(size=16)*integer(size=16)->integer(size=32) (=actual type),
	 * but declared type of output is perhaps integer(size=16).
	 * 
	 * The "actual" type may also be narrower than the declared type , for instance:
	 * noop: integer(size=12)->integer(size=12) (=actual type),
	 * but declared type of output is perhaps integer(size=16)
	 */
	XlimType actualOutputType();
}
