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

package eu.actorsproject.xlim.dependence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import eu.actorsproject.util.IntrusiveList;
import eu.actorsproject.util.Linkage;
import eu.actorsproject.xlim.XlimOperation;

public class CallSite  {

	private XlimOperation mOperation;
	private CallSiteLinkage mCallerLink=new CallSiteLinkage(); 
	private CallSiteLinkage mCalleeLink=new CallSiteLinkage();
	private ArrayList<ValueUsage> mInputValues=new ArrayList<ValueUsage>();
	private ArrayList<ValueNode> mOutputValues=new ArrayList<ValueNode>();
		
	public CallSite(XlimOperation operation) {
		mOperation=operation;
	}
	
	public XlimOperation getTaskCall() {
		return mOperation;
	}
	
	public CallNode getCaller() {
		return mCallerLink.getCallNode();
	}

	public CallNode getCallee() {
		return mCalleeLink.getCallNode();
	}

	public Iterable<ValueNode> getInputValues() {
		return new InputValueIteration(mInputValues);
	}
	
	public Iterable<ValueNode> getOutputValues() {
		return mOutputValues;
	}
	
	public ValueNode getInputValue(StateLocation location) {
		for (ValueUsage usage: mInputValues) {
			ValueNode usedValue=usage.getValue();
			if (usedValue.actsOnLocation()==location)
				return usedValue;
		}
		return null;
	}
	
	public ValueNode getOutputValue(StateLocation location) {
		for (ValueNode sideEffect: mOutputValues)
			if (sideEffect.actsOnLocation()==location)
				return sideEffect;
		return null;
	}
	
	public ValueNode getCalleeInput(ValueNode inputInCaller) {
		DataDependenceGraph ddg=getCallee().getDataDependenceGraph();
		Location location=inputInCaller.actsOnLocation();
		assert(location!=null && location.isStateLocation());
		return ddg.getInputValue(location.asStateLocation());
	}
	
	public ValueNode getCalleeOutput(ValueNode outputInCaller) {
		DataDependenceGraph ddg=getCallee().getDataDependenceGraph();
		Location location=outputInCaller.actsOnLocation();
		assert(location!=null && location.isStateLocation());
		return ddg.getOutputValue(location.asStateLocation());
	}
	
	public void createInputValues(Collection<StateLocation> stateCarriers) {
		for (StateLocation carrier: stateCarriers)
			mInputValues.add(new CallSiteStateUsage(carrier,mOperation.getValueOperator()));
	}
	
	public void createOutputValues(Collection<StateLocation> stateCarriers) {
		for (StateLocation carrier: stateCarriers)
			mOutputValues.add(new CallSiteSideEffect(carrier));
	}

	public void remove() {
		getCaller().markModified();
		mCallerLink.remove();
		mCalleeLink.remove();
		for (ValueNode output: mOutputValues) {
			Location location=output.actsOnLocation();
			assert(location!=null && location.isStateLocation());
			ValueNode input=getInputValue(location.asStateLocation());
			output.substitute(input);
			assert(output.isReferenced()==false);
		}
		mOutputValues.clear();
		for (ValueUsage input: mInputValues) {
			input.setValue(null); // removes links between definition and usage
		}
		mInputValues.clear();
	}

	public void removeStateAccess(StateLocation location) {
		Iterator<ValueUsage> pUsage=mInputValues.iterator();
		ValueNode inputValue=null;
		
		while (pUsage.hasNext()) {
			ValueUsage usage=pUsage.next();
			ValueNode usedValue=usage.getValue();
			if (usedValue.actsOnLocation()==location) {
				inputValue=usedValue;
				usage.out();
				pUsage.remove();
				break;
			}
		}
		
		if (inputValue==null)
			throw new IllegalArgumentException("No such state carrier");
		
		Iterator<ValueNode> pValue=mOutputValues.iterator();
		while (pValue.hasNext()) {
			ValueNode value=pValue.next();
			if (value.actsOnLocation()==location) {
				value.substitute(inputValue);
				pValue.remove();
				break;
			}
		}
	}
	
	void setCaller(CallNode caller, IntrusiveList<CallSite> list) {
		caller.markModified();
		mCallerLink.setCallNode(caller, list);
	}

	void setCallee(CallNode callee, IntrusiveList<CallSite> list) {
		mCalleeLink.setCallNode(callee, list);
	}
	
	public Iterable<ValueUsage> getUsedValues() {
		return mInputValues;
	}
		
	private class CallSiteLinkage extends Linkage<CallSite> {
		private CallNode mCallNode;
		
		@Override
		public CallSite getElement() {
			return CallSite.this;
		}
		
		CallNode getCallNode() {
			return mCallNode;
		}
		
		void setCallNode(CallNode callNode, IntrusiveList<CallSite> list) {
			if (mCallNode!=null)
				out();
			mCallNode=callNode;
			list.addLast(this);
		}
		
		void remove() {
			mCallNode=null;
			out();
		}
	}
	
	private class CallSiteStateUsage extends ValueUsage {
		private StateLocation mStateCarrier;
		
		CallSiteStateUsage(StateLocation carrier, ValueOperator op) {
			super(null /* no value set yet */);
			mStateCarrier=carrier;
		}

		@Override
		public boolean needsFixup() {
			return true;
		}
		
		@Override
		public Location getFixupLocation() {
			return mStateCarrier;
		}

		@Override
		public ValueOperator usedByOperator() {
			return getTaskCall().getValueOperator();
		}
		
		@Override
		public <Result,Arg> Result accept(Visitor<Result,Arg> visitor, Arg arg) {
			return visitor.visitCall(this,CallSite.this,arg);
		}
	}
	
	private class CallSiteSideEffect extends SideEffect {
		private StateLocation mLocation;
		
		CallSiteSideEffect(StateLocation location) {
			mLocation=location;
		}
		
		
		@Override
		public Location actsOnLocation() {
			return mLocation;
		}


		@Override
		public ValueOperator getDefinition() {
			return getTaskCall().getValueOperator();
		}
		
		
		@Override
		public ValueNode getDominatingDefinition() {
			return getInputValue(mLocation);
		}

		@Override
		public <Result,Arg> Result accept(Visitor<Result,Arg> visitor, Arg arg) {
			return visitor.visitCall(this,CallSite.this,arg);
		}
	}
}
