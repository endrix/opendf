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

import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.implementation.BasicXlimOperations;
import eu.actorsproject.xlim.implementation.ListOperations;
import eu.actorsproject.xlim.implementation.RealOperations;
import eu.actorsproject.xlim.implementation.SoftwareExtensions;
import eu.actorsproject.xlim.io.IXlimReader;
import eu.actorsproject.xlim.io.XlimReaderWithDiagnostics;
import eu.actorsproject.xlim.type.BasicXlimTypes;
import eu.actorsproject.xlim.type.ListTypeFeature;
import eu.actorsproject.xlim.type.RealTypeFeature;
import eu.actorsproject.xlim.util.Session;

/**
 * Reads an XLIM file and produces diagnostic messages in the event
 * of an error. If the file is OK, it exits silently with exit code 0,
 * in the event of errors the exit code is non-zero (1 for errors,
 * 2 for fatal errors).
 * 
 * Usage: CheckXlim input-files...
 */
public class CheckXlim extends Session {

	protected IXlimReader mReader; 
	protected boolean mHasErrors;
	protected File mInputFile;
	protected XlimDesign mXlimDesign;
	
	private boolean mReadAll;
	
	public CheckXlim() {
		register(new BasicXlimTypes());
		register(new BasicXlimOperations());
		register(new SoftwareExtensions());
		register(new RealTypeFeature());
		register(new RealOperations());
		register(new ListTypeFeature());
		register(new ListOperations());
	}
	
	@Override
	protected void initSession(String args[]) {
		// Constructions of fields after initialization of session
		super.initSession(args);
		mReader = new XlimReaderWithDiagnostics();
	}
	
	public XlimDesign read(File input) {
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
		if (design==null)
			mHasErrors=true;
		return design;
	}
	
	
	protected void printHelp() {
		String myName=getClass().getSimpleName();
		System.out.println("\nUsage: "+myName+" input-files...");
		System.out.println("\nChecks one or several XLIM files and prints diagnostics\n");
	}
	
	
	protected void setInputFile(String fileName) {
		mInputFile=null;
		mInputFile=new File(fileName);
		if (!mInputFile.canRead()) {
			fatalError("Unable to open file for reading: "+fileName);
		}
	}
	
	protected void parseCommandLine(String args[]) {
		if (args.length==0) {
			printHelp();
			reportError("Missing input file");
		}
		else  {
			mReadAll=true;
		}
	}

	protected void runFromCommandLine(String[] args) {
		initSession(args);
		parseCommandLine(args);
		if (mReadAll)
			for (String fileName: args) {
				setInputFile(fileName);
				if (mInputFile!=null)
					read(mInputFile);
			}
		else if (mInputFile!=null) {
			mXlimDesign=read(mInputFile);
		}
	}
	
	protected void reportError(String message) {
		System.err.println(message);
		mHasErrors=true;
	}
	
	protected void fatalError(String message) {
		reportError(message);
		System.err.println();
		System.exit(2);
	}
	
	public static void main(String[] args) {
		CheckXlim compilerSession=new CheckXlim();
		compilerSession.runFromCommandLine(args);
		if (compilerSession.mHasErrors)
			System.exit(1);
	}
}
