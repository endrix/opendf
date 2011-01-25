/* 
 * Copyright (c) Ericsson AB, 2010
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

package eu.actorsproject.xlim.schedule;

import eu.actorsproject.util.XmlPrinter;


/**
 * Represents all possible action schedules of the actor
 */
public abstract class ActionSchedule {

	
	/**
	 * @return true if there is a static action schedule
	 */
	public abstract boolean isStatic();
	
	/**
	 * @return true if the actor may terminate (transition to a state,
	 *         from which no further action scheduling is possible);
	 *         false if there is no terminal state (actor will always
	 *         be able to fire, given sufficient input).
	 */
	public abstract boolean mayTerminate();
	
	/**
	 * @return true if action selection tests for absense of input,
	 *         which *might* lead to a timing-dependent action schedule
	 *         (which in turn might result in timing-dependent behavior).
	 */
	public abstract boolean isTimingDependent();
	
	/**
	 * @return the static action schedule (if one has been found, see isStatic)
	 */
	public abstract StaticActionSchedule getStaticSchedule();
	
	/**
	 * Prints the action schedule in XML format
	 * 
	 * @param printer  where the XML output is generated
	 */
	public void printActionSchedule(XmlPrinter printer) {
		String mayTerminate=" may-terminate="+(mayTerminate()? "\"yes\"" : "\"no\"");
		String kind;
		
		if (isTimingDependent()) {
			kind=" kind=\"timing-dependent\"";
		}
		else if (isStatic()) {
			kind=" kind=\"static\"";	
		}
		else {
			kind=" kind=\"dynamic\"";
		}
		
		printer.println("<actionSchedule"+kind+mayTerminate+">");
		if (isStatic()) {
			printer.increaseIndentation();
			StaticActionSchedule staticSchedule=getStaticSchedule();
			printer.printElement(staticSchedule.asXmlElement());
			printer.decreaseIndentation();
		}
		printer.println("</actionSchedule>");
	}	
}
