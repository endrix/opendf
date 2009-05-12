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

import java.io.File;
import java.io.PrintStream;
import java.util.Date;
import java.util.List;

import eu.actorsproject.util.OutputGenerator;
import eu.actorsproject.xlim.XlimInitValue;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.codegenerator.OperationGenerator;


/**
 * A Translation unit represents the translation of one actor/xlim document to a C file
 */
public class Actor2c extends OutputGenerator {

	private XlimDesign mDesign;
	private File mSourceFile;
    private CSymbolTable mSymbols;
    private OperationGenerator mTaskGeneratorPlugIn;
     
	protected static final String sTranslatorVersion="0.5 (Jan 29, 2009)";
	protected static final String sIncludedHeaderFile="actors-rts.h";
	protected static final String sActorClassType="ActorClass";
	protected static final String sActorInstanceBaseType="AbstractActorInstance";
	protected static final String sActorInstanceBaseName="base";
	protected static final String sActorInputPortArray="base.inputPort";
	protected static final String sActorOutputPortArray="base.outputPort";
	protected static final String sActorInstanceType="ActorInstance";
	protected static final String sPortType="ActorPort";
	protected static final String sConstructorName="constructor";
	protected static final String sCreatePortAPI="createPort";
	protected static final String sActionDescriptionType="ActionDescription";
	
	public Actor2c(XlimDesign design,
                   File sourceFile,
			       PrintStream output, 
			       OperationGenerator plugIn) {
		super(output);
		mDesign=design;
		mSourceFile=sourceFile;
		mSymbols=new CSymbolTable();
		mSymbols.createActorClassName(sourceFile.getName());
		mSymbols.declareActorScope(design);
		mTaskGeneratorPlugIn=plugIn;
	}
	
	public void translate() {
		generateOutput();
	}
	
	protected void generateOutput() {
		createHeader();
		defineMacros();
		declareActorInstance();
		declareFunctions();
		defineActorClass();
		defineAggregateInitializers();
		defineTasks();
		defineConstructor();
	}
	

	/**
	 * This header goes first in the output file
	 */
	protected void createHeader() {
		println("/*");
		println(" * Actor "+mDesign.getName()+" ("+mSymbols.getActorClassName()+")");
		println(" * Generated on "+new Date()+" from "+mSourceFile.toString());
		println(" * by xlim2c version "+sTranslatorVersion);
		println(" */");
		println();
		println("#include \""+sIncludedHeaderFile+"\"");
	}

	protected void defineMacros() {
		println();
		definePortMacros(mDesign.getInputPorts(), sActorInputPortArray);
		definePortMacros(mDesign.getOutputPorts(), sActorOutputPortArray);
	}
	
	protected void definePortMacros(Iterable<? extends XlimTopLevelPort> ports,
			                        String portArray) {
		int index=0;
		for (XlimTopLevelPort port: ports) {
			String cName=mSymbols.getTargetName(port);
			String portReference=portArray+"["+index+"]";
			println("#define "+cName+" "+portReference);
			index++;
		}
	}

	protected int describePorts(List<? extends XlimTopLevelPort> ports,
			                    String name) {
		println();

		if (ports.isEmpty()) {
			println("#define "+name+" 0 /* empty */");
			return 0;
		}
		else {	
			int i=0;
		
			println("static PortDescription "+name+"[]={");
			increaseIndentation();
			for (XlimTopLevelPort p: ports) {
				if (i!=0)
					println(",");
				String type=mSymbols.getTargetTypeName(p.getType());
				print("{\"" + p.getSourceName() + "\", sizeof("+type+")}");
				++i;
			}
			decreaseIndentation();
			println();
			println("};");
			return i;
		}
	}
	
	private void actionOnPorts(XlimTaskModule action, 
				              List<? extends XlimTopLevelPort> ports,
				              String arrayName) {
		if (ports.isEmpty()==false) {
			int i=0;
			println();
			println("static const int " + arrayName + "[] = {");
			increaseIndentation();
			for (XlimTopLevelPort port: ports) {
				if (i!=0)
					print(", ");
				lineWrap(60);
				print("0");
				++i;
			}
			decreaseIndentation();
			println();
			println("};");
		}
	}
	
	protected void describeAction(XlimTaskModule action, int index) {
		print("{\""+action.getName()+"\", ");
		if (mDesign.getInputPorts().isEmpty())
			print("0, ");
		else
			print("consumption" + index + ", ");
			
		if (mDesign.getOutputPorts().isEmpty())
			print("0}");
		else
			print("production" + index + "}");
	}

	protected int describeActions(String descriptionArray) {
		int numActions=0;
		XlimTaskModule actionScheduler = mDesign.getActionScheduler();
		
		for (XlimTaskModule action: mDesign.getTasks())
		    if (action!=actionScheduler) {
		    	actionOnPorts(action, mDesign.getInputPorts(), "consumption"+numActions);
				actionOnPorts(action, mDesign.getOutputPorts(), "production"+numActions);
		    	++numActions;
		    }
		
		println();
		if (numActions==0) 
			println("#define actionDescriptions 0 /* empty */");
		else {
			int index=0;
			println("static const " + sActionDescriptionType + " " 
					+ descriptionArray + "[] = {");
			increaseIndentation();
			for (XlimTaskModule action: mDesign.getTasks()) 
			    if (action!=actionScheduler) {
			    	if (index!=0)
			    		println(",");
			    	describeAction(action, index);
			    	++index;
			    }
			println();
			decreaseIndentation();
			println("};");
		}
		
		return numActions;
	}
	
	/**
	 * Generate the actor class struct (which is used to instantiate the actor)
	 */
	protected void defineActorClass() {
		// Describe ports
		int numInputPorts=describePorts(mDesign.getInputPorts(), "inputPortDescriptions");
		int numOutputPorts=describePorts(mDesign.getOutputPorts(), "outputPortDescriptions");
		
		// Describe actions
		int numActions=describeActions("actionDescriptions");
		
		// ActorClass
		println();
		println(sActorClassType+" "+mSymbols.getActorClassName()+" ={");
		increaseIndentation();
		println("\""+mDesign.getName()+"\",");
		println(numInputPorts +", /* numInputPorts */");
		println(numOutputPorts+", /* numOutputPorts */");
		println("sizeof("+sActorInstanceType+"),");
		println(mSymbols.getTargetName(mDesign.getActionScheduler())+",");
		println(sConstructorName+",");
		println("0, /* destructor */");
		println("0, /* set_param */");
		println("inputPortDescriptions,");
		println("outputPortDescriptions,");
		println("0, /* actorExecMode */");
		println(numActions + ", /* numActions */");
		println("actionDescriptions");
		decreaseIndentation();
		println("};");
		println();
	}
	
	
	/**
	 * Generate the data type of the actor instance struct
	 */
	protected void declareActorInstance() {
		println();
		println("typedef struct {");
		increaseIndentation();
		println(sActorInstanceBaseType+" "+sActorInstanceBaseName+";");
		declareInternalPorts();
		declareStateVariables();
		decreaseIndentation();
		println("} "+sActorInstanceType+";");
		println();
	}

	/**
	 * Generates the definition/body of the actor constructor, which creates the actor instance
	 */
	protected void defineConstructor() {
		println();
		println("static void "+sConstructorName+"("+sActorInstanceBaseType+" *pBase) {");
		increaseIndentation();
		
		boolean emptyConstructor=mDesign.getStateVars().isEmpty()
		                         && mDesign.getInternalPorts().isEmpty();
		if (emptyConstructor==false) {
			// Avoid declaring an unused actor-instance pointer (C-compiler warning)
			println(sActorInstanceType+" *"+mSymbols.getActorInstanceReference()+
				"=("+sActorInstanceType+"*) pBase;");
			createInternalPorts();
			initializeStateVariables();
		}
		decreaseIndentation();
		println("}");
	}

	/**
	 * Declare the internal ports of the actor
	 */
	protected void declareInternalPorts() {
		for (XlimTopLevelPort port: mDesign.getInternalPorts()) {
			println(sPortType+" "+mSymbols.getTargetName(port));
		}
	}
	
	/**
	 * Creates internal ports (the duty of the actor constructor)
	 */
	protected void createInternalPorts() {
		for (XlimTopLevelPort port: mDesign.getInternalPorts()) {
			println(sCreatePortAPI+"(&" + mSymbols.getReference(port) + ");");
		}
	}
	
	protected String getElementType(XlimInitValue initValue) {
		XlimType elementT=initValue.getCommonElementType();
		return mSymbols.getTargetTypeName(elementT);
	}
	
	/**
	 * Declare the state variables within the actor instance struct
	 */
	protected void declareStateVariables() {
		for (XlimStateVar stateVar: mDesign.getStateVars()) {
			XlimInitValue initValue=stateVar.getInitValue();
			XlimType scalarType=initValue.getScalarType();
			String type=getElementType(initValue);
			String optArray="";
			if (scalarType==null) {
				int length=initValue.totalNumberOfElements();
				optArray="["+length+"]";
			}
			
			println(type + " " + mSymbols.getTargetName(stateVar) + optArray + ";");
		}
	}
	
	protected void defineAggregateInitializers() {
		for (XlimStateVar stateVar: mDesign.getStateVars()) {
			XlimInitValue initValue=stateVar.getInitValue();
			if (initValue.getScalarType()==null && initValue.isZero()==false) {
				String type=getElementType(initValue);
				String name=mSymbols.getAggregateInitializer(stateVar);
				int length=initValue.totalNumberOfElements();
				println("const " + type + " " + name + "[" + length + "] = {");
				increaseIndentation();
				
				String delimiter="";
				for (XlimInitValue v: initValue.getChildren()) {
					print(delimiter);
					delimiter=", ";
					lineWrap(60);
					
					String scalarValue=v.getScalarValue();
					assert(scalarValue!=null);
					print(scalarValue);
				}
				println();
				decreaseIndentation();
				println("};");
			}
		}
	}
	
	
	/**
	 * Initializes state variables (the duty of the actor constructor)
	 */
	protected void initializeStateVariables() {
		for (XlimStateVar stateVar: mDesign.getStateVars()) {
			XlimInitValue initValue=stateVar.getInitValue();
			XlimType scalarType=initValue.getScalarType();
			if (scalarType!=null) {
				println(mSymbols.getReference(stateVar) + "=" + initValue.getScalarValue() + ";");
			}
			else {
				int length=initValue.totalNumberOfElements();
				String type=getElementType(initValue);
				
				if (initValue.isZero())
					println("memset(" + mSymbols.getReference(stateVar) + ", 0, "
							+ length + "*sizeof(" + type + "));");
				else
					println("memcpy(" + mSymbols.getReference(stateVar) + ", "
					    	+ mSymbols.getAggregateInitializer(stateVar) + ", "
						    + length + "*sizeof(" + type + "));");
			}
		}
	}
	
	/**
	 * Generates the prototypes of the functions representing
	 * the actions, the action scheduler and the constructor of the actor
	 */
	protected void declareFunctions() {
		boolean actionSchedulerFound=false;
		
		println();
		for (XlimTaskModule task: mDesign.getTasks()) {
			if (task.isAutostart()) {
				// TODO: can there be multiple action schedulers? How to handle them in that case?
				if (actionSchedulerFound)
					throw new RuntimeException("multiple action schedulers");
				actionSchedulerFound=true;
				println("static void "+mSymbols.getTargetName(task)+"("+sActorInstanceBaseType+"*);");
			}
			else {
				println("static void "+mSymbols.getTargetName(task)+"("+sActorInstanceType+"*);");
			}
		}
		if (!actionSchedulerFound)
			throw new RuntimeException("no action scheduler");
		println("static void "+sConstructorName+"("+sActorInstanceBaseType+"*);");
	}
	
	/**
	 * Generates a C function per action and one for the action scheduler
	 */
	protected void defineTasks() {
		int actionIndex=0;
		XlimTaskModule actionScheduler = mDesign.getActionScheduler();
		for (XlimTaskModule task: mDesign.getTasks()) {
			println();
			if (task!=actionScheduler) {
				println("static void "+mSymbols.getTargetName(task)+"("+
						sActorInstanceType+" *"+mSymbols.getActorInstanceReference()+") {");
				increaseIndentation();
				println("TRACE_ACTION(&" + mSymbols.getActorInstanceReference() + "->base, "
						+ actionIndex + ", \"" + task.getName() + "\");");
				++actionIndex;
			}
			else {
				println("static void "+mSymbols.getTargetName(task)+"("+sActorInstanceBaseType
						+" *pBase) {");
				increaseIndentation();
				println(sActorInstanceType+" *"+mSymbols.getActorInstanceReference()+
						"=("+sActorInstanceType+"*) pBase;");
			}
		    generateBody(task);
		    decreaseIndentation();
		    println("}");
		}
	}
	
	protected void generateBody(XlimTaskModule task) {
		Task2c gen = new Task2c(mSymbols,mTaskGeneratorPlugIn,this);
		gen.translateTask(task);
	}
}
