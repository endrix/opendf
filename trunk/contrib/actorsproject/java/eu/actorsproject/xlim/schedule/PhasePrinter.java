/* 
 * Copyright (c) Ericsson AB, 2010
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

package eu.actorsproject.xlim.schedule;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Iterator;

import eu.actorsproject.util.XmlPrinter;

/**
 * Implements iteration over StaticPhases and XLIM print-out
 */

public class PhasePrinter {

	private Iterator<StaticPhase> mPhase;
	private ByteArrayOutputStream mOutput;
	private XmlPrinter mPrinter;
	private PlugIn mPlugIn;
	
	public interface PlugIn {

		/**
		 * @param printer  sets the printer to use for output
		 */
		void setPrinter(XmlPrinter printer);
		
		/**
		 * @param phase    the StaticPhase to print
		 */
		void printPhase(StaticPhase phase);	
	}
	/**
	 * @param phases the sequence of phase (may be infinite)
	 */
	public PhasePrinter(Iterable<StaticPhase> phases, PlugIn plugIn) {
		mPhase=phases.iterator();
		mOutput=new ByteArrayOutputStream();
		mPrinter=new XmlPrinter(new PrintStream(mOutput));
		mPlugIn=plugIn;
		mPlugIn.setPrinter(mPrinter);
	}
	
	/**
	 * @return true (if there are additional phases)
	 * 
	 * In the case of indefintely repeating action schedules, true is always returned.
	 */
	public boolean hasNext() {
		return mPhase.hasNext();
	}
	
	/**
	 * Print the next phase as XLIM and advance the phase iterator
	 * @return XLIM sequence as a String
	 */
	public String printNextPhase() {
		StaticPhase phase=mPhase.next();
		mOutput.reset();
		mPlugIn.printPhase(phase);
		return mOutput.toString();
	}
	
	/**
	 * @return the XmlPrinter that is associated with this PhasePrinter
	 */
	public XmlPrinter getPrinter() {
		return mPrinter;
	}
}
