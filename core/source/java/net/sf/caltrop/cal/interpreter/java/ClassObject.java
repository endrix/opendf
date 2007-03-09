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


import java.lang.reflect.Constructor;

import net.sf.caltrop.cal.interpreter.Context;
import net.sf.caltrop.cal.interpreter.Function;
import net.sf.caltrop.cal.interpreter.InterpreterException;

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

    public Object apply(Object[] args) {
        // find constructor in _classObject
        Class [] classes = new Class[args.length];
        for (int i = 0; i < classes.length; i++) {
            classes[i] = _context.getJavaClassOfObject(args[i]);
        }
        try {
            Constructor ctor = findConstructor(_classObject, _classObject.getName(), classes);
            Object [] newargs = new Object[args.length];
            for (int i = 0; i < newargs.length; i++) {
                newargs[i] = _context.toJavaObject(args[i]);
            }
            return _context.fromJavaObject(ctor.newInstance(newargs));
        } catch (Exception e) {
            throw new InterpreterException("Couldn't apply constructor of class " + _classObject.getName(), e);
        }
    }

    public int arity() {
        return -1;
    }

    //
    //  Ctor
    //

    public ClassObject(Class classObject, Context context) {
        this._classObject = classObject;
        this._context = context;
    }

    //
    //  private
    //

    private static Constructor findConstructor(Class c, String name, Class [] classes) {
        Constructor [] constructors;
        constructors = c.getConstructors();
        for (int i = 0; i < constructors.length; i++) {
            Constructor ctor = constructors[i];
            if (name.equals(ctor.getName())) {
                Class [] pt = ctor.getParameterTypes();
                if (pt.length == classes.length) {
                    boolean found = true;
                    for (int j = 0; j < pt.length; j++) {
                        if (classes[j] != null && !isAssignableFrom(pt[j], classes[j]))
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

    public Class getClassObject() {
        return _classObject;
    }

    private Class   _classObject;
    private Context _context;
}
