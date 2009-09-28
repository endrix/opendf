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

package eu.actorsproject.xlim.decision;

import java.util.Iterator;

import eu.actorsproject.xlim.XlimTopLevelPort;

/**
 * Data structure that is used to keep track of asserted/refuted
 * token-availability tests. 
 * It is implemented in a non-mutable manner: adding a test 
 * results in a new PortMap and does not affect existing references 
 * to the PortMap.
 * At most a single AvailabilityTest is associated with each port,
 * adding a test replaces a possibly existing association for that
 * port.
 */
public interface PortMap extends Iterable<AvailabilityTest> {
	/**
	 * @param test  test to add
	 * @return      new PortMap, in which the port of "test" replaces
	 *              a possibly existing association for that port
	 */
	PortMap add(AvailabilityTest test);
	
	/**
	 * @param port  port, whose AvailabilityTest is retrieved
	 * @return      AvailabilityTest or null if "port" is not
	 *              associated with any AvailabilityTest in this map.
	 */
	AvailabilityTest get(XlimTopLevelPort port);
	
	/**
	 * Intersects two PortMaps (with respect to the ports) and select
	 * an AvailabilityTest with minimal tokenCount if the PortMaps have
	 * different AvailabilityTests for a particular port.
	 * @param portMap
	 * @return intersection of this PortMap and "portMap"
	 * 
	 * This operation is useful when combining the assertions made on
	 * two paths. 
	 */
	PortMap intersectMin(PortMap portMap);
	
	/**
	 * @param portMap
	 * @return The difference of this PortMap and "portMap"
	 * 
	 * An AvailabilityTest is in the difference if either:
	 * (a) "portMap" has no association for the port of the test, or
	 * (b) the test of this PortMap has a higher token count than
	 *     the corresponding test in "portMap"
	 *     
	 * This operation is useful when finding the assertions made on
	 * one path but not on another one.
	 */
	PortMap diffMax(PortMap portMap);
	
	@Override
	Iterator<AvailabilityTest> iterator();
}
