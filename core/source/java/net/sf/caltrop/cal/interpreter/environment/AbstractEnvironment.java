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

package net.sf.caltrop.cal.interpreter.environment;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sf.caltrop.cal.interpreter.InterpreterException;

/**
 * This is an abstract base class for environments that handles the access to structured objects
 * via a data structure manipulator.
 *
 * @see DataStructureManipulator
 *
 *  @author Jörn W. Janneck <janneck@eecs.berkeley.edu>
 */

public abstract class AbstractEnvironment implements Environment {


    public void set(Object variable, Object [] location, Object value) {
        if (isLocalVar(variable)) {
            Object v = get(variable);
            if (v instanceof Environment.StateVariableContainer) {
                ((Environment.StateVariableContainer)v).setValue(location, value);
            } else {
                dsm.setLocation(v, location, value);
            }
        } else {
            if (parent != null)
                parent.set(variable, location, value);
            else
            {
                // Logging.user().warning("Undefined variable: '" + variable + "' in environment");
                // Logging.dbg().info(this.toString());
                throw new InterpreterException("Undefined variable: '" + variable + "' in environment.");
            }
        }
    }


    public boolean isLocalVar(Object variable) {
        return localVars().contains(variable);
    }

    public Environment newFrame() {
        return this.newFrame(this);
    }

    public Map  localBindings() {
        Map m = new HashMap();
        for (Iterator i = this.localVars().iterator(); i.hasNext(); ) {
            Object k = i.next();
            m.put(k, this.get(k));
        }
        return m;
    }


    public AbstractEnvironment(Environment parent, DataStructureManipulator dsm) {
        this.parent = parent;
        this.dsm = dsm;
    }

    protected Environment               parent;
    protected DataStructureManipulator  dsm;

}
