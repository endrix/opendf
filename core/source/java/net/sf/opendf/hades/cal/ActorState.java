/* 
BEGINCOPYRIGHT X,UC
	
	Copyright (c) 2007, Xilinx Inc.
	Copyright (c) 2003, The Regents of the University of California
	All rights reserved.
	
	Redistribution and use in source and binary forms, 
	with or without modification, are permitted provided 
	that the following conditions are met:
	- Redistributions of source code must retain the above 
	  copyright notice, this list of conditions and the 
	  following disclaimer.
	- Redistributions in binary form must reproduce the 
	  above copyright notice, this list of conditions and 
	  the following disclaimer in the documentation and/or 
	  other materials provided with the distribution.
	- Neither the names of the copyright holders nor the names 
	  of contributors may be used to endorse or promote 
	  products derived from this software without specific 
	  prior written permission.
	
	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
	CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
	INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
	MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
	DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
	CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
	SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
	NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
	LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
	HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
	CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
	OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
	SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
	
ENDCOPYRIGHT
*/

package net.sf.caltrop.hades.cal;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * This class contains the state of an actor in a form that is 
 * separated from (and thus unaffected by changes to) the original 
 * actor state.
 * 
 * The actor interpreter provides methods to extract the actor state 
 * and to define its state based on the specifications inside an
 * ActorState object.
 * 
 * @author jornj
 */
public class ActorState {
	
	public Map  getStateVariables() {
		return stateVariables;
	}
	
	public Set  getSchedulerState() {
		return schedulerState;
	}
	
	public Map  getInputQueues() {
		return inputQueues;
	}
	
	public ActorState(Map stateVariables, Set schedulerState, Map inputQueues) {
		this.stateVariables = Collections.unmodifiableMap(stateVariables);
		this.schedulerState = schedulerState;
		this.inputQueues = Collections.unmodifiableMap(inputQueues);
	}
	
	private Map stateVariables;
	private Set schedulerState;
	private Map inputQueues;

}
