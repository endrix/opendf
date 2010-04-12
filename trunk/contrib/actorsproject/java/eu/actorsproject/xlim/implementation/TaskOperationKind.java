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

import java.util.Collection;
import java.util.List;


import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.dependence.CallNode;
import eu.actorsproject.xlim.dependence.CallSite;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueUsage;
import eu.actorsproject.xlim.io.XlimAttributeList;
import eu.actorsproject.xlim.io.ReaderContext;
import eu.actorsproject.xlim.type.TypeRule;


/**
 * @author ecarvon
 *
 */
public class TaskOperationKind extends OperationKind {
	private String mAttributeName;
	
	protected TaskOperationKind(String kindAttribute, 
			                 TypeRule typeRule,
			                 String attributeName) {
		super(kindAttribute, typeRule);
		mAttributeName=attributeName;
	}
		
	@Override
	public boolean dependsOnLocation(Operation op) {
		return true;
	}
	
	@Override
	public boolean modifiesLocation(Operation op) {
		return true;
	}
	
	@Override
	public boolean mayModifyState(Operation op) {
		return true;
	}
	
	@Override
	public Operation create(List<? extends XlimSource> inputs,
			                    List<? extends XlimOutputPort> outputs,
			                    ContainerModule parent) {
		return new TaskOperation(this,inputs,outputs,parent);
	}
	
	@Override
	public String getAttributeDefinitions(XlimOperation op, XmlAttributeFormatter formatter) {
		XlimTaskModule task=op.getTaskAttribute();
		if (task!=null) {
			String name=task.getName();
			if (name!=null)
				return super.getAttributeDefinitions(op, formatter)+" "+mAttributeName+"=\""+name+"\"";
		}
		return super.getAttributeDefinitions(op, formatter);
	}
	
	@Override
	public void setAttributes(XlimOperation op,
			                  XlimAttributeList attributes, 
			                  ReaderContext context) {
		String ident=getRequiredAttribute(mAttributeName,attributes);
		XlimTaskModule task=context.getTask(ident);
		if (task!=null) {
		    op.setTaskAttribute(task);
		}
		else {
			throw new RuntimeException("No such task/action: \""+ident+"\"");
		}
	}
}

class TaskOperation extends Operation {
	private XlimTaskModule mTask;
	private CallSite mCallSite;
	
	public TaskOperation(OperationKind kind,
			Collection<? extends XlimSource> inputs,
			Collection<? extends XlimOutputPort> outputs,
			ContainerModule parent) {
		super(kind,inputs,outputs,parent);
		mCallSite=new CallSite(this);
	}
	
	@Override
	public XlimTaskModule getTaskAttribute() {
		return mTask;
	}
	
	@Override
	public CallSite getCallSite() {
		return mCallSite;
	}
					
	@Override
	public Iterable<ValueUsage> getUsedValues() {
		return mCallSite.getUsedValues();
	}
	
	@Override
	public Iterable<ValueNode> getInputValues() {
		return mCallSite.getInputValues();
	}
	
	@Override
	public Iterable<? extends ValueNode> getOutputValues() {
		return mCallSite.getOutputValues();
	}
	
	@Override
	public boolean setTaskAttribute(XlimTaskModule task) {
		if (mTask!=null)
			throw new IllegalStateException("task attribute already set"); // Do this once!
		mTask=task;
		CallNode myNode=getParentModule().getTask().getCallNode();
		CallNode calledNode=task.getCallNode();
		myNode.addCallSite(mCallSite);
		calledNode.addCaller(mCallSite);
		return true;
	}

	@Override
	public void removeReferences() {
		super.removeReferences();
		mCallSite.remove();
	}
	
	/**
	 * @return additional attributes to show up in debug printouts
	 */
	@Override
	public String attributesToString() {
		if (mTask!=null)
			return mTask.getName();
		else
			return "";
	}
}