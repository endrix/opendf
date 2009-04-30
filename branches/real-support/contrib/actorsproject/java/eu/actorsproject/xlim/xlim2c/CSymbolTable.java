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
	protected static final String sActorInstanceReference="thisActor";
	protected static final String sInputPortPrefix="IN";
	protected static final String sOutputPortPrefix="OUT";
	protected static final String sInternalPortPrefix="IO";
	protected static final String sActionPrefix="a";
	protected static final String sInitializerPrefix="init_";
	
	protected String mActorClassName;
	protected HashMap<XlimType, String> mTargetTypeNames;
	
	public CSymbolTable() {
		mTargetTypeNames=new HashMap<XlimType,String>();

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
	
	public String createActorClassName(String fileName) {
		int end=fileName.lastIndexOf('.');
		if (end>0) 
			fileName=fileName.substring(0, end);
		mActorClassName=sActorClassPrefix+createCName(fileName);
		return mActorClassName;
	}
	
	public String getActorClassName() {
		return mActorClassName;
	}
	
	public String getActorInstanceReference() {
		return sActorInstanceReference;
	}
	
	public String getReference(XlimTopLevelPort port) {
		return getActorInstanceReference()+"->"+getTargetName(port);
	}
	
	public String getReference(XlimStateVar stateVar) {
		return getActorInstanceReference()+"->"+getTargetName(stateVar);
	}

	public String getAggregateInitializer(XlimStateVar stateVar) {
		return sInitializerPrefix+getTargetName(stateVar);
	}

	public String getReference(XlimTaskModule task) {
		return getTargetName(task);
	}
	
	public String getTargetName(TemporaryVariable temp) {
		XlimOutputPort classLeader=temp.getClassLeader().getOutputPort();
		return classLeader.getUniqueId();
	}
	
	@Override
    protected String createTargetName(XlimDesign design) {
		return sActorNamePrefix+createCName(design.getName());
	}
	
	@Override
    protected String createTargetName(XlimTopLevelPort port, int index) {
		String prefix;
		switch (port.getDirection()) {
		case in:   prefix=sInputPortPrefix; break;
		case out:  prefix=sOutputPortPrefix; break;
		default:   prefix=sInternalPortPrefix;
		}
    	return prefix+index+createCName(port.getSourceName());
    }
    
	@Override
    protected String createTargetName(XlimStateVar stateVar, int index) {
    	return stateVar.getUniqueId()+createCName(stateVar.getSourceName());
    }
    
    @Override
    protected String createTargetName(XlimTaskModule task, int index) {
    	return sActionPrefix+index+createCName(task.getName());
    }    
}
