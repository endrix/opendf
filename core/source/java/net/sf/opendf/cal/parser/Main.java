/* NiceXSL to XSL translator main program

 Copyright (c) 2002 The Regents of the University of California.
 All rights reserved.
 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the above
 copyright notice and the following two paragraphs appear in all copies
 of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.

                                        PT_COPYRIGHT_VERSION_2
                                        COPYRIGHTENDKEY

@ProposedRating Red (Ed.Willink@uk.thalesgroup.com)
@AcceptedRating Red

Created : September 2002

*/
package net.sf.caltrop.cal.parser;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import net.sf.caltrop.util.xml.Util;

import org.w3c.dom.Document;

///////////main//////////////////////////////////////////////////////////////
//// main

/**
This class provides a main program for a simple example activation of the Caltrop parser,
reading an input file from args[1] parsing it to create a DOM tree, and writing
it out again as XML to args[2].

@author Ed Willink
@version $Id: Main.java 52 2007-01-22 15:56:51Z imiller $
@see net.sf.caltrop.cal.parser.Lexer net.sf.caltrop.cal.parser.Parser
*/
public class Main {

    //
    //  Main program parsing args[0] as NiceXSL to DOM and then writing to args[1] as XSL.
    //
    public static void main(String args[]) throws Exception {
        if (args.length != 2) {
            printUsage();
            return;
        }
        FileInputStream inputStream = new FileInputStream(args[0]);
        Lexer aLexer = new Lexer(inputStream);
        Parser aParser = new Parser(aLexer);
        try {
            Document aDocument = aParser.parseActor(args[0]);
            String result = Util.createXML(aDocument);
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
            return;
        }
        catch (Exception e) {
            throw new Exception(e.toString());  // Throw a new exception to lose the back trace.
        }
    }


    //
    //  Print the program usage..
    //
    private static void printUsage() {
        System.out.println("net.sf.caltrop.cal.parser.Main <infile> <outfile> (\".\" is standard out)");
    }


//    private static String defaultDBFI = System.getProperty("javax.xml.parsers.DocumentBuilderFactory");

//    public  static void setDefaultDBFI() {
//        System.setProperty("javax.xml.parsers.DocumentBuilderFactory", defaultDBFI == null ? "" : defaultDBFI);
//    }
}

