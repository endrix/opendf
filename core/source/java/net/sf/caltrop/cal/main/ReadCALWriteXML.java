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

package net.sf.caltrop.cal.main;


import net.sf.caltrop.cal.parser.Lexer;
import net.sf.caltrop.cal.parser.Parser;
import net.sf.caltrop.util.Logging;
import net.sf.caltrop.util.Util;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

public class ReadCALWriteXML {

    public static void main (String [] args) throws Exception {

        int argNo = 0;
        boolean niceInput = false;
        boolean quiet = false;
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
            }
            else {
                Logging.user().warning("Unknown command line arg: " + args[argNo]);
                printUsage();
                return;
            }
        }
        if (args.length - argNo < 2) {
            printUsage();
            return;
        }
        
        FileInputStream inputStream = new FileInputStream(args[argNo + 0]);
        Lexer calLexer = new Lexer(inputStream);
        Parser calParser = new Parser(calLexer);
        Node doc;
        doc = calParser.parseActor(args[argNo + 0]);

        String [] xfs = new String [args.length - argNo - 2];
        for (int i = 0; i < xfs.length; i++)
            xfs[i] = args[argNo + i + 2];

        if (xformsAreResource)
            doc = Util.applyTransformsAsResources(doc, xfs);
        else
            doc = Util.applyTransforms(doc, xfs);

        String result = Util.createXML(doc);
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
    }

    static private void printUsage() {
        System.out.println("ReadCALWriteXML <infile> <outfile> <transformation>* (\".\" is standard out)");
    }
}
