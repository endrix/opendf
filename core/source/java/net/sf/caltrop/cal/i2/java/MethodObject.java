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


import java.lang.reflect.Method;

import net.sf.caltrop.cal.i2.Configuration;
import net.sf.caltrop.cal.i2.Evaluator;
import net.sf.caltrop.cal.i2.Executor;
import net.sf.caltrop.cal.i2.Function;
import net.sf.caltrop.cal.i2.InterpreterException;
import net.sf.caltrop.cal.i2.OperandStack;
import net.sf.caltrop.cal.i2.Procedure;

/**
 * A MethodObject represents a Java method inside the interpreter. It encapsulates the functionality required
 * to apply the method like a function, or to call it like a procedure.
 *
 *  @author Jörn W. Janneck <janneck@eecs.berkeley.edu>, Christopher Chang <cbc@eecs.berkeley.edu>
 */

public class MethodObject implements Function, Procedure {

    //
    //  implement: Function
    //
    // FIXME i don't know wtf i'm doing
	public void apply(int n, Evaluator evaluator) {

        Method m;
        // (1) find method in thisInstance.getClass()
        m = findMethod(_configuration, _thisInstance.getClass(), _methodName, n, evaluator);
        if (m == null) {
            // (2) if thisInstance instanceof Class, look there.
            if (_thisInstance instanceof ClassObject) {
                m = findMethod(_configuration, ((ClassObject) _thisInstance).getClassObject(), _methodName, n, evaluator);
            }
        }
        if (m == null) {
            // (2) if thisInstance instanceof Class, look there.
            if (_thisInstance instanceof Class) {
                m = findMethod(_configuration, (Class) _thisInstance, _methodName, n, evaluator);
            }
        }
        if (m == null) {
        	String msg = "Couldn't find method " + _methodName + "(";
        	boolean first = true;
    		for (int i = 0; i < n; i++) {
    			Object v = evaluator.getValue(i);
    			Class c = (v != null) ? v.getClass() : null;
    			if (!first) {
    				msg += ", ";
    			}
    			msg += c.getName() + "  ";
    			first = false;
    		}        	
        	msg += ") belonging to " + _thisInstance.toString();
            throw new InterpreterException(msg);
        }

        assert m != null;
     
        try {
        	Class [] argTypes = m.getParameterTypes();
            Object [] newargs = new Object[n];

            for (int i = 0; i < n; i++) {
                newargs[i] = _configuration.convertToJavaType(evaluator.getValue(i), argTypes[i]);
            }
            evaluator.replaceWithResult(n, m.invoke(_thisInstance, newargs));
        } catch (Exception e) {
            String parlist = "(";
            for (int i = 0; i < n; i++) {
                if (i != 0) parlist += ", ";
    			Object v = evaluator.getValue(i);
    			Class c = (v != null) ? v.getClass() : null;
                parlist += c.toString();
            }
            parlist += ")";

            throw new InterpreterException("Couldn't invoke method " + m.getName() + parlist, e);
        }
    }

    //
    //  implement: Procedure

	public void call(int n, Executor executor) {
        apply(n, executor);
        executor.pop();
    }

    //
    //  Ctor
    //

    public MethodObject(Object thisInstance, String methodName, Configuration configuration) {
        this._thisInstance = thisInstance;
        this._methodName = methodName;
        this._configuration = configuration;
    }

    //
    // private
    //

    private static Method findMethod(Configuration config, Class c, String name, int n, OperandStack args) {
        Method [] methods;
        methods = c.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            if (name.equals(m.getName())) {
                Class [] pt = m.getParameterTypes();
                if (pt.length == n) {
                    boolean found = true;
                    for (int j = 0; j < pt.length; j++) {
                        if (!config.isAssignableToJavaType(args.getValue(j), pt[j]))
                            found = false;
                    }
                    if (found) {
                        return m;
                    }
                }
            }
        }
        return null;
    }

    private Object      _thisInstance;
    private String      _methodName;
    private Configuration _configuration;
}
