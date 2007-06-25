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

import java.util.Iterator;
import java.util.List;

import net.sf.caltrop.cal.i2.Environment;
import net.sf.caltrop.cal.interpreter.ast.Import;


/**
 * 
 * @author jornj
 */
public class ImportUtil {

	public static Environment handleImportList(Environment parent, ImportHandler [] importHandlers, Import [] imports, ImportMapper [] mappers) {
		
		Environment current = parent;
		for (int i = 0; i < imports.length; i++) {
			Environment env = handleImport(current, importHandlers, mapImport(imports[i], mappers));
			if (env == null)
				return null;
			current = env;
		}
		return current;
	}
	
	public static Environment handleImportList(Environment parent, List importHandlers, Import [] imports, ImportMapper [] mappers) {
		
		Environment current = parent;
		for (int i = 0; i < imports.length; i++) {
			Environment env = handleImport(current, importHandlers, mapImport(imports[i], mappers));
			if (env == null)
				return null;
			current = env;
		}
		return current;
	}
	
	public static Environment handleImport(Environment parent, ImportHandler [] importHandlers, Import imp) {
		try {
			for (int i = 0; i < importHandlers.length; i++) {
				Environment env = importHandlers[i].extendWithImport(parent, imp);
				if (env != null)
					return env;
			}
			return null;
		} catch (Exception ex) {
			return null;
		}
	}

	public static Environment handleImport(Environment parent, List importHandlers, Import imp) {
		try {
			for (Iterator i = importHandlers.iterator(); i.hasNext(); ) {
				ImportHandler h = (ImportHandler)i.next();
				Environment env = h.extendWithImport(parent, imp);
				if (env != null)
					return env;
			}
			return null;
		} catch (Exception ex) {
			return null;
		}
	}
		
	private static  Import mapImport(Import imp, ImportMapper [] mappers) {
		for (ImportMapper m : mappers) {
			Import i = m.map(imp);
			if (i != null) {
				return i;
			}
		}
		return imp;
	}
}
