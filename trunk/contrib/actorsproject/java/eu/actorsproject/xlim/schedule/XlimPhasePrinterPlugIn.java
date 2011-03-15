/* 
 * Copyright (c) Ericsson AB, 2011
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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.util.XmlPrinter;
import eu.actorsproject.xlim.XlimBlockElement;
import eu.actorsproject.xlim.XlimBlockModule;
import eu.actorsproject.xlim.XlimIfModule;
import eu.actorsproject.xlim.XlimInputPort;
import eu.actorsproject.xlim.XlimInstruction;
import eu.actorsproject.xlim.XlimLoopModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;

/**
 * Prints the contents of a basic block. Used when printing action schedules (see XlimPhasePrinter)
 */
public class XlimPhasePrinterPlugIn implements PhasePrinter.PlugIn {

	private static int sNextId; // For renaming of OutputPorts (shared by multiple instances in Model Compiler)
	
	private XmlPrinter mPrinter;
	private BlockElementVisitor mBlockElementVisitor=new BlockElementVisitor();
	private Set<XlimOperation> mPrintedOperations=new HashSet<XlimOperation>();
	private Set<XlimOperation> mCurrentScope;
	private Deque<Set<XlimOperation>> mScopes=new ArrayDeque<Set<XlimOperation>>();
	private OutputPortRenaming mOutputPortRenaming;
	
	/**
	 * @param attributeRenaming  specifies renaming of XLIM attributes (optional/may be null)
	 * @param renameOutputs      if true, new unique names are generated for the outputs of
	 *                           XLIM operations 
	 */
	public XlimPhasePrinterPlugIn(boolean renameOutputs) {
		if (renameOutputs) {
			mOutputPortRenaming=new OutputPortRenaming();
			
		}
	}
	
	@Override 
	public void setPrinter(XmlPrinter printer) {
		assert(mPrinter==null); // do this once
		
		mPrinter=printer;
		
		// Register a possible OutputPortRenaming with the printer
		if (mOutputPortRenaming!=null) {
			mPrinter.register(mOutputPortRenaming);
		}
	}
	
	/**
	 * Prints the XLIM code of a static scheduling phase (used by PhasePrinter)
	 * 
	 * @param phase   A static phase
	 */
	@Override
	public void printPhase(StaticPhase phase) {
		assert(mCurrentScope==null);
		BasicBlock actionSelection=phase.getActionSelection();
		
		// Allocate a (topmost) scope
		mCurrentScope=new HashSet<XlimOperation>();
		actionSelection.printPhase(this);
		
		// remove scope again
		removeCurrentScope();
		assert(mScopes.isEmpty());
		mCurrentScope=null;
	}
	
	public XmlPrinter getPrinter() {
		return mPrinter;
	}
	
	/**
	 * Creates a new scope and pushes the old scope on a stack.
	 * Scopes are used to give XlimOutputPorts unique names 
	 * -even if evaluated multiple times in different scopes.
	 */
	public void enterScope() {
		mScopes.push(mCurrentScope);
		mCurrentScope=new HashSet<XlimOperation>();
	}
	
	/**
	 * "Forgets" all evaluations in current scope and pops stack of scopes. 
	 * Scopes are used to give XlimOutputPorts unique names 
	 * -even if evaluated multiple times in different scopes.
	 */
	public void leaveScope() {
		removeCurrentScope();
		mCurrentScope=mScopes.pop();
	}
	
	private void removeCurrentScope() {
		if (mOutputPortRenaming!=null) {
			for (XlimOperation op: mCurrentScope) {
				mOutputPortRenaming.removeAll(op.getOutputPorts());
			}
		}
		mPrintedOperations.removeAll(mCurrentScope);
	}
	
	/**
	 * Prints an XLIM "block element" (=an operation in this context)
	 * @param blockElement
	 * 
	 * Called back from BasicBlock.printPhase()
	 */
	public void printBlockElement(XlimBlockElement blockElement) {
		blockElement.accept(mBlockElementVisitor, null);
	}
	
	/**
	 * @param output  an XlimOutputPort (the result of an XlimOperation)
	 * @return the attribute name to use for the port in the XLIM
	 * 
	 * Useful when overriding printOperation: normally one wouldn't
	 * have to worry about the renaming of XlimOutputPorts, since
	 * the XmlPrinter takes care of that.
	 */
	protected String getName(XlimOutputPort output) {
		if (mOutputPortRenaming!=null) {
			return mOutputPortRenaming.getAttributeValue(output);
		}
		else {
			return output.getUniqueId();
		}
	}
	
	/**
	 * @param inputs  a collection of input ports
	 * 
	 * Makes sure that all the operations, which generate the inputs are printed
	 */
	protected void printRequiredOperations(Iterable<? extends XlimInputPort> inputs) {
		// First make sure that we have printed all operations, 
		// on which 'op' depends
		for (XlimInputPort input: inputs) {
			printSource(input.getSource());
		}
	}
	
	
	/**
	 * @param op  an XlimOperation
	 * 
	 * Prints all operations, on which 'op' depends, then 'op'.
	 * Don't call this method directly (use printOperationIfNeeded instead)
	 * Can be overridden to achieve simple transformations of the produced XLIM
	 */
	protected void printOperation(XlimOperation op) {
		// First make sure that we have printed all operations, 
		// on which 'op' depends
		printRequiredOperations(op.getInputPorts());

		// Then print the actual operation
		mPrinter.printElement(op);		
	}

	/**
	 * @param op  an XlimOperation
	 * 
	 * Prints an operation (using printOperation) unless it already is printed
	 * in the current scope
	 */
	protected void printOperationIfNeeded(XlimOperation op) {
		// Unless op is already printed, print it!
		if (mPrintedOperations.add(op)) {
			// Also add to current scope
			mCurrentScope.add(op);
			
			printOperation(op);
		}
	}

	public void printSource(XlimSource source) {
		XlimOutputPort output=source.asOutputPort();
		if (output!=null) {
			XlimInstruction instr=output.getParent();
			XlimOperation op=instr.isOperation();
			if (op!=null) {
				printOperationIfNeeded(op);
			}
		}
	}
	
	
	private class BlockElementVisitor implements XlimBlockElement.Visitor<Object, Object> {

		public Object visitOperation(XlimOperation op, Object arg) {
			printOperationIfNeeded(op);
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
	
	private class OutputPortRenaming extends XmlAttributeFormatter.PlugIn<XlimOutputPort> {

		private Map<XlimOutputPort,Integer> mMapping=new HashMap<XlimOutputPort,Integer>();
		
		public OutputPortRenaming() {
			super(XlimOutputPort.class);
		}

		@Override
		public String getAttributeValue(XlimOutputPort port) {
			Integer index=mMapping.get(port);
			if (index==null) {
				index=sNextId++;
				mMapping.put(port,index);
			}
			return "temp"+index;
		}
		
		void removeAll(Iterable<? extends XlimOutputPort> ports) {
			for (XlimOutputPort port: ports)
				mMapping.remove(port);
		}
	}
}
