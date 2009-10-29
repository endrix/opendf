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

import eu.actorsproject.util.IntrusiveList;
import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimTaskModule;

public class CallNode {

	private IntrusiveList<CallSite> mCallers, mCallees;
	private DataDependenceGraph mDataDependenceGraph;
	private XlimTaskModule mTask;
	private boolean mModified;
	
	public CallNode(XlimTaskModule task) {
		mCallers=new IntrusiveList<CallSite>();
		mCallees=new IntrusiveList<CallSite>();
		mDataDependenceGraph=new DataDependenceGraph(this);
		mTask=task;
		mModified=true;
	}
	
	public Iterable<CallSite> getCallers() {
		return mCallers.asUnmodifiableList();
	}

	public Iterable<CallSite> getCallSites() {
		return mCallees.asUnmodifiableList();
	}

	public boolean isReferenced() {
		return (mCallers.isEmpty()==false);
	}
	
	public DataDependenceGraph getDataDependenceGraph() {
		return mDataDependenceGraph;
	}
	
	public void addCaller(CallSite caller) {
		caller.setCallee(this, mCallers);
	}
	
	public void addCallSite(CallSite callSite) {
		callSite.setCaller(this, mCallees);
		markModified();
	}
	
	public void removeAllCallers() {
		CallSite caller=mCallers.getFirst();
		while (caller!=null) {
			XlimOperation taskCall=caller.getTaskCall();
			XlimContainerModule parent=taskCall.getParentModule();
			parent.remove(taskCall);
			caller=mCallers.getFirst();
		}
	}
	
	public void removeAllCallSites() {
		CallSite callSite=mCallees.getFirst();
		while (callSite!=null) {
			XlimOperation taskCall=callSite.getTaskCall();
			XlimContainerModule parent=taskCall.getParentModule();
			parent.remove(taskCall);
			callSite=mCallees.getFirst();
		}
	}
	
	void markModified() {
		if (mModified==false) {
			mModified=true;
			for (CallSite callSite: mCallers)
				callSite.getCaller().markModified();
		}
	}
	
	boolean isModified() {
		return mModified;
	}
	
	public XlimTaskModule getTask() {
		return mTask;
	}
	
	/**
	 * Removes the record of this call node accessing a specific stateful resource
	 * As a side-effect, the input/output context of the associated data dependence
	 * graph and the data dependence surrounding all call sites are updated. 
	 * @param carrier, a state variable or a port
	 */
	public void removeStateAccess(StateLocation carrier) {
		for (CallSite caller: mCallers) {
			caller.removeStateAccess(carrier);
		}
		mDataDependenceGraph.remove(carrier);
	}
}
