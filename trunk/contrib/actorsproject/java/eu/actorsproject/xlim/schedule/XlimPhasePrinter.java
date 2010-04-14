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

import java.util.HashSet;
import java.util.Set;

import eu.actorsproject.xlim.XlimBlockElement;
import eu.actorsproject.xlim.XlimBlockModule;
import eu.actorsproject.xlim.XlimIfModule;
import eu.actorsproject.xlim.XlimInputPort;
import eu.actorsproject.xlim.XlimInstruction;
import eu.actorsproject.xlim.XlimLoopModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.util.XlimAttributeRenaming;


public class XlimPhasePrinter extends PhasePrinter {

	private BlockElementVisitor mBlockElementVisitor=new BlockElementVisitor();
	private Set<XlimOperation> mPrintedOperations=new HashSet<XlimOperation>();
	
	/**
	 * @param phases             the sequence of phase (may be infinite)
	 * @param attributeRenaming  specifies renaming of XLIM attributes (optional/may be null)
	 * @param renameOutputs      if true, new unique names are generated for the outputs of
	 *                           XLIM operations 
	 */
	public XlimPhasePrinter(Iterable<StaticPhase> phases,
			                XlimAttributeRenaming attributeRenaming,
			                boolean renameOutputs) {
		super(phases);
		if (attributeRenaming!=null)
			attributeRenaming.registerPlugIns(mPrinter);
		// TODO: if renameOutputs, create/register plug-in for XlimOutputPorts
	}
	
	@Override
	protected void printPhase(StaticPhase phase) {
		// TODO: the actual output goes here!
		BasicBlock actionSelection=phase.getActionSelection();
		actionSelection.printPhase(this);
	}

	/**
	 * Prints an XLIM "block element" (=an operation in this context)
	 * @param blockElement
	 * 
	 * Called back from BasicBlock.printPhase()
	 */
	void printBlockElement(XlimBlockElement blockElement) {
		blockElement.accept(mBlockElementVisitor, null);
	}
	
	void printOperation(XlimOperation op) {
		// Unless op is already printed, print it!
		if (mPrintedOperations.add(op)) {
			// First make sure that we have printed all operations, 
			// on which 'op' depends
			for (XlimInputPort input: op.getInputPorts()) {
				printSource(input.getSource());
			}
			mPrinter.printElement(op);
		}
	}
	
	void printSource(XlimSource source) {
		XlimOutputPort output=source.asOutputPort();
		if (output!=null) {
			XlimInstruction instr=output.getParent();
			XlimOperation op=instr.isOperation();
			if (op!=null)
				printOperation(op);
		}
	}
	
	private class BlockElementVisitor implements XlimBlockElement.Visitor<Object, Object> {

		public Object visitOperation(XlimOperation op, Object arg) {
			printOperation(op);
			return null;
		}
		
		public Object visitBlockModule(XlimBlockModule m, Object arg) {
			// We just handle operations in this context
			throw new UnsupportedOperationException();
		}

		public Object visitIfModule(XlimIfModule m, Object arg) {
			// We just handle operations in this context
			throw new UnsupportedOperationException();
		}

		public Object visitLoopModule(XlimLoopModule m, Object arg) {
			// We just handle operations in this context
			throw new UnsupportedOperationException();			
		}
	}
}
