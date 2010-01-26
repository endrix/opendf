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


import java.util.Set;

import net.sf.opendf.cal.interpreter.Context;
import net.sf.opendf.cal.interpreter.InterpreterException;

/**
 * A single class environment represents an imported Java class, which is optionally given an
 * alias. It maps the class name or the alias to the
 * corresponding class object. The class object is constructed by the context.
 *
 *  @see Context
 *
 *  @author Jorn W. Janneck <janneck@eecs.berkeley.edu>
 */

public class SingleClassEnvironment extends AbstractEnvironment implements Environment {

    //
    //  implement: Environment
    //

    public Object get(Object variable) {
        if (variableName.equals(variable)) {
            if (classObject != null)
                return classObject;
            try {
                Class c = classLoader.loadClass(packagePrefix + "." + className);
                classObject = context.createClass(c);
                return classObject;
            } catch (ClassNotFoundException e) {
                throw new InterpreterException("Cannot find class for variable '" + variable + "'.", e);
            }

        } else {
            if (parent == null)
                throw new InterpreterException("Undefined variable '" + variable + "'.");
            return parent.get(variable);
        }
    }

    public void bind(Object variable, Object value) {
        throw new InterpreterException("Cannot create binding in single class environment.");
    }

    public void set(Object variable, Object value) {
        if (parent == null)
            throw new InterpreterException("Undefined variable '" + variable + "'.");

        parent.set(variable, value);
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
    // SingleClassEnvironment
    //

    public String  getClassName() {
        return className;
    }

    public String  getPackagePrefix() {
        return packagePrefix;
    }

    //
    // Ctor
    //

    public SingleClassEnvironment(Environment parent, ClassLoader classLoader, Context context, String packagePrefix, String className, String variableName) {
        super(parent, context);
        this.classLoader = classLoader;
        this.context = context;
        this.packagePrefix = packagePrefix;
        this.className = className;
        this.variableName = variableName;
    }

    public SingleClassEnvironment(Environment parent, ClassLoader classLoader, Context context, String packagePrefix, String className) {
        this(parent, classLoader, context, packagePrefix, className, className);
    }

    //
    // private
    //

    private ClassLoader     classLoader;
    private Context         context;
    private String          packagePrefix;
    private String          className;
    private String          variableName;

    private Object          classObject = null;
}
