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

package eu.actorsproject.util;

import java.io.PrintStream;

public class OutputGenerator {

	private PrintStream mOutput;
	private int mIndent;
	private String mIndentString;
	private int mPosition;
	
	public OutputGenerator(PrintStream output) {
		mOutput=output;
		mIndentString="  ";
	}
	
	public OutputGenerator(PrintStream output, int colWidth) {
		mOutput=output;
		mIndentString="";
		for (int i=0; i<colWidth; ++i)
			mIndentString += " ";
	}
	
	public void increaseIndentation() {
		mIndent++;
	}
	
	public void decreaseIndentation() {
		mIndent--;
	}
	
	public boolean atStartOfLine() {
		return mPosition==0;
	}

	public int getPosition() {
		if (atStartOfLine())
			return mIndent*mIndentString.length();
		else
			return mPosition;
	}
	
	public void lineWrap(int maxPosition) {
		if (mPosition>=maxPosition)
			println();
	}
	
	public int getIndentationLevel() {
		return mIndent;
	}

	public void print(String s) {
		if (atStartOfLine())
			indent();
		mOutput.print(s);
		mPosition+=s.length();
	}
	
	public void println(String s) {
		if (atStartOfLine())
			indent();
		mOutput.println(s);
		mPosition=0;
	}
	
	public void println() {
		mOutput.println();
		mPosition=0;
	}
	
	private void indent() {
		for (int i=0; i<mIndent; ++i) {
			mOutput.print(mIndentString);
			mPosition+=mIndentString.length();
		}
	}
}
