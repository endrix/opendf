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
import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException; 

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
import eu.actorsproject.xlim.util.Session;

public class XlimReader {
	
	protected ReaderPlugIn mPlugIn;
	protected XlimFactory mFactory;
	protected HashMap<String,Integer> mTags;
	
	/* pass plug-ins */
	protected DesignPass1 mDesignPass1=new DesignPass1();
	protected DesignPass2 mDesignPass2=new DesignPass2();
	protected DeclarationPass mDeclarationPass=new DeclarationPass();
	protected BlockContainerPass mBlockContainerPass=new BlockContainerPass();
	protected IfModulePass mIfModulePass=new IfModulePass();
	protected LoopModulePass mLoopModulePass=new LoopModulePass();
	protected PortPass mPortPass=new PortPass();
	protected InitValuePass mInitValuePass=new InitValuePass();
	
	/* tag constants */
	protected static final int UNKNOWN_TAG=-1;

	protected static final int DESIGN_TAG=0;
	protected static final int ACTOR_PORT_TAG=1;
	protected static final int INTERNAL_PORT_TAG=2;
	protected static final int STATEVAR_TAG=3;
	protected static final int INITVALUE_TAG=4;
	protected static final int MODULE_TAG=5;
	protected static final int OPERATION_TAG=6;
	protected static final int PHI_TAG=7;
	protected static final int PORT_TAG=8;
	protected static final int NOTE_TAG=9;
	
	public XlimReader() {
		mPlugIn=Session.getReaderPlugIn();
		mFactory=Session.getXlimFactory();
		mTags=new HashMap<String,Integer>();
		mTags.put("design", DESIGN_TAG);
		mTags.put("actor-port", ACTOR_PORT_TAG);
		mTags.put("internal-port", INTERNAL_PORT_TAG);
		mTags.put("stateVar", STATEVAR_TAG);
		mTags.put("initValue", INITVALUE_TAG);
		mTags.put("module", MODULE_TAG);
		mTags.put("operation", OPERATION_TAG);
		mTags.put("PHI", PHI_TAG);
		mTags.put("port", PORT_TAG);
		mTags.put("note", NOTE_TAG);
	}
	
	public XlimDesign read(File f) 
		throws IOException, SAXException, ParserConfigurationException {
		Document document=null;
		DocumentBuilderFactory factory =
		    DocumentBuilderFactory.newInstance();
		factory.setCoalescing(true);
		factory.setExpandEntityReferences(true);
		factory.setIgnoringComments(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		document = builder.parse( f );
		return createDesign(document.getDocumentElement());
	}

	public XlimDesign createDesign(Element domElement) {
		ReaderContext context = new ReaderContext();
		String name=getRequiredAttribute("name",domElement);
		XlimDesign design=mFactory.createDesign(name);
		mDesignPass1.createTopLevelElements(domElement, context, design);
		mDesignPass2.createModules(domElement, context);
		return design;
	}
	

	protected XlimTopLevelPort createActorPort(Element domElement,
                                               XlimDesign design) {
		String name=getRequiredAttribute("name",domElement);
		String dir=getRequiredAttribute("dir",domElement);
		XlimType type=getType(domElement);
		XlimTopLevelPort.Direction direction;
		if (dir.equals("in"))
			direction=XlimTopLevelPort.Direction.in;
		else if (dir.equals("out"))
			direction=XlimTopLevelPort.Direction.out;
		else
			throw new RuntimeException("Unexpected attribute dir=\""+dir+"\"");
		XlimTopLevelPort port=design.addTopLevelPort(name,direction,type);
		return port;
	}

	protected XlimTopLevelPort createInternalPort(Element domElement,
			                                      XlimDesign design) {
		String name=getRequiredAttribute("name", domElement);
		XlimType type=getType(domElement);
		XlimTopLevelPort port=design.addTopLevelPort(name,XlimTopLevelPort.Direction.internal,type);
		return port;
	}

	protected XlimStateVar createStateVar(Element domElement,
			                              XlimDesign design) {
		ArrayList<XlimInitValue> initValues=new ArrayList<XlimInitValue>();
		mInitValuePass.createInitValues(domElement,initValues);
		XlimInitValue value;

		if (initValues.size()==1) {
			value=initValues.get(0);
		}
		else {
			value=mFactory.createInitValue(initValues);
		}
		String sourceName=getAttribute("sourceName",domElement);
		XlimStateVar var=design.addStateVar(sourceName,value);
		return var;
	}

	protected XlimTaskModule createTask(Element domElement,
			                            XlimDesign design) {
		String kind=getRequiredAttribute("kind",domElement);
		String name=getRequiredAttribute("name",domElement);
		String autostart=getAttribute("autostart",domElement);
		boolean isAutoStart=false;
		if (autostart!=null && autostart.equals("true"))
			isAutoStart=true;
		return design.addTask(kind, name, isAutoStart);
	}

	protected void createOperation(Element child, 
			                       XlimContainerModule parent,
			                       ReaderContext context) {
		ArrayList<XlimSource> inputs=new ArrayList<XlimSource>();
		ArrayList<XlimOutputPort> outputs=new ArrayList<XlimOutputPort>();
		String kind=getRequiredAttribute("kind",child);

		mPortPass.findPorts(child,context,inputs,outputs);
		XlimOperation op=parent.addOperation(kind,inputs,outputs);
		mPlugIn.setAttributes(op,child.getAttributes(),context);
	}

	protected void createModule(Element child,
			                    XlimContainerModule parent,
			                    ReaderContext context) {
		String kind=getRequiredAttribute("kind",child);
		if (kind.equals("if")) {
			XlimIfModule ifModule=parent.addIfModule();
			setIfAttributes(ifModule,child,context);
			mIfModulePass.createContents(child,context,ifModule);
		}
		else if (kind.equals("loop")) {
			XlimLoopModule loopModule=parent.addLoopModule();
			setLoopAttributes(loopModule,child,context);
			mLoopModulePass.createContents(child,context,loopModule);
		}
		else {
			XlimBlockModule module=parent.addBlockModule(kind);
			setContainerModuleAttributes(module,child,context);
			mBlockContainerPass.createContents(child,context,module);
		}
	}

	protected XlimInstruction createPhiNode(Element child,
			                                XlimPhiContainerModule container,
			                                XlimModule dominator0,
			                                XlimModule dominator1,
			                                ReaderContext context) {
		ArrayList<XlimSource> inputs=new ArrayList<XlimSource>();
		ArrayList<XlimOutputPort> outputs=new ArrayList<XlimOutputPort>();
		mPortPass.findPorts(child,context,inputs,outputs);
		if (inputs.size()!=2 || outputs.size()!=1)
			throw new RuntimeException("Unexpected number of ports");
		
		/*
		 * 
		 * The document order of the input ports has no significance
		 * We order them so that the first port corresponds to the path from "then" or loop-preheader
		 * and the second port corresponds to the path from "else" or loop "body".
		 */
		if (belongsToPath(inputs.get(0), dominator0) || belongsToPath(inputs.get(1),dominator1))
			return container.addPhiNode(inputs.get(0), inputs.get(1), outputs.get(0));
		else if (belongsToPath(inputs.get(1), dominator0) || belongsToPath(inputs.get(0),dominator1))
			return container.addPhiNode(inputs.get(1), inputs.get(0), outputs.get(0));
		else
			throw new RuntimeException("ambiguous phi-node");
	}

	/**
	 * Return true if it is *certain* that definition of source is dominated by the given module
	 * (otherwise -if not dominated or if it is uncertain- false is returned).
	 */
	private boolean belongsToPath(XlimSource source, XlimModule dominator) {
		if (dominator!=null) {
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

	protected void setContainerModuleAttributes(XlimContainerModule containerModule,
			                         Element domElement,
                                     ReaderContext context) {
		// Default implementation is to set no additional attributes
	}
	
	protected void setIfAttributes(XlimIfModule ifModule,
			                       Element domElement,
                                   ReaderContext context) {
		// Default implementation is to set no additional attributes
	}
	
	protected void setLoopAttributes(XlimLoopModule loopModule,
                                     Element domElement,
			                         ReaderContext context) {
		// Default implementation is to set no additional attributes
	}
	
	protected void setTestAttributes(XlimTestModule testModule,
                                     Element domElement,
                                     ReaderContext context) {
		setContainerModuleAttributes(testModule,domElement,context);
		String decision=getRequiredAttribute("decision",domElement);
		testModule.setDecision(context.getSource(decision));
	}
		
	protected int getTag(Element domElement) {
		Integer tag=mTags.get(domElement.getTagName());
		return (tag==null)? UNKNOWN_TAG : tag;
	}

	protected void unhandledTag(Element domElement) {
		// Default implementation is to take no action for unexpected elements...
	}

	protected String getAttribute(String attributeName, Element domElement) {
		Node node=domElement.getAttributes().getNamedItem(attributeName);
		if (node==null)
			return null;
		else
			return node.getNodeValue();
	}
	
	protected String getRequiredAttribute(String attributeName, Element domElement) {
		String value=getAttribute(attributeName,domElement);
		if (value==null)
			throw new RuntimeException("Missing attribute "+attributeName);
		return domElement.getAttributes().getNamedItem(attributeName).getNodeValue();
	}
	
	protected XlimType getType(Element domElement) {
		String typeName=getRequiredAttribute("typeName",domElement);
		XlimType type=mPlugIn.getType(typeName,domElement.getAttributes());
		if (type==null)
			throw new RuntimeException("Unsupported or incomplete type: typeName=\""+typeName+"\"");
		return type;
	}
	
	protected abstract class PassPlugin<Arg1,Arg2> {
		protected abstract void processChild(Element child, 
				                             ReaderContext context,
				                             Arg1 arg1,
				                             Arg2 arg2);
		protected void processChildren(Element domElement, 
				             ReaderContext context,
				             Arg1 arg1,
				             Arg2 arg2) {
			for (Node child=domElement.getFirstChild(); child!=null; child=child.getNextSibling()) {
				switch (child.getNodeType()) {
				case Node.ELEMENT_NODE:
					processChild((Element) child, context, arg1, arg2);
					break;
				case Node.TEXT_NODE:
					// Should only be white space here...
					String text=child.getNodeValue().trim();
					if (text.isEmpty()==false)
						throw new RuntimeException("Unexpected text: "+text);
					break;
				default:
					throw new RuntimeException("Unexpected node: "+child.getNodeName());
				}		
			}
		}
	}

	/**
	 * Creates top-level elements in design, 
	 * complete state variables (with initial values)
	 * and declares all output ports in all modules
	 */
	protected class DesignPass1 extends PassPlugin<XlimDesign,Object> {
	
				
		protected void processChild(Element child, 
				                    ReaderContext context,
				                    XlimDesign parent,
				                    Object dummy) {
			switch (getTag(child)) {
			case ACTOR_PORT_TAG:
				context.addTopLevelPort(createActorPort(child,parent));
				break;
			case INTERNAL_PORT_TAG:
				context.addTopLevelPort(createInternalPort(child,parent));
				break;
			case STATEVAR_TAG:
				context.addStateVar(getRequiredAttribute("name",child), 
						            createStateVar(child,parent));
				break;
			case MODULE_TAG:
				context.addTask(createTask(child,parent));
				break;
			default:
				unhandledTag(child);
			}
		}
		
		public void createTopLevelElements(Element domElement, ReaderContext context, XlimDesign design) {
			processChildren(domElement,context,design,null);
		}
	}


	/**
	 * Completes the creation of all modules in a design
	 */
	protected class DesignPass2 extends PassPlugin<Object,Object> {
		
		protected void processTask(Element child, ReaderContext context, XlimTaskModule task) {
			context.enterTask(task);
			mDeclarationPass.declarePorts(child,context);
			mBlockContainerPass.createContents(child,context,task);
			context.leaveTask();
		}
		
		protected void processChild(Element child,  
				                    ReaderContext context, 
				                    Object dummy1,
				                    Object dummy2) {
			if (getTag(child)==MODULE_TAG) {
				String name=getRequiredAttribute("name",child);
				XlimTaskModule task=context.getTask(name);
				processTask(child,context,task);
			}
		}
		
		public void createModules(Element domElement, ReaderContext context) {
			processChildren(domElement,context,null,null);
		}
	}

	/**
	 * Declares all output ports in a module
	 */
	protected class DeclarationPass extends PassPlugin<Object,Object> {
		protected void processChild(Element child, 
                                    ReaderContext context, 
                                    Object dummy1,
                                    Object dummy2) {
			switch (getTag(child)) {
			case OPERATION_TAG:
			case PHI_TAG:
			case MODULE_TAG:
				declarePorts(child,context);
				break;
			case PORT_TAG:
				if (getRequiredAttribute("dir",child).equals("out")) {
					String sourceId=getRequiredAttribute("source",child);
					XlimType type=getType(child);
					XlimOutputPort port=mFactory.createOutputPort(type);
					context.addOutputPort(sourceId, port);
				}
				break;
			default:
				// we don't care about unhandled tags until second pass
			}
		}
		
		public void declarePorts(Element domElement, ReaderContext context) {
			processChildren(domElement,context,null,null);
		}
	}

	/**
	 * Completes the creation of a block module
	 */
	protected class BlockContainerPass extends PassPlugin<XlimContainerModule,Object> {
		
		protected void processChild(Element child, 
                                    ReaderContext context, 
                                    XlimContainerModule parent, 
                                    Object dummy) {
			int tag=getTag(child);
			if (tag==OPERATION_TAG)
				createOperation(child,parent,context);
			else if (tag==MODULE_TAG)
				createModule(child,parent,context);
			else if (tag==NOTE_TAG)
				processNote(child, context);
			else
				unhandledTag(child);
		}
		
		protected void processNote(Element note, ReaderContext context) {
			String kind=getRequiredAttribute("kind",note);
			
			if (kind.equals("consumptionRates") || kind.equals("productionRates")) {
				String name=getRequiredAttribute("name",note);
				String rate=getRequiredAttribute("value", note);
				XlimTopLevelPort port=context.getTopLevelPort(name);
				if (port==null)
				    throw new IllegalArgumentException("No such port (in <note>): "+name);
				context.setPortRate(port, Integer.valueOf(rate));
			}
		}
		
		public void createContents(Element domModule, ReaderContext context, XlimContainerModule xlimModule) {
			processChildren(domModule,context,xlimModule,null);
		}
	}
	
	/**
	 * Completes the creation of an if-module
	 */
	protected class IfModulePass extends PassPlugin<XlimIfModule,List<Element>> {
		protected void createModule(Element child,
                                    XlimIfModule parent,
                                    ReaderContext context) {
			String kind=getRequiredAttribute("kind",child);
			XlimContainerModule module;
			
			if (kind.equals("test")) {
				XlimTestModule test=parent.getTestModule();
				setTestAttributes(test,child,context);
				module=test;
			}
			else if (kind.equals("then")) {
				module=parent.getThenModule();
				setContainerModuleAttributes(module,child,context);
			}
			else if (kind.equals("else")) {
				module=parent.getElseModule();
				setContainerModuleAttributes(module,child,context);
			}
			else {
				throw new RuntimeException("Unexpected attribute kind=\""+kind+"\"");
			}
			
			mBlockContainerPass.createContents(child,context,module);
		}
		
		protected void processChild(Element child, 
                                    ReaderContext context,
                                    XlimIfModule parent, 
                                    List<Element> phiNodes) {
			int tag=getTag(child);
			if (tag==MODULE_TAG)
				createModule(child,parent,context);
			else if (tag==PHI_TAG)
				phiNodes.add(child); // Postpone creation of phi-nodes
			else
				unhandledTag(child);
		}
		
		public void createContents(Element domModule, ReaderContext context, XlimIfModule xlimModule) {
			List<Element> phiNodes=new ArrayList<Element>();
			processChildren(domModule,context,xlimModule,phiNodes);
			// Now when we have created "then" or "else" modules we are ready to take on the phi-nodes
			for (Element phiElement: phiNodes)
			    createPhiNode(phiElement,
			    		      xlimModule,
			    		      xlimModule.getThenModule(),
			    		      xlimModule.getElseModule(),
			    		      context);
		}
	}

	/**
	 * Completes the creation of a loop-module
	 */

	protected class LoopModulePass extends PassPlugin<XlimLoopModule,List<Element>> {
		protected void createModule(Element child,
                                    XlimLoopModule parent,
                                    ReaderContext context) {
			String kind=getRequiredAttribute("kind",child);
			XlimContainerModule module;
			
			if (kind.equals("test")) {
				XlimTestModule test=parent.getTestModule();
				setTestAttributes(test,child,context);
				module=test;
			}
			else if (kind.equals("body")) {
				module=parent.getBodyModule();
				setContainerModuleAttributes(module,child,context);
			}
			else {
				throw new RuntimeException("Unexpected attribute kind=\""+kind+"\"");
			}
			
			mBlockContainerPass.createContents(child,context,module);
		}
				
		protected void processChild(Element child, 
                                    ReaderContext context, 
                                    XlimLoopModule parent, 
                                    List<Element> phiNodes) {
			int tag=getTag(child);
			if (tag==MODULE_TAG)
				createModule(child,parent,context);
			else if (tag==PHI_TAG) 
				phiNodes.add(child);
			else
				unhandledTag(child);
		}
		
		public void createContents(Element domModule, ReaderContext context, XlimLoopModule xlimModule) {
			List<Element> phiNodes=new ArrayList<Element>();
			processChildren(domModule,context,xlimModule,phiNodes);
			// Now when we have created the "body" module we are ready to take on the phi-nodes
			for (Element phiElement: phiNodes)
			    createPhiNode(phiElement,
			    		      xlimModule,
			    		      null,
			    		      xlimModule.getBodyModule(),
			    		      context);
		}
	}

	
	/**
	 * Finds the ports of an operation/phi-node
	 */
	protected class PortPass extends PassPlugin<ArrayList<XlimSource>,
	                                            ArrayList<XlimOutputPort> > {
		protected void processChild(Element child, 
                ReaderContext context, 
                ArrayList<XlimSource> inputs, 
                ArrayList<XlimOutputPort> outputs) {
			
			if (getTag(child)==PORT_TAG) {
				String source=getRequiredAttribute("source",child);
				XlimSource xlimSrc=context.getSource(source);
				if (xlimSrc==null)
					throw new RuntimeException("No such source in context: source=\""+source+"\"");
				String dir=getRequiredAttribute("dir",child);
				
				if (dir.equals("in")) {
					inputs.add(xlimSrc);
				}
				else if (dir.equals("out")) {
					XlimOutputPort port=xlimSrc.isOutputPort();
					if (port==null)
						throw new RuntimeException("Not a port: source=\""+source+"\"");
					outputs.add(port);
				}
				else
					throw new RuntimeException("Unexpected attribute dir=\""+dir+"\"");
			}
			else
				unhandledTag(child);
		}
		
		public void findPorts(Element domElement,
				              ReaderContext context,
				              ArrayList<XlimSource> inputs, 
				              ArrayList<XlimOutputPort> outputs) {
			processChildren(domElement,context,inputs,outputs);
		}
	}
	
	/**
	 * Creates the initial values of a state variable
	 */
	protected class InitValuePass extends PassPlugin<Collection<XlimInitValue>,Object> {
		protected XlimInitValue createScalar(Element domElement) {
			XlimType type=getType(domElement);
			String value=getRequiredAttribute("value",domElement);
			return mFactory.createInitValue(value,type);
		}
		
		protected XlimInitValue createAggregate(Element domElement) {
			List<XlimInitValue> aggregate=new ArrayList<XlimInitValue>();
			createInitValues(domElement,aggregate);
			return mFactory.createInitValue(aggregate);
		}
		
		protected void processChild(Element child, 
                ReaderContext dummyContext, 
                Collection<XlimInitValue> initValues, 
                Object dummy) {
			
			if (getTag(child)==INITVALUE_TAG) {
				XlimInitValue initValue;
				if (getRequiredAttribute("typeName",child).equals("List")) {
					initValue=createAggregate(child);
				}
				else {
					initValue=createScalar(child);
				}
				initValues.add(initValue);
			}
			else
				unhandledTag(child);
		}
		
		public void createInitValues(Element domElement,
				                     Collection<XlimInitValue> initValues) {
			processChildren(domElement,null,initValues,null);
		}
	}
}

