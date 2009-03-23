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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import eu.actorsproject.util.IntrusiveList;
import eu.actorsproject.util.Linkage;
import eu.actorsproject.xlim.XlimBlockElement;
import eu.actorsproject.xlim.XlimBlockModule;
import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimIfModule;
import eu.actorsproject.xlim.XlimLoopModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.dependence.FixupContext;


abstract class ContainerModule extends AbstractModule implements XlimContainerModule {

	private String mKind;
	private Factory mFactory;
	private boolean mMutex;
	
	protected ElementList mChildren;
	
	public ContainerModule(String kind, AbstractModule parent, Factory factory) {
		super(parent);
		mKind = kind;
		mFactory = factory;
		mChildren = new ElementList();
	}

	@Override
	public String getKind() {
		return mKind;
	}

	@Override
	public Iterable<AbstractBlockElement> getChildren() {
		return mChildren;
	}

	@Override
	public boolean isMutex() {
		return mMutex;
	}
	
	@Override
	public void setMutex() {
		mMutex = true;
	}
		
	@Override
	public String getAttributeDefinitions() {
		String mutex="";
		
		if (mMutex)
			mutex = " mutex=\"true\"";
		
		return "kind=\"" + mKind + "\"" + mutex;
	}

	@Override
	public XlimBlockModule addBlockModule(String kind) {
		BlockModule module = mFactory.createBlockModule(kind,this);
		mChildren.add(module);
		return module;
	}

	@Override
	public XlimIfModule addIfModule() {
		IfModule module = mFactory.createIfModule(this);
		mChildren.add(module);
		return module;
	}

	@Override
	public XlimLoopModule addLoopModule() {
		LoopModule module = mFactory.createLoopModule(this);
		mChildren.add(module);
		return module;
	}

	@Override
	public XlimOperation addOperation(String kind,
			List<? extends XlimSource> inputs,
			List<? extends XlimOutputPort> outputs) {
		Operation op = mFactory.createOperation(kind,inputs,outputs,this);
		mChildren.add(op);
		return op;
	}
	
	@Override
	public XlimOperation addOperation(String kind, List<? extends XlimSource> inputs) {
		Operation op = mFactory.createOperation(kind, inputs, this);
		mChildren.add(op);
		return op;
	}
	
	@Override
	public XlimOperation addOperation(String kind) {
		List<XlimSource> empty=Collections.emptyList();
		return addOperation(kind, empty);
	}
	
	@Override
	public XlimOperation addOperation(String kind, XlimSource input) {
		return addOperation(kind, Collections.singletonList(input));
	}
	
	@Override
	public XlimOperation addOperation(String kind, XlimSource input1, XlimSource input2) {
		ArrayList<XlimSource> inputs=new ArrayList<XlimSource>(2);
		return addOperation(kind, inputs);
	}
	
	@Override
	public XlimOperation addOperation(String kind, List<? extends XlimSource> inputs,  XlimType outT) {
		XlimOutputPort output=mFactory.createOutputPort(outT);
		return addOperation(kind, inputs, Collections.singletonList(output));
	}
	
	@Override
	public XlimOperation addOperation(String kind, XlimType outT) {
		List<XlimSource> inputs=Collections.emptyList();
		return addOperation(kind, inputs, outT);
	}
	
	@Override
	public XlimOperation addOperation(String kind, XlimSource input, XlimType outT) {
		return addOperation(kind, Collections.singletonList(input), outT);
	}
	
	@Override
	public XlimOperation addOperation(String kind, 
			                          XlimSource input1, XlimSource input2, XlimType outT) {
		List<XlimSource> inputs=new ArrayList<XlimSource>(2);
		inputs.add(input1);
		inputs.add(input2);
		return addOperation(kind, inputs, outT);
	}
	
	@Override
	public XlimOperation addLiteral(long value) {
		Operation op = mFactory.createLiteral(value, this);
		mChildren.add(op);
		return op;
	}
	
	@Override
	public XlimOperation addLiteral(boolean value) {
		Operation op = mFactory.createLiteral(value, this);
		mChildren.add(op);
		return op;
	}
	
	@Override
	public void remove(XlimBlockElement child) {
		mChildren.remove(checkChild(child));
	}
	
	private AbstractBlockElement checkChild(XlimBlockElement child) {
		if (child.getParentModule()==this && child instanceof AbstractBlockElement)
			return (AbstractBlockElement) child;
		else
			throw new IllegalArgumentException("element not contained in block module");
	}
	
	/**
	 * Removes all references this module makes to state variables and output ports
	 */
	@Override
	public void removeReferences() {
		for(AbstractBlockElement child: mChildren)
			child.removeReferences();
	}
	
	@Override
	public void startPatchAtEnd() {
		// We don't want to loose an ongoing patch (has to be fixed-up)!
		assert(!mChildren.isPatched());
		// Otherwise, this is simple: the default patch point is at the end.
	}
	
	@Override
	public void startPatchBefore(XlimBlockElement child) {
		mChildren.startPatchBefore(checkChild(child).getLinkage());
	}

	@Override
	public void startPatchAfter(XlimBlockElement element) {
		AbstractBlockElement child=checkChild(element);
		mChildren.startPatchBefore(child.getLinkage().getNext());
	}

	/**
	 * Fixes up the data dependence graph after the most recent set 
	 * of added elements. The fix-up progresses down into added sub-modules
	 * and into following code that uses possible new values.
	 */
	@Override
	public void completePatchAndFixup() {
		// TODO: optimize the common case of trivial patches (no state carriers/no sub-modules)
		FixupContext context=new FixupContext();
		Iterator<AbstractBlockElement> pResolve=mChildren.resolveIterator();
		Iterator<AbstractBlockElement> pPropagate=mChildren.propagateIterator();
		
		// As far as possible fix-up uses in the patch
		fixupAddedCode(context);
		
		// Try to resolve exposed uses by code prior to the patch
		resolveExposedUses(context, pResolve);
		resolveInParent(context);
		
		// Propagate new values to code that follows the patch
		propagateNewValues(context, pPropagate);
		propagateToEndOfBlock(context);
		propagateInParent(context);
	}	 
	
	/**
	 * Sets dependence links (of stateful resources) in added code,
	 * computes the set of exposed uses and new values
	 * @param context
	 */
	public void fixupAddedCode(FixupContext context) {
		Iterator<AbstractBlockElement> p=mChildren.fixupIterator();
		while (p.hasNext()) {
			AbstractBlockElement element=p.next();
			element.fixupAddedCode(context);
		}
		mChildren.closePatch();
	}
	
	/**
	 * Resolves exposed uses by looking for definitions
	 * @param context
	 * Called top-down: from parent module 
	 */
	public void resolveExposedUses(FixupContext context) {
		resolveExposedUses(context, mChildren.reverseIterator());
	}
	
	private void resolveExposedUses(FixupContext context, Iterator<AbstractBlockElement> p) {
		while (p.hasNext() && context.remainingExposedUses()) {
			p.next().resolveExposedUses(context);
		}
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
		assert(child.getParentModule()==this 
			   && child instanceof AbstractBlockElement);
		AbstractBlockElement element=(AbstractBlockElement) child;
		Linkage<AbstractBlockElement> start=element.getLinkage().getPrevious();
		
		resolveExposedUses(context, mChildren.reverseIterator(start));
		resolveInParent(context);
	}
	
	/**
	 * Propagates new values to uses in this element
	 * @param context
	 * Called top-down: from parent module
	 */
	public void propagateNewValues(FixupContext context) {
		propagateNewValues(context, mChildren.iterator());
		propagateToEndOfBlock(context);
	}
	
	private void propagateNewValues(FixupContext context, Iterator<AbstractBlockElement> p) {
		while (p.hasNext() && context.remainingNewValues()) {
			p.next().propagateNewValues(context);
		}
	}
	
	protected void propagateToEndOfBlock(FixupContext context) {
		// Nothing to do in the general case
	}
	
	/**
	 * Propagates new values in the elements following child
	 * @param context
	 * @param child
	 * Called bottom-up: from child to its parent module
	 */
	@Override
	protected void propagateNewValues(FixupContext context, AbstractModule child) {
		assert(child.getParentModule()==this 
			   && child instanceof AbstractBlockElement);
		AbstractBlockElement element=(AbstractBlockElement) child;
		Linkage<AbstractBlockElement> start=element.getLinkage().getNext();
		
		propagateNewValues(context, mChildren.iterator(start));
		propagateToEndOfBlock(context);
		propagateInParent(context);	
	}
}

class ElementList extends IntrusiveList<AbstractBlockElement> {
	
	private Linkage<AbstractBlockElement> mLastFixup;
	private Linkage<AbstractBlockElement> mEndOfPatch;
	
	ElementList() {
		mLastFixup=mHead;
		mEndOfPatch=mHead;
	}

	@Override
	public Iterator<AbstractBlockElement> iterator() {
		return new ForwardIteratorWithRemove() {
			@Override
			public void remove() {
				updateBeforeRemove(mCurr);
				mCurr.getElement().removeReferences();
				super.remove();
			}
		};
	}
	
	void add(AbstractBlockElement element) {
		Linkage<AbstractBlockElement> linkage=element.getLinkage();
		linkage.precede(mEndOfPatch);
	}
	
	void remove(AbstractBlockElement element) {
		Linkage<AbstractBlockElement> linkage=element.getLinkage();
		updateBeforeRemove(linkage);
		element.removeReferences();
		element.getLinkage().out();	
	}

	private void updateBeforeRemove(Linkage<AbstractBlockElement> linkage) {
		if (linkage==mLastFixup)
			mLastFixup=mLastFixup.getPrevious();
		if (linkage==mEndOfPatch)
			mEndOfPatch=mEndOfPatch.getNext();
	}

	void startPatchBefore(Linkage<AbstractBlockElement> endOfPatch) {
		assert(!isPatched());  // We don't want to loose an ongoing patch! 
		mEndOfPatch=endOfPatch;
		mLastFixup=endOfPatch.getPrevious();
	}
	
	boolean isPatched() {
		// Unless last fixup and end of patch are adjacent, there's a patch
		return mEndOfPatch.getPrevious()!=mLastFixup;
	}

	void closePatch() {
		mLastFixup=mHead.getPrevious();
		mEndOfPatch=mHead;
	}
	
	
	/**
	 * @return Iterator from first to last added element
	 */
	Iterator<AbstractBlockElement> fixupIterator() {
		return iterator(mLastFixup.getNext(), mEndOfPatch);
	}
	
	/**
	 * @return Iterator from end of patch to end of list
	 */
	Iterator<AbstractBlockElement> propagateIterator() {
		return iterator(mEndOfPatch);
	}
	
	/**
	 * @return reverse iterator from start of patch (non-inclusive) to 
	 * start of list
	 */
	Iterator<AbstractBlockElement> resolveIterator() {
		return reverseIterator(mLastFixup);
	}
}

