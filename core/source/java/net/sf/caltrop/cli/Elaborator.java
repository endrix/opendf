package net.sf.caltrop.cli;

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

import net.sf.caltrop.cli.lib.ClassLoaderModelClassLocator;
import net.sf.caltrop.cli.lib.DirectoryModelClassLocator;
import net.sf.caltrop.util.Logging;
import net.sf.caltrop.util.Util;
import net.sf.caltrop.xslt.util.Loading;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.logging.Level;

import static net.sf.caltrop.util.Util.createTransformer;
import static net.sf.caltrop.util.Util.applyTransform;
import static net.sf.caltrop.util.Util.createXML;


public class Elaborator {

	public static void main (String [] args) throws Exception {
		if (args.length != 2) {
			printUsage();
			return;
		}
				
		Logging.dbg().setLevel(Level.ALL);
		
		initializeLocators();

		Node doc = Loading.loadActorSource(args[0]);
		
		Transformer t = createTransformer(Elaborator.class.getResourceAsStream(ElaboratorTransformationName));
		
		Node res = applyTransform(doc, t);

		String result = createXML(res);

		OutputStream os = null;
		boolean closeStream = false;
		if (".".equals(args[1])) {
			os = System.out;
		} else {
			os = new FileOutputStream(args[1]);
			closeStream = true;
		}
		PrintWriter pw = new PrintWriter(os);
		pw.print(result);
		if (closeStream)
			pw.close();
		else
			pw.flush();
	}
	
	static private void  initializeLocators() {
		Loading.setLocators(new ModelClassLocator [] {
				new DirectoryModelClassLocator("."),
				new ClassLoaderModelClassLocator(Elaborator.class.getClassLoader())
		});
	}

	static private void printUsage() {
		System.err.println(
				"Elaborator [options] <toplevelclass> <outfile> (\".\" is standard out)"
		);
	}
	
	private final static String ElaboratorTransformationName = "/net/sf/caltrop/transforms/Elaborate.xslt";

}

