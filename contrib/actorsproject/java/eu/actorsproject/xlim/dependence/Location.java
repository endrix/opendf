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

package eu.actorsproject.xlim.dependence;

import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimType;

/**
 * A Location represents the target of a side-effect:
 * an actor port, a state variable or a local aggregate (vector)
 */
public interface Location {

	/**
	 * @return the type of the location
	 * 
	 * This is the as the type of the state variable, the actor port
	 * or the type of the local aggregate, which is represented by the Location
	 */
	XlimType getType();

	/**
	 * @return true iff this Location represents a stateful resource (state variable
	 *         or actor port).
	 */
	boolean isStateLocation();
	
	/**
	 * @return the stateful resource (state variable or actor port), which is 
	 *         represented by this location (or null if there is no such StateLocation)
	 */
	StateLocation asStateLocation();
	
	/**
	 * @return true iff this Location is defined by an XlimSource (state variable or
	 *         output port of an operation).
	 */
	boolean hasSource();
	
	/**
	 * @return the XlimSource that defines the Location  (or null if there is no such 
	 *         source, i.e. the Location is an actor port)
	 */
	XlimSource getSource();
	
	/**
	 * @return a string that describes the location for the purposes of debug printouts
	 *         and diagnostics (neither guaranteed to be unique nor a legal C/CAL idententifier)
	 */
	String getDebugName();
}
