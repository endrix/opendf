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

import eu.actorsproject.util.Linkage;
import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTestModule;
import eu.actorsproject.xlim.dependence.FixupContext;

abstract class AbstractModule extends Linkage<AbstractBlockElement> implements XlimModule {

	private int mLevel;
	protected int mUniqueId;
	private static int mNextId;
	
	protected AbstractModule(AbstractModule parent) {
		if (parent!=null)
			mLevel=parent.mLevel+1;
		mUniqueId=mNextId++;
	}
	
	@Override
	public abstract AbstractModule getParentModule();

	@Override
	public XlimModule leastCommonAncestor(XlimModule module) {
		if (module instanceof AbstractModule) {
			AbstractModule m1=this;
			AbstractModule m2=(AbstractModule) module;
			
			while (m1.mLevel > m2.mLevel)
				m1=m1.getParentModule();
			while (m2.mLevel > m1.mLevel)
				m2=m2.getParentModule();
			while (m1!=m2) {
				m1=m1.getParentModule();
				m2=m2.getParentModule();
			}
			return m1;
		}
		else
			return null; // otherwise they can't share an ancestor
	}

	@Override
	public XlimTestModule getControlDependence() {
		AbstractModule m=getParentModule();
		while (m!=null) {
			XlimTestModule t=m.getTestModule();
			if (t!=null)
				return t;
			m=m.getParentModule();
		}
		return null;
	}
	
	protected XlimTestModule getTestModule() {
		return null;
	}
	
	@Override
	public XlimTaskModule getTask() {
		return getParentModule().getTask();
	}
	
	@Override
	public String getTagName() {
		return "module";
	}
	
	@Override
	public AbstractBlockElement getElement() {
		// This base class (AbstractModule) isn't an XlimBlockElement
		// and can't be added to XlimContainerModules
		throw new UnsupportedOperationException("not an XlimBlockElement");
	}
	
	@Override
	public abstract<Result,Arg> Result accept(Visitor<Result,Arg> visitor, Arg arg);
	
	/**
	 * Removes all references this module makes to state variables and output ports
	 */
	public void removeReferences() {
	}
	
	/**
	 * Resolves exposed uses by looking for definitions in reverse order
	 * starting from the predecessor of 'child'
	 * @param context
	 * @param child
	 * Called bottom-up: from child to its parent module
	 */
	
	protected abstract void resolveExposedUses(FixupContext context, 
			                                   AbstractModule child);
	
	/**
	 * Continues the bottom-up resolution of exposed uses after completing this module
	 * @param context
	 */
	protected void resolveInParent(FixupContext context) {
		if (context.remainingExposedUses())
			getParentModule().resolveExposedUses(context,this);
	}
	
	/**
	 * Propagates new values in the elements following child
	 * @param context
	 * @param child
	 * Called bottom-up: from child to its parent module
	 */
	protected abstract void propagateNewValues(FixupContext context, AbstractModule child);
	
	/**
	 * Continues the bottom-up propagation of new values after completing this module
	 * @param context
	 */
	protected void propagateInParent(FixupContext context) {
		if (context.remainingNewValues())
			getParentModule().propagateNewValues(context,this);
	}
	
	@Override
	public String toString() {
		return getKind()+"-module M"+mUniqueId;
	}	
}
