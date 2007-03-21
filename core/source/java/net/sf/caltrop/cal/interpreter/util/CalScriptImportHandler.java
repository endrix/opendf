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

package net.sf.caltrop.cal.interpreter.util;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sf.caltrop.cal.interpreter.Context;
import net.sf.caltrop.cal.interpreter.environment.Environment;
import net.sf.caltrop.cal.interpreter.environment.HashEnvironment;
import net.sf.caltrop.cal.interpreter.environment.SingleEntryEnvironment;
import net.sf.caltrop.cal.shell.Shell;


/**
 * This import handler imports the definitions made as the result of executing a Cal
 * script file (see also {@link net.sf.caltrop.cal.shell.Shell the shell definition}).
 * 
 * It reads the script as the path corresponding to the package name plus the 
 * extension ".cal", relative to the class loader. If such a file 
 * exists, it is executed as a script and the resulting
 * bindings become the package that is either imported as a whole, or imported
 * from.
 * 
 * @author jornj
 */
public class CalScriptImportHandler extends AbstractImportHandler {

	protected Environment extendWithPackageImport(Environment env,
			String packagePrefix) {
		Map m = loadShellScript(getPlatform(), packagePrefix);
		if (m == null)
			return null;
		Environment v = getPlatform().context().newEnvironmentFrame(env);
		for (Iterator i = m.keySet().iterator(); i.hasNext(); ) {
			Object var = i.next();
			v.bind(var, m.get(var));
		}
		return v;
	}

	protected Environment extendWithSingleImport(Environment env,
			String packagePrefix, String className, String alias) {
		
		Map m = loadShellScript(getPlatform(), packagePrefix);
		if (m == null)
			return null;
		if (m.keySet().contains(className)) {
			return new SingleEntryEnvironment(env, getPlatform().context(), alias, m.get(className));
		} else {
			throw new UnsatisfiedImportException(packagePrefix, className, alias);
		}
	}
	
	public CalScriptImportHandler(Platform platform) {
		this(platform, CalScriptImportHandler.class.getClassLoader());
	}
	
	public CalScriptImportHandler(Platform platform, ClassLoader classLoader) {
		super (platform);
		this.classLoader = classLoader;
	}
	
	private Map  loadShellScript(Platform platform, String packagePrefix) {

		InputStream in = null;
		try {
			String scriptName = packagePrefix.replace('.', '/') + ".cal";
			in = classLoader.getResourceAsStream(scriptName);
			if (in == null)
				return null;
			Shell shell = new Shell(platform, new HashMap(), in, NullOutputStream.devNull, NullOutputStream.devNull, false);
			return shell.executeAll();	
		} catch (Exception ex) {
			return null;
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (Exception ex) {
				return null;
			}
		}
	}
	
	private ClassLoader classLoader;
}
