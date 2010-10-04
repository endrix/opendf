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

package eu.actorsproject.xlim.implementation;

import java.util.Iterator;
import java.util.NoSuchElementException;

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimBlockElement;
import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimLoopModule;
import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.dependence.FixupContext;
import eu.actorsproject.xlim.dependence.ValueNode;

class LoopModule extends PhiContainerModule implements XlimLoopModule {

	ContainerModule mBodyModule;
		
	public LoopModule(ContainerModule parent) {
		super(parent);
		mBodyModule = new SubModule("body");
	}
	
	public XlimContainerModule getBodyModule() {
		return mBodyModule;
	}

	@Override
	protected boolean updateModuleLevel(AbstractModule parent) {
		if (super.updateModuleLevel(parent)) {
			getTestModule().updateModuleLevel(this);
			mBodyModule.updateModuleLevel(this);
			return true;
		}
		else
			return false;
	}
	/**
	 * Gets the predecessor module corresponding to the input ports of the phi-nodes
	 * @param path (0 or 1) pre-header and loop body, respectively
	 * @return the predecessor on the given path
	 */
	
	@Override
	public XlimContainerModule predecessorModule(int path) {
		assert(path==0 || path==1);
		return (path==0)? getParentModule() : mBodyModule;
	}
	
	public String getKind() {
		return "loop";
	}

	@Override
	boolean isLoop() {
		return true;
	}
	
	public <Result, Arg> Result accept(XlimModule.Visitor<Result, Arg> v, Arg arg) {
		return v.visitLoopModule(this, arg);
	}
	
	public <Result, Arg> Result accept(XlimBlockElement.Visitor<Result, Arg> v, Arg arg) {
		return v.visitLoopModule(this, arg);
	}
	
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		return "kind=\"loop\"";
	}
		
	/**
	 * Removes all references this module makes to state variables and output ports
	 */
	public void removeReferences() {
		super.removeReferences();
		mBodyModule.removeReferences();
	}
	
	/**
	 * Sets (or updates) dependence links (of stateful resources)
	 * computes the set of exposed uses and new values
	 * @param dominatingContext (keeps track of exposed uses and new definitions)
	 */
	@Override
	public void fixupAll(FixupContext dominatingContext) {
		// Create a sub-context (initially empty set of new values) and process the loop
		FixupContext loopContext=new FixupContext();
		getTestModule().fixupAll(loopContext);
		mBodyModule.fixupAll(loopContext);

		// Create phi-nodes and use them to resolve exposed uses in loop
		createStatePhiOperators(loopContext.getNewValues());
		Iterable<ValueNode> phiOutputs=getStatePhiOutputs();
		loopContext.resolveExposedUses(phiOutputs);
		loopContext.resolveExposedUses(getNormalPhiOutputs());
		
		// Fixup the (inputs of) the phi-nodes
		dominatingContext.fixup(getStatePhiInputs(/*path:*/ 0));
		loopContext.fixup(getStatePhiInputs(/*path:*/ 1));
		
		// Also the the "normal" phi-nodes (should they have state variables as source)
		dominatingContext.fixup(getNormalPhiInputs(/*path:*/ 0));
		loopContext.fixup(getNormalPhiInputs(/*path:*/ 1));
				
		// Finally add the exposed uses and the new values to the dominating context
		dominatingContext.fixup(loopContext);
		dominatingContext.setNewValues(phiOutputs);
		dominatingContext.setNewValues(getNormalPhiOutputs());
	}

	
	/**
	 * Resolves exposed uses by looking for definitions in reverse order
	 * starting from the predecessor of 'child'
	 * @param context
	 * @param child
	 * Called bottom-up: from child to its parent module
	 */

	@Override
	protected void resolveExposedUses(FixupContext context, AbstractModule child) {
		// Process the test module if we're coming from the body 
		TestModule test=getTestModule();
		if (child!=test)
			test.resolveExposedUses(context);
		
		// Process the phi-nodes
		context.resolveExposedUses(getStatePhiOutputs());
		
		// Then proceed to parent
		resolveInParent(context);
	}

	/**
	 * Propagates new values to uses in this element
	 * @param dominatingContext
	 */
	@Override
	public void propagateNewValues(FixupContext dominatingContext) {
		// Set initial values of phi-nodes and stop propagation of those values
		dominatingContext.propagateNewValues(getStatePhiInputs(/*path:*/ 0));
		dominatingContext.propagateNewValues(getNormalPhiInputs(/*path:*/ 0));
		dominatingContext.endPropagation(getStatePhiOutputs());
		
		if (dominatingContext.remainingNewValues()) {
			// Propagate new loop-invariants into loop using a sub-context with its own
			// copy of the set of new values
			FixupContext loopContext=dominatingContext.createPropagationSubContext();;
			getTestModule().propagateNewValues(loopContext);
			mBodyModule.propagateNewValues(loopContext);
			
			// To cover an awkward case: when a "normal" phi-node has a loop-invariant
			// state-variable as input from the back-edge
			loopContext.propagateNewValues(getNormalPhiInputs(/*path:*/ 1));
			
			// Note that there isn't the corresponding case for state-phis: if modified
			// in the loop, there is a phi at the beginning and no loop-invariants can slip in.
		}
	}
	
	/**
	 * Propagates new values in the elements following child
	 * @param context
	 * @param child
	 * Called bottom-up: from child to its parent module
	 */
	@Override
	protected void propagateNewValues(FixupContext context, AbstractModule child) {
		// Process body modules if we're coming from test module
		TestModule test=getTestModule();
		if (child==test) {
			mBodyModule.propagateNewValues(context);
		}
		else {
			assert(child==mBodyModule);
		}
	
		
		// propagate new values along back edge and stop propagation for those values
		context.propagateNewValues(getStatePhiInputs(/*path:*/ 1));
		context.propagateNewValues(getNormalPhiInputs(/*path:*/ 1));
		context.endPropagation(getStatePhiOutputs());
		// then there should be no remaining new values (since we don't support
		// creation of new phi-nodes). No need to proceed to parent, thus.
		assertNoRemainingNewValues(context);
	}
	
	public Iterable<? extends XmlElement> getChildren() {
		// returns a custom Iterable object, whose iterator first visits 
		// the phi-nodes, followed by the test- and body-modules
		return new Iterable<XmlElement>() {
			
			public Iterator<XmlElement> iterator() {
		
				return new Iterator<XmlElement>() {
					Iterator<PhiNode> mPhi=getPhiNodes().iterator();
					int module = 0;
					public boolean hasNext()  { return mPhi.hasNext() || module<2; }
					public void remove()      { throw new UnsupportedOperationException(); }
					public XmlElement next() {
						if (mPhi.hasNext())
							return mPhi.next();
						else if (module>=2)
							throw new NoSuchElementException();
						else if (module++ ==0) 
							return getTestModule();
						else 
							return mBodyModule;
					}
				};
			}
		};
	}
}
