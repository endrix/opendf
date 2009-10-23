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

import eu.actorsproject.util.OutputGenerator;
import eu.actorsproject.xlim.XlimBlockElement;
import eu.actorsproject.xlim.XlimBlockModule;
import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.XlimIfModule;
import eu.actorsproject.xlim.XlimInitValue;
import eu.actorsproject.xlim.XlimInputPort;
import eu.actorsproject.xlim.XlimLoopModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimPhiContainerModule;
import eu.actorsproject.xlim.XlimPhiNode;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTestModule;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.XlimType;

/**
 * Translates XLIM into "human readable" form
 */
public class XlimVisualPrinter extends OutputGenerator {

	private BlockElementVisitor mVisitor=new BlockElementVisitor();
	
	public XlimVisualPrinter(PrintStream output) {
		super(output);
	}
	
	public void printDesign(XlimDesign design) {
		println("design "+design.getName()+" {");
		increaseIndentation();
		for (XlimTopLevelPort port: design.getInputPorts())
			printPort(port);
		for (XlimTopLevelPort port: design.getOutputPorts())
			printPort(port);
		for (XlimTopLevelPort port: design.getInternalPorts())
			printPort(port);
		println();
		for (XlimStateVar stateVar: design.getStateVars())
			printStateVar(stateVar);
		for (XlimTaskModule task: design.getTasks()) {
			println();
			printTask(task);
		}
		decreaseIndentation();
		println("}");
	}

	public void printPort(XlimTopLevelPort port) {
		println(port.getTagName()+"<"+port.getType()+","+port.getDirection()+"> "+port.getSourceName()+";");
	}
	
	public void printStateVar(XlimStateVar stateVar) {
		XlimInitValue initValue=stateVar.getInitValue();
		XlimType elementType=initValue.getCommonElementType();
		print("stateVar<"+elementType+"> "+getSourceName(stateVar));
		
		if (initValue.getScalarType()!=null) {
			println("="+initValue.getScalarValue()+";");
		}
		else {
			int N=initValue.totalNumberOfElements();
			print("["+N+"]");
			if (initValue.isZero())
				println(";  // zero");
			else {
				println(" = {");
				increaseIndentation();
				printInitValue(initValue);
				decreaseIndentation();
				println();
				println("};");
			}
		}
	}
	
	private void printInitValue(XlimInitValue initValue) {
		String delimiter="";
		for (XlimInitValue child: initValue.getChildren()) {
			if (child.getScalarType()!=null) {
				print(delimiter);
				lineWrap(60);
				print(child.getScalarValue());
			}
			else {
				if (delimiter.length()!=0)
					println(delimiter);
				println("{");
				increaseIndentation();
				printInitValue(child);
				decreaseIndentation();
				println();
				print("}");
			}
			delimiter=",";
		}
	}
	
	public void printTask(XlimTaskModule task) {
		println(task.getName()+"() {");
		increaseIndentation();
		println("// kind=\""+task.getKind()+"\"");
		println("// autostart=\""+(task.isAutostart()? "yes" : "no")+"\"");
		printContainerModule(task);
		decreaseIndentation();
		println("}");
	}
	
	public void printBlockModule(XlimBlockModule module) {
		println("{");
		increaseIndentation();
		printContainerModule(module);
		decreaseIndentation();
		println("}");
	}
	
	public void printIfModule(XlimIfModule ifModule) {
		XlimTestModule testModule=ifModule.getTestModule();
		String decision=getSourceName(testModule.getDecision());
		
		println("// test module, decision=\""+decision+"\"");
		printContainerModule(testModule);
		println("if ("+decision+") {");
		increaseIndentation();
		printContainerModule(ifModule.getThenModule());
		decreaseIndentation();
		println("else {");
		increaseIndentation();
		printContainerModule(ifModule.getThenModule());
		decreaseIndentation();
		println("}");
		printPhiNodes(ifModule);
	}
	
	public void printLoopModule(XlimLoopModule loopModule) {
		XlimTestModule testModule=loopModule.getTestModule();
		String decision=getSourceName(testModule.getDecision());
		
		println("loop {");
		increaseIndentation();
		printPhiNodes(loopModule);
		println("// test module, decision=\""+decision+"\"");
		printContainerModule(testModule);
		decreaseIndentation();
		println("while ("+decision+") do");
		increaseIndentation();
		println("// loop body");
		printContainerModule(loopModule.getBodyModule());
		decreaseIndentation();
		println("}");
	}
	
	private void printPhiNodes(XlimPhiContainerModule module) {
		for (XlimPhiNode phi: module.getPhiNodes()) {
			XlimOutputPort out=phi.getOutputPort(0);
			println(out.getType()+" "+out.getUniqueId()+"=phi("+
					getSourceName(phi.getInputPort(0).getSource())+","+
					getSourceName(phi.getInputPort(1).getSource())+");");
		}
	}
	
	private void printOperation(XlimOperation op) {
		String delimiter;
		int N=op.getNumOutputPorts();
		if (N>=1) {
			if (N==1) {
				XlimOutputPort out=op.getOutputPort(0);
				print(out.getType()+" "+out.getUniqueId()+"=");
			}
			else {
				delimiter="(";
				for (XlimOutputPort out: op.getOutputPorts()) {
					print(delimiter+out.getUniqueId()+":"+out.getType());
					delimiter=",";
				}
				print(")=");	
			}
		}
		print(op.getKind()+"(");
		delimiter="";
		XlimTopLevelPort port=op.getPortAttribute();
		if (port!=null) {
			print(delimiter+port.getSourceName());
			delimiter=",";
		}
		XlimSource location=op.getStateVarAttribute();
		if (location!=null) {
			print(delimiter+getSourceName(location));
			delimiter=",";
		}
		XlimTaskModule task=op.getTaskAttribute();
		if (task!=null) {
			print(delimiter+task.getName());
			delimiter=",";
		}
		String value=op.getValueAttribute();
		if (value!=null) {
			print(delimiter+"\""+value+"\"");
			delimiter=",";
		}
		for (XlimInputPort in: op.getInputPorts()) {
			print(delimiter+getSourceName(in.getSource()));
			delimiter=",";
		}
		println(");");
	}
	
	
	private String getSourceName(XlimSource source) {
		XlimStateVar stateVar=source.isStateVar();
		if (stateVar!=null) {
			String sourceName=stateVar.getSourceName();
			if (sourceName!=null)
				return sourceName;
		}
		return source.getUniqueId();
	}
	
	public void printContainerModule(XlimContainerModule module) {
		for (XlimBlockElement element: module.getChildren()) {
			element.accept(mVisitor, null);
		}
	}
	
	class BlockElementVisitor implements XlimBlockElement.Visitor<Object,Object> {

		public Object visitBlockModule(XlimBlockModule m, Object arg) {
			printBlockModule(m);
			return null;
		}

		public Object visitIfModule(XlimIfModule m, Object arg) {
			printIfModule(m);
			return null;
		}

		public Object visitLoopModule(XlimLoopModule m, Object arg) {
			printLoopModule(m);
			return null;
		}

		public Object visitOperation(XlimOperation op, Object arg) {
			printOperation(op);
			return null;
		}
		
	}
}
