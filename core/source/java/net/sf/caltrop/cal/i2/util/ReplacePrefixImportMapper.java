/* 
BEGINCOPYRIGHT X
	
	Copyright (c) 2007, Xilinx Inc.
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
	- Neither the name of the copyright holder nor the names 
	  of its contributors may be used to endorse or promote 
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

import net.sf.caltrop.cal.ast.Import;
import net.sf.caltrop.cal.ast.PackageImport;
import net.sf.caltrop.cal.ast.SingleImport;

public class ReplacePrefixImportMapper implements ImportMapper {

	public Import map(Import imp) {
		String [] qid = imp.getQname();
		
		if (a.length <= qid.length) {
			for (int i = 0; i < a.length; i++) {
				if (!a[i].equals(qid[i])) {
					return null;
				}
			}
			String [] newimp = new String[b.length + qid.length - a.length];
			for (int i = 0; i < b.length; i++) {
				newimp[i] = b[i];
			}
			for (int i = 0; i < qid.length - a.length; i++) {
				newimp[b.length + i] = qid[a.length + i];
			}
			if (imp instanceof SingleImport) {
				return new SingleImport(newimp, ((SingleImport)imp).getAlias());
			} else if (imp instanceof PackageImport) {
				return new PackageImport(newimp);
			} else {
				throw new RuntimeException("Unknown import type: " + imp.getClass().getName());
			}
		} else {
			return null;
		}
	}
	
	public ReplacePrefixImportMapper(String [] a, String [] b) {
		this.a = a;
		this.b = b;
	}
	
	private String [] a;
	private String [] b; 

}
