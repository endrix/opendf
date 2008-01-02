package net.sf.opendf.cli;

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

import static net.sf.opendf.util.xml.Util.applyTransform;
import static net.sf.opendf.util.xml.Util.applyTransformAsResource;
import static net.sf.opendf.util.xml.Util.applyTransformsAsResources;
import static net.sf.opendf.util.xml.Util.createTransformer;
import static net.sf.opendf.util.xml.Util.createXML;
import static net.sf.opendf.cli.Util.elaborate;
import static net.sf.opendf.cli.Util.extractPath;
import static net.sf.opendf.cli.Util.initializeLocators;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.logging.Level;

import javax.xml.transform.Transformer;

import net.sf.opendf.util.Loading;
import net.sf.opendf.util.io.ClassLoaderStreamLocator;
import net.sf.opendf.util.io.DirectoryStreamLocator;
import net.sf.opendf.util.io.StreamLocator;
import net.sf.opendf.util.logging.Logging;
import net.sf.opendf.util.xml.Util;
import net.sf.opendf.util.exception.*;

import org.w3c.dom.Node;


public class Elaborator {

	public static void main (String [] args) throws Exception {
		String networkClass = null;
		String outputFileName = null;
		String cachePath = null;
		String [] modelPath = null;
		boolean postProcessing = true; 
		boolean doInlining = true; // functions/procedures will be inlined if true.
		Map<String, String> params = new HashMap<String, String>();
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-D")) {
				i += 1;
				if (i >= args.length) usage("-D option must be followed by variable definition.");

				String s = args[i].trim();
				int n = s.indexOf('=');
				if (n < 0) usage("Variable definition must contain '=' sign.");

				String v = s.substring(0, n).trim();
				if ("".equals(v)) usage("Variable name must not be empty.");
				String expr = s.substring(n + 1);

				params.put(v, expr);
            } else if (args[i].equals("-npp")) {
            	postProcessing = false;
            } else if (args[i].equals("-ni")) {
            	doInlining = false;
			} else if (args[i].equals("-o")) {
				if (outputFileName != null) usage("Doubly defined output file.");
				i += 1;
				if (i >= args.length) usage();
				outputFileName = args[i];
            } else if (args[i].equals("-qq")) {
                Logging.user().setLevel(Level.OFF);
            } else if (args[i].equals("-q")) {
                if (Logging.user().isLoggable(Level.WARNING))
                {
                    // If WARNING level is allowed, then make it the
                    // least restrictive level.  If it is not, then do
                    // NOT further relax to the WARNING level
                    Logging.setUserLevel(Level.WARNING);
                }
            } else if (args[i].equals("-v")) {
                Logging.user().setLevel(Level.ALL);                
            } else if (args[i].equals("-debug")) {
                Logging.user().setLevel(Level.ALL);
            } else if (args[i].equals("-debug0")) {
                Logging.dbg().setLevel(Level.ALL);
			} else if (args[i].equals("-cache")) {
				if (cachePath != null) usage("Doubly defined cache path.");
				i += 1;
				if (i >= args.length) usage("-cache option must be followed by cache path.");
				cachePath = args[i];				
			} else if (args[i].equals("-mp")) {
				if (modelPath != null) usage("Doubly defined model path.");
				i += 1;
				if (i >= args.length) usage("-mp option must be followed by model path.");
				modelPath = extractPath(args[i]);
            } else if (args[i].equals("--version")) {
                VersionInfo.printVersion();
                System.exit(0);
            }
            else if (!args[i].startsWith("-")) {
				if (networkClass != null) {
					if (outputFileName != null)
						usage("Network class and output file already defined.");
					outputFileName = args[i];
				} else {
					networkClass = args[i];
				}
			} else {
				usage("Unknown option: " + args[i]);
			}
		}

        if (modelPath == null) {
            modelPath = new String [] {"."};
        }

        Logging.user().info("Model Path: " + Arrays.asList(modelPath));
						
        ClassLoader classLoader = new SimulationClassLoader(Simulator.class.getClassLoader(), modelPath, cachePath);
		initializeLocators(modelPath, Elaborator.class.getClassLoader());

		try {
            // Create the output stream first (fail fast on io issues)
			OutputStream os = null;
			boolean closeStream = false;
			if (outputFileName == null || ".".equals(outputFileName)) {
				os = System.out;
			} else {
				os = new FileOutputStream(outputFileName);
				closeStream = true;
			}
			PrintWriter pw = new PrintWriter(os);
            
            Node res = elaborate(networkClass, modelPath, classLoader, params, postProcessing, doInlining);

            String xmlRes = createXML(res);
            
            if (Logging.dbg().isLoggable(Level.FINE))
            {
                try{
                    PrintWriter upw = new PrintWriter(new FileOutputStream(networkClass + ".exdf"));
                    upw.print(xmlRes);
                    upw.flush();
                }
                catch (Exception e){System.out.println("Could not output intermediate XML for debug: " + networkClass + ".exdf");}
            }
            
			pw.print(xmlRes);
            
			if (closeStream) pw.close();
			else pw.flush();
			
			Logging.user().info("Network '" + networkClass + "' successfully elaborated.");
		}
        catch (Throwable t)
        {
            (new ReportingExceptionHandler()).process(t);
            System.exit(-1);
        }
        
	}

	static private void usage(String message) {
		System.err.println(message);
		usage();
	}
	
	static private void usage() {
		System.out.println("Elaborator [options] <toplevelclass> <outfile> (\".\" is standard out)");
        System.out.println("  -o <file>           defines the output file");
        System.out.println("  -D <param def>      allows specification of parameter defs");
        System.out.println("  -q                  run quietly");
        System.out.println("  -v                  run verbosely");
        System.out.println("  -npp                no post-processing");        
        System.out.println("  -mp <paths>         specifies the search paths for model files");        
        System.out.println("  -cache <path>       the path to use for caching precompiled models");        
        System.out.println("                      If none is specified, caching is turned off.");        
        System.out.println("  --version           Display Version information and quit");
	}


}

