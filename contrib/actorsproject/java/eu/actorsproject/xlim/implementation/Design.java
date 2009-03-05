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

/**
 * Simple and stupid implementation of XlimDesign
 */
package eu.actorsproject.xlim.implementation;

import java.util.List;
import java.util.ArrayList;


import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.XlimInitValue;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.XlimTopLevelPort.Direction;
import eu.actorsproject.xlim.dependence.CallGraph;

class Design implements XlimDesign {

	private String mName;
	private Factory mFactory;
	private CallGraph mCallGraph;
	
	protected ArrayList<XlimTopLevelPort> mInputPorts, mOutputPorts, mInternalPorts;
	protected ArrayList<XlimStateVar> mStateVars;
	protected ArrayList<XlimTaskModule> mTasks;
	protected ArrayList<XmlElement> mChildren;
	
	public Design(String name, Factory factory) {
		mName = name;
		mFactory = factory;
		mInputPorts = new ArrayList<XlimTopLevelPort>();
		mOutputPorts = new ArrayList<XlimTopLevelPort>();
		mInternalPorts = new ArrayList<XlimTopLevelPort>();
		mStateVars = new ArrayList<XlimStateVar>();
		mTasks = new ArrayList<XlimTaskModule>();
		mChildren = new ArrayList<XmlElement>();
	}

	@Override
	public String getTagName() {
		return "design";
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return mChildren;
	}

	@Override
	public String getAttributeDefinitions() {
		return "name=\"" + mName + "\"";
	}

	@Override
	public String getName() {
		return mName;
	}


	@Override
	public List<? extends XlimTopLevelPort> getInputPorts() {
		return mInputPorts;
	}

	@Override
	public List<? extends XlimTopLevelPort> getOutputPorts() {
		return mOutputPorts;
	}

	@Override
	public List<? extends XlimTopLevelPort> getInternalPorts() {
		return mInternalPorts;
	}

	@Override
	public List<? extends XlimStateVar> getStateVars() {
		return mStateVars;
	}

	@Override
	public List<? extends XlimTaskModule> getTasks() {
		return mTasks;
	}


	@Override
	public XlimTaskModule getActionScheduler() {
		return mCallGraph.getAutoStartNode().getTask();
	}

	@Override
	public XlimStateVar addStateVar(String sourceName, XlimInitValue initValue) {
		XlimStateVar stateVar = mFactory.createStateVar(sourceName, initValue);
		mStateVars.add(stateVar);
		mChildren.add(stateVar);
		return stateVar;
	}

	@Override
	public XlimTaskModule addTask(String kind, String name, boolean autostart) {
		XlimTaskModule task = mFactory.createTask(kind,name,autostart,null);
		mTasks.add(task);
		mChildren.add(task);
		return task;
	}

	@Override
	public XlimTopLevelPort addTopLevelPort(String name, Direction dir, XlimType type) {
		XlimTopLevelPort port = mFactory.createTopLevelPort(name,dir,type);
		switch (dir) {
		case in:
			mInputPorts.add(port);
			break;
		case out:
			mOutputPorts.add(port);
			break;
		case internal:
			mInternalPorts.add(port);
			break;
		}
		mChildren.add(port);
		return port;
	}
	
	@Override
	public void removeTopLevelPort(XlimTopLevelPort port) {
		switch (port.getDirection()) {
		case in:
			mInputPorts.remove(port);
			break;
		case out:
			mOutputPorts.remove(port);
			break;
		case internal:
			mInternalPorts.remove(port);
			break;
		}
		mChildren.remove(port);
	}
	
	@Override
	public void removeStateVar(XlimStateVar stateVar) {
		mStateVars.remove(stateVar);
		mChildren.remove(stateVar);
	}
	
	@Override
	public void removeTask(XlimTaskModule task) {
		mTasks.remove(task);
		mChildren.remove(task);
	}
	
	@Override
	public CallGraph createCallGraph() {
		mCallGraph=CallGraph.create(this);
		return mCallGraph;
	}
	
	@Override
	public CallGraph getCallGraph() {
		return mCallGraph;
	}
}
