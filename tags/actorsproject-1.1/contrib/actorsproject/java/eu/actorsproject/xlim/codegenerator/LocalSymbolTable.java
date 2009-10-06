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
 * 
 */
package eu.actorsproject.xlim.codegenerator;

import java.util.HashMap;


import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueUsage;

/**
 * The symbol table maintains the mapping from output ports to temporary variables
 * Temporary variables are organized in scopes, which correspond to block modules.
 * Loop modules and if-modules live in the scope of their surrounding block module.
 */
public class LocalSymbolTable {
	
	private HashMap<XlimModule,LocalScope> mModuleMap=new HashMap<XlimModule,LocalScope>();
	private HashMap<XlimOutputPort,TemporaryVariable> mPortMap=new HashMap<XlimOutputPort,TemporaryVariable>();
	
	/**
	 * Creates scope of module m if it not already exists
	 * @param m  Module
	 * @return   Scope of module
	 */
	public LocalScope createScope(XlimModule m) {
		LocalScope scope=mModuleMap.get(m);
	    if (scope==null) {
	    	scope=new LocalScope();
	    	mModuleMap.put(m,scope);
	    }
	    return scope;
	}

	/**
	 * @param m
	 * @return Scope of given module (null if none created, in particular if-modules 
	 * have no scope of their own, though their parent --a block module-- may have).
	 */

	public LocalScope getScope(XlimModule m) {
		return mModuleMap.get(m);
	}

	/**
	 * @param m
	 * @return The scope that immediately encloses module m
	 */
	protected LocalScope getEnclosingScope(XlimModule m) {
		LocalScope scope=mModuleMap.get(m);
		while (m!=null && scope==null) {
			m=m.getParentModule();
			scope=mModuleMap.get(m);
		}
		return scope;
	}
	
	/**
	 * Allocate a temporary variable for port if it not already exists
	 * @param port   an OutputPort
	 * @param module greatest descendant: temporary is created in the scope of
	 *               "module" or an enclosing scope (depending on references)
	 */

	public void createTemporaryVariable(XlimOutputPort port, XlimModule module) {
		if (mPortMap.get(port)==null) {
			ValueNode value=port.getValue();
			for (ValueUsage use: value.getUses()) {
				module=module.leastCommonAncestor(use.usedInModule());
			}				
			LocalScope scope=getEnclosingScope(module);
			TemporaryVariable temp=new TemporaryVariable(port);
			scope.add(temp);
			mPortMap.put(port,temp);
		}
	}
	
	/**
	 * @param port
	 * @return Allocation of given port (or null if none created)
	 */
	public TemporaryVariable getTemporaryVariable(XlimOutputPort port) {
		TemporaryVariable temp=mPortMap.get(port);
		if (temp!=null)
			return temp.getClassLeader();
		else
			return null;
	}
}