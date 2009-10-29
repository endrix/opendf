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

import java.io.File;
import java.io.PrintStream;

import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.util.XlimTransformer;
import eu.actorsproject.xlim.xlim2c.CCodeGenerator;

public class Xlim2c extends XlimNorm {
	
	protected XlimTransformer mTransformer;
	protected CCodeGenerator mCodeGen; 
	
	/**
	 * @param input   input file (XLIM)
	 * @param output  output file (C)
	 * @return true if there were errors, false for successful compilation
	 */
	public boolean compile(File input, PrintStream output) {
		XlimDesign design=read(input);
		if (mHasErrors==false)
			generateOutput(design,output);
		return mHasErrors;
	}
	
	@Override
	protected void initSession(String args[]) {
		// Constructions of fields after initialization of session
		super.initSession(args);
		mTransformer = new XlimTransformer();
		mCodeGen = new CCodeGenerator();
	}

	
	@Override
	protected void generateOutput(XlimDesign design, PrintStream output) {
		design.createCallGraph();
		mTransformer.transform(design);
		mCodeGen.generateCode(design, mInputFile, output);
	}
	
	@Override
	protected void parseCommandLine(String args[]) {
		if (args.length!=2) {
			printHelp();
			if (args.length==0)
				reportError("Missing input file");
			else
				reportError("Too many files/command-line arguments");
		}
		else {
			setInputFile(args[0]);
			setOutputFile(args[1]);
		}
	}
	
	@Override
	protected void printHelp() {
		String myName=getClass().getSimpleName();
		System.out.println("\nUsage: "+myName+" input-file.xlim output-file.c");
		System.out.println("\nTranslates XLIM into C\n");
	}
	
	
	public static void main(String[] args) {
		Xlim2c compilerSession=new Xlim2c();
		compilerSession.runFromCommandLine(args);
		if (compilerSession.mHasErrors)
			System.exit(1);
	}
}
