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

import net.sf.caltrop.cal.interpreter.Context;
import net.sf.caltrop.cal.interpreter.environment.Environment;
import net.sf.caltrop.cal.interpreter.environment.PackageEnvironment;
import net.sf.caltrop.cal.interpreter.environment.SingleEntryEnvironment;

/**
 * 
 * @author jornj
 */
public class ClassLoadingImportHandler extends AbstractImportHandler {

	protected Environment extendWithPackageImport(Environment env,
			String packagePrefix) {
		return new PackageEnvironment(env, classLoader, getPlatform().context(), packagePrefix);
	}

	protected Environment extendWithSingleImport(Environment env,
			String packagePrefix, String className,
			String alias) {
		try {
			Context context = getPlatform().context();
			Class c = classLoader.loadClass(packagePrefix + "." + className);
			return new SingleEntryEnvironment(env, context, alias, context.createClass(c));
		} catch (Exception ex) {
			return null;
		}
		
	}
	
	/**
	 * This constructor uses the class loader of this class for loading the
	 * classes to be put into the environments it creates.
	 *
	 */
	public ClassLoadingImportHandler(Platform platform) {
		this (platform, ClassLoadingImportHandler.class.getClassLoader());
	}
	
	public ClassLoadingImportHandler(Platform platform, ClassLoader classLoader) {
		super (platform);
		this.classLoader = classLoader;
	}
	
	
	private ClassLoader classLoader;

}
