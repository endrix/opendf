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

import java.util.Collections;

import eu.actorsproject.util.Linkage;
import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimModule;

public abstract class ValueUsage extends Linkage<ValueUsage> implements XmlElement {

	protected ValueNode mValue;
	
	public ValueUsage(ValueNode value) {
		mValue=value;
		if (value!=null)
			value.addUsage(this);
	}
		
	/**
	 * @return the ValueOperator that uses the value (null for the "final" usage
	 *         of state values that represent the output of a CallNode)
	 */
	public abstract ValueOperator usedByOperator();
	
	/**
	 * @return the ValueNode, referred to by this ValueUsage
	 */
	public ValueNode getValue() {
		return mValue;
	}
	
	/**
	 * Changes the value node, referred to by this ValueUsage
	 * @param value new ValueNode
	 */
	public void setValue(ValueNode value) {
		if (mValue!=null)
			out(); // remove from current list of accesses
		mValue=value;
		if (value!=null)
			value.addUsage(this);
	}
	
	/**
	 * @return true iff this ValueUsage represents the use of a Location,
	 *         that has to be fixed-up after creating or patching the code.
	 *         
	 * The purpose of fixing-up references is to set the value to the
	 * immediately preceding side effect that acts on the Location.
	 */
	public abstract boolean needsFixup();
	
	/**
	 * @return Location to be used for fixing up this ValueUsage
	 *         (null if needsFixup() returns false) 
	 *
	 * The purpose of fixing-up references is to set the value to the
	 * immediately preceding side effect that acts on the Location.
	 */
	public abstract Location getFixupLocation();
	
	/**
	 * @return the XlimModule, in which the usage is made 
	 * 
	 * In most cases this is the parent module of the operator that uses the value.
	 * Two exceptions:
	 * 1) The "final" usage of a state variable/port, which represents the output of
	 *    a CallNode/TaskModule. It has no associated operator. Instead the value is
	 *    considered used in the XlimTaskModule, itself.
	 * 2) PhiNodes, in which the usage is considered to take place in one of the
	 *    predecessors: the pre-header or the body of a LoopModule,
	 *    and the then/else modules of an IfModule.
	 *    This not only models what is actually going on, but this is also required to
	 *    maintain the property that a value can only be used in a context that is
	 *    dominated by the definition of the value.
	 */
	public XlimModule usedInModule() {
		return usedByOperator().usedInModule(this);
	}
		
	public interface Visitor<Result,Arg> {
		Result visitCall(ValueUsage use, CallSite call, Arg arg);
		Result visitAssign(ValueUsage use, ValueNode newValue, boolean killsUse, Arg arg);
		Result visitFinal(ValueUsage use, CallNode inCallNode, Arg arg);
		Result visitOther(ValueUsage use, Arg arg);
	}
	
	public <Result,Arg> Result accept(Visitor<Result,Arg> visitor, Arg arg) {
		return visitor.visitOther(this,arg);
	}
	
	/* Implementation of Linkage<ValueAccess> */
	
	@Override
	public ValueUsage getElement() {
		return this;
	}
	
	/* Implementation of XmlElement */
	
	@Override
	public String getTagName() {
		return "ValueUsage";
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		return mValue.getAttributeDefinitions(formatter);
	}
}
