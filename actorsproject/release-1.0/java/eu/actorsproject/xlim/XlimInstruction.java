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
 * Represents an XLIM PHI-nodes and Operations
 */

package eu.actorsproject.xlim;

import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.dependence.ValueOperator;

public interface XlimInstruction extends XmlElement {
	/**
	 * @return kind-attribute (e.g. "$add"), for phi-nodes: "phi"
	 */
	String getKind();

	/**
	 * @return module, to which this element belongs
	 */
	XlimModule getParentModule();
	
	// Input ports
	Iterable<? extends XlimInputPort> getInputPorts();
	int getNumInputPorts();
	XlimInputPort getInputPort(int n);

	// Output ports
	Iterable<? extends XlimOutputPort> getOutputPorts();
	int getNumOutputPorts();
	XlimOutputPort getOutputPort(int n);
		
	
	XlimOperation isOperation();
	
	/**
	 * @return true if instruction may access (use or modify) state variables or ports,
	 * either by the effect of the instruction itself (e.g. assign, pinPeek) or by
	 * referring to a state variable via at least one input port.
	 */
	boolean mayAccessState();
	
	/**
	 * @return true if the operation modifies state (state variable or top-level port).
	 * This is a property of the operation kind: pinRead, pinWrite, assign and taskCall
	 * may modify state. Note that mayModifyState() implies mayAccessState()
	 */
	boolean mayModifyState();
	
	/**
	 * @return true if the instruction has an output port that is referenced
	 * Note that an instruction can also contribute to the effect of a program by
	 * modifying state; an unreferenced instruction may still provide a useful side-effect.
	 * In particular, there are operations with no output port (e.g. taskCall, pinWrite)
	 * that are never referenced.
	 */
	boolean isReferenced();
	
	/**
	 * @return the value of the "removable" attribute (false if none)
	 * An operation that is not "removable" mustn't be optimized away -it has an effect other than
	 * those exposed by the data dependence graph (e.g. blocking on ports).
	 */
	boolean isRemovable();
	
	/**
	 * @return The node in the data dependence graph, which corresponds to this Instruction
	 */
	ValueOperator getValueOperator();
}
