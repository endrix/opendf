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
import eu.actorsproject.xlim.implementation.DefaultXlimImplementation;
import eu.actorsproject.xlim.io.XlimReader;
import eu.actorsproject.xlim.util.XlimTransformer;
import eu.actorsproject.xlim.xlim2c.CodeGenerator;
import eu.actorsproject.xlim.xlim2c.OperationGenerators;

public class Xlim2c {
	
	protected XlimReader mReader = new XlimReader(new DefaultXlimImplementation());
	protected XlimTransformer mTransformer = new XlimTransformer();
	protected CodeGenerator mCodeGen = new CodeGenerator(new OperationGenerators());
	
	public void compile(File input, PrintStream output) {
		XlimDesign design=null;
		
		try {
			design=mReader.read(input);
		} catch (Exception ex) {
			String message=ex.getMessage();
			if (message==null)
				message="Exception: "+ex.toString();
			reportError(message);
			fatalError("Error reading "+input.getPath());
		}
		design.createCallGraph();
		mTransformer.transform(design);
		mCodeGen.generateCode(design, input, output);
	}
	
	private void compile(String args[]) {
		if (args.length!=2) {
			fatalError("Usage: Xlim2c input-file.xlim output-file.c");
		}
		
		File input=new File(args[0]);
		if (!input.canRead()) {
			fatalError("Unable to open file for reading: "+args[0]);
		}
	
		PrintStream output=null;
		try {
			output=new PrintStream(args[1]);
		} catch (Exception ex) {
			fatalError("Unable to open file for writing: "+args[1]+"\n"+ex.toString());
		}
		
		compile(input,output);
	}
	
	protected void reportError(String message) {
		System.err.println(message);
	}
	
	protected void fatalError(String message) {
		reportError(message);
		System.err.println();
		System.exit(2);
	}
	
	public static void main(String[] args) {
		Xlim2c compilerSession=new Xlim2c();
		compilerSession.compile(args);
	}
}
