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

import java.util.List;

import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.dependence.CallGraph;
import eu.actorsproject.xlim.util.BagOfTranslationOptions;


public interface XlimDesign extends XmlElement {
	String getName();
	List<? extends XlimTopLevelPort> getInputPorts();
	List<? extends XlimTopLevelPort> getOutputPorts();
	List<? extends XlimTopLevelPort> getInternalPorts();
	List<? extends XlimStateVar> getStateVars();
	List<? extends XlimTaskModule> getTasks();
	
	XlimTaskModule getActionScheduler();
	
	XlimTopLevelPort addTopLevelPort(String name, XlimTopLevelPort.Direction dir, XlimType type);
	XlimStateVar addStateVar(String sourceName, XlimInitValue initValue);
	XlimTaskModule addTask(String kind, String name, boolean autostart);
	
	void removeTopLevelPort(XlimTopLevelPort port);
	void removeStateVar(XlimStateVar stateVar);
	void removeTask(XlimTaskModule task);
	
	CallGraph createCallGraph();
	CallGraph getCallGraph();
    BagOfTranslationOptions getTranslationOptions();
}
