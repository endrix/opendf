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
import eu.actorsproject.xlim.XlimBlockElement;
import eu.actorsproject.xlim.XlimBlockModule;
import eu.actorsproject.xlim.XlimModule;

class BlockModule extends ContainerModule implements XlimBlockModule, AbstractBlockElement {

	ContainerModule mParent;
	
	public BlockModule(String kind, ContainerModule parent, Factory factory) {
		super(kind, parent, factory);
		mParent=parent;
	}

	@Override
	public ContainerModule getParentModule() {
		return mParent;
	}

	@Override
	public void setParentModule(ContainerModule parent) {
		mParent=parent;
		if (parent!=null)
			updateModuleLevel(parent);
	}
	
	@Override
	public void substituteStateValueNodes() {
		// Substitute StateValueNodes (definitions) in the operations that use them
		// so that this module can be moved
		for(AbstractBlockElement child: mChildren)
			child.substituteStateValueNodes();
	}
	
	@Override
	public <Result, Arg> Result accept(XlimBlockElement.Visitor<Result, Arg> visitor, Arg arg) {
		return visitor.visitBlockModule(this, arg);
	}
	
	@Override
	public <Result, Arg> Result accept(XlimModule.Visitor<Result, Arg> visitor, Arg arg) {
		return visitor.visitContainerModule(this, arg);
	}

	@Override
	public AbstractBlockElement getElement() {
		return this;
	}
	
	@Override
	public Linkage<AbstractBlockElement> getLinkage() {
		return this;
	}
}
