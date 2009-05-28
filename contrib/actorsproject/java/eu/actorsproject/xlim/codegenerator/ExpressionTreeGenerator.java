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

package eu.actorsproject.xlim.codegenerator;

import eu.actorsproject.xlim.XlimInputPort;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.XlimType;

public interface ExpressionTreeGenerator {

	/**
	 * @param root Root of expression tree
	 * Generates an expression that represents an input port of an operation 
	 */
	void translateSubTree(XlimInputPort root);
		
	/**
	 * @param t
	 * @return Name of type 't' that is used in the target language
	 */
	String getTargetTypeName(XlimType t);
	
	/**
	 * @param s
	 * prints a string to the resulting output stream
	 */
	void print(String s);
	
	/**
	 * @param temp
	 * prints a reference to a "temporary variable" that is allocated for an output port
	 */
	void print(XlimOutputPort port);

	/**
	 * @param port
	 * prints a reference to an actor port or internal port
	 */
	void print(XlimTopLevelPort port);
	
	/**
	 * @param stateVar
	 * prints a reference to a state variable
	 */
	void print(XlimStateVar stateVar);
	
	/**
	 * @param task
	 * prints a reference to a "task" (top-level module)
	 */
	void print(XlimTaskModule task);	
}
