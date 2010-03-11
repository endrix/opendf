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

import eu.actorsproject.xlim.dependence.Location;

/**
 * @author ecarvon
 * An XlimSource object represents an output port or a state variable, 
 * both of which may constitute the source of an input port
 */
public interface XlimSource {
	/**
	 * @return unique identifier (no connection to source)
	 */
	String getUniqueId();
	
	/**
	 * @return type of the state variable or output port
	 */
	XlimType getType();
	
	
	/**
	 * @return true iff the source is associated with a location
	 * 
	 * This is the case for output ports that represent local aggregates
	 * and for all state variables (but not for scalar output ports)
	 */
	boolean hasLocation();
	
	/**
	 * @return the location that is associated with the source (possibly null)
	 */
	Location getLocation();
	
	/**
	 * @return state variable (or null if not a state variable)
	 */
	XlimStateVar asStateVar();
	
	/**
	 * @return output port (or null if not an output port)
	 */
	XlimOutputPort asOutputPort();
	
	/**
	 * @return a string that describes the location for the purposes of debug printouts
	 *         and diagnostics (neither guaranteed to be unique nor a legal C/CAL idententifier)
	 */
	String getDebugName();
}
