/* 
BEGINCOPYRIGHT X
	
	Copyright (c) 2009, Xilinx Inc.
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

package net.sf.opendf.eclipse.debug.model;

/**
 * Interface for events received from the execution engine.
 * 
 * @author Rob Esser
 * @version 20 March 2009
 */
public interface IOpendfEventListener {

	/**
	 * A started event has been received from the execution engine (guaranteed
	 * to be the first event sent)
	 */
	public void handleStartedEvent();

	/**
	 * A terminate event has been received from the execution engine (guaranteed
	 * to be the last event sent)
	 */
	public void handleTerminatedEvent();

	/**
	 * A suspend event has been received from the execution engine
	 * 
	 * suspended N:X - the interpreter has suspended component N and entered
	 * debug mode; X is the cause of the suspension:
	 * 
	 * breakpoint L - a breakpoint at line L was hit client - a client request
	 * to suspend has completed drop - a client request to drop a frame has
	 * completed event E - an error was encountered, where E describes the error
	 * step - a step request has completed watch V A - a watchpoint for variable
	 * V was hit for reason A (read or write), on variable V
	 * 
	 * @param compName
	 *            the name of the component that is suspended
	 * @param event
	 *            the type of the event
	 */
	public void handleSuspendedEvent(String compName, String event);

	/**
	 * A resume event has been received from the execution engine
	 * 
	 * resumed N:X - the interpreter has resumed execution of component N in run
	 * mode; X is the cause of the resume:
	 * 
	 * step - a step request has been initiated client - a client request to
	 * resume has been initiated
	 * 
	 * @param compName
	 *            the name of the component that is suspended
	 * @param event
	 *            the type of the event
	 */
	public void handleResumedEvent(String compName, String event);

	/**
	 * Handle all other events
	 * 
	 * @param event
	 */
	public void handleEvent(String event);

}
