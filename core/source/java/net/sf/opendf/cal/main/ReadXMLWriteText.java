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

package net.sf.opendf.cal.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;

import net.sf.opendf.util.io.ClassLoaderStreamLocator;
import net.sf.opendf.util.logging.Logging;
import net.sf.opendf.util.xml.Util;

import org.w3c.dom.Node;

public class ReadXMLWriteText {

    public static void main (String [] args) throws Exception {
        int argNo = 0;
        boolean niceInput = false;
        boolean quiet = false;
        boolean debug = false;
        boolean xformsAreResource = false;
        for ( ; (argNo < args.length) && (args[argNo].charAt(0) == '-'); argNo++) {
            if (args[argNo].equals("-n")) {
                niceInput = true;
            }
            else if (args[argNo].equals("-q")) {
                quiet = true;
            }
            else if (args[argNo].equals("-r")) {
                xformsAreResource = true;
            } else if (args[argNo].equals("-d")) {
            	debug = true;
            }
            else {
                printUsage();
                return;
            }
        }
        if (args.length - argNo < 2) {
            printUsage();
            return;
        }
        if (!quiet)
            Logging.user().info("ReadXMLWriteText version 1.1");
        String [] xfs = new String [args.length - 3 - argNo];
        for (int i = 0; i < xfs.length; i++) {
            xfs[i] = args[argNo + i + 2];
        }
        //doc = Util.applyTransforms(doc, xfs);
        //String result = Util.createTXT(Util.createTransformer(args[args.length -1]), doc);
        try {
	        Node doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(args[argNo + 0]));
	        Transformer txform;
	        if (xformsAreResource)
	        {
	            doc = Util.applyTransformsAsResources(doc, xfs, new ClassLoaderStreamLocator(ReadXMLWriteText.class.getClassLoader()));
	            java.io.InputStream is = Util.class.getClassLoader().getResourceAsStream(args[args.length -1]);
	            txform = Util.createTransformer(is);
	        }
	        else
	        {
	            doc = Util.applyTransforms(doc, xfs);
	            txform = Util.createTransformer(args[args.length -1]);
	        }
	        String result = Util.createTXT(txform, doc);
	        OutputStream os = null;
	        boolean closeStream = false;
	        if (".".equals(args[argNo + 1])) {
	            os = System.out;
	        } else {
	            os = new FileOutputStream(args[argNo + 1]);
	            closeStream = true;
	        }
	        PrintWriter pw = new PrintWriter(os);
	        pw.print(result);
	        if (closeStream)
	            pw.close();
	        else
	            pw.flush();
	        if (!quiet)
	            Logging.user().info("Done.");        
        }
        catch (Exception e) {
        	if (debug) {
        		System.err.println("Transformation error: " + e.getMessage());
        		e.printStackTrace();
        	}
        	throw e;
        }
    }

    static private void printUsage() {
        System.err.println(
            "ReadXMLWriteText [options] <infile> <outfile> <transformation>* (\".\" is standard out)\n"
            + " options are:\n"
            + "   -n transformations are in NiceXSL\n"
            + "   -q quiet (suppress version message)\n"
            + "   -r load transformations as resources (instead of as files)\n"
            );
    }

}

