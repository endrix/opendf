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

package eu.actorsproject.xlim.codegenerator;

import eu.actorsproject.util.OutputGenerator;
import eu.actorsproject.xlim.XlimBlockElement;
import eu.actorsproject.xlim.XlimBlockModule;
import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimIfModule;
import eu.actorsproject.xlim.XlimInputPort;
import eu.actorsproject.xlim.XlimInstruction;
import eu.actorsproject.xlim.XlimLoopModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTopLevelPort;

/**
 * Generates code for a TaskModule and its sub-modules
 */
public abstract class LocalCodeGenerator implements ExpressionTreeGenerator {
	
	protected SymbolTable mActorScope;
	protected LocalSymbolTable mLocalSymbols;
	protected OperationGenerator mPlugIn;
	protected OutputGenerator mOutput;
	
	private BlockElementVisitor mVisitor;

	
	public LocalCodeGenerator(SymbolTable topLevelSymbols, 
			                  OperationGenerator plugIn, 
			                  OutputGenerator output) {
		mActorScope = topLevelSymbols;
		mLocalSymbols = new LocalSymbolTable();
		mPlugIn = plugIn;
		mOutput = output;
		mVisitor=new BlockElementVisitor();
	}

	public void translateTask(XlimTaskModule task) {
		storageAllocation(task);
		generateCode(task);
	}
	
	protected void storageAllocation(XlimTaskModule task) {
		LocalStorageAllocation storageAllocation=new LocalStorageAllocation(mLocalSymbols, mPlugIn);
		storageAllocation.allocateStorage(task);
	}
	
	protected void generateCode(XlimContainerModule m) {
		LocalScope scope=mLocalSymbols.getScope(m);
		if (scope!=null) {
			generateDeclaration(scope);
		}
		
		for (XlimBlockElement child: m.getChildren())
			child.accept(mVisitor, null);
	}

	protected abstract void generateDeclaration(LocalScope scope);
	protected abstract void generateIf(XlimIfModule m);
	protected abstract void generateLoop(XlimLoopModule m);
	protected abstract void generateStatement(XlimOperation stmt);
	protected abstract void generatePhi(XlimInputPort input, TemporaryVariable dest);

	/**
	 * @param temp
	 * prints a reference to a "temporary variable" 
	 */
	protected void print(TemporaryVariable temp) {
		mOutput.print(mActorScope.getReference(temp));
	}

	/**
	 * @param port
	 * prints a reference to an actor port or internal port
	 */
	@Override
	public void print(XlimTopLevelPort port) {
		mOutput.print(mActorScope.getReference(port));
	}
	
	/**
	 * @param stateVar
	 * prints a reference to a state variable
	 */
	@Override
	public void print(XlimStateVar stateVar) {
		mOutput.print(mActorScope.getReference(stateVar));
	}

	/**
	 * @param task
	 * prints a reference/call to a "task" (top-level module)
	 */
	@Override
	public abstract void print(XlimTaskModule task);	

	/**
	 * @param root Root of expression tree
	 * Generates an expression that represents an input port of an operation.
	 */
	@Override
	public void translateSubTree(XlimInputPort root) {
		generateExpression(root.getSource());
	}
	
	protected void generateExpression(XlimSource source) {
		XlimOutputPort port=source.isOutputPort();
		
		if (port!=null) {
			TemporaryVariable temp=mLocalSymbols.getTemporaryVariable(port);
			if (temp!=null) {
				// a temporary variable has been assigned to the port
				print(temp);
			}
			else {
				// The operation (parent) of port is not the root of a "statement"
				// (that would require generateStatement).
				// In particular it is not a phi-node. Generate code in-line:
				XlimOperation op=(XlimOperation) port.getParent();
				mOutput.print("(");
				mPlugIn.generateExpression(op,this);
				mOutput.print(")");
				// TODO: handle the parantheses in the plug-in?
			}
		}
		else
			print(source.isStateVar());
	}

	
	protected boolean isRoot(XlimOperation op) {
		if (op.getNumOutputPorts()==1) {
			TemporaryVariable temp=mLocalSymbols.getTemporaryVariable(op.getOutputPort(0));
			return (temp!=null);
		}
		else
			return true;
	}

	protected boolean isUseful(XlimOperation op) {
		return (op.isReferenced() || op.mayModifyState() || op.isRemovable()==false);
	}
	
	protected boolean needsElse(XlimIfModule m) {
		// Generate else-part if non-empty or if there are phi-nodes
		return (m.getElseModule().getChildren().iterator().hasNext()
				|| m.getPhiNodes().iterator().hasNext());
	}
	
	protected void visitPhiNodes(Iterable<? extends XlimInstruction> phiNodes, int fromPath) {
		for (XlimInstruction phi: phiNodes) {
			TemporaryVariable dest=mLocalSymbols.getTemporaryVariable(phi.getOutputPort(0));
			XlimInputPort input=phi.getInputPort(fromPath);
			generatePhi(input,dest);
		}
	}
		
	/**
	 * @param s
	 * prints a string to the resulting output stream
	 */
	@Override
	public void print(String s) {
		mOutput.print(s);
	}
	
	/**
	 * @param port
	 * prints a reference to the "temporary variable" that is allocated for an output port
	 */
	@Override
	public void print(XlimOutputPort port) {
		TemporaryVariable temp=mLocalSymbols.getTemporaryVariable(port);
		if (temp==null)
			throw new IllegalArgumentException("No storage assigned to "+port.getUniqueId());
		print(temp);
	}
		

	private class BlockElementVisitor implements XlimBlockElement.Visitor<Object,Object> {
		@Override
		public Object visitBlockModule(XlimBlockModule m, Object dummyArg) {
			generateCode(m);			
			return null;
		}

		@Override
		public Object visitIfModule(XlimIfModule m, Object dummyArg)  {
			generateIf(m);
			return null;
		}

		@Override
		public Object visitLoopModule(XlimLoopModule m, Object dummyArg)  {
			generateLoop(m);
			return null;
		}

		@Override
		public Object visitOperation(XlimOperation op, Object dummyArg) {
			if (isRoot(op))
				generateStatement(op);
			// else op is part of another expression tree
			return null;
		}
	}
}
