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

import eu.actorsproject.util.IntrusiveList;
import eu.actorsproject.util.Linkage;
import eu.actorsproject.xlim.XlimInstruction;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimPhiContainerModule;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimStateCarrier;
import eu.actorsproject.xlim.dependence.FixupContext;
import eu.actorsproject.xlim.dependence.PhiOperator;
import eu.actorsproject.xlim.dependence.StatePhiOperator;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueUsage;

/**
 * The PhiContainerModule factors out functionality that is common to If- and LoopModules.
 */
abstract class PhiContainerModule extends AbstractModule 
                                  implements XlimPhiContainerModule, AbstractBlockElement {

	private ContainerModule mParent;
	private TestModule mTestModule;
	private PhiList<PhiNode> mPhiNodes;
	private PhiList<StatePhiOperator> mStatePhis; // state variables/ports
	
	protected PhiContainerModule(ContainerModule parent, Factory factory) {
		super(parent);
		mParent=parent;
		mTestModule = factory.createTestModule(this);
		mPhiNodes=new PhiList<PhiNode>();
		mStatePhis=new PhiList<StatePhiOperator>();
	}
	
	@Override
	public ContainerModule getParentModule() {
		return mParent;
	}
	
	@Override
	public TestModule getTestModule() {
		return mTestModule;
	}

	abstract boolean isLoop();
	
	public Iterable<PhiNode> getPhiNodes() {
		return mPhiNodes;
	}
	
	public Iterable<? extends StatePhiOperator> getStatePhiOperators() {
		return mStatePhis;
	}
	
	public XlimInstruction addPhiNode(XlimSource in1, XlimSource in2, XlimOutputPort out) {
		PhiNode phiNode = new PhiNode(in1,in2,out,this);
		mPhiNodes.addLast(phiNode);
		return phiNode;
	}
	
	public void removePhiNode(XlimInstruction instr) {
		// Check that this is one of my guys
		if ((instr instanceof PhiNode)==false || instr.getParentModule()!=this)
			throw new IllegalArgumentException("phi-node not contained in module");
		PhiNode phiNode=(PhiNode) instr;
		phiNode.removeReferences();
		phiNode.out();
	}
		
	@Override
	public Linkage<AbstractBlockElement> getLinkage() {
		return this;
	}

	@Override
	public AbstractBlockElement getElement() {
		return this;
	}
	
	@Override
	public void removeReferences() {
		mTestModule.removeReferences();
		for (PhiNode phi: mPhiNodes)
			phi.removeReferences();
		for (StatePhiOperator phi: mStatePhis)
			phi.removeReferences();
	}
	
	
	/**
	 * Resolves exposed uses by looking for definitions
	 * @param context
	 * Called top-down: from parent module 
	 */
	@Override
	public void resolveExposedUses(FixupContext context) {
		// Any output is from the phi-nodes (no need to look inside the module)
		context.resolveExposedUses(getStatePhiOutputs());
	}
	
	/**
	 * Creates phi-nodes (StatePhiOperator) for each of the stateful resources in carriers
	 * @param carriers
	 */
	protected void createStatePhiOperators(Iterable<XlimStateCarrier> carriers) {
		TestModule test=getTestModule();
		boolean isLoop=isLoop();
		for (XlimStateCarrier carrier: carriers) {
			StatePhiOperator phi=new StatePhiOperator(test,carrier,isLoop);
			mStatePhis.addLast(phi);
		}
	}
	
	protected Iterable<ValueNode> getStatePhiOutputs() {
		return mStatePhis.getOutputs();
	}
	
	protected Iterable<ValueUsage> getStatePhiInputs(int path) {
		return mStatePhis.getInputs(path);
	}
	
	protected Iterable<ValueUsage> getNormalPhiInputs(int path) {
		return mPhiNodes.getInputs(path);
	}
	
	protected void assertNoRemainingNewValues(FixupContext context) {
		if (context.remainingNewValues()) {
			// Currently we do not support creation of new phi-nodes, which would be
			// required if a patch (in a sub-module) introduces modification of a new
			// stateful resource.
			// Implementation of such support is complicated by the need of resolving
			// the other (unpatched) path of the phi-node.
			String firstName=context.getNewValues().iterator().next().getSourceName();
			throw new UnsupportedOperationException("Patch requires new phi-node: "+firstName);
		}
	}
	
	@Override
	public String toString() {
		XlimSource decision=mTestModule.getDecision();
		String arg=(decision!=null)? decision.getUniqueId() : "";
		return getKind()+"("+arg+") module M"+mUniqueId;
	}
	
	protected class SubModule extends ContainerModule {
		
		public SubModule(String kind, Factory factory) {
			super(kind, PhiContainerModule.this, factory);
		}
		
		@Override
		public AbstractModule getParentModule() {
			return PhiContainerModule.this;
		}

		@Override
		public <Result, Arg> Result accept(Visitor<Result, Arg> visitor, Arg arg) {
			return visitor.visitContainerModule(this,arg);
		}
	}	
}

class PhiList<T extends PhiOperator> extends IntrusiveList<T> {
	@Override
	public Iterator<T> iterator() {
		return new ForwardIteratorWithRemove() {
			@Override
			public void remove() {
				mCurr.getElement().removeReferences();
				super.remove();
			}
		};
	}

	Iterable<ValueNode> getOutputs() {
		return new OutputList();
	}
	
	Iterable<ValueUsage> getInputs(int path) {
		return new InputList(path);
	}
	
	abstract class IteratorWrapper<R> implements Iterator<R> {
		Iterator<T> p=iterator();
		
		@Override
		public boolean hasNext() { 
			return p.hasNext(); 
		}
		
		@Override
		public abstract R next();

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}		
	}

	/**
	 * Iteration over the output values
	 */

	class OutputList implements Iterable<ValueNode> {
		@Override
		public Iterator<ValueNode> iterator() {
			return new IteratorWrapper<ValueNode>() {
				@Override
				public ValueNode next() {
					return p.next().getOutput();
				}
			};
		}
	}
	
	
	/**
	 * Iteration over the ValueUsages (of one of the paths: 0 or 1)
	 */
	class InputList implements Iterable<ValueUsage> {
		int mPath;
		
		InputList(int path) {
			mPath=path;
		}
		
		@Override
		public Iterator<ValueUsage> iterator() {
			return new IteratorWrapper<ValueUsage>() {
				@Override
				public ValueUsage next() {
					return p.next().getUsedValue(mPath);
				}
			};
		}
	}
}
