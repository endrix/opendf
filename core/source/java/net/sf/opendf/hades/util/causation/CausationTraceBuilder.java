/* 
BEGINCOPYRIGHT X
	
	Copyright (c) 2007, Xilinx Inc.
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
	- Neither the name of the copyright holder nor the names 
	  of its contributors may be used to endorse or promote 
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

package net.sf.caltrop.hades.util.causation;

/**
 * A CausationTraceBuilder is an object that allows applications 
 * to create causation traces. A causation trace is a sequence of steps, and
 * each step contains a number of attributes and dependencies. Dependencies in
 * turn may contain attributes.
 * 
 * Steps and dependencies are created using a nested begin/end protocol, i.e. 
 * there are methods to begin and end a step, and between calling those any number of
 * dependencies may be created in a similar manner. Attributes are either assigned to 
 * an "open" step or dependency.
 * 
 * Steps that have not been ended may be canceled, in which case no record of them 
 * will be kept.
 * 
 * Specific implementations of this interface will differ in the way they store and
 * represent the traces. 
 * 
 * @author jwj
 */
public interface CausationTraceBuilder {
	
	/**
	 * Start a trace.
	 */

	public void beginTrace();
	
	/**
	 * End a trace. This method might also perform necessary clean-up and housekeeping
	 * functions.
	 */
	
	public void endTrace();
	
	/**
	 * Create a new step entry.
	 * 
	 * @return New unique step ID.
	 */
	Object  beginStep();

	void  setStepAttribute(String name, Object value);
	Object  getStepAttribute(String name);
	void  removeStepAttribute(String name);

	/**
	 * Return the ID of the current step. The current step is the one 
	 * that was begun most recently.
	 * 
	 * @return The ID of the current step.
	 */
	Object  currentStep();
	
	/**
	 * End the current step. This method must be called after a 
	 *
	 */
	void  endStep();
	
	void  cancelStep();
	
	void  beginDependency(Object stepID);
	
	void  setDependencyAttribute(String name, Object value);
	Object  getDependencyAttribute(String name);
	void  removeDependencyAttribute(String name);
	
	void  endDependency();
}
