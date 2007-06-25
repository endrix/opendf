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


import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.caltrop.cal.i2.Configuration;
import net.sf.caltrop.cal.i2.InterpreterException;
import net.sf.caltrop.cal.i2.ObjectSink;
import net.sf.caltrop.cal.i2.UndefinedInterpreterException;
import net.sf.caltrop.cal.i2.types.Type;
import net.sf.caltrop.cal.interpreter.util.SequentialMap;

/**
 * This environment allows the set of imported classes and packages to be changed at runtime.
 * It uses the PackageEnvironment and SingleClassEnvironment to perform the actual
 * classloading.
 * 
 * The ability to import and unimport classes and packages is relevant in interactive
 * contexts, such as the {@link net.sf.caltrop.cal.shell.Shell Cal shell}.
 *
 * @see PackageEnvironment
 * @see SingleClassEnvironment
 *
 *  @author Jörn W. Janneck <janneck@eecs.berkeley.edu>
 */

public class ImportEnvironment extends AbstractEnvironment {
	
	//
	//  AbstractEnvironment
	//
	
	@Override
	protected int localGet(Object var, ObjectSink s) {
        if (classImports.containsKey(var)) {
            SingleClassEnvironment sce = (SingleClassEnvironment)classImports.get(var);
            s.putObject(sce.getByName(var));
            return (int)NOPOS;
        } 
        
        List packages = packageImports.keyList();
        for (Iterator i = packages.iterator(); i.hasNext(); ) {
            Object p = i.next();
            PackageEnvironment pe = (PackageEnvironment)packageImports.get(p);
            try {
                Object value = pe.getByName(var);
                s.putObject(value);
                return (int)NOPOS;
            } catch (Exception e) { /* swallow exception */ }
        }

        return UNDEFINED;
	}
	
	@Override
	protected void localGetByPos(int pos, ObjectSink s) {
		throw new UndefinedInterpreterException("Positional access not supported by ImportEnvironment.");
	}
	
	@Override
	protected int localSet(Object var, Object value) {
		throw new InterpreterException("Cannot set variable in import environment.");
	}
	
	@Override
	protected void localSetByPos(int varPos, Object value) {
		throw new InterpreterException("Cannot set variable in import environment.");
	}
	
	@Override
	protected Object localGetVariableName(int varPos) {
		throw new UndefinedInterpreterException("Positional access not supported by ImportEnvironment.");
	}
	
	@Override
	protected Type localGetVariableType(int varPos) {
		throw new UndefinedInterpreterException("Positional access not supported by ImportEnvironment.");
	}
	
	@Override
	public void freezeLocal() {
	}
	

    //
    //  ImportEnvironment
    //

    public void  importPackage(String packagePrefix) {
        if (packageImports.containsKey(packagePrefix))
            throw new ImportException("Package already imported: " + packagePrefix);

        packageImports.put(packagePrefix, new PackageEnvironment(null, this.getClass().getClassLoader(), configuration, packagePrefix));
    }

    public void  unimportPackage(String packagePrefix) {
        if (!packageImports.containsKey(packagePrefix))
            throw new ImportException("Package not imported: " + packagePrefix);

        packageImports.remove(packagePrefix);
    }

    public void  importClass(String packagePrefix, String className) {
        importClass(packagePrefix, className, className);
    }

    public void  importClass(String packagePrefix, String className, String variable) {

        if (classImports.containsKey(variable)) {
            SingleClassEnvironment env = (SingleClassEnvironment)classImports.get(variable);

            throw new ImportException("Class '" + variable + "' already imported as '" + env.getClassName() + "' from package: " + env.getPackagePrefix());
        }

        classImports.put(variable, new SingleClassEnvironment(null, classLoader, configuration, packagePrefix, className, variable));
    }

    public void  unimportClass(String variable) {
        if (!classImports.containsKey(variable))
            throw new ImportException("Class not imported: " + variable);

        classImports.remove(variable);
    }

    public void  clear() {
        packageImports.clear();
        classImports.clear();
    }

    public List  importedPackages() {
        return packageImports.keyList();
    }

    public Set  importedClasses() {
        return classImports.keySet();
    }

    public String  importedClassName(String name) {
        SingleClassEnvironment sce = (SingleClassEnvironment)classImports.get(name);
        if (sce == null)
            throw new InterpreterException("Unknown imported class: " + name);
        return sce.getClassName();
    }

    public String  importedClassPackage(String name) {
        SingleClassEnvironment sce = (SingleClassEnvironment)classImports.get(name);
        if (sce == null)
            throw new InterpreterException("Unknown imported class: " + name);
        return sce.getPackagePrefix();
    }

    //
    //  ctor
    //

    public ImportEnvironment(Configuration config) {
        this(config, ImportEnvironment.class.getClassLoader());
    }

    public ImportEnvironment(Configuration config, ClassLoader classLoader) {
    	super (null);
        this.configuration = config;
        this.classLoader = classLoader;
    }

    //
    //  private
    //

    private Configuration configuration;
    private ClassLoader classLoader;

    private SequentialMap  packageImports = new SequentialMap();
    private Map  classImports = new HashMap();

    public static class ImportException extends RuntimeException {

        public ImportException(String msg) {
            super(msg);
        }

        public ImportException(String msg, Throwable cause) {
            super(msg, cause);
        }

        public ImportException(Throwable cause) {
            super(cause);
        }
    }

}
