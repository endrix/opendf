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

package net.sf.caltrop.cal.i2.environment;


import net.sf.caltrop.cal.i2.Configuration;
import net.sf.caltrop.cal.i2.Environment;
import net.sf.caltrop.cal.i2.InterpreterException;
import net.sf.caltrop.cal.i2.ObjectSink;

/**
 * A single class environment represents an imported Java class, which is optionally given an
 * alias. It maps the class name or the alias to the
 * corresponding class object. The class object is constructed by the context.
 *
 *
 *  @author Jörn W. Janneck <janneck@eecs.berkeley.edu>
 */

public class SingleClassEnvironment extends SingleEntryEnvironment {

    //
    //  implement: Environment
    //
	
	@Override
	protected int localGet(Object var, ObjectSink s) {
        if (this.var.equals(var)) {
        	if (!loaded) {
                try {
                    Class c = classLoader.loadClass(packagePrefix + "." + className);
                    Object classObject = configuration.createClassObject(c);
                    super.localSetByPos(0, classObject);
                } catch (ClassNotFoundException e) {
                    throw new InterpreterException("Cannot find class '" + (packagePrefix + className) + "' for variable '" + this.var + "'.", e);
                }
        	}
        	return super.localGet(var, s);
        } else {
        	return UNDEFINED;
        }
    }

	@Override
	protected int localSet(Object var, Object value) {
		if (this.var.equals(var))
			throw new InterpreterException("Cannot set variable '" + var + "' in SingleClassEnvironment.");
		else
			return UNDEFINED;
	}
	
	@Override
	protected void localSetByPos(int varPos, Object value) {
		throw new InterpreterException("Cannot set variable '" + this.var + "' in SingleClassEnvironment.");
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

    public SingleClassEnvironment(Environment parent, ClassLoader classLoader, Configuration configuration, String packagePrefix, String className, String variableName) {
        super(parent, variableName, null, null);	// TYPEFIXME
        this.classLoader = classLoader;
        this.configuration = configuration;
        this.packagePrefix = packagePrefix;
        this.className = className;
    }

    public SingleClassEnvironment(Environment parent, ClassLoader classLoader, Configuration configuration, String packagePrefix, String className) {
        this(parent, classLoader, configuration, packagePrefix, className, className);
    }

    //
    // private
    //
    
    private Configuration	configuration;

    private ClassLoader     classLoader;
    private String          packagePrefix;
    private String          className;

    private boolean			loaded;
}
