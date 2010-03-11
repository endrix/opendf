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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.io.XlimAttributeList;
import eu.actorsproject.xlim.io.ReaderContext;

public class InstructionSet implements OperationFactory {

	protected HashMap<String,List<OperationKind>> mOperations = 
		new HashMap<String,List<OperationKind>>();

	@Override
	public Operation createOperation(String name, 
                                     List<? extends XlimSource> inputs,
                                     ContainerModule parent) {
		OperationKind kind=getOperationKind(name, inputs);
		if (kind==null)
			throw new RuntimeException("no such operation \""+name+"\"");
		return kind.create(inputs, parent);
	}

	@Override
	public Operation createOperation(String name, 
                                     List<? extends XlimSource> inputs,
                                     List<? extends XlimOutputPort> outputs,
                                     ContainerModule parent) {
		OperationKind kind=getOperationKind(name, inputs, outputs);
		if (kind==null)
			throw new RuntimeException("no such operation \""+name+"\"");
		return kind.create(inputs, outputs, parent);
	}

	public OperationKind getOperationKind(String name, 
                                          List<? extends XlimSource> inputs) {
		return getOperationKind(name, inputs, null);
	}
	
	public OperationKind getOperationKind(String name, 
                                          List<? extends XlimSource> inputs,
        			                      List<? extends XlimOutputPort> outputs) {
		List<OperationKind> candidates=mOperations.get(name);
		if (candidates!=null)
			return resolve(candidates, inputs, outputs, name);
		else
			return null;  // No operation 'name'
	} 
	
	public void setAttributes(XlimOperation xlimOp, XlimAttributeList attributes, ReaderContext context) {
		assert(xlimOp instanceof Operation);
		Operation op=(Operation) xlimOp;
		op.setAttributes(attributes, context);
	}
	
	public void registerOperation(OperationKind operation) {
		String name=operation.getKindAttribute();
		List<OperationKind> l=mOperations.get(name);
		if (l==null) {
			l=new ArrayList<OperationKind>();
			mOperations.put(name, l);
		}
		l.add(operation);
	}
	
	private OperationKind resolve(List<OperationKind> candidates,
			                      List<? extends XlimSource> inputs,
			                      List<? extends XlimOutputPort> outputs,
			                      String name) {
		// System.out.println("resolve: "+signature(name,inputs,outputs));
		OperationKind bestCandidate=null;
		for (OperationKind kind: candidates) {
			// System.out.print("         "+kind.getSignature());
			if (kind.matches(inputs) && (outputs==null || kind.matchesOutputs(outputs))) {
				if (bestCandidate==null 
				    || bestCandidate.betterMatch(kind, inputs)==false)
					bestCandidate=kind; // try 'kind' instead...
				// System.out.println(" matches"+((bestCandidate==kind)? " (better)" : ""));
			}
			/*
			 * else {
			 *	if (kind.matches(inputs)==false)
			 *		System.out.print(" inputs");
			 *	else if (outputs!=null && kind.matchesOutputs(outputs)==false)
			 *		System.out.print(" outputs");
			 *	System.out.println(" don't match");
			 * }
			 */
		}
		
		if (bestCandidate==null) {
			throw new RuntimeException("resolve: no operation matching "+signature(name,inputs,outputs));
		}
		
		// Now we have established that bestCandidate is better than
		// all candidates that follow, is it also better than the preceding ones?
		for (OperationKind kind: candidates) {
			if (kind==bestCandidate)
				return bestCandidate; // Yes, a unique best match found
			else if (kind.matches(inputs) 
					 && (outputs==null || kind.matchesOutputs(outputs))
				     && bestCandidate.betterMatch(kind, inputs)==false) {
				 return null; // No, ambiguous matching 
			}
		}
		
		return bestCandidate;  // Unreachable (returns in for-loop)
	}
	
	private String signature(String name,
			                 List<? extends XlimSource> inputs,
                             List<? extends XlimOutputPort> outputs) {
		String result=name+": ";
		if (inputs.size()==1)
			result += inputs.get(0).getType();
		else {
			String delimiter="";
			result += "(";
			for (XlimSource source: inputs) {
				result += delimiter+source.getType();
				delimiter=",";
			}
			result += ")";
		}
		result += " -> ";
		if (outputs==null)
			result += "?";
		else if (outputs.size()==1)
			result += outputs.get(0).getType();
		else {
			String delimiter="";
			result += "(";
			for (XlimOutputPort port: outputs) {
				result+=delimiter+port.getType();
				delimiter=",";
			}
			result += ")";
		}
		return result;
	}
}
