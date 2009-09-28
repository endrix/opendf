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

import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.dependence.CallNode;
import eu.actorsproject.xlim.dependence.DataDependenceGraph;
import eu.actorsproject.xlim.dependence.FixupContext;

class TaskModule extends ContainerModule implements XlimTaskModule {

	private String mName;
	private boolean mAutoStart;
	private CallNode mCallNode;
	private ArrayList<XlimTopLevelPort> mPorts;
	private ArrayList<Integer> mRates;
	
	public TaskModule(String kind,
				      String name,
			          boolean autoStart) {
		super(kind,null /* parent */);
		mName=name;
		mAutoStart=autoStart;
		mCallNode=new CallNode(this);
		mPorts=new ArrayList<XlimTopLevelPort>();
		mRates=new ArrayList<Integer>();
	}
	
	@Override
	public AbstractModule getParentModule() {
		return null;
	}
	
	@Override
	public XlimTaskModule getTask() {
		return this;
	}
	
	@Override
	public boolean isAutostart() {
		return mAutoStart;
	}

	@Override
	public String getName() {
		return mName;
	}
	
	@Override 
	public CallNode getCallNode() {
		return mCallNode;
	}
	
	
	@Override 
	public int getPortRate(XlimTopLevelPort port) {
		for (int i=0; i<mPorts.size(); ++i)
			if (mPorts.get(i)==port)
				return mRates.get(i);
		return 0;
	}

	@Override 
	public void setPortRate(XlimTopLevelPort port, int rate) {
		for (int i=0; i<mPorts.size(); ++i)
			if (mPorts.get(i)==port) {
				mRates.set(i, rate);
				return;
			}
		mPorts.add(port);
		mRates.add(rate);
	}

	@Override
	public <Result, Arg> Result accept(Visitor<Result, Arg> visitor, Arg arg) {
		return visitor.visitTaskModule(this,arg);
	}
	
	@Override
	protected void resolveInParent(FixupContext context) {
		// A TaskModule has no parent module, instead resolve using the 
		// initial values of the CallNode
		DataDependenceGraph ddg=mCallNode.getDataDependenceGraph();
		ddg.resolveExposedUses(context);
	}
	
	@Override
	protected void propagateInParent(FixupContext context) {
		// A TaskModule has no parent module, instead propagate to the 
		// output values of the CallNode
		DataDependenceGraph ddg=mCallNode.getDataDependenceGraph();
		ddg.propagateNewValues(context);
	}
	
	@Override
	public String getAttributeDefinitions() {
		return super.getAttributeDefinitions()
			+ " name=\"" + mName + "\" autostart=\"" + mAutoStart + "\"";			
	}	
	
	@Override
	public String toString() {
		return getKind()+"-module "+mName;
	}
}
