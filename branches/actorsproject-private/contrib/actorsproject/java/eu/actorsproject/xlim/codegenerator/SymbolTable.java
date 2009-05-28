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

import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.XlimType;

/**
 * Translation to the names used in the target language for
 * (1) the actor class
 * (2) the input, output and internal ports of the actor
 * (3) the state variables of the actor
 * (4) the tasks (actions and action scheduler)
 * (5) type names
 */
public interface SymbolTable {
	/**
	 * Declares the actor class, its ports, state variables and tasks
	 * @param design XlimDesign element of the actor
	 */
	void declareActorScope(XlimDesign design);
	
	String getTargetActorName();
	
	/**
	 * @param port 
	 * @return the name of 'port', which is used in the target language
	 *         (to be used in declarations)
	 */
	String getTargetName(XlimTopLevelPort port);

	/**
	 * @param stateVar 
	 * @return the name of 'stateVar', which is used in the target language
	 *         (to be used in declarations)
	 */
	String getTargetName(XlimStateVar stateVar);
	
	/**
	 * @param task 
	 * @return the name of 'task', which is used in the target language
	 *         (to be used in declarations)
	 */
	String getTargetName(XlimTaskModule task);
	
	/**
	 * @param temp 
	 * @return the name of 'temp', which is used in the target language
	 *         (to be used in declarations)
	 */
	String getTargetName(TemporaryVariable task);
	
	/**
	 * @param port 
	 * @return a reference to 'port' (to be used in the generated code)
	 */
	String getReference(XlimTopLevelPort port);
	
	/**
	 * @param stateVar 
	 * @return a reference to 'stateVar' (to be used in the generated code)
	 */
	String getReference(XlimStateVar stateVar);
	
	/**
	 * @param task 
	 * @return a reference/call of 'task' (to be used in the generated code)
	 */
	String getReference(XlimTaskModule task);
	
	/**
	 * @param task 
	 * @return a reference of 'temp' (to be used in the generated code)
	 */
	String getReference(TemporaryVariable temp);
	
	/**
	 * @param type an XlimType
	 * @return the type name, which is used in the target language
	 */
	String getTargetTypeName(XlimType type);
}
