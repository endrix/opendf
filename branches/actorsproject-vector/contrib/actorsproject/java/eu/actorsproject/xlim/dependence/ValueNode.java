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
import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimType;

public abstract class ValueNode implements XmlElement {

	private IntrusiveList<ValueUsage> mUses=new IntrusiveList<ValueUsage>();

	/**
	 * @return the operator which defines this value (null for initial values)
	 */
	public abstract ValueOperator getDefinition();
		
	/**
	 * @return unique identifier of the value (for debug printouts)
	 */
	public abstract String getUniqueId();
	
	
	public Iterable<ValueUsage> getUses() {
		return mUses;
	}
	
	public boolean isReferenced() {
		return mUses.isEmpty()==false;
	}
	
	/**
	 * Adds an access to this value's list of accesses
	 * @param access
	 */
	public void addUsage(ValueUsage access) {
		mUses.addLast(access);
	}

	/**
	 * Substitutes "newValue" for this value in all the uses of this value
	 * (after calling this method this value is unreferenced).
	 * @param newValue
	 */
	public void substitute(ValueNode newValue) {
		if (newValue!=this) {
			ValueUsage use=mUses.getFirst();
			while (use!=null) {
				use.setValue(newValue);
				use=mUses.getFirst();
			}
		}
	}
	
	/**
	 * @return true iff the ValueNode represents a side effect
	 *         (i.e. is not among the output ports of an operation)
	 */
	public abstract boolean isSideEffect();
	
	/**
	 * @return the location, which a side-effect acts on (null, if
	 *         this ValueNode doesn't represent a side effect).
	 */
	public abstract Location actsOnLocation();
	
	/**
	 * @return the type of the value
	 */
	public abstract XlimType getType();
	
	/**
	 * @return the dominating definition, which is superseded by this ValueNode
	 *         null for values that correspond to OutputPorts,
	 *         by convention also null for initial values of XLIM tasks
	 */
	public abstract ValueNode getDominatingDefinition();

	// TODO: we would really need the type of a ValueNode, but that is complicated by
	// the lack of representation for aggregate types, currently only List(T)
	// /**
	//  * @return the type of the value (declared type of stateful resources)
	//  */
	// public abstract XlimType getType();
	
	public interface Visitor<Result,Arg> {
		Result visitInitial(ValueNode node, CallNode inCallNode, Arg arg);
		Result visitCall(ValueNode node, CallSite call, Arg arg);
		Result visitAssign(ValueNode node, ValueNode old, boolean killsOld, Arg arg);
		Result visitOther(ValueNode node, Arg arg);
	}
	
	public <Result,Arg> Result accept(Visitor<Result,Arg> visitor, Arg arg) {
		return visitor.visitOther(this,arg);
	}
}
