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

package eu.actorsproject.xlim.util;

import java.io.PrintStream;

import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimPhiContainerModule;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.dependence.CallNode;
import eu.actorsproject.xlim.dependence.DataDependenceGraph;
import eu.actorsproject.xlim.dependence.Location;
import eu.actorsproject.xlim.dependence.SideEffectPhiOperator;
import eu.actorsproject.xlim.dependence.StateLocation;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueOperator;

/**
 * Extends XlimVisualPrinter by also displaying data-dependences
 * relating to side effects.
 */
public class XlimDependencePrinter extends XlimVisualPrinter {

	public XlimDependencePrinter(PrintStream output) {
		super(output);
	}

	@Override
	protected void printInitialValues(XlimTaskModule task) {
		boolean first=true;
		CallNode callNode=task.getCallNode();
		DataDependenceGraph ddg=callNode.getDataDependenceGraph();
		for (StateLocation loc: ddg.getAccessedState()) {
			ValueNode value=ddg.getInputValue(loc);
			if (first) {
				println("// symbolic initial values of state variables and ports:");
				first=false;
			}
			println("// "+loc.getDebugName()+"="+value.getUniqueId());
		}
	}

	@Override
	protected void printFinalValues(XlimTaskModule task) {
		boolean first=true;
		CallNode callNode=task.getCallNode();
		DataDependenceGraph ddg=callNode.getDataDependenceGraph();
		for (StateLocation loc: ddg.getModifiedState()) {
			ValueNode value=ddg.getOutputValue(loc);
			if (first) {
				println("// final values of modified state variables and ports:");
				first=false;
			}
			println("// "+loc.getDebugName()+"="+value.getUniqueId());
		}
	}
	
	
	@Override
	protected void printPhiNodes(XlimPhiContainerModule module) {
		boolean first=true;
		
		super.printPhiNodes(module);
		
		for (SideEffectPhiOperator phi: module.getStatePhiOperators()) {
			ValueNode out=phi.getOutput();
			Location loc=out.getLocation();
			if (first) {
				println("// Join of side effects:");
				first=false;
			}
			println("// "+loc.getDebugName()+"="+out.getUniqueId()+" joins "+
					phi.getInputValue(0).getUniqueId()+" and "+
					phi.getInputValue(1).getUniqueId());
		}
	}

	@Override
	protected void printComment(XlimOperation xlimOp) {
		boolean first=true;
		String delimiter=" // uses: ";
		ValueOperator valueOp=xlimOp.getValueOperator();
		
		for (ValueNode input: valueOp.getInputValues()) {
			if (input.hasLocation()) {
				print(delimiter+currentValue(input));
				delimiter=", ";
				first=false;
			}
		}
		
		delimiter=first? "// defines: " : " defines: ";
		for (ValueNode output: valueOp.getOutputValues()) {
			if (output.hasLocation()) {
				print(delimiter+currentValue(output));
				delimiter=", ";
			}
		}
	}

	private String currentValue(ValueNode node) {
		Location loc=node.getLocation();
		
		if (loc.isStateLocation()==false) {
			assert(loc.hasSource());
			XlimOutputPort output=loc.getSource().asOutputPort();
			if (node==output.getValue()) {
				// ValueNode corresponds to OutputPort,
				// which means that this is the construction of a local vector
				// just return the name (name=name would otherwise result)
				return loc.getDebugName();
			}
		}
		// else: this is a mutated location
		return loc.getDebugName()+"="+node.getUniqueId();
	}
}
