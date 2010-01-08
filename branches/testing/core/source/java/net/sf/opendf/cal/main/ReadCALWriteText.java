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


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import net.sf.opendf.cal.parser.Lexer;
import net.sf.opendf.cal.parser.Parser;
import net.sf.opendf.util.source.GenericError;
import net.sf.opendf.util.source.MultiErrorException;
import net.sf.opendf.util.xml.Util;
import net.sf.opendf.util.xml.XmlImplementation;

import org.w3c.dom.Node;

public class ReadCALWriteText {

    public static void main (String [] args)
    {

        if (args.length < 2) {
            printUsage();
            return;
        }
        Node doc = null;
        
        try
        {
            FileInputStream inputStream = new FileInputStream(args[0]);
            Lexer calLexer = new Lexer(inputStream);
            Parser calParser = new Parser(calLexer);
            doc = calParser.parseActor(args[0]);
        }
        catch (IOException ioe)
        {
            System.err.println(ioe.getMessage());
            System.exit(-1);
        }
        catch (MultiErrorException mee)
        {
            for(GenericError err : mee.getErrors())
            {
                System.out.println(err.toString());
            }
            System.exit(-1);
        }

        //Util.setSAXON();
        
        String [] xfs = new String [args.length - 3];
        for (int i = 0; i < xfs.length; i++)
            xfs[i] = args[i + 2];

        String result = "";
        try
        {
            doc = Util.applyTransforms(doc, xfs, Util.getSaxonImplementation());
            result = Util.createTXT(Util.createTransformer(args[args.length -1], Util.getSaxonImplementation()), doc);
        }
        catch (Exception e)
        {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        OutputStream os = null;
        boolean closeStream = false;
        if (".".equals(args[1])) {
            os = System.out;
        } else {
            try
            {
                os = new FileOutputStream(args[1]);
            }
            catch (FileNotFoundException fnfe)
            {
                System.err.println(fnfe.getMessage());
                System.exit(-1);
            }
            
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
        System.out.println("ReadXMLWriteText <infile> <outfile> <transformation>* (\".\" is standard out)");
    }

}

