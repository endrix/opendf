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

package net.sf.opendf.cal.i2.environment;


import net.sf.opendf.cal.i2.Configuration;
import net.sf.opendf.cal.i2.Environment;
import net.sf.opendf.cal.i2.InterpreterException;
import net.sf.opendf.cal.i2.ObjectSink;

/**
 * A package environment represents an imported Java package. It maps all class names in that package to the
 * corresponding class object. The class object is constructed by the context.
 *
 *
 *
 *  @author Jorn W. Janneck <janneck@eecs.berkeley.edu>
 */

public class PackageEnvironment extends DynamicEnvironmentFrame {

    //
    //  implement: AbstractEnvironment
    //
	
	@Override
	protected int localGet(Object variable, ObjectSink s) {

		int n = super.localGet(variable, s);
		if (n >= 0) {
			return n;
		}

        String className = packagePrefix + "." + variable;
        try {
            Class c = classLoader.loadClass(className);
            Object classObject = configuration.createClassObject(c);
            return super.bind(variable, classObject, null);	// TYPEFIXME
        } catch (ClassNotFoundException e) {
        	return UNDEFINED;
        }
    }
	
    public void freezeLocal() {
    }

    @Override
    protected int localSet(Object var, Object value) {
    	throw new InterpreterException("Cannot set variable in package: '" + var + "'.");
    }
    
    @Override
    protected void localSetByPos(int varPos, Object value) {
    	throw new InterpreterException("Cannot set variable in package: '" + localGetVariableName(varPos) + "'.");
    }

    //
    //  Ctor
    //

    public PackageEnvironment(Environment parent, ClassLoader classLoader, Configuration configuration, String packagePrefix) {
        super(parent);
        this.classLoader = classLoader;
        this.configuration = configuration;
        this.packagePrefix = packagePrefix;
    }

    //
    //  private
    //

    private Configuration	configuration;
    private ClassLoader     classLoader;
    private String          packagePrefix;
}
