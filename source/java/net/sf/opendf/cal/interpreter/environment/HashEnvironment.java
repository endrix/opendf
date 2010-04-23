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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.sf.opendf.cal.interpreter.InterpreterException;
import net.sf.opendf.cal.interpreter.UndefinedVariableException;

/**
 * This class implements the <tt>Environment</tt> interface based on <tt>java.util.HashMap</tt> objects.
 * The local environment frame is stored as a HashMap.
 *
 * @author Jorn W. Janneck <janneck@eecs.berkeley.edu>
 */

public class HashEnvironment extends AbstractEnvironment implements Environment {

    public Object get(Object variable) {
        if (localFrame.containsKey(variable)) {
            Object v = localFrame.get(variable);
            if (v instanceof Environment.VariableContainer) {
                return ((Environment.VariableContainer)v).value();
            } else {
                return v;
            }
        } else {
            if (parent != null)
                return parent.get(variable);
            else
            {
                // Logging.user().warning("Undefined variable: '" + variable + "' in environment");
                // Logging.dbg().info(this.toString());
                throw new InterpreterException("Undefined variable: '" + variable + "' in environment.");
            }
        }
    }

    public void set(Object variable, Object value) {
        if (localFrame.containsKey(variable)) {
            Object v = localFrame.get(variable);
            if (v instanceof Environment.StateVariableContainer) {
                ((Environment.StateVariableContainer)v).setValue(value);
            } else {
                localFrame.put(variable, value);
            }
        } else if (parent != null) {
            parent.set(variable, value);
        } else {
            throw new UndefinedVariableException("Cannot set undefined variable '" + variable + "'.");
        }
    }

    public void bind(Object variable, Object value) {
        localFrame.put(variable, value);
    }

    public Set localVars() {
        return localFrame.keySet();
    }

    public boolean isLocalVar(Object variable) {
        return localFrame.containsKey(variable);
    }

    public Map localBindings() {
        return new HashMap(localFrame);
    }

    public Environment newFrame(Environment parent) {
        return new HashEnvironment(parent, dsm);
    }

    public void freezeLocal() {
        for (Iterator i = localFrame.keySet().iterator(); i.hasNext(); ) {
            Object v = localFrame.get(i.next());
            if (v instanceof VariableContainer) {
                ((VariableContainer)v).freeze();
            }
        }
    }

    public HashEnvironment(DataStructureManipulator dsm) {
        this(null, dsm);
    }

    public HashEnvironment(Environment parent, DataStructureManipulator dsm) {
        super(parent, dsm);
        localFrame = new HashMap();
    }

    public String toString() {
        return "[" + localFrame.toString() +
                ((parent == null) ? "" : " >> " + parent.toString()) + "]";
    }

    protected Map         localFrame;
}
