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

import java.util.Set;

import net.sf.caltrop.cal.interpreter.Context;
import net.sf.caltrop.cal.interpreter.InterpreterException;


/**
 * This class represents an environment with only one variable-value association.
 * 
 * @author jornj
 */
public class SingleEntryEnvironment extends AbstractEnvironment {

    //
    //  implement: Environment
    //

    public Object get(Object variable) {
        if (variableName.equals(variable)) {
        	return value;
        } else {
            if (parent == null)
                throw new InterpreterException("Undefined variable '" + variable + "'.");
            return parent.get(variable);
        }
    }

    public void bind(Object variable, Object value) {
        throw new InterpreterException("Cannot create binding in single entry environment.");
    }

    public void set(Object variable, Object value) {
        if (parent == null)
            throw new InterpreterException("Undefined variable '" + variable + "'.");

        if (variableName.equals(variable)) {
        	this.value = value;
        } else {
        	parent.set(variable, value);
        }
    }

    public void freezeLocal() {
    }

    public Set localVars() {
        throw new InterpreterException("Cannot compute local variable set of single class environment.");
    }

    public boolean  isLocalVar(Object variable) {
        return variableName.equals(variable);
    }

    public Environment newFrame(Environment parent) {
        throw new InterpreterException("Cannot create new frame of single class environment.");
    }


    //
    // Ctor
    //

    public SingleEntryEnvironment(Environment parent, DataStructureManipulator dsm, String variableName, Object value) {
        super(parent, dsm);
        this.variableName = variableName;
        this.value = value;
    }

    //
    // private
    //

    private String          variableName;
    private Object          value;
}

