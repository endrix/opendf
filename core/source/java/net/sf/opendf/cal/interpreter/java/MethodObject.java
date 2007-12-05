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

package net.sf.caltrop.cal.interpreter.java;


import java.lang.reflect.Method;

import net.sf.caltrop.cal.interpreter.Context;
import net.sf.caltrop.cal.interpreter.Function;
import net.sf.caltrop.cal.interpreter.InterpreterException;
import net.sf.caltrop.cal.interpreter.Procedure;

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
    public Object apply(Object[] args) {
        Class [] classes = new Class[args.length];
        for (int i = 0; i < classes.length; i++) {
            classes[i] = _context.getJavaClassOfObject(args[i]);
        }
        Object [] newargs = new Object[args.length];
        for (int i = 0; i < newargs.length; i++) {
            newargs[i] = _context.toJavaObject(args[i]);
        }
        Method m;
        // (1) find method in thisInstance.getClass()
        m = findMethod(_thisInstance.getClass(), _methodName, classes);
        if (m == null) {
            // (2) if thisInstance instanceof Class, look there.
            if (_thisInstance instanceof ClassObject) {
                m = findMethod(((ClassObject) _thisInstance).getClassObject(), _methodName, classes);
            }
        }
        if (m == null) {
            // (2) if thisInstance instanceof Class, look there.
            if (_thisInstance instanceof Class) {
                m = findMethod((Class) _thisInstance, _methodName, classes);
            }
        }
        if (m == null) {
        	String msg = "Couldn't find method " + _methodName + "(";
        	boolean first = true;
    		for (Class c : classes) {
    			if (!first) {
    				msg += ", ";
    			}
    			msg += c.getName() + "  ";
    			first = false;
    		}        	
        	msg += ") belonging to " + _thisInstance.toString();
            throw new InterpreterException(msg);
        }
        try {
            return _context.fromJavaObject(m.invoke(_thisInstance, newargs));
        } catch (Exception e) {
            String parlist = "(";
            for (int i = 0; i < args.length; i++) {
                if (i != 0) parlist += ", ";
                parlist += classes[i].toString();
            }
            parlist += ")";

            throw new InterpreterException("Couldn't invoke method " + m.getName() + parlist, e);
        }
    }

    //
    //  implement: Procedure

    public void call(Object[] args) {
        apply(args);
    }

    public int arity() {
        return -1;
    }

    //
    //  Ctor
    //

    public MethodObject(Object thisInstance, String methodName, Context context) {
        this._thisInstance = thisInstance;
        this._methodName = methodName;
        this._context = context;
    }

    //
    // private
    //

    private static Method findMethod(Class c, String name, Class [] classes) {
        Method [] methods;
        methods = c.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            if (name.equals(m.getName())) {
                Class [] pt = m.getParameterTypes();
                if (pt.length == classes.length) {
                    boolean found = true;
                    for (int j = 0; j < pt.length; j++) {
                        if (classes[j] != null && !isAssignableFrom(pt[j], classes[j]))
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

    private static boolean isAssignableFrom(Class c1, Class c2) {
        return getObjectType(c1).isAssignableFrom(getObjectType(c2));
    }

    private static Class getObjectType(Class c) {

        if (c == Boolean.TYPE) return Boolean.class;
        if (c == Character.TYPE) return Character.class;
        if (c == Byte.TYPE) return Byte.class;
        if (c == Short.TYPE) return Short.class;
        if (c == Integer.TYPE) return Integer.class;
        if (c == Long.TYPE) return Long.class;
        if (c == Float.TYPE) return Float.class;
        if (c == Double.TYPE) return Double.class;

        return c;
    }

    private Object      _thisInstance;
    private String      _methodName;
    private Context     _context;
}
