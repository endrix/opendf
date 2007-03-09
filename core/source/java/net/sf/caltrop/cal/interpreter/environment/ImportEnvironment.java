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
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import net.sf.caltrop.cal.interpreter.Context;
import net.sf.caltrop.cal.interpreter.InterpreterException;
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

public class ImportEnvironment implements Environment {

    //
    //  Environment
    //

    public void bind(Object variable, Object value) {
        throw new InterpreterException("Cannot bind in ImportEnvironment.");
    }

    public void freezeLocal() {
    }

    public Object get(Object variable) {
        if (classImports.containsKey(variable)) {
            SingleClassEnvironment sce = (SingleClassEnvironment)classImports.get(variable);
            return sce.get(variable);
        }

        List packages = packageImports.keyList();
        for (Iterator i = packages.iterator(); i.hasNext(); ) {
            Object p = i.next();
            PackageEnvironment pe = (PackageEnvironment)packageImports.get(p);
            try {
                return pe.get(variable);
            } catch (Exception e) { /* swallow exception */ }
        }

        throw new InterpreterException("Undefined variable: " + variable);
    }

    public Object get(Object variable, Object [] location) {
        throw new InterpreterException("ImportEnvironment does not support indexed references.");
    }

    public Set localVars() {
        throw new InterpreterException("ImportEnvironment does not support computing the set of local variables.");
    }

    public boolean isLocalVar(Object variable) {
        throw new InterpreterException("ImportEnvironment does not support checking for locality of variable.");
    }

    public Map localBindings() {
        throw new InterpreterException("ImportEnvironment does not support computing the set of local variables.");
    }

    public Environment newFrame() {
        return newFrame(null);
    }

    public Environment newFrame(Environment parent) {
        throw new InterpreterException("ImportEnvironment does not support creation of frames.");
    }

    public void set(Object variable, Object value) {
        throw new InterpreterException("ImportEnvironment does not support assignment.");
    }

    public void set(Object variable, Object [] location, Object value) {
        throw new InterpreterException("ImportEnvironment does not support mutation.");
    }

    //
    //  ImportEnvironment
    //

    public void  importPackage(String packagePrefix) {
        if (packageImports.containsKey(packagePrefix))
            throw new ImportException("Package already imported: " + packagePrefix);

        packageImports.put(packagePrefix, new PackageEnvironment(null, this.getClass().getClassLoader(), context, packagePrefix));
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

        classImports.put(variable, new SingleClassEnvironment(null, classLoader, context, packagePrefix, className, variable));
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

    public ImportEnvironment(Context context) {
        this(context, ImportEnvironment.class.getClassLoader());
    }

    public ImportEnvironment(Context context, ClassLoader classLoader) {
        this.context = context;
        this.classLoader = classLoader;
    }

    //
    //  private
    //

    private Context     context;
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
