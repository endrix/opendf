/* 
 * Copyright (c) Ericsson AB, 2009
 * Author: Carl von Platen (carl.von.platen@ericsson.com)
 * All rights reserved.
 *
 * License terms:
 *
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met:
 *     * Redistributions of source code must retain the above 
 *       copyright notice, this list of conditions and the 
 *       following disclaimer.
 *     * Redistributions in binary form must reproduce the 
 *       above copyright notice, this list of conditions and 
 *       the following disclaimer in the documentation and/or 
 *       other materials provided with the distribution.
 *     * Neither the name of the copyright holder nor the names 
 *       of its contributors may be used to endorse or promote 
 *       products derived from this software without specific 
 *       prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package eu.actorsproject.cli;


import java.io.PrintStream;

import eu.actorsproject.util.XmlPrinter;
import eu.actorsproject.xlim.XlimDesign;

/**
 * Reads an XLIM file, reports possible errors, and prints it with 
 * normalized source identifiers to facilitate comparison of XLIM files.
 * 
 * Usage: XlimNorm input-file.xlim [optional-output-file.xlim]
 * 
 * System.out is used when no output file is specified
 */
public class XlimNorm extends CheckXlim {
	
	protected PrintStream mOutputStream;
	
	protected void generateOutput(XlimDesign design, PrintStream output) {
		XmlPrinter printer=new XmlPrinter(output);
	    printer.printDocument(design);
    }
	
	@Override
	protected void printHelp() {
		String myName=getClass().getSimpleName();
		System.out.println("\nUsage: "+myName+" input-file.xlim [optional-output-file.xlim]");
		System.out.println("\nChecks one or several XLIM files and prints diagnostics");
		System.out.println("stdout is used unless an output file is specified\n");
	}
	
	protected void setOutputFile(String fileName) {
		mOutputStream=null;
		try {
			mOutputStream=new PrintStream(fileName);
		} catch (Exception ex) {
			fatalError("Unable to open file for writing: "+fileName+"\n"+ex.toString());
		}
	}
	
	@Override
	protected void parseCommandLine(String args[]) {
		if (args.length<1 || args.length>2) {
			printHelp();
			if (args.length==0)
				reportError("Missing input file");
			else
				reportError("Too many files/command-line arguments");
		}
		else {
			setInputFile(args[0]);
			if (args.length==2)
				setOutputFile(args[1]);
			else
				mOutputStream=System.out;
		}
	}
	
	protected void runFromCommandLine(String[] args) {
		super.runFromCommandLine(args);
		if (mHasErrors==false) {
			generateOutput(mXlimDesign,mOutputStream);
		}
	}
	
	public static void main(String[] args) {
		XlimNorm compilerSession=new XlimNorm();
		compilerSession.runFromCommandLine(args);
		if (compilerSession.mHasErrors)
			System.exit(1);
	}
}
