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

package net.sf.opendf.cal.interpreter.util;


import java.util.List;
import java.util.Map;

import net.sf.opendf.cal.interpreter.environment.DataStructureManipulator;


/**
 *  @author Jörn W. Janneck <janneck@eecs.berkeley.edu>
 */

public class DefaultDataStructureManipulator implements DataStructureManipulator {

    public Object getLocation(Object structure, Object[] location) {
        if (structure instanceof Map) {
            if (location.length != 1)
                throw new IllegalArgumentException("Maps expect 1 index, got: " + location.length + ".");
            return ((Map)structure).get(location[0]);
        } else if (structure instanceof List) {
        	Object a = structure;
        	for (int i = 0; i < location.length; i ++) {
        		if (!(a instanceof List))
        			throw new IllegalArgumentException("Expected " + location.length + "-dimensional list structure: " + structure);
        		if (!(location[i] instanceof Integer))
        			throw new IllegalArgumentException("List indices must all be integers: " + location);
        		
        		a = ((List)a).get(((Integer)location[i]).intValue());
        	}
        	return a;
        } else {
            throw new RuntimeException("Unknown data structure: " + structure);
        }
    }

    public void setLocation(Object structure, Object[] location, Object value) {
        if (structure instanceof Map) {
            if (location.length != 1)
                throw new IllegalArgumentException("Maps expect 1 index, got: " + location.length + ".");
            ((Map)structure).put(location[0], value);
        } else if (structure instanceof List) {
        	List a = (List)structure;
        	for (int i = 0; i < location.length - 1; i ++) {
        		if (!(location[i] instanceof Integer))
        			throw new IllegalArgumentException("List indices must all be integers: " + location);
        		
        		Object b = ((List)a).get(((Integer)location[i]).intValue());
        		if (!(b instanceof List))
        			throw new IllegalArgumentException("Expected " + location.length + "-dimensional list structure: " + structure);
        		a = (List)b;
        	}
            a.set(((Integer)location[location.length - 1]).intValue(), value);
        } else {
            throw new RuntimeException("Unknown data structure: " + structure);
        }
    }

}
