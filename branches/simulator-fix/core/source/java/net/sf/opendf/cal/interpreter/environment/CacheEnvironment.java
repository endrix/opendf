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


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.sf.opendf.cal.interpreter.InterpreterException;
import net.sf.opendf.util.logging.Logging;


/**
 * A CacheEnvironment caches the values retrieved through it from its parent environment.
 * This is useful if some environments take significant time to look up values, such as is
 * the case for classloader-based environments.
 *
 *  @author Jorn W. Janneck <janneck@eecs.berkeley.edu>
 */

public class CacheEnvironment extends AbstractEnvironment implements Environment {

    //
    //  implement: Environment
    //

    public Object get(Object variable) {
        if (cache.containsKey(variable)) {
            return cache.get(variable);
        } else {
            Object val = parent.get(variable);
            cache.put(variable, val);
            return val;
        }
    }

    public void bind(Object variable, Object value) {
        parent.bind(variable,  value);
        cache.put(variable, value);
    }

    public void set(Object variable, Object value) {
    	if (!allowWrite) {
	    	Logging.user().warning("Writing to variable '" + variable + "' although it was declared constant. (Value: " + value + ")");
    	}
    	parent.set(variable, value);
    	cache.put(variable, value);
    }
    
    public void set(Object variable, Object [] location, Object value) {
    	if (!allowWrite) {
	    	Logging.user().warning("Modifying a variable '" + variable + "' although it was declared constant. (Value: " + value + ")");
    	}
    	set(variable, location, value);
    }
    

    public void freezeLocal() {
        parent.freezeLocal();
    }

    public Set localVars() {
        return parent.localVars();
    }

    public Environment newFrame(Environment parent) {
        throw new InterpreterException("Cannot create new frame of cache environment.");
    }

    //
    //  CacheEnvironment
    //

    public void  clearCache() {
        cache.clear();
    }

    //
    //  Ctor
    //

    public CacheEnvironment(Environment cachedEnv, DataStructureManipulator dsm) {
        this(cachedEnv, true, dsm);
    }

    public CacheEnvironment(Environment cachedEnv, boolean allowWrite, DataStructureManipulator dsm) {
        super(cachedEnv, dsm);
        this.allowWrite = allowWrite;
    }

    //
    //  private
    //

    private Map         cache = new HashMap();
    private boolean     allowWrite;
}
