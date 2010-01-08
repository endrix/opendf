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


import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.util.XlimVisualPrinter;

/**
 * Translates an XLIM file into "human readable form".
 * The idea is to (unlike Xlim2c) reflect the exact contents 
 * of all elements, but present it in a format that is easier
 * to read than XML.
 * 
 * Usage: XlimVisual input-file.xlim [optional-output-file.xlim]
 * 
 * System.out is used when no output file is specified
 */
public class XlimVisual extends XlimNorm {

	@Override
	protected void generateOutput(XlimDesign design, PrintStream output) {
		XlimVisualPrinter printer=new XlimVisualPrinter(output);
	    printer.printDesign(design);
    }
	
	@Override
	protected void printHelp() {
		String myName=getClass().getSimpleName();
		System.out.println("\nUsage: "+myName+" input-file.xlim [optional-output-file.xlim]");
		System.out.println("\nTranslates XLIM into \"human readable\" form");
		System.out.println("stdout is used unless an output file is specified\n");
	}
	
	public static void main(String[] args) {
		XlimVisual compilerSession=new XlimVisual();
		compilerSession.runFromCommandLine(args);
		if (compilerSession.mHasErrors)
			System.exit(1);
	}
}
