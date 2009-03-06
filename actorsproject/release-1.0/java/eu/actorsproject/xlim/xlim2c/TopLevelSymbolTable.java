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

import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.util.NativeTypePlugIn;

/**
 * @author ecarvon
 * Determines the naming of actor ports, state variables, tasks and temoraries
 */

public class TopLevelSymbolTable {
	
	protected static final String sActorInstanceName="thisActor";
	protected static final String sActorClassNamePrefix="Actor";
	protected static final String sInputPortPrefix="IN";
	protected static final String sOutputPortPrefix="OUT";
	protected static final String sInternalPortPrefix="IO";
	protected static final String sActionPrefix="a";
	protected static final String sInitializerPrefix="init_";
	
	protected HashMap<XmlElement,String> mNameMap;	
    
    public TopLevelSymbolTable() {
    	mNameMap=new HashMap<XmlElement,String>();
    }
    
    public void declareTopLevelElements(XlimDesign design) {
    	mNameMap.put(design, createCName(design));
    	int index=0;
    	for (XlimTopLevelPort port: design.getInputPorts())
    		mNameMap.put(port, createCName(port, index++));
    	index=0;
    	for (XlimTopLevelPort port: design.getOutputPorts())
    		mNameMap.put(port, createCName(port, index++));
    	index=0;
    	for (XlimTopLevelPort port: design.getInternalPorts())
    		mNameMap.put(port, createCName(port, index++));
    	for (XlimStateVar stateVar: design.getStateVars())
    		mNameMap.put(stateVar, createCName(stateVar));
    	index=0;
    	for (XlimTaskModule task: design.getTasks())
    		mNameMap.put(task, createCName(task,index++));
    }
    
    /**
     * @param element top-level element: a state variable, tasks or an internal port (but actor ports have no name) 
     * @return "C name" that corresponds to the element
     */
    public String getCName(XmlElement element) {
    	String cName=mNameMap.get(element);
    	if (cName==null)
    		throw new IllegalArgumentException("Element has no name");
    	return cName;
    }
    
    public String getCName(XlimType type) {
    	if (type.isBoolean())
    		return "bool_t";
    	else {
    		assert(type.isInteger());
    		return "int"+type.getSize()+"_t";
    	}
    }
    
	public String getActorInstanceName() {
		return sActorInstanceName;
	}
	
	public String getReference(XlimTopLevelPort port) {
		return getActorInstanceName()+"->"+getCName(port);
	}
	
	public String getReference(XlimStateVar stateVar) {
		return getActorInstanceName()+"->"+getCName(stateVar);
	}

	public String getAggregateInitializer(XlimStateVar stateVar) {
		return sInitializerPrefix+getCName(stateVar);
	}

	public String getReference(XlimTaskModule task) {
		return getCName(task);
	}
	
	protected String createCName(XlimDesign design) {
		return sActorClassNamePrefix+createCName(design.getName());
	}
	
	protected String createCName(XlimTopLevelPort port, int index) {
		String prefix;
		switch (port.getDirection()) {
		case in:   prefix=sInputPortPrefix; break;
		case out:  prefix=sOutputPortPrefix; break;
		default:   prefix=sInternalPortPrefix;
		}
    	return prefix+index+createCName(port.getSourceName());
    }
    
    protected String createCName(XlimStateVar stateVar) {
    	return stateVar.getUniqueId()+createCName(stateVar.getSourceName());
    }
    
    protected String createCName(XlimTaskModule task, int index) {
    	return sActionPrefix+index+createCName(task.getName());
    }
    
	/**
	 * @param xlimName Name used in XLIM, possibly null or containing weird characters
	 * @return string containing only alpha-numericals and '_' (not guaranteed to be unique 
	 *         nor non-empty, but useful as suffix).
	 */
	protected static String createCName(String xlimName) {
		if (xlimName==null)
			return "";
		else {
			StringBuffer buf=new StringBuffer(xlimName);
			for (int i=0; i<buf.length(); ++i) {
				char c=buf.charAt(i);
				if (!(c>='a' && c<='z' || c>='A' && c<='Z' || c>='0' && c<='9')) {
					buf.setCharAt(i, '_');
				}
			}
			return "_"+buf.toString();
		}
	}
}
