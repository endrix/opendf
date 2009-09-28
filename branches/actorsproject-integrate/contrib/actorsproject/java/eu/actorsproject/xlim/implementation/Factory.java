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
import java.util.Collections;
import java.util.List;

import eu.actorsproject.xlim.XlimFactory;
import eu.actorsproject.xlim.XlimInitValue;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.XlimTopLevelPort.Direction;
import eu.actorsproject.xlim.type.TypeFactory;

// TODO: is the detour around Factory really needed for everything..?
class Factory implements XlimFactory {

	private InstructionSet mInstructionSet;
	private TypeFactory mTypeFactory;
	
	public Factory(InstructionSet instructionSet, TypeFactory typeFactory) {
		mInstructionSet = instructionSet;
		mTypeFactory = typeFactory;
	}
	
	public Design createDesign(String name) {
		return new Design(name, this);
	}

	public OutputPort createOutputPort(XlimType type) {
		return new OutputPort(type);
	}

	public XlimInitValue createInitValue(int value, XlimType type) {
		return new ScalarInitValue(value,type);
	}

	public XlimInitValue createInitValue(List<? extends XlimInitValue> aggregate) {
		return new InitValueList(aggregate);
	}

	public XlimTopLevelPort createTopLevelPort(String name, Direction dir, XlimType type) {
		return new TopLevelPort(name,dir,type);
	}

	public XlimStateVar createStateVar(String sourceName, XlimInitValue initValue) {
		return new StateVar(sourceName,initValue);
	}

	public TaskModule createTask(String kind, String name, boolean isAutoStart, AbstractModule parent) {
		return new TaskModule(kind,name,isAutoStart,parent,this);
	}
	
	public BlockModule createBlockModule(String kind, ContainerModule parent) {
		return new BlockModule(kind, parent, this);
	}

	public TestModule createTestModule(PhiContainerModule parent) {
		return new TestModule(parent, this);
	}

	public IfModule createIfModule(ContainerModule parent) {
		return new IfModule(parent, this);
	}

	public LoopModule createLoopModule(ContainerModule parent) {
		return new LoopModule(parent, this);
	}

	public Operation createOperation(String kind,
         	                         List<? extends XlimSource> inputs,
                                     List<? extends XlimOutputPort> outputs,
                                     ContainerModule parent)
	{
		OperationKind plugIn = mInstructionSet.getOperationPlugIn(kind);
		Operation op=plugIn.create(inputs, outputs, parent);
		return op;
	}
	
	public Operation createOperation(String kind,
			                         List<? extends XlimSource> inputs,
			                         ContainerModule parent)
	{
		OperationKind plugIn = mInstructionSet.getOperationPlugIn(kind);
		List<XlimType> types = plugIn.defaultOutputTypes(inputs);
		ArrayList<XlimOutputPort> outputs=new ArrayList<XlimOutputPort>();
		for (XlimType t: types)
			outputs.add(new OutputPort(t));
		Operation op=plugIn.create(inputs, outputs, parent);
		return op;
	}
	
	private Operation createLiteral(XlimType type, ContainerModule parent) {
		List<XlimSource> inputs=Collections.emptyList();
		XlimOutputPort output=new OutputPort(type);
		return createOperation("$literal_Integer", 
				               inputs, 
				               Collections.singletonList(output), 
				               parent);
	}
	
	public Operation createLiteral(boolean value, ContainerModule parent) {
		Operation op=createLiteral(mTypeFactory.createBoolean(), parent);
		op.setIntegerValueAttribute(value? 1 : 0);
		return op;
	}

	public Operation createLiteral(long value, ContainerModule parent) {
		int width=widthOfLiteral(value);
		Operation op=createLiteral(mTypeFactory.createInteger(width), parent);
		op.setIntegerValueAttribute(value);
		return op;
	}
	
	private int widthOfLiteral(long value) {
		int width=1;
		while (value>=256 || value<-256) {
			width+=8;
			value>>=8;
		}
		while (value!=0 && value!=-1) {
			width++;
			value>>=1;
		}
		return width;
	}
	
	
}
