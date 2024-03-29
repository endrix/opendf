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

package eu.actorsproject.xlim.dependence;


public interface PhiOperator extends ValueOperator {
	/**
	 * @param path, index of input value (0 or 1)
	 * @return inputs values: from first or second path
	 * path=0 corresponds to "then" in an If-module and the pre-header of a loop.
	 * path=1 corresponds to "else" or value propagated back from the loop body.
	 */
	ValueNode getInputValue(int path);
	
	/**
	 * @param path, index of input value (0 or 1)
	 * @return value usage: of first or second path
	 * path=0 corresponds to "then" in an If-module and the pre-header of a loop.
	 * path=1 corresponds to "else" or value propagated back from the loop body.
	 */
	ValueUsage getUsedValue(int path);
	
	/**
	 * @return the sole output value of a PhiOperator
	 */
	ValueNode getOutput();
	
	/**
	 * @return the value corresponding to the "decision" attribute.
	 */
	ValueNode getControlDependence();
	
	
	/**
	 * @return true iff the PhiOperator belongs to a loop module 
	 *         (and thus may cause a cyclic dependence)
	 */
	boolean inLoop();
}
