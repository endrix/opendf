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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.actorsproject.util.OutputGenerator;
import eu.actorsproject.xlim.XlimInitValue;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimPhiNode;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.codegenerator.AbstractSymbolTable;
import eu.actorsproject.xlim.codegenerator.OperationGenerator;
import eu.actorsproject.xlim.decision.BlockingCondition;
import eu.actorsproject.xlim.decision.YieldAttribute;
import eu.actorsproject.xlim.util.XlimTraversal;


/**
 * A Translation unit represents the translation of one actor/xlim document to a C file
 */
public class Actor2c extends OutputGenerator {

	private XlimDesign mDesign;
	private File mSourceFile;
    private CSymbolTable mSymbols;
    private OperationGenerator mTaskGeneratorPlugIn;
     
	protected static final String sTranslatorVersion="0.6 (June 3, 2009)";
	protected static final String sIncludedHeaderFile="actors-rts.h";
	protected static final String sActorClassType="ActorClass";
	protected static final String sActorInstanceBaseType="AbstractActorInstance";
	protected static final String sActorInstanceBaseName="base";
	protected static final String sActorInputPortMacro="INPUT_PORT";
	protected static final String sActorOutputPortMacro="OUTPUT_PORT";
	protected static final String sPortType="ActorPort";
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
	 * Generate header that goes first in the output file
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

	/**
	 * Generate port macros
	 */
	protected void defineMacros() {
		println();
		definePortMacros(mDesign.getInputPorts(), sActorInputPortMacro);
		definePortMacros(mDesign.getOutputPorts(), sActorOutputPortMacro);
	}
	
	protected void definePortMacros(Iterable<? extends XlimTopLevelPort> ports,
			                        String portMacro) {
		int index=0;
		for (XlimTopLevelPort port: ports) {
			String cName=mSymbols.getTargetName(port);
			println("#define " + cName + "(thisActor) " + portMacro 
					+ "(thisActor->" + sActorInstanceBaseName + "," + index+")");
			index++;
		}
	}

	/**
	 * Generate description of 'ports'
	 * @param ports
	 * @param name  Name of array holding port descriptions
	 * @return number of ports
	 */
	protected int describePorts(List<? extends XlimTopLevelPort> ports,
			                    String name) {
		println();

		if (ports.isEmpty()) {
			return 0;
		}
		else {	
			int i=0;
		
			println("static const PortDescription "+name+"[]={");
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
	
	
	/**
	 * Generate array holding per-actor consumption (or production) rates
	 * @param action
	 * @param ports      All input (or output) ports
	 * @param generated  Set of already generated arrays
	 * @return           Name of generated array
	 */
	private String actionOnPorts(XlimTaskModule action, 
				              List<? extends XlimTopLevelPort> ports, 
	                          Set<String> generated) {
		if (ports.isEmpty()==false) {
			// encode the port rate in 'arrayName'
			String arrayName="portRate";
			for (XlimTopLevelPort port: ports)
				arrayName += "_" + String.valueOf(action.getPortRate(port));

			if (generated.add(arrayName)) {
				// Create a new array
				boolean comma=false;
				println();
				println("static const int " + arrayName + "[] = {");
				increaseIndentation();
				for (XlimTopLevelPort port: ports) {
					if (comma)
						print(", ");
					lineWrap(60);
					print(String.valueOf(action.getPortRate(port)));
					comma=true;
				}
				decreaseIndentation();
				println();
				println("};");
			}
			return arrayName;
		}
		else
			return "0"; // Null (empty)
	}
	
	/**
	 * Generate action description
	 * @param action
	 * @param consumption  The action's array of consumption rates
	 * @param production   The action's array of production rates
	 */
	protected void describeAction(XlimTaskModule action, String consumption, String production) {
		print("{\"" + action.getName() + "\", " 
				+ consumption + ", " + production + "}");
	}

	/**
	 * Generate array holding action descriptions
	 * @param descriptionArray
	 * @return Number of actions
	 */
	protected int describeActions(String descriptionArray) {
		int numActions=0;
		XlimTaskModule actionScheduler = mDesign.getActionScheduler();
		HashSet<String> rateArrays=new HashSet<String>();
		ArrayList<String> consumption=new ArrayList<String>();
		ArrayList<String> production=new ArrayList<String>();
		
		for (XlimTaskModule action: mDesign.getTasks())
		    if (action!=actionScheduler) {
		    	String cRate=actionOnPorts(action, mDesign.getInputPorts(), rateArrays);
				String pRate=actionOnPorts(action, mDesign.getOutputPorts(), rateArrays);
				consumption.add(cRate);
				production.add(pRate);
		    	++numActions;
		    }
		
		println();
		if (numActions!=0) {
			int index=0;
			println("static const " + sActionDescriptionType + " " 
					+ descriptionArray + "[] = {");
			increaseIndentation();
			for (XlimTaskModule action: mDesign.getTasks()) 
			    if (action!=actionScheduler) {
			    	if (index!=0)
			    		println(",");
			    	describeAction(action, consumption.get(index), production.get(index));
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
		String inputPortDescriptions="inputPortDescriptions";
		int numInputPorts=describePorts(mDesign.getInputPorts(), inputPortDescriptions);
		String outputPortDescriptions="outputPortDescriptions";
		int numOutputPorts=describePorts(mDesign.getOutputPorts(), outputPortDescriptions);
		
		if (numInputPorts==0)
			inputPortDescriptions="0"; // null descriptor array
		if (numOutputPorts==0)
			outputPortDescriptions="0"; // null descriptor array
		
		// Describe actions
		String actionDescriptions = "actionDescriptions";
		int numActions=describeActions(actionDescriptions);
		
		if (numActions==0)
			actionDescriptions="0"; // null descriptor array -admittedly a weird case...
		
		// ActorClass
		println();
		println(sActorClassType+" "+mSymbols.getActorClassName()+" = INIT_ActorClass(");
		increaseIndentation();
		println("\"" + mDesign.getName() + "\",");
		println(mSymbols.getActorInstanceType() + ",");
		println(mSymbols.getConstructorName() + ",");
		println("0, /* no setParam */");
		println(mSymbols.getTargetName(mDesign.getActionScheduler()) + ",");
		println("0, /* no destructor */");
		println(numInputPorts + ", " + inputPortDescriptions + ",");
		println(numOutputPorts+ ", " +outputPortDescriptions + ",");
		println(numActions + ", " + actionDescriptions);		
		decreaseIndentation();
		println(");");
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
		println("} "+mSymbols.getActorInstanceType()+";");
		println();
	}

	/**
	 * Generates the definition/body of the actor constructor, which creates the actor instance
	 */
	protected void defineConstructor() {
		String constructorName=mSymbols.getConstructorName();
		println();
		println("static void "+constructorName+"("+sActorInstanceBaseType+" *pBase) {");
		increaseIndentation();
		
		boolean emptyConstructor=mDesign.getStateVars().isEmpty()
		                         && mDesign.getInternalPorts().isEmpty();
		if (emptyConstructor==false) {
			// Avoid declaring an unused actor-instance pointer (C-compiler warning)
			println(mSymbols.getActorInstanceType()+" *"+mSymbols.getActorInstanceReference()+
				"=("+mSymbols.getActorInstanceType()+"*) pBase;");
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
			println(sCreatePortAPI+"(" + mSymbols.getReference(port) + ");");
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
				println("static const int *"+mSymbols.getTargetName(task)+"("+sActorInstanceBaseType+"*);");
			}
			else {
				println("static void "+mSymbols.getTargetName(task)+"("+mSymbols.getActorInstanceType()+"*);");
			}
		}
		if (!actionSchedulerFound)
			throw new RuntimeException("no action scheduler");
		println("static void "+mSymbols.getConstructorName()+"("+sActorInstanceBaseType+"*);");
	}
	
	/**
	 * Generates a C function per action and one for the action scheduler
	 */
	protected void defineTasks() {
		int actionIndex=0;
		XlimTaskModule actionScheduler = mDesign.getActionScheduler();
		for (XlimTaskModule task: mDesign.getTasks()) {
			if (task!=actionScheduler) {
				println();
				println("static void "+mSymbols.getTargetName(task)+"("+
						mSymbols.getActorInstanceType()+" *"+mSymbols.getActorInstanceReference()+") {");
				increaseIndentation();
				println("TRACE_ACTION(&" + mSymbols.getActorInstanceReference() + "->base, "
						+ actionIndex + ", \"" + task.getName() + "\");");
				++actionIndex;
			}
			else {
				ExitCodeGenerator ecg=new ExitCodeGenerator();
				ecg.traverse(task, null);
				println();
				println("static const int *"+mSymbols.getTargetName(task)+"("+sActorInstanceBaseType
						+" *pBase) {");
				increaseIndentation();
				println(mSymbols.getActorInstanceType()+" *"+mSymbols.getActorInstanceReference()+
						"=("+mSymbols.getActorInstanceType()+"*) pBase;");
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
	
	
	/**
	 * Generates the arrays used for exit codes (blocking conditions)
	 */
	class ExitCodeGenerator extends XlimTraversal<Object,Object> {

		Map<XlimTopLevelPort,Integer> mPortNumbers;
		
		ExitCodeGenerator() {
			int n=0;
			mPortNumbers=new HashMap<XlimTopLevelPort,Integer>();
			for (XlimTopLevelPort in: mDesign.getInputPorts())
				mPortNumbers.put(in, n++);
			for (XlimTopLevelPort out: mDesign.getOutputPorts())
				mPortNumbers.put(out, n++);
		}
		
		@Override
		protected Object handleOperation(XlimOperation op, Object dummy) {
			if (op.getKind().equals("yield")) {
				YieldAttribute yieldAttrib=(YieldAttribute) op.getGenericAttribute();
				String arrayName=mSymbols.getGenericAttribute(yieldAttrib);
				if (arrayName==null) {
					int size=yieldAttrib.size();

					if (size==0) {
						arrayName= "EXITCODE_TERMINATE";
					}
					else {
						arrayName="exitcode_block";
						for (BlockingCondition bc: yieldAttrib) {
							String portName=AbstractSymbolTable.createCName(bc.getPort().getSourceName());
							arrayName += "_" + portName + "_" + bc.getTokenCount();
						}
					
						println();
						println("static const int " + arrayName + "[] = {");
						increaseIndentation();
						print("EXITCODE_BLOCK(" + String.valueOf(size) +")");
						for (BlockingCondition bc: yieldAttrib) {
							int port=mPortNumbers.get(bc.getPort());
							print(", ");
							lineWrap(60);
							print(port + ", " + bc.getTokenCount());
						}
						decreaseIndentation();
						println();
						println("};");
					}
					
					mSymbols.setGenericAttribute(yieldAttrib,arrayName);
				}
			}
			return null;
		}

		@Override
		protected Object handlePhiNode(XlimPhiNode phi, Object dummy) {
			return null;
		}
		
	}
}
