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

package eu.actorsproject.xlim.xlim2c;

import java.util.Iterator;


import eu.actorsproject.util.OutputGenerator;
import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimBlockElement;
import eu.actorsproject.xlim.XlimBlockModule;
import eu.actorsproject.xlim.XlimIfModule;
import eu.actorsproject.xlim.XlimInputPort;
import eu.actorsproject.xlim.XlimInstruction;
import eu.actorsproject.xlim.XlimLoopModule;
import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTestModule;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueUsage;

public class TaskGenerator {

	protected TopLevelSymbolTable mTopLevelSymbols;
	protected LocalSymbolTable mLocalSymbols;
	protected TaskGeneratorPlugIn mPlugIn;
	protected OutputGenerator mOutput;
	
	public TaskGenerator(TopLevelSymbolTable topLevelSymbols,
			             TaskGeneratorPlugIn plugIn,
			             OutputGenerator output) {
		mTopLevelSymbols=topLevelSymbols;
		mLocalSymbols=new LocalSymbolTable();
		mPlugIn=plugIn;
		mOutput=output;
	}
	
	public void translateTask(XlimTaskModule task) {
		storageAllocation(task);
		generateCode(task);
	}
	
	protected void storageAllocation(XlimTaskModule task) {
		StorageAllocation storageAllocation=new StorageAllocation();
		storageAllocation.allocateStorage(task);
	}
	
	protected void generateCode(XlimTaskModule task) {
		CodeGeneration cg=new CodeGeneration();
		cg.generateCode(task);
	}
	
	protected void translateStatement(XlimOperation root) {
		String comment=isUseful(root)? "" : " /* unused */";
		mPlugIn.generateStatement(root,treeGenerator);
		mOutput.println(";" + comment);
	}
	
	protected void translateExpression(XlimSource source) {
		XlimOutputPort port=source.isOutputPort();
		
		if (port!=null) {
			TemporaryVariable temp=mLocalSymbols.getTemporaryVariable(port);
			if (temp!=null) {
				// a temporary variable has been assigned to the port
				mOutput.print(temp.getCName());
			}
			else {
				// The operation (parent) of port is not the root of a "statement"
				// (that would require generateStatement).
				// In particular it is not a phi-node. Generate code in-line:
				XlimOperation op=(XlimOperation) port.getParent();
				mOutput.print("(");
				mPlugIn.generateExpression(op,treeGenerator);
				mOutput.print(")");
			}
		}
		else
			mOutput.print(mTopLevelSymbols.getReference(source.isStateVar()));
	}
	
	protected boolean isUseful(XlimOperation op) {
		return (op.isReferenced() || op.mayModifyState() || op.isRemovable()==false);
	}
	
	protected boolean generatePhi(XlimInputPort input, TemporaryVariable dest, String comment) {
		XlimSource source=input.getSource();
		XlimOutputPort port=source.isOutputPort();
		if (port!=null) {
			// source and destination represented by same temporary?
			TemporaryVariable temp=mLocalSymbols.getTemporaryVariable(port);
			if (temp==dest) {
				mOutput.println("/* "+dest.getCName()+"=COPY("+dest.getCName()+"); */");
				return false;
			}
		}
		
		mOutput.print(dest.getCName()+"=COPY(");
		translateExpression(source);
		mOutput.println(" /* PHI */);"+comment);
		return true;
	}
	
	protected ExpressionTreeGenerator treeGenerator= new ExpressionTreeGenerator() {
		public void print(String s) {
			mOutput.print(s);
		}

		public void print(XlimOutputPort port) {
			TemporaryVariable temp=mLocalSymbols.getTemporaryVariable(port);
			if (temp==null)
				throw new IllegalArgumentException("No storage assigned to "+port.getUniqueId());
			mOutput.print(temp.getCName());
		}

		public void print(XlimTopLevelPort port) {
			mOutput.print(mTopLevelSymbols.getReference(port));
		}

		public void print(XlimStateVar stateVar) {
			mOutput.print(mTopLevelSymbols.getReference(stateVar));

		}

		public void print(XlimTaskModule task) {
			mOutput.print(mTopLevelSymbols.getReference(task)+"("+
					      mTopLevelSymbols.getActorInstanceName()+")");
		}

		
		public void translateSubTree(XlimInputPort root) {
			translateExpression(root.getSource());
		}
	};
	
	/**
	 * @author ecarvon
	 * Implements the storage allocation pass, which allocates temporaries for output ports
	 */
	protected class StorageAllocation implements XlimBlockElement.Visitor<Object,XlimModule> {
		public void allocateStorage(XlimTaskModule task) {
			allocateStorage(task,null);
		}
		
		protected void allocateStorage(XlimContainerModule m, XlimModule inScopeOfModule) {
			if (inScopeOfModule==null) {
				// Create a new scope unless m belongs to scope of parent
				inScopeOfModule=m;
				mLocalSymbols.createScope(m);
			}
			for (XlimBlockElement child: m.getChildren())
				child.accept(this, inScopeOfModule);

		}
		
		@Override
		public Object visitBlockModule(XlimBlockModule m, XlimModule inScopeOfModule) {
			allocateStorage(m,inScopeOfModule);
			return null;
		}
		
		@Override
		public Object visitIfModule(XlimIfModule m, XlimModule inScopeOfModule)  {
			// test-module belongs to scope of parent
			allocateStorage(m.getTestModule(),inScopeOfModule);
			// then-module creates its own scope
			allocateStorage(m.getThenModule(),null);
			// else-module creates its own scope
			allocateStorage(m.getElseModule(),null);
			// phi-nodes belong to scope of parent
			for (XlimInstruction phi: m.getPhiNodes())
				visitPhiNode(phi,inScopeOfModule); 
			return null;
		}
		
		@Override
		public Object visitLoopModule(XlimLoopModule m, XlimModule inScopeOfModule)  {
			// test-and body modules belong to scope of loop
			mLocalSymbols.createScope(m);
			allocateStorage(m.getTestModule(),inScopeOfModule);		
			allocateStorage(m.getBodyModule(), m);
			// phi-nodes belong to scope of parent
			for (XlimInstruction phi: m.getPhiNodes())
				visitPhiNode(phi,inScopeOfModule);
			return null;
		}
		
		@Override
		public Object visitOperation(XlimOperation op, XlimModule inScopeOfModule) {
			if (mustBeStatement(op))
				for (int i=0; i<op.getNumOutputPorts(); ++i)
					mLocalSymbols.createTemporaryVariable(op.getOutputPort(i),inScopeOfModule);
			return null;
		}
		
		protected boolean mustBeStatement(XlimOperation op) {
			
			if (op.getNumOutputPorts()!=1   // generateExpression can only handle scalars
				|| op.isReferenced()==false // Also print out unreferenced stuff (someone else should remove!)
				|| op.mayAccessState()      // operation may access state, make it a statement
				|| mPlugIn.hasGenerateExpression(op)==false) // handler can't generate expressions
				return true;  // make it a statement!
			else {
				if (mPlugIn.reEvaluate(op)==false) {
					// if multiple or non-local references, make it a statement
					// FIXME: Not quite this simple, we get unnecessary temporaries for conditions.
					// Why? We get multiple references from phis (control dependence)
					// Another problem is that a phi-node is modelled as a use in If/Loop, but
					// is actually a use in predecessor blocks (then/else/preheader/body)
					// We would have to (a) model only "true" uses (b) allow for different modules per used value 
					ValueNode value=op.getOutputPort(0).getValue();
					Iterator<? extends ValueUsage> pUse=value.getUses().iterator();
					XlimModule localModule=op.getParentModule();
					if (pUse.next().getModule()!=localModule || pUse.hasNext())
						return true;
				}
				
				// Else: qualified as an expression!
				// In particular, op has a unique reference or it can be re-evaluated
				return false; 
			}
		}
		
		protected void visitPhiNode(XlimInstruction phi, XlimModule inScopeOfModule) {
			mLocalSymbols.createTemporaryVariable(phi.getOutputPort(0), inScopeOfModule);
		}		
	}
	
	/**
	 * @author ecarvon
	 * Implements the code generation pass
	 */
	protected class CodeGeneration implements XlimBlockElement.Visitor<Object,Object> {
		
		public void generateCode(XlimContainerModule m) {
			Scope scope=mLocalSymbols.getScope(m);
			if (scope!=null) {
				scope.generateDeclaration(mOutput, mTopLevelSymbols);
			}
			
			for (XlimBlockElement child: m.getChildren())
				child.accept(this, null);
		}
		
		public Object visitBlockModule(XlimBlockModule m, Object dummyArg) {
			generateCode(m);			
			return null;
		}
		
		public Object visitIfModule(XlimIfModule m, Object dummyArg)  {
			XlimTestModule test=m.getTestModule();
			
			generateCode(test);
			mOutput.print("if (");
			translateExpression(test.getDecision());
			mOutput.println(") {");

			mOutput.increaseIndentation();
			generateCode(m.getThenModule());
			visitPhiNodes(m.getPhiNodes(),0);
			mOutput.decreaseIndentation();
			mOutput.println("}");

			// Generate else-part if non-empty or if there are phi-nodes
			if (m.getElseModule().getChildren().iterator().hasNext()
				|| m.getPhiNodes().iterator().hasNext()) {
				mOutput.println("else {");
				mOutput.increaseIndentation();
				generateCode(m.getElseModule());
				visitPhiNodes(m.getPhiNodes(),1);
				mOutput.decreaseIndentation();
				mOutput.println("}");
			}
			return null;
		}
		
		public Object visitLoopModule(XlimLoopModule m, Object dummyArg)  {
			XlimTestModule test=m.getTestModule();
			
			visitPhiNodes(m.getPhiNodes(),0);
			mOutput.println("while (1) {");
			mOutput.increaseIndentation();			
			
			Scope scope=mLocalSymbols.getScope(m);
			if (scope!=null) {
				scope.generateDeclaration(mOutput, mTopLevelSymbols);
			}
			
			generateCode(m.getTestModule());
			mOutput.print("if (!");
			translateExpression(test.getDecision());
			mOutput.println(") break;");
			
			generateCode(m.getBodyModule());
			
			visitPhiNodes(m.getPhiNodes(),1);
			mOutput.decreaseIndentation();
			mOutput.println("}");
			return null;
		}
		
		public Object visitOperation(XlimOperation op, Object dummyArg) {
			if (isRoot(op))
				translateStatement(op);
			// else op is part of another expression tree
			return null;
		}
		
		protected boolean isRoot(XlimOperation op) {
			if (op.getNumOutputPorts()==1) {
				TemporaryVariable temp=mLocalSymbols.getTemporaryVariable(op.getOutputPort(0));
				return (temp!=null);
			}
			else
				return true;
		}
		
		protected void visitPhiNodes(Iterable<? extends XlimInstruction> phiNodes, int fromPath) {
			for (XlimInstruction phi: phiNodes) {
				TemporaryVariable dest=mLocalSymbols.getTemporaryVariable(phi.getOutputPort(0));
				XlimInputPort input=phi.getInputPort(fromPath);
				String comment=(phi.isReferenced())? "" : " /* unused */";
				generatePhi(input,dest,comment);
			}
		}		
	}

}
