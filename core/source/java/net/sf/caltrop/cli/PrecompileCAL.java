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

package net.sf.caltrop.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.caltrop.cal.interpreter.util.ASTFactory;
import net.sf.caltrop.cal.interpreter.util.SourceReader;
import net.sf.caltrop.cal.parser.Lexer;
import net.sf.caltrop.cal.parser.Parser;
import net.sf.caltrop.util.Util;

import org.w3c.dom.Document;
import org.w3c.dom.Node;


public class PrecompileCAL {

	public static void main(String [] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			files.add(args[i]);
		}
		doCompiling();
	}
		
	static private void doCompiling() throws Exception {
		for (String s : files) {
			String fn = s.trim();
			String baseName = fn;
			File inFile = new File(fn);
			boolean isCal = false;
			
			System.out.print("Compiling " + fn + ": ");
			
			if (fn.endsWith(suffixCAL)) {	
				isCal = true;
				baseName = fn.substring(0, fn.length() - suffixCAL.length());
			} else if (fn.endsWith(suffixCALML)) {	
				baseName = fn.substring(0, fn.length() - suffixCALML.length());
			} else {
				fn = baseName + suffixCAL;
				inFile = new File(fn);
				if (inFile.exists()) {
					isCal = true;
				} else {
					fn = baseName + suffixCALML;
					inFile = new File(fn);
					if (!inFile.exists()) {
						throw new RuntimeException("Cannot locate source file: " + s.trim());
					}
				}
			}
			
			System.out.print("read ");
			Document doc;
			InputStream is = new FileInputStream(inFile);
			if (isCal) {
				doc = SourceReader.parseActor(new InputStreamReader(is));
			} else {
				doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
			}
			is.close();

			System.out.print("transform ");
			Node a = ASTFactory.preprocessActor(doc);
			String result = Util.createXML(a);
			
			System.out.print("write ");
			OutputStream os = new FileOutputStream(baseName + suffixPCalML);
			PrintWriter pw = new PrintWriter(os);
			pw.println(result);
			pw.close();
			System.out.println("done.");
		}		
	}

	static private String  destinationDir = null;
	static private List<String> files = new ArrayList<String>();
	
	final static String suffixCAL = ".cal";
	final static String suffixCALML = ".calml";
	final static String suffixPCalML = ".pcalml";
}
