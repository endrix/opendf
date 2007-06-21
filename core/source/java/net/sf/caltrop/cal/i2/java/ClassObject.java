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

package net.sf.caltrop.cal.i2.java;


import java.lang.reflect.Constructor;
import java.lang.reflect.Type;

import net.sf.caltrop.cal.i2.Configuration;
import net.sf.caltrop.cal.i2.Evaluator;
import net.sf.caltrop.cal.i2.Function;
import net.sf.caltrop.cal.i2.InterpreterException;
import net.sf.caltrop.cal.i2.OperandStack;

/**
 * A ClassObject represents a class inside the interpreter. In particular, it encapsulates
 * functionality that allows to apply the class to arguments in order to construct new instances
 * of it.
 *
 *  @author Jörn W. Janneck <janneck@eecs.berkeley.edu>, Christopher Chang <cbc@eecs.berkeley.edu>
 */

public class ClassObject implements Function {

    //
    //  implement: Function
    //

	public void apply(int n, Evaluator evaluator) {
		
        // find constructor in _classObject
            Constructor ctor = findConstructor(_configuration, _classObject, _classObject.getName(), n, evaluator);
            Object [] newargs = new Object[n];
            Class [] argTypes = ctor.getParameterTypes();
            for (int i = 0; i < newargs.length; i++) {
                newargs[i] = _configuration.convertToJavaType(evaluator.getValue(i), argTypes[i]);
            }
        try {
            evaluator.replaceWithResult(n, ctor.newInstance(newargs));
        } catch (Exception e) {
            throw new InterpreterException("Couldn't apply constructor of class " + _classObject.getName() + ": " + e.getMessage(), e);
        }
    }

    //
    //  Ctor
    //

    public ClassObject(Class classObject, Configuration configuration) {
        this._classObject = classObject;
        this._configuration = configuration;
    }

    //
    //  private
    //

    private static Constructor findConstructor(Configuration configuration, Class c, String name, int n, OperandStack args) {
        Constructor [] constructors;
        constructors = c.getConstructors();
        for (int i = 0; i < constructors.length; i++) {
            Constructor ctor = constructors[i];
            if (name.equals(ctor.getName())) {
                Class [] pt = ctor.getParameterTypes();
                if (pt.length == n) {
                    boolean found = true;
                    for (int j = 0; j < pt.length; j++) {
                        if (!configuration.isAssignableToJavaType(args.getValue(j), pt[j]))
                            found = false;
                    }
                    if (found) {
                        return ctor;
                    }
                }
            }
        }
        return null;
    }


    public Class getClassObject() {
        return _classObject;
    }

    private Class   _classObject;
    private Configuration  _configuration;
}
