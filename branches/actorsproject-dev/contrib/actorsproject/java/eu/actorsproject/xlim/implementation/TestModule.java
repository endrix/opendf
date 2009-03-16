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

import java.util.Collections;

import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimTestModule;
import eu.actorsproject.xlim.dependence.FixupContext;
import eu.actorsproject.xlim.dependence.TestOperator;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueOperator;
import eu.actorsproject.xlim.dependence.ValueUsage;

class TestModule extends ContainerModule implements XlimTestModule {

	private SourceValueUsage mDecision;
	private PhiContainerModule mParent;
	private ValueOperator mTestOp=new TestValueOperator();
	
	public TestModule(PhiContainerModule parent, Factory factory) {
		super("test",parent,factory);
		mParent=parent;
	}
	
	@Override
	public PhiContainerModule getParentModule() {
		return mParent;
	}
	
	@Override
	public XlimSource getDecision() {
		return mDecision.getSource();
	}

	@Override
	public void setDecision(XlimSource decision) {
		if (mDecision!=null)
			mDecision.setValue(null);  // remove from current list of accesses
		mDecision=new SourceValueUsage(decision, mTestOp);
	}
	
	@Override
	public <Result, Arg> Result accept(Visitor<Result, Arg> visitor, Arg arg) {
		return visitor.visitTestModule(this,arg);
	}
	
	@Override
	public ValueUsage getValueUsage() {
		return mDecision;
	}

	@Override
	public void removeReferences() {
		super.removeReferences();
		mDecision.setValue(null);
	}
	
	/**
	 * Sets dependence links (of stateful resources) in added code,
	 * computes the set of exposed uses and new values
	 * @param context
	 */
	@Override
	public void fixupAddedCode(FixupContext context) {
		super.fixupAddedCode(context);
		if (mDecision!=null && mDecision.getValue()==null)
			context.fixup(mDecision);
	}
	
	@Override
	protected void propagateToEndOfBlock(FixupContext context) {
		// Propagate to the value usage of the decision attribute
		context.propagateNewValue(mDecision);
	}
	
	
	@Override
	public String getAttributeDefinitions() {
		if (mDecision!=null)
			return super.getAttributeDefinitions() + " decision=\"" + getDecision().getUniqueId() + "\"";
		else
			return super.getAttributeDefinitions();
	}	

	/*
	 * A ValueOperator is required to get the usage of the "decision" value 
	 * into the data dependence graph
	 */

	private class TestValueOperator implements TestOperator {
		@Override
		public XlimTestModule getTestModule() {
			return TestModule.this;
		}
		
		@Override
		public String getOperatorName() {
			return "test";
		}

		@Override
		public String attributesToString() {
			return "";
		}

		@Override
		public XlimModule getParentModule() {
			return TestModule.this;
		}
		
		
		@Override
		public void removeReferences() {
			mDecision.setValue(null);
		}

		@Override
		public Iterable<? extends ValueUsage> getUsedValues() {
			if (mDecision!=null)
				return Collections.singletonList(mDecision);
			else
				return Collections.emptyList();
		}
		
		@Override
		public Iterable<? extends ValueNode> getInputValues() {
			if (mDecision!=null)
				return Collections.singletonList(mDecision.getValue());
			else
				return Collections.emptyList();
		}

		@Override
		public Iterable<? extends ValueNode> getOutputValues() {
			return Collections.emptyList();
		}

		@Override
		public <Result, Arg> Result accept(Visitor<Result, Arg> visitor, Arg arg) {
			return visitor.visitTest(this, arg);
		}
	}
}
