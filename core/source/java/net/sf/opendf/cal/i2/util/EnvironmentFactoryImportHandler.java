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

package net.sf.caltrop.cal.i2.util;

import net.sf.caltrop.cal.i2.Environment;
import net.sf.caltrop.cal.i2.environment.EnvironmentFactory;
import net.sf.caltrop.cal.i2.environment.SingleEntryEnvironment;

/**
 * 
 * @author jornj
 */
public class EnvironmentFactoryImportHandler extends AbstractImportHandler {

	protected Environment extendWithPackageImport(Environment env, String packagePrefix) {
		try {
			EnvironmentFactory ef = loadFactory(packagePrefix);
			if (ef == null)
				return null;
			return ef.createEnvironment(env, getConfiguration());
		} catch (Exception ex) {
			return null;
		}
	}

	protected Environment extendWithSingleImport(Environment env, String packagePrefix, String className, String alias) {
		try {
			EnvironmentFactory ef = loadFactory(packagePrefix);
			if (ef == null)
				return null;
			Environment e = ef.createEnvironment(null, getConfiguration());
			return new SingleEntryEnvironment(env, alias, e.getByName(className), null);  // TYPEFIXME
		} catch (Exception ex) {
			return null;
		}
	}
	
	public EnvironmentFactoryImportHandler(Platform platform) {
		this (platform, "");
	}
	
	public EnvironmentFactoryImportHandler(Platform platform, String platformSpecificPackagePrefix) {
		super (platform.configuration());
		this.platformSpecificPackagePrefix = platformSpecificPackagePrefix;
	}
	
	private EnvironmentFactory loadFactory(String packagePrefix) {
		try {
			Class c = this.getClass().getClassLoader().loadClass(platformSpecificPackagePrefix + packagePrefix);
			return (EnvironmentFactory)c.newInstance();
		} catch (Exception ex) {
			return null;
		}
	}
	
	private Platform getPlatform() {
		return platform;
	}
	
	private Platform platform;
	
	private String platformSpecificPackagePrefix;

}
