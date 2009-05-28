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

package eu.actorsproject.xlim.codegenerator;

import java.util.HashMap;

import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.XlimType;

/**
 * A utility class -one way of implementing the SymbolTable interface
 */
public abstract class AbstractSymbolTable implements SymbolTable {

	protected HashMap<XmlElement,String> mNameMap;	
    protected String mTargetActorName;
    
	public AbstractSymbolTable() {
		mNameMap=new HashMap<XmlElement,String>();
	}

	@Override
	public void declareActorScope(XlimDesign design) {
		int index=0;
		mTargetActorName=createTargetName(design);
		for (XlimTopLevelPort port: design.getInputPorts())
			mNameMap.put(port, createTargetName(port, index++));
		index=0;
		for (XlimTopLevelPort port: design.getOutputPorts())
			mNameMap.put(port, createTargetName(port, index++));
		index=0;
		for (XlimTopLevelPort port: design.getInternalPorts())
			mNameMap.put(port, createTargetName(port, index++));
		index=0;
		for (XlimStateVar stateVar: design.getStateVars())
			mNameMap.put(stateVar, createTargetName(stateVar, index++));
		index=0;
		for (XlimTaskModule task: design.getTasks())
			mNameMap.put(task, createTargetName(task, index++));;
	}

	@Override
	public String getTargetActorName() {
		return mTargetActorName;
	}
	
	@Override
	public String getTargetName(XlimTopLevelPort element) {
		return get(element);
	}
	
	@Override
	public String getTargetName(XlimStateVar element) {
		return get(element);
	}
	
	@Override
	public String getTargetName(XlimTaskModule element) {
		return get(element);
	}

	private String get(XmlElement element) {
		String cName=mNameMap.get(element);
		if (cName==null)
			throw new IllegalArgumentException("Element has no name");
		return cName;
	}

	@Override
	public abstract String getTargetName(TemporaryVariable temp);
	
	/**
	 * @param port 
	 * @return a reference to 'port' (to be used in the generated code)
	 * 
	 * Default implementation is same as getTargetName() -override if needed
	 */
	@Override
	public String getReference(XlimTopLevelPort port) {
		return getTargetName(port); 
	}
	
	/**
	 * @param stateVar 
	 * @return a reference to 'stateVar' (to be used in the generated code)
	 * 
	 * Default implementation is same as getTargetName() -override if needed
	 */
	@Override
	public String getReference(XlimStateVar stateVar) {
		return getTargetName(stateVar);
	}
	
	/**
	 * @param task 
	 * @return a reference/call of 'task' (to be used in the generated code)
	 */
	@Override
	public abstract String getReference(XlimTaskModule task);
	
	/**
	 * @param stateVar 
	 * @return a reference to 'temp' (to be used in the generated code)
	 * 
	 * Default implementation is same as getTargetName() -override if needed
	 */
	@Override
	public String getReference(TemporaryVariable temp) {
		return getTargetName(temp);
	}
	
	@Override
	public abstract String getTargetTypeName(XlimType type);
	
	protected abstract String createTargetName(XlimDesign design);

	protected abstract String createTargetName(XlimTopLevelPort port, int index);

	protected abstract String createTargetName(XlimStateVar stateVar, int index);

	protected abstract String createTargetName(XlimTaskModule task, int index);

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
