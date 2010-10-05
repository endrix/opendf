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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


import eu.actorsproject.util.OutputGenerator;
import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.XlimIfModule;
import eu.actorsproject.xlim.XlimInputPort;
import eu.actorsproject.xlim.XlimLoopModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTestModule;
import eu.actorsproject.xlim.codegenerator.LocalCodeGenerator;
import eu.actorsproject.xlim.codegenerator.LocalScope;
import eu.actorsproject.xlim.codegenerator.OperationGenerator;
import eu.actorsproject.xlim.codegenerator.TemporaryVariable;
import eu.actorsproject.xlim.decision.ActionSchedulerParser;

public class Task2c extends LocalCodeGenerator {

	private CSymbolTable mTopLevelSymbols;
	private int mActionIndex;
	
	public Task2c(XlimDesign design,
			      CSymbolTable topLevelSymbols,
			      OperationGenerator plugIn,
			      OutputGenerator output) {
		super(design,topLevelSymbols, plugIn, output);
		mTopLevelSymbols=topLevelSymbols;
		mActionIndex=0;
	}
	
	@Override
	public void translateTask(XlimTaskModule task) {
		storageAllocation(task);
	
		if (task.isAutostart()) {
			// Action Scheduler
			int numInputs=mDesign.getInputPorts().size();
			int numOutputs=mDesign.getOutputPorts().size();
			String actorInstanceT=mTopLevelSymbols.getActorInstanceType();
			String actorInstanceRef=mTopLevelSymbols.getActorInstanceReference();
			
			mOutput.println(actorInstanceT+" *"+actorInstanceRef+"=("+actorInstanceT+"*) pBase;");
			mOutput.println("const int *exitCode=EXIT_CODE_YIELD;");
			declareTemporaries(task);
			
			mOutput.println("ART_ACTION_SCHEDULER_ENTER("+numInputs+","+numOutputs+");");
			generateSchedulerLoop(task);
			mOutput.println("action_scheduler_exit:");
			mOutput.println("ART_ACTION_SCHEDULER_EXIT("+numInputs+","+numOutputs+");");
			mOutput.println("return exitCode;");
		}
		else {
			// Action
			String name=mTopLevelSymbols.getTargetName(task);
			
			declareTemporaries(task);
			mOutput.println("ART_ACTION_ENTER("+name+","+mActionIndex+");");
			generateBlockElements(task);
		    mOutput.println("ART_ACTION_EXIT("+name+","+mActionIndex+");");	
		    
		    mActionIndex++;
		}
		
	}
	
	protected void generateSchedulerLoop(XlimTaskModule actionScheduler) {
		ActionSchedulerParser parser=new ActionSchedulerParser();
		XlimLoopModule schedulerLoop=parser.findSchedulingLoop(actionScheduler);
		LocalScope loopScope=mLocalSymbols.getScope(schedulerLoop);
		
		mOutput.println("ART_ACTION_SCHEDULER_LOOP {");
		mOutput.increaseIndentation();
		mOutput.println("ART_ACTION_SCHEDULER_LOOP_TOP;");
		if (loopScope!=null)
			generateDeclaration(loopScope);
		generateCode(schedulerLoop.getBodyModule());
		mOutput.println("ART_ACTION_SCHEDULER_LOOP_BOTTOM;");
		mOutput.decreaseIndentation();
		mOutput.println("}");
	}
	
	@Override
	protected void generateDeclaration(LocalScope scope) {
		// Sort allocated temporaries after type
		HashMap<String,ArrayList<TemporaryVariable>> typeMap =
			new HashMap<String,ArrayList<TemporaryVariable>>();
			
		for (TemporaryVariable temp: scope.getTemporaries()) {
			String cType=mTopLevelSymbols.getTargetTypeName(temp.getElementType());
			ArrayList<TemporaryVariable> list=typeMap.get(cType);
			if (list==null) {
				list=new ArrayList<TemporaryVariable>();
				typeMap.put(cType, list);
			}
			list.add(temp);
		}
			
		// Print them
		for (Map.Entry<String,ArrayList<TemporaryVariable>> entry: typeMap.entrySet()) {
			char delimiter=' ';
			mOutput.print(entry.getKey());
			for (TemporaryVariable temp: entry.getValue()) {
				mOutput.print(delimiter+mTopLevelSymbols.getTargetName(temp));
				if (temp.getType().isList()) {
					mOutput.print("["+temp.getNumberOfElements()+"]");
				}
				delimiter=',';
			}
			mOutput.println(";");
		}
	}

	@Override
	protected void generateIf(XlimIfModule m) {
		XlimTestModule test=m.getTestModule();
		
		// Do we need to generate test module separately?
		// (i.e. does it contain "roots"/statements)
		if (simpleTest(test)==false)
			generateCode(test);  
		mOutput.print("if (");
		generateExpression(test.getDecision());
		mOutput.println(") {");

		mOutput.increaseIndentation();
		generateCode(m.getThenModule());
		visitPhiNodes(m.getPhiNodes(),0);
		mOutput.decreaseIndentation();
		mOutput.println("}");

		// Generate else-part if non-empty or if there are phi-nodes
		if (needsElse(m)) {
			mOutput.println("else {");
			mOutput.increaseIndentation();
			generateCode(m.getElseModule());
			visitPhiNodes(m.getPhiNodes(),1);
			mOutput.decreaseIndentation();
			mOutput.println("}");
		}
	}

	@Override
	protected void generateLoop(XlimLoopModule m) {
		XlimTestModule test=m.getTestModule();
		LocalScope scope=mLocalSymbols.getScope(m);
		
		visitPhiNodes(m.getPhiNodes(),0);
		if (simpleTest(test)) {
			// Generate condition with while
			mOutput.print("while (");
			generateExpression(test.getDecision());
			mOutput.println(") {");
			mOutput.increaseIndentation();
			
			if (scope!=null)
				generateDeclaration(scope);
		}
		else {
			// Here, we have a complex test that
			// requires multiple statements
			mOutput.println("while (1) {");
			mOutput.increaseIndentation();			
		
		
			if (scope!=null)
				generateDeclaration(scope);

			generateCode(test);
			mOutput.print("if (!");
			generateExpression(test.getDecision());
			mOutput.println(") break;");
		}
		
		generateCode(m.getBodyModule());
		
		visitPhiNodes(m.getPhiNodes(),1);
		mOutput.decreaseIndentation();
		mOutput.println("}");
	}


	@Override
	protected void generateStatement(XlimOperation stmt) {
		String comment=isUseful(stmt)? "" : " /* unused */";
		mPlugIn.generateStatement(stmt,this);
		mOutput.println(";"+comment);
	}

	@Override
	protected void generatePhi(XlimInputPort input, XlimOutputPort output) {
		BasicGenerator.generateCopy(input, output, this);
		mOutput.println("; /* PHI */");
	}
	
	@Override
	public void print(XlimTaskModule task) {
		mOutput.print("ART_FIRE_ACTION("+mTopLevelSymbols.getReference(task)+")");
	}
}
