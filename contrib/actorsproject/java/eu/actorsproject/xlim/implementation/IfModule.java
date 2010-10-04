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

import java.util.HashSet;
import java.util.Iterator;

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimBlockElement;
import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimIfModule;
import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.dependence.FixupContext;
import eu.actorsproject.xlim.dependence.Location;

class IfModule extends PhiContainerModule implements XlimIfModule {

	private ContainerModule mThenModule, mElseModule;
	
	public IfModule(ContainerModule parent) {
		super(parent);
		mThenModule = new SubModule("then");
		mElseModule = new SubModule("else");
	}

	public XlimContainerModule getThenModule() {
		return mThenModule;
	}

	public XlimContainerModule getElseModule() {
		return mElseModule;
	}

	@Override
	protected boolean updateModuleLevel(AbstractModule parent) {
		if (super.updateModuleLevel(parent)) {
			getTestModule().updateModuleLevel(this);
			mThenModule.updateModuleLevel(this);
			mElseModule.updateModuleLevel(this);
			return true;
		}
		else
			return false;
	}
	
	/**
	 * Gets the predecessor module corresponding to the input ports of the phi-nodes
	 * @param path (0 or 1) meaning 'then' and 'else' modules, respectively
	 * @return the predecessor on the given path
	 */
	
	@Override
	public XlimContainerModule predecessorModule(int path) {
		assert(path==0 || path==1);
		return (path==0)? mThenModule : mElseModule;
	}
	
	public String getKind() {
		return "if";
	}

	@Override
	boolean isLoop() {
		return false;
	}
	
	public <Result, Arg> Result accept(XlimBlockElement.Visitor<Result, Arg> v, Arg arg) {
		return v.visitIfModule(this,arg);
	}

	public <Result, Arg> Result accept(XlimModule.Visitor<Result, Arg> v, Arg arg) {
		return v.visitIfModule(this,arg);
	}
	
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		return "kind=\"if\"";
	}

	/**
	 * Removes all references this module makes to state variables and output ports
	 */
	@Override
	public void removeReferences() {
		super.removeReferences();
		mThenModule.removeReferences();
		mElseModule.removeReferences();
	}

	/**
	 * Sets (or updates) dependence links (of stateful resources)
	 * computes the set of exposed uses and new values
	 * @param dominatingContext (keeps track of exposed uses and new definitions)
	 */
	@Override
	public void fixupAll(FixupContext dominatingContext) {
		getTestModule().fixupAll(dominatingContext);
		
		// Create sub-contexts for then/else parts (initially empty set of new values).
		FixupContext thenContext=dominatingContext.createFixupSubContext();
		FixupContext elseContext=dominatingContext.createFixupSubContext();

		// Process the sub-modules
		mThenModule.fixupAll(thenContext);
		mElseModule.fixupAll(elseContext);
		
		// Create phi-nodes for the union of new values in the two paths
		HashSet<Location> phis=new HashSet<Location>(thenContext.getNewValues());
		phis.addAll(elseContext.getNewValues());
		createStatePhiOperators(phis);
		
		// Fix-up the inputs of the state phi-nodes
		thenContext.fixup(getStatePhiInputs(/*path:*/ 0));
		elseContext.fixup(getStatePhiInputs(/*path:*/ 1));
		
		// Also the "normal" phi-nodes (should they have state variables as source)
		thenContext.fixup(getNormalPhiInputs(/*path:*/ 0));
		elseContext.fixup(getNormalPhiInputs(/*path:*/ 1));
		
		// Finally add the new values
		dominatingContext.setNewValues(getStatePhiOutputs());
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
		// Process the test module if we're coming from the then/else module
		TestModule test=getTestModule();
		if (child!=test)
			test.resolveExposedUses(context);
		
		// Then proceed to parent
		resolveInParent(context);
	}

	/**
	 * Propagates new values to uses in this element
	 * @param context
	 */
	@Override
	public void propagateNewValues(FixupContext context) {
		getTestModule().propagateNewValues(context);
		propagateFromTest(context);
	}
	
	/**
	 * Propagates new values into then/else and phi-nodes
	 * @param dominatingContext
	 */
	private void propagateFromTest(FixupContext dominatingContext) {
		// create a sub-context (with its own copy of the set of new values)
		FixupContext thenContext=dominatingContext.createPropagationSubContext();
		mThenModule.propagateNewValues(thenContext);
		if (thenContext.remainingNewValues()) {
			thenContext.propagateNewValues(getStatePhiInputs(/*path:*/ 0));
			// Also the "normal" phi-nodes (should they have state variables as source)
			thenContext.propagateNewValues(getNormalPhiInputs(/*path:*/ 0));
		}

		// create a sub-context (with its own copy of the set of new values)
		FixupContext elseContext=dominatingContext.createPropagationSubContext();
		mElseModule.propagateNewValues(elseContext);

		if (elseContext.remainingNewValues()) {
			elseContext.propagateNewValues(getStatePhiInputs(/*path:*/ 1));
			// Also the "normal" phi-nodes (should they have state variables as source)
			elseContext.propagateNewValues(getNormalPhiInputs(/*path:*/ 1));
		}
		
		dominatingContext.endPropagation(getStatePhiOutputs());
	}
	
	/**
	 * Propagates new values in the elements following child
	 * @param context
	 * @param child
	 * Called bottom-up: from child to its parent module
	 */
	@Override
	protected void propagateNewValues(FixupContext context, AbstractModule child) {
		// Process then/else modules if we're coming from test module
		TestModule test=getTestModule();
		if (child==test) {
			propagateFromTest(context);
		}
		else {
			if (child==mThenModule) {
				context.propagateNewValues(getStatePhiInputs(/*path:*/ 0));
				// Also the "normal" phi-nodes (should they have state variables as source)
				context.propagateNewValues(getNormalPhiInputs(/*path:*/ 0));
			}
			else {
				assert(child==mElseModule);
				context.propagateNewValues(getStatePhiInputs(/*path:*/ 1));
				// Also the "normal" phi-nodes (should they have state variables as source)
				context.propagateNewValues(getNormalPhiInputs(/*path:*/ 1));
			}
			context.endPropagation(getStatePhiOutputs());
		}
		// then there should be no remaining new values (since we don't support
		// creation of new phi-nodes). No need to proceed to parent, thus.
		assertNoRemainingNewValues(context);
		propagateInParent(context);
	}
	
	public Iterable<? extends XmlElement> getChildren() {
		// returns a custom Iterable object, whose iterator first visits 
		// the test-, then- and else-modules followed by the phi-nodes
		return new Iterable<XmlElement>() {
			
			public Iterator<XmlElement> iterator() {
		
				return new Iterator<XmlElement>() {
					Iterator<PhiNode> mPhi=getPhiNodes().iterator();
					int element = 0;
					public boolean hasNext()  { return element<=2 || mPhi.hasNext(); }
					public void remove()      { throw new UnsupportedOperationException(); }
					public XmlElement next() { 
						switch (element++) {
						case 0: return getTestModule();
						case 1: return mThenModule;
						case 2: return mElseModule;
						default:
							return mPhi.next();
						}
					}
				};
			}
		};
	}	
}
