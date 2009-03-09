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

import eu.actorsproject.xlim.dependence.StatePhiOperator;

/**
 * Represents loops and if-modules (that contain phi-nodes)
 */
public interface XlimPhiContainerModule extends XlimModule {
	Iterable<? extends XlimPhiNode> getPhiNodes();
	Iterable<? extends StatePhiOperator> getStatePhiOperators();
	
	@Override
	XlimContainerModule getParentModule();
	
	/**
	 * @return the test module that controls the loop/if-module
	 */
	XlimTestModule getTestModule();

	/**
	 * @param in1 value arriving from the 'then' module or loop pre-header (initial value)
	 * @param in2 value arriving from the 'else' module or loop 'body' module
	 * @param out value representing the joined flow 
	 * @return PHI-node, out = PHI(in1,in2)
	 */
	XlimInstruction addPhiNode(XlimSource in1, XlimSource in2, XlimOutputPort out);
	
	void removePhiNode(XlimInstruction phi);
	
	/**
	 * Gets the predecessor module corresponding to the input ports of the phi-nodes
	 * @param path (0 or 1) meaning then/else or pre-header/loop body, respectively
	 * @return the predecessor on the given path
	 */
	XlimContainerModule predecessorModule(int path);
}
