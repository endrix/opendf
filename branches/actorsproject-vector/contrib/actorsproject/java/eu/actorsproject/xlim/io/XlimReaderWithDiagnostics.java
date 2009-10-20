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

package eu.actorsproject.xlim.io;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;


import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;


import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimBlockModule;
import eu.actorsproject.xlim.XlimFactory;
import eu.actorsproject.xlim.XlimIfModule;
import eu.actorsproject.xlim.XlimLoopModule;
import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.XlimPhiContainerModule;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTestModule;
import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimInitValue;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimInstruction;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimTypeArgument;
import eu.actorsproject.xlim.XlimTypeKind;
import eu.actorsproject.xlim.util.Session;

/**
 * Experiment with an alternative XlimReader that uses XMLEvents
 * instead of a DOM-tree, to facilitate error messages with line numbers.
 */
public class XlimReaderWithDiagnostics implements IXlimReader {
	
	protected ReaderPlugIn mPlugIn;
	protected XlimFactory mFactory;
	protected HashMap<String,XlimTag> mTags;
	protected int mNumErrors;
	
	/* tag constants */
	protected enum XlimTag {
		UNKNOWN_TAG(null),
		DESIGN_TAG("design"),
		ACTOR_PORT_TAG("actor-port"),
		INTERNAL_PORT_TAG("internal-port"),
		STATEVAR_TAG("stateVar"),
		INITVALUE_TAG("initValue"),
		MODULE_TAG("module"),
		OPERATION_TAG("operation"),
		PHI_TAG("PHI"),
		PORT_TAG("port"),
		NOTE_TAG("note"),
		TYPEDEF_TAG("typeDef"),
		TYPE_TAG("type"),
		VALUEPAR_TAG("valuePar"),
		TYPEPAR_TAG("typePar");
		
		private String mTagName;
		
		private XlimTag(String tagName) {
			mTagName=tagName;
		}
		
		public String getTagName() {
			return mTagName;
		}
	}
	
	public XlimReaderWithDiagnostics() {
		mPlugIn=Session.getReaderPlugIn();
		mFactory=Session.getXlimFactory();
		mTags=new HashMap<String,XlimTag>();
		for (XlimTag tag: XlimTag.values()) {
			String tagName=tag.getTagName();
			if (tagName!=null)
				mTags.put(tagName,tag);
		}
	}
	
	@Override
	public XlimDesign read(File file) throws IOException {
		XlimDocument document=null;
		try {
			document=XmlEventDocument.read(file);
		} catch (XMLStreamException ex) {
			reportFatalError(file.getPath(), ex);
		}
		
		DesignHandler designHandler=new DesignHandler();
		int numErrors=mNumErrors;
		XlimDesign design=designHandler.readDesign(document.getRootElement());
		return (numErrors==mNumErrors)? design : null; 
	}

	/**
	 * Base class for XLIM-element parsers
	 */
	protected abstract class ElementHandler<Arg1,Arg2> {
		protected abstract void processChild(XlimElement child, 
				                             MutableReaderContext context,
				                             Arg1 arg1,
				                             Arg2 arg2);
		
		protected void processChildren(XlimElement parent, 
				                       MutableReaderContext context,
				                       Arg1 arg1,
				                       Arg2 arg2) {
			for (XlimElement child: parent.getElements()) {
	        	processChild(child, context, arg1, arg2);
	        }
		}
				
		protected XlimTag getTag(XlimElement element) {
			XlimTag result=mTags.get(element.getTagName());
			if (result==null)
				return XlimTag.UNKNOWN_TAG;
			else
				return result;
		}
		
		protected String getRequiredAttribute(String attributeName, XlimElement element) {
			String value=element.getAttributeValue(attributeName);
			if (value==null) {
				String tagName=element.getTagName();
				reportError(element,"Tag <"+tagName+"> missing attribute "+attributeName);
			}
			return value;
		}
		
		protected XlimType getType(XlimElement element, ReaderContext context) {
			String typeName=getRequiredAttribute("typeName", element);
			
			if (typeName!=null) {
				XlimType type=context.getTypeDef(typeName);
				
				if (type==null) {
					try {
						XlimTypeKind typeKind=mPlugIn.getTypeKind(typeName);

						if (typeKind!=null) {
							String sizeAttribute=element.getAttributeValue("size");				

							if (sizeAttribute!=null) {
								// Special fix for legacy "int" type with "size" attribute
								int size=Integer.valueOf(sizeAttribute);
								type=typeKind.createType(size);
							}
							else {
								type=typeKind.createType();
							}
						}
						else {
							reportError(element, "Unsupported type: typeName=\""+typeName+"\"");
						}
					} catch (RuntimeException ex) {
						reportError(element, ex.getMessage());
					}
				}
				// else: we have a typedef of that name
				return type;
			}
			else
				return null; // no typeName
		}
		
		protected void unhandledTag(XlimElement element) {
			String tagName=element.getTagName();
			reportWarning(element, "Unknown/unhandled tag <"+tagName+">");
		}
		
		protected void checkThatEmpty(XlimElement element) {
			if (element.getElements().iterator().hasNext()) {
				String tagName=element.getTagName();
				reportWarning(element, "Unexpected/unhandled element(s) in <"+tagName+">");
			}
		}
	}
	
	/**
	 * Parses the document root, the <design> element
	 */
	protected class DesignHandler extends ElementHandler<XlimDesign, List<XlimElement>> {

		private InitValueHandler mInitValueHandler;
		private ModuleHandler mModuleHandler;
		private TypeHandler mTypeHandler;
		
		public DesignHandler() {
			mInitValueHandler=new InitValueHandler();
			mModuleHandler=new ModuleHandler();
			mTypeHandler=new TypeHandler();
		}

		public XlimDesign readDesign(XlimElement rootElement) {
			if (getTag(rootElement)!=XlimTag.DESIGN_TAG) {
				reportFatalError(rootElement, "Expecting <design> element, found <"+rootElement.getTagName()+">");
			}
			MutableReaderContext context = new MutableReaderContext();
			String name=getRequiredAttribute("name",rootElement);
			if (name==null)
				name=""; // Error recovery: give it an empty name
			XlimDesign design=mFactory.createDesign(name);
			List<XlimElement> taskElements=new ArrayList<XlimElement>();
			
			// First declare all top-level constructs
			processChildren(rootElement,context,design,taskElements);
			
			// Then read the contents of the task modules
			for (XlimElement task: taskElements) {
				readTaskModule(task,context,design);
			}
			return design;			
		}
		
		protected void processChild(XlimElement child, 
				                    MutableReaderContext context,
				                    XlimDesign parent,
				                    List<XlimElement> tasks) {
			switch (getTag(child)) {
			case ACTOR_PORT_TAG:
				readActorPort(child,context,parent);
				break;
			case INTERNAL_PORT_TAG:
				readInternalPort(child,context,parent);
				break;
			case STATEVAR_TAG:
				readStateVar(child,context,parent);
				break;
			case MODULE_TAG:
				readTaskElement(child,context,parent);
				tasks.add(child);
				break;
			case TYPEDEF_TAG:
				readTypeDef(child,context);
				break;
			default:
				unhandledTag(child);
			}
		}
		
		protected void readActorPort(XlimElement portElement,
				                     MutableReaderContext context,
				                     XlimDesign design) {
			String name=getRequiredAttribute("name",portElement);
			String dir=getRequiredAttribute("dir",portElement);
			XlimType type=getType(portElement,context);
			XlimTopLevelPort.Direction direction=XlimTopLevelPort.Direction.in;
			if (dir!=null) {
				if (dir.equals("in"))
					direction=XlimTopLevelPort.Direction.in;
				else if (dir.equals("out"))
					direction=XlimTopLevelPort.Direction.out;
				else
					reportError(portElement, "Unexpected attribute dir=\""+dir+"\"");
			}
			if (name!=null && type!=null) {
				XlimTopLevelPort port=design.addTopLevelPort(name,direction,type);
				try {
					context.addTopLevelPort(port);
				} catch (RuntimeException ex) {
					reportError(portElement, ex.getMessage());
				}
			}
			// Match end
			checkThatEmpty(portElement);
		}	
		
		protected void readInternalPort(XlimElement portElement,
                                        MutableReaderContext context,
                                        XlimDesign design) {
			String name=getRequiredAttribute("name", portElement);
			XlimType type=getType(portElement,context);
			
			if (name!=null && type!=null) {
			    XlimTopLevelPort port=design.addTopLevelPort(name,XlimTopLevelPort.Direction.internal,type);
			    try {
			    	context.addTopLevelPort(port);
			    } catch (RuntimeException ex) {
					reportError(portElement, ex.getMessage());
				}
			}
			
			// Match end
			checkThatEmpty(portElement);
		}

		protected void readStateVar(XlimElement stateVarElement,
                                    MutableReaderContext context,
                                    XlimDesign design) {
			XlimInitValue initValue=mInitValueHandler.readInitValue(stateVarElement,context);
			String sourceName=stateVarElement.getAttributeValue("sourceName");
			String name=getRequiredAttribute("name",stateVarElement);
			if (name!=null) {
				XlimStateVar stateVar=design.addStateVar(sourceName,initValue);
				try {
					context.addStateVar(name, stateVar);
				} catch (RuntimeException ex) {
					reportError(stateVarElement, ex.getMessage());
				}
			}
		}

		protected void readTypeDef(XlimElement typeDefElement,
				                   MutableReaderContext context) {
			String name=getRequiredAttribute("name",typeDefElement);
			XlimType type=mTypeHandler.readType(typeDefElement, context);
			if (name!=null && type!=null) {
				context.addTypeDef(name,type);
			}
		}
		
		protected void readTaskElement(XlimElement taskElement,
                                    MutableReaderContext context,
                                    XlimDesign design) {
			String kind=getRequiredAttribute("kind",taskElement);
			String name=getRequiredAttribute("name",taskElement);
			String autostart=taskElement.getAttributeValue("autostart");
			boolean isAutoStart=false;
			if (autostart!=null && autostart.equals("true"))
				isAutoStart=true;

			if (kind==null)
				kind="action"; // Error recovery
			
			if (name!=null) {
				XlimTaskModule task=design.addTask(kind, name, isAutoStart);
				try {
					context.addTask(task);
				} catch (RuntimeException ex) {
					reportError(taskElement, ex.getMessage());
				}
			}
		}
				
		private void readTaskModule(XlimElement taskElement,
                                    MutableReaderContext context,
                                    XlimDesign design) {
			String name=taskElement.getAttributeValue("name");
			
			if (name!=null) {
				XlimTaskModule task=context.getTask(name);
				context.enterTask(task);
				mModuleHandler.readContainerModule(taskElement,context,task);
				context.leaveTask();
			}
		}
	}

	
	/**
	 * Reads the initializer of a state variable
	 */
	protected class InitValueHandler extends ElementHandler<List<XlimInitValue>, Object> {
		
		public XlimInitValue readInitValue(XlimElement element, MutableReaderContext context) {
			ArrayList<XlimInitValue> initValues=new ArrayList<XlimInitValue>();
			processChildren(element,context,initValues,null);
			if (initValues.size()==1)
				return initValues.get(0);
			else
				return mFactory.createInitValue(initValues);
		}
		
		protected void processChild(XlimElement child, 
                                    MutableReaderContext context, 
                                    List<XlimInitValue> initValues, 
                                    Object dummy) {

			if (getTag(child)==XlimTag.INITVALUE_TAG) {
				XlimInitValue initValue;
				String typeName=getRequiredAttribute("typeName",child);
				if (typeName!=null) {
					if (typeName.equals("List")) {
						initValue=createAggregate(child,context);
					}
					else {
						initValue=createScalar(child,context);
					}
					if (initValue!=null)
						initValues.add(initValue);
				}
			}
			else
				unhandledTag(child);
		}

		protected XlimInitValue createScalar(XlimElement element, ReaderContext context) {
			XlimType type=getType(element,context);
			String value=getRequiredAttribute("value",element);			
			checkThatEmpty(element);
			if (value!=null)
				return mFactory.createInitValue(value,type);
			else
				return null;
		}
		
		protected XlimInitValue createAggregate(XlimElement element, 
				                                MutableReaderContext context) {
			List<XlimInitValue> aggregate=new ArrayList<XlimInitValue>();
			processChildren(element,context,aggregate,null);
			return mFactory.createInitValue(aggregate);
		}
	}

	protected class TypeHandler extends ElementHandler<List<XlimTypeArgument>,Object> {
		
		public XlimType readType(XlimElement parent, MutableReaderContext context) {
			Iterator<XlimElement> pType=parent.getElements().iterator();
			boolean typeTagFound=false;
			XlimType result=null;
			
			while (pType.hasNext()) {
				XlimElement typeElement=pType.next();
				
				if (typeTagFound) {
					reportWarning(typeElement, 
						    "Unhandled element <"+typeElement.getTagName()+"> follows <type>");
				}
				else if (getTag(typeElement)==XlimTag.TYPE_TAG) {
					typeTagFound=true;
					
					String typeName=getRequiredAttribute("name", typeElement);
					if (typeName!=null) {
						result=context.getTypeDef(typeName);
						if (result!=null) {
							// it was a typedef: there should be no enclosed elements
							checkThatEmpty(typeElement);
						}
						else {
							// it was a built-in type name/type constructor: create type
							List<XlimTypeArgument> typeArguments=new ArrayList<XlimTypeArgument>();
							processChildren(typeElement,context,typeArguments,null);
							result=createType(typeElement,typeName,typeArguments);
						}
					}
					// else: no "name" attribute (error already reported)
				}
				else {					
					reportWarning(typeElement, 
							    "Expected <type> element, found unhandled element <"+typeElement.getTagName()+">");
				}
			}
			
			if (typeTagFound==false) {
				reportError(parent,"Expecting a <type> element");
			}
			
			return result;
		}
		
		
		private XlimType createType(XlimElement typeElement, 
				                    String typeName,
				                    List<XlimTypeArgument> typeArguments) {
			
			try {
				XlimTypeKind typeKind=mPlugIn.getTypeKind(typeName);
				if (typeKind!=null)
					return typeKind.createType(typeArguments);
				else
					reportError(typeElement, 
							"Unsupported type: typeName=\""+typeName+"\"");
			} catch (RuntimeException ex) {
				reportError(typeElement, ex.getMessage());
			}
			
			return null;
		}
		
		protected void processChild(XlimElement child, 
                                    MutableReaderContext context,
                                    List<XlimTypeArgument> typeArguments,
                                    Object dummy) {
			XlimTag tag=getTag(child);
			if (tag==XlimTag.VALUEPAR_TAG || tag==XlimTag.TYPEPAR_TAG) {
				String name=getRequiredAttribute("name", child);
				XlimTypeArgument arg=null;
				
				if (tag==XlimTag.VALUEPAR_TAG) {
					String value=getRequiredAttribute("value", child);
					arg=mPlugIn.createTypeArgument(name,value);					
				}
				else {
					XlimType type=readType(child,context);
					arg=mPlugIn.createTypeArgument(name,type);
				}
				typeArguments.add(arg);
			}
			else {
				unhandledTag(child);
			}
		}
	}
	
	protected class ModuleHandler extends ElementHandler<XlimContainerModule,Object> {

		private OperationHandler mOperationHandler;
		private IfHandler mIfHandler;
		private LoopHandler mLoopHandler;
		
		public ModuleHandler() {
			mOperationHandler=new OperationHandler();
			mIfHandler=new IfHandler(this);
			mLoopHandler=new LoopHandler(this);
		}

		public void readContainerModule(XlimElement moduleElement, 
				                        MutableReaderContext context, 
				                        XlimContainerModule module) {
			processChildren(moduleElement,context,module,null);
		}

		public void readTestModule(XlimElement moduleElement, 
				                   MutableReaderContext context, 
				                   XlimTestModule testModule) {
			String decision=getRequiredAttribute("decision",moduleElement);
			
			processChildren(moduleElement,context,testModule,null);
			if (decision!=null) {
				testModule.setDecision(context.getSource(decision));
			}
		}
		
		protected void processChild(XlimElement child, 
                                    MutableReaderContext context, 
                                    XlimContainerModule parent, 
                                    Object dummy) {
			switch (getTag(child)) {
			case OPERATION_TAG:
				mOperationHandler.readOperation(child,parent,context);
				break;
			case MODULE_TAG:
				readSubModule(child,parent,context);
				break;
			case NOTE_TAG:
				readNote(child, context);
				break;
			default:
				unhandledTag(child);
			}
		}

		protected void readSubModule(XlimElement child,
                                     XlimContainerModule parent,
                                     MutableReaderContext context) {
			String kind=getRequiredAttribute("kind",child);
			if (kind.equals("if")) {
				XlimIfModule ifModule=parent.addIfModule();
				mIfHandler.readIf(child,context,ifModule);
			}
			else if (kind.equals("loop")) {
				XlimLoopModule loopModule=parent.addLoopModule();
				mLoopHandler.readLoop(child,context,loopModule);
			}
			else {
				XlimBlockModule module=parent.addBlockModule(kind);
				readContainerModule(child,context,module);
			}
		}

		protected void readNote(XlimElement note, MutableReaderContext context) {
			String kind=getRequiredAttribute("kind",note);

			if (kind.equals("consumptionRates") || kind.equals("productionRates")) {
				String name=getRequiredAttribute("name",note);
				String rate=getRequiredAttribute("value", note);
				XlimTopLevelPort port=context.getTopLevelPort(name);
				if (port==null)
					reportError(note, "No such port: "+name);
				try {
					context.setPortRate(port, Integer.valueOf(rate));
				} catch (RuntimeException ex) {
					reportError(note, ex.getMessage());
				}
				
				// match end
				checkThatEmpty(note);
			}
		}
	}
	
	/**
	 * Reads an if-module
	 */
	protected class IfHandler extends ElementHandler<XlimIfModule,List<ProtoPhiNode>> {
		
		private ModuleHandler mModuleHandler;
		
		public IfHandler(ModuleHandler moduleHandler) {
			mModuleHandler=moduleHandler;
		}

		public void readIf(XlimElement ifElement, 
				           MutableReaderContext context, 
				           XlimIfModule ifModule) {
			List<ProtoPhiNode> phiNodes=new ArrayList<ProtoPhiNode>();
			processChildren(ifElement,context,ifModule,phiNodes);
			
			// Now when we have created "then" or "else" modules we are ready to take on the phi-nodes
			for (ProtoPhiNode phi: phiNodes) {
				phi.resolve(context, ifModule);
			}
		}
		
		
		protected void processChild(XlimElement child, 
                                    MutableReaderContext context,
                                    XlimIfModule parent, 
                                    List<ProtoPhiNode> phiNodes) {
			switch (getTag(child)) {
			case MODULE_TAG:
				readSubModule(child,parent,context);
				break;
			case PHI_TAG:
				ProtoPhiNode  phi=new ProtoPhiNode();
				phi.processChildren(child, context, parent, null);
				phiNodes.add(phi); // Postpone creation of phi-nodes
				break;
			default:
				unhandledTag(child);
			}
		}
		
		protected void readSubModule(XlimElement child,
                                     XlimIfModule parent,
                                     MutableReaderContext context) {
			String kind=getRequiredAttribute("kind",child);

			if (kind.equals("test")) {
				mModuleHandler.readTestModule(child,context,parent.getTestModule());
			}
			else if (kind.equals("then")) {
				mModuleHandler.readContainerModule(child,context,parent.getThenModule());
			}
			else if (kind.equals("else")) {
				mModuleHandler.readContainerModule(child,context,parent.getElseModule());
			}
			else {
				reportError(child, "Unexpected attribute kind=\""+kind+"\"");
			}				
		}
	}
	
	/**
	 * Reads a loop-module
	 */

	protected class LoopHandler extends ElementHandler<XlimLoopModule,List<ProtoPhiNode>> {
		
		private ModuleHandler mModuleHandler;
		
		
		public LoopHandler(ModuleHandler moduleHandler) {
			mModuleHandler = moduleHandler;
		}

		public void readLoop(XlimElement loopElement, 
				             MutableReaderContext context, 
				             XlimLoopModule loopModule) {
			List<ProtoPhiNode> phiNodes=new ArrayList<ProtoPhiNode>();
			processChildren(loopElement,context,loopModule,phiNodes);
			
			// Now when we have created the "body" module we are ready to take on the phi-nodes
			for (ProtoPhiNode phi: phiNodes) {
				phi.resolve(context, loopModule);
			}
		}
		
						
		protected void processChild(XlimElement child, 
                                    MutableReaderContext context, 
                                    XlimLoopModule parent, 
                                    List<ProtoPhiNode> phiNodes) {
			switch (getTag(child)) {
			case MODULE_TAG:
				readSubModule(child,parent,context);
				break;
			case PHI_TAG: 
				ProtoPhiNode  phi=new ProtoPhiNode();
				phi.processChildren(child, context, parent, null);
				phiNodes.add(phi); // Postpone creation of phi-nodes
				break;
			default:
				unhandledTag(child);
			}
		}
		
		protected void readSubModule(XlimElement child,
                                     XlimLoopModule parent,
                                     MutableReaderContext context) {
			String kind=getRequiredAttribute("kind",child);
			
			if (kind.equals("test")) {
				mModuleHandler.readTestModule(child,context,parent.getTestModule());
			}
			else if (kind.equals("body")) {
				mModuleHandler.readContainerModule(child,context,parent.getBodyModule());
			}
			else {
				reportError(child,"Unexpected attribute kind=\""+kind+"\"");
			}
		}
	}

	/**
	 * Common base class for OperationHandler and ProtoPhiNode
	 */
	protected abstract class AbstractPortHandler<Arg1,Arg2> extends ElementHandler<Arg1,Arg2> {

		protected void processChild(XlimElement child, 
                                    MutableReaderContext context,
                                    Arg1 arg1,
                                    Arg2 arg2) {

			if (getTag(child)==XlimTag.PORT_TAG) {
				String sourceId=getRequiredAttribute("source",child);
				String dir=getRequiredAttribute("dir",child);

				if (sourceId!=null && dir!=null) {
					if (dir.equals("in")) {
						processInputPort(sourceId,child,context,arg1,arg2);
					}
					else if (dir.equals("out")) {
						XlimType type=getType(child,context);
						if (type!=null) {
							XlimOutputPort port=mFactory.createOutputPort(type);
							try {
								context.addOutputPort(sourceId, port);
							} catch (RuntimeException ex) {
								reportError(child, ex.getMessage());
							}
							processOutputPort(port,child,arg1,arg2);
						}
					}
					else
						reportError(child,"Unexpected attribute dir=\""+dir+"\"");
				}

				// match end
				checkThatEmpty(child);
			}
			else
				unhandledTag(child);
		}
		
		protected abstract void processInputPort(String sourceId,
				                                 XlimElement portElement,
				                                 MutableReaderContext context,
				                                 Arg1 arg1,
				                                 Arg2 arg2);
		
		protected abstract void processOutputPort(XlimOutputPort output,
				                                  XlimElement portElement,
				                                  Arg1 arg1,
				                                  Arg2 arg2);
	}
		
	
	/**
	 * Finds the ports of an operation/phi-node
	 */
	protected class OperationHandler extends AbstractPortHandler<ArrayList<XlimSource>,
	                                                             ArrayList<XlimOutputPort> > {
		
		
		public void readOperation(XlimElement opElement, 
				                  XlimContainerModule parent,
				                  MutableReaderContext context) {
			ArrayList<XlimSource> inputs=new ArrayList<XlimSource>();
			ArrayList<XlimOutputPort> outputs=new ArrayList<XlimOutputPort>();
			String kind=getRequiredAttribute("kind",opElement);

			processChildren(opElement,context,inputs,outputs);
			try {
				XlimOperation op=parent.addOperation(kind,inputs,outputs);
				XlimAttributeList attributes=opElement.getAttributes();
				mPlugIn.setAttributes(op,attributes,context);
			} catch (RuntimeException ex) {
				reportError(opElement, ex.getMessage());
			}
		}
		
		
		@Override
		protected void processInputPort(String sourceId, 
				                        XlimElement portElement, 
				                        MutableReaderContext context, 
				                        ArrayList<XlimSource> inputs, 
				                        ArrayList<XlimOutputPort> outputs) {
			if (sourceId!=null) {
				XlimSource xlimSource=context.getSource(sourceId);
				if (xlimSource!=null)
					inputs.add(xlimSource);
				else
					reportError(portElement, "No such source in context: source=\""+sourceId+"\"");
			}
		}


		@Override
		protected void processOutputPort(XlimOutputPort output,
				                         XlimElement portElement,
				                         ArrayList<XlimSource> inputs, 
				                         ArrayList<XlimOutputPort> outputs) {
			if (output!=null)
				outputs.add(output);
		}
	}

	protected class ProtoPhiNode extends AbstractPortHandler<XlimPhiContainerModule,Object> {
	
		private String mSrc1,mSrc2;
		private XlimOutputPort mOut;

		public void readPhi(XlimElement phiElement,
				            MutableReaderContext context,
				            XlimPhiContainerModule parent) {
			processChildren(phiElement,context,parent, null);
			if (mSrc1==null || mSrc2==null)
				reportError(phiElement, "Phi-node has too few input ports (should be two)");
			if (mOut!=null)
				reportError(phiElement, "Phi-node has no output port");
		}
		
		@Override
		protected void processInputPort(String sourceId, 
				                        XlimElement portElement, 
				                        MutableReaderContext context, 
				                        XlimPhiContainerModule parent, 
				                        Object dummy) {
			if (mSrc1==null)
				mSrc1=sourceId;
			else if (mSrc2==null)
				mSrc2=sourceId;
			else
				reportError(portElement, "Phi-node has too many input ports");
		}

		@Override
		protected void processOutputPort(XlimOutputPort output, 
				                         XlimElement portElement,
				                         XlimPhiContainerModule parent, 
				                         Object dummy) {
			if (mOut==null)
				mOut=output;
			else
				reportError(portElement, "Phi-node has multiple output ports");
		}

		public void resolve(ReaderContext context,
				            XlimLoopModule loop) {
			XlimModule body=loop.getBodyModule();
			resolve(null,body,context,loop); 
		}
		
		public void resolve(ReaderContext context,
				            XlimIfModule ifModule) {
			resolve(ifModule.getThenModule(),ifModule.getElseModule(),context,ifModule);
		}
		
		private void resolve(XlimModule dominator1,
				            XlimModule dominator2,
				            ReaderContext context, 
				            XlimPhiContainerModule phiContainer) {
			XlimSource src1=context.getSource(mSrc1);
			XlimSource src2=context.getSource(mSrc2);
			/*
			 * The document order of the input ports has no significance
			 * We order them so that the first port corresponds to the path from "then" or loop-preheader
			 * and the second port corresponds to the path from "else" or loop "body".
			 */
			if (belongsToPath(src1, dominator1) || belongsToPath(src2,dominator2))
				phiContainer.addPhiNode(src1, src2, mOut);
			else if (belongsToPath(src2, dominator1) || belongsToPath(src1,dominator2))
				phiContainer.addPhiNode(src2, src1, mOut);
			else
				throw new RuntimeException("ambiguous phi-node");
		}
		
		/**
		 * Return true if it is *certain* that definition of source is dominated by the given module
		 * (otherwise -if not dominated or if it is uncertain- false is returned).
		 */
		private boolean belongsToPath(XlimSource source, XlimModule dominator) {
			if (source!=null && dominator!=null) {
				XlimModule defInModule=null;
				XlimOutputPort port=source.isOutputPort();
				if (port!=null) {
					XlimInstruction instr=port.getParent();
					if (instr!=null)
						defInModule=instr.getParentModule();
					/* else: the output port has not yet been enclosed in an instruction */
				}
				/* else: source is a state-variable (which is available everywhere) */

				if (defInModule!=null)
					return (dominator.leastCommonAncestor(defInModule)==dominator);
			}
			return false;
		}
	}
	
	protected String formatDiagnostic(String fileName, int lineNumber, String message) {
		return fileName+"("+lineNumber+"): "+message;
	}
	
	protected String formatDiagnostic(XlimLocation loc, String message) {
		if (loc!=null)
			return formatDiagnostic(loc.getFileName(), loc.getLineNumber(), message);
		else
			return message;
	}

	protected String formatDiagnostic(XlimElement atElement, String message) {
		return formatDiagnostic(atElement.getLocation(), message);
	}

	public void reportWarning(XlimElement atElement, String message) {
		System.err.println(formatDiagnostic(atElement, "warning: "+message));
	}

	public void reportError(XlimElement atElement, String message) {
		System.err.println(formatDiagnostic(atElement, "error: "+message));
		mNumErrors++;
	}

	public void reportFatalError(XlimElement atElement, String message) {
		System.err.println(formatDiagnostic(atElement, "fatal error: "+message));
		System.err.flush();
		mNumErrors++;
		throw new RuntimeException(message);
	}

	public void reportFatalError(String fileName, XMLStreamException ex) {
		String message="fatal error: "+ex.getMessage();
		Location loc=ex.getLocation();
		String messageWithLocation=(loc!=null)? 
			formatDiagnostic(fileName, loc.getLineNumber(), message) : message;
		System.err.println(messageWithLocation);
		System.err.flush();
		mNumErrors++;
		throw new RuntimeException(ex);
	}
}
