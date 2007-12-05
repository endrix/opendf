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

package net.sf.opendf.cal.interpreter.environment;

/**
 * Implementors of this interface provide access to structured data objects. Specifically, they
 * allow to retrieve information from those objects at specified <em>locations</em>, and to
 * selectively modify structured objects at those locations.
 *
 * @see net.sf.opendf.cal.interpreter.Context
 *
 * @author Jörn W. Janneck <jwj@acm.org>
 */

public interface DataStructureManipulator {

    /**
     * This method implements indexing into structured data objects. The location is specified by a number of
     * indices. This method returns the value at the specified location.
     *
     * @param structure The structured data object.
     * @param location The indices specifying the location.
     * @return The value at the specified location.
     */
    Object  getLocation(Object structure, Object [] location);

    /**
     * This method allows modification of mutable structured data objects. It modifies the object at the
     * specified location by setting that location to the given value.
     *
     * @param structure  The mutable structured data object.
     * @param location   The location given as the indices into the object.
     * @param value      The new value at the location.
     */
    void    setLocation(Object structure, Object [] location, Object value);
}
