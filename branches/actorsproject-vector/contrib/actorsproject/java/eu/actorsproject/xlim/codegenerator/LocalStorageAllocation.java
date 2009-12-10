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

import eu.actorsproject.xlim.XlimBlockElement;
import eu.actorsproject.xlim.XlimBlockModule;
import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimIfModule;
import eu.actorsproject.xlim.XlimInstruction;
import eu.actorsproject.xlim.XlimLoopModule;
import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimPhiContainerModule;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.dependence.PhiOperator;
import eu.actorsproject.xlim.dependence.TestOperator;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueOperator;
import eu.actorsproject.xlim.dependence.ValueUsage;

/**
 * Implements the storage allocation pass, which allocates temporaries for output ports
 */

public class LocalStorageAllocation implements XlimBlockElement.Visitor<Object,XlimModule> {

	protected LocalSymbolTable mLocalSymbols;
	protected OperationGenerator mPlugIn;
	private RealUseInModule mRealUseInModule;
	
	public LocalStorageAllocation(LocalSymbolTable localSymbols, OperationGenerator plugIn) {
		mLocalSymbols = localSymbols;
		mPlugIn = plugIn;
		mRealUseInModule=new RealUseInModule();
	}

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
		visitPhiNodes(m,inScopeOfModule);
		
		return null;
	}
	
	@Override
	public Object visitLoopModule(XlimLoopModule m, XlimModule inScopeOfModule)  {
		// phi-nodes belong to scope of parent
		visitPhiNodes(m,inScopeOfModule);
		
		// test-and body modules belong to scope of loop
		mLocalSymbols.createScope(m);
		allocateStorage(m.getTestModule(),inScopeOfModule);		
		allocateStorage(m.getBodyModule(), m);
		
		return null;
	}

    private void visitPhiNodes(XlimPhiContainerModule m, XlimModule inScopeOfModule) {
    	for (XlimInstruction phi: m.getPhiNodes()) {
    		createTemporary(phi.getOutputPort(0),inScopeOfModule);
    	}
    }
    
	@Override
	public Object visitOperation(XlimOperation op, XlimModule inScopeOfModule) {
		// Just allocate storage for statements
		// -expressions don't need temporaries.
		if (mustBeStatement(op)) {
			// Create temporaries for output ports
			for (XlimOutputPort port: op.getOutputPorts())
				createTemporary(port,inScopeOfModule);			
		}
		return null;
	}
	
	protected void createTemporary(XlimOutputPort output, XlimModule inScopeOfModule) {
		mLocalSymbols.createTemporaryVariable(output, inScopeOfModule);
	}
	
	protected boolean mustBeStatement(XlimOperation op) {
		
		if (op.getNumOutputPorts()!=1   // generateExpression can only handle scalars
			|| op.isReferenced()==false // Also print out unreferenced stuff (someone else should remove!)
			|| op.dependsOnLocation()   // operation depends on a side effect, make it a statement
			|| op.modifiesLocation()      // operation has a side effect
			|| mPlugIn.hasGenerateExpression(op)==false) // handler can't generate expressions
			return true;  // make it a statement!
		else {
			if (mPlugIn.reEvaluate(op)==false) {
				// if multiple or non-local references, make it a statement
				// FIXME: Not quite this simple, we get unnecessary temporaries for conditions.
				// Why? We get multiple references from phis (control dependence)
				ValueOperator valueOp=op.getValueOperator();
				ValueNode value=op.getOutputPort(0).getValue();
				XlimModule localModule=op.getParentModule();
				boolean foundFirstUse=false;
				for (ValueUsage use: value.getUses()) {
					XlimModule usedInModule=valueOp.accept(mRealUseInModule, use);
					if (usedInModule!=null)
						if (usedInModule!=localModule || foundFirstUse)
							return true; // Non-local usage or multiple uses
						else
							foundFirstUse=true;							
				}
			}
			
			// Else: qualified as an expression (needs no temporary)!
			// In particular, op has a unique reference or it can be re-evaluated
			return false; 
		}
	}
	
	/**
	 * RealUseInModule is used to identify a multiple and non-local uses of values
	 * (which implies that we need a temporary variable)
	 * 
	 * It finds the module, in which a value is used: 
	 * - The enclosing module of an operation that uses the value
	 * - The predecessor of a phi-node that corresponds to the use
	 *   (the then- or else-module of an If-module, the pre-header or the body of a loop)
	 * - The test module that uses the value as a condition (the "decision" attribute)
	 * - null (signifying no module/not a "real" use) for control dependences
	 *   (the dependence from condition to phi-nodes)
	 */
	private static class RealUseInModule implements ValueOperator.Visitor<XlimModule,ValueUsage> {

		public XlimModule visitOperation(XlimOperation xlimOp, ValueUsage use) {
			return xlimOp.getParentModule();
		}

		public XlimModule visitPhi(PhiOperator phi, ValueUsage use) {
			if (phi.getControlDependence()==use.getValue())
				return null;  // Not a "real" use
			else
				return use.usedInModule();
		}

		public XlimModule visitTest(TestOperator test, ValueUsage use) {
			return test.getTestModule();
		}
	}
}
