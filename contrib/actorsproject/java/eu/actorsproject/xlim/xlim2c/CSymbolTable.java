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

package eu.actorsproject.xlim.xlim2c;

import java.util.HashMap;
import java.util.Map;

import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.codegenerator.AbstractSymbolTable;
import eu.actorsproject.xlim.codegenerator.TemporaryVariable;
import eu.actorsproject.xlim.type.TypeFactory;
import eu.actorsproject.xlim.util.Session;

/**
 * @author ecarvon
 * Determines the naming of actor ports, state variables, tasks and temoraries
 */

public class CSymbolTable extends AbstractSymbolTable {
	
	protected static final String sActorClassPrefix="ActorClass";
	protected static final String sActorNamePrefix="Actor";
	protected static final String sActorInstancePrefix="ActorInstance";
	protected static final String sActorInstanceReference="thisActor";
	protected static final String sInputPortPrefix="IN";
	protected static final String sOutputPortPrefix="OUT";
	protected static final String sInternalPortPrefix="IO";
	protected static final String sActionPrefix="a";
	protected static final String sInitializerPrefix="init_";
	
	protected String mActorName;
	protected Map<XlimType, String> mTargetTypeNames;
	private Map<Object,String> mGenericAttributes;
	
	public CSymbolTable() {
		mTargetTypeNames=new HashMap<XlimType,String>();
		mGenericAttributes=new HashMap<Object,String>();
		
		TypeFactory fact=Session.getTypeFactory();
		mTargetTypeNames.put(fact.createInteger(8),  "int8_t");
		mTargetTypeNames.put(fact.createInteger(16), "int16_t");
		mTargetTypeNames.put(fact.createInteger(32), "int32_t");
		mTargetTypeNames.put(fact.createInteger(64), "int64_t");
		mTargetTypeNames.put(fact.create("bool"),    "bool_t");
		mTargetTypeNames.put(fact.create("real"),    "double");
	}
	
	@Override
	public String getTargetTypeName(XlimType type) {
		String result=mTargetTypeNames.get(type);
		if (result==null)
			throw new RuntimeException("CSymbolTable: Unsupported target type: "+type.toString());
		return result;
    }
	
	public void createActorClassName(String fileName) {
		int end=fileName.lastIndexOf('.');
		if (end>0) 
			fileName=fileName.substring(0, end);
		mActorName=createCName(fileName);
	}
	
	public String getActorClassName() {
		return sActorClassPrefix + "_" + mActorName;
	}
	
	public String getActorInstanceType() {
		return sActorInstancePrefix + "_" + mActorName;
	}
	
	public String getActorInstanceReference() {
		return sActorInstanceReference;
	}
	
	public String getConstructorName() {
		return mActorName+"_constructor";
	}
	
	@Override
	public String getReference(XlimTopLevelPort port) {
		return getTargetName(port);
	}
	
	@Override
	public String getReference(XlimStateVar stateVar) {
		return getActorInstanceReference()+"->"+getTargetName(stateVar);
	}

	public String getAggregateInitializer(XlimStateVar stateVar) {
		return sInitializerPrefix+getTargetName(stateVar);
	}

	@Override
	public String getReference(XlimTaskModule task) {
		return getTargetName(task);
	}
	
	@Override
	public String getTargetName(TemporaryVariable temp) {
		XlimOutputPort classLeader=temp.getClassLeader().getOutputPort();
		return classLeader.getUniqueId();
	}
	
	@Override
	public String getGenericAttribute(Object key) {
		return mGenericAttributes.get(key);
	}

	public void setGenericAttribute(Object key, String value) {
		mGenericAttributes.put(key,value);
	}
	
	@Override
    protected String createTargetName(XlimDesign design) {
		return sActorNamePrefix + "_" + createCName(design.getName());
	}
	
	@Override
    protected String createTargetName(XlimTopLevelPort port, int index) {
		String prefix;
		switch (port.getDirection()) {
		case in:   prefix=sInputPortPrefix; break;
		case out:  prefix=sOutputPortPrefix; break;
		default:   prefix=sInternalPortPrefix;
		}
    	return prefix + index + "_" + createCName(port.getName());
    }
    
	@Override
    protected String createTargetName(XlimStateVar stateVar, int index) {
		String id=stateVar.getUniqueId();
		String cName=createCName(stateVar.getDebugName());
		if (cName.isEmpty())
			return id;
		else
			return id+"_"+cName;
    }
    
    @Override
    protected String createTargetName(XlimTaskModule task, int index) {
    	if (task.isAutostart())
    		return mActorName+"_action_scheduler";
    	else
    		return mActorName+"_"+sActionPrefix+index+"_"+createCName(task.getName());
    }    
}
