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
import java.util.List;

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.absint.AbstractDomain;
import eu.actorsproject.xlim.absint.Context;

/**
 * Represents a CallSite in a DependenceComponent
 */
public class CallComponent implements DependenceComponent {

	private DependenceComponent mCalleeComponent;
	private List<ValueNode> mInputsInCaller;
	private List<ValueNode> mOutputsInCaller;
	private CallNode mCallee;
	
	
	public CallComponent(DependenceComponent calleeComponent, 
			             List<ValueNode> inputsInCaller, 
			             List<ValueNode> outputsInCaller, 
			             CallNode callee) {
		mCalleeComponent = calleeComponent;
		mInputsInCaller = inputsInCaller;
		mOutputsInCaller = outputsInCaller;
		mCallee=callee;
	}


	public <T> boolean evaluate(Context<T> callerContext, AbstractDomain<T> domain) {
		Context<T> calleeContext=createInputContext(callerContext);
		
		mCalleeComponent.evaluate(calleeContext, domain);
		
		return propagateOutputContext(calleeContext, callerContext);
	}
	
	/**
	 * @param callerContext  the context of the caller
	 * @return               the input context of the callee
	 */
	private <T> Context<T> createInputContext(Context<T> callerContext) {
		Context<T> result=new Context<T>();
		DataDependenceGraph ddg=mCallee.getDataDependenceGraph();
		
		// Copy the actual inputs to the formal inputs...
		for (ValueNode inputInCaller: mInputsInCaller) {
			T aValue=callerContext.get(inputInCaller);
			Location location=inputInCaller.getLocation();
			assert(location!=null && location.isStateLocation());
			ValueNode inputInCallee=ddg.getInputValue(location.asStateLocation());
			result.put(inputInCallee, aValue);
		}
			
		return result;
	}
	
	/**
	 * Propagates outputs to the caller context
	 * 
	 * @param outputContext  the output context
	 * @param callerContext  the caller context
	 * @return               true if the caller context was updated 
	 */
	private <T> boolean propagateOutputContext(Context<T> outputContext,
			                                   Context<T> callerContext) {
		DataDependenceGraph ddg=mCallee.getDataDependenceGraph();
		boolean changed=false;
		
		//Copy the outputs from callee to the caller context
		for (ValueNode outputInCaller: mOutputsInCaller) {
			Location location=outputInCaller.getLocation();
			assert(location!=null && location.isStateLocation());
			ValueNode outputInCallee=ddg.getOutputValue(location.asStateLocation());
			T aValue=outputContext.get(outputInCallee);
			if (callerContext.put(outputInCaller, aValue))
				changed=true;
		}
	
		return changed;
	}
	
	@Override
	public String getTagName() {
		return "CallComponent";
	}
	
	@Override
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		return "target=\"" + mCallee.getTask().getName() + "\"";
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return Collections.emptyList();
	}
}
