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

package net.sf.caltrop.nl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import net.sf.caltrop.nl.parser.Lexer;
import net.sf.caltrop.nl.parser.Parser;
import net.sf.caltrop.nl.util.Lib;
import net.sf.caltrop.util.Logging;
import net.sf.caltrop.util.Util;

import org.w3c.dom.Document;


public class NL2XNL {
	
	/**
	 * 
	 * @author jornj
	 */
	
	public static void main (String [] args) throws Exception {
		if (args.length == 0) {
			printUsage();
			return;
		}
		boolean verbose = true;
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-q"))
			{
				verbose=false;
				continue;
			}
			
			try {
				if (verbose) Logging.user().info("Compiling '" + args[i] + "'...");
				compileSource(args[i]);
				if (verbose) Logging.user().info("done.");
			} catch (Exception e) {
				Logging.user().severe("ERROR: " + e.getMessage() + "(" + e.getClass().getName() + ").");
			}
		}
	}
	
	public static File compileSource (String fileName) throws Exception
	{
		String basename = fileName;
		if (fileName.endsWith(suffixNL)) {
			basename = fileName.substring(0, fileName.length() - suffixNL.length());
		}			
		FileInputStream inputStream = new FileInputStream(fileName);
		Document doc = Lib.readNL(fileName);
		
		String result = Util.createXML(doc);
		File outputFile = new File(basename + suffixXNL);
		OutputStream os = new FileOutputStream(outputFile);
		PrintWriter pw = new PrintWriter(os);
		pw.print(result);
		pw.close();
		
		return outputFile;
	}
	
	
	static private void printUsage() {
		System.out.println("NL2XNL <source> ...");
	}
	
	private final static String suffixNL = ".nl";
	private final static String suffixXNL = ".xnl";
}

