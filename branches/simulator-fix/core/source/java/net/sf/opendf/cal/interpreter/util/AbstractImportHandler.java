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

package net.sf.opendf.cal.interpreter.util;

import net.sf.opendf.cal.ast.Import;
import net.sf.opendf.cal.ast.PackageImport;
import net.sf.opendf.cal.ast.SingleImport;
import net.sf.opendf.cal.interpreter.environment.Environment;

/**
 * 
 * @author jornj
 */
public abstract class AbstractImportHandler implements ImportHandler {

	public Environment extendWithImport(Environment env, Import imp) {
		
        String packagePrefix = imp.getPackagePrefix();
        if (imp instanceof PackageImport) {
        	return extendWithPackageImport(env, packagePrefix);
        } else if (imp instanceof SingleImport) {
    		String className = ((SingleImport) imp).getClassName();
    		String alias = ((SingleImport) imp).getAlias();
    		if (alias == null || "".equals(alias))
    			alias = className;
            return extendWithSingleImport(env, packagePrefix, className, alias);
        } else 
        	return null;
	}
	
	protected AbstractImportHandler(Platform platform) {
		this.platform = platform;
	}
	
	protected Platform  getPlatform() {
		return platform;
	}
        
	protected abstract Environment extendWithPackageImport(Environment env, String packagePrefix);
	protected abstract Environment extendWithSingleImport(Environment env, String packagePrefix, String className, String alias);
	
	private Platform platform;
}