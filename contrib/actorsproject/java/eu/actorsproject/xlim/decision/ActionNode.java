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

package eu.actorsproject.xlim.decision;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimBlockElement;
import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.absint.AbstractValue;
import eu.actorsproject.xlim.absint.Context;
import eu.actorsproject.xlim.absint.DemandContext;
import eu.actorsproject.xlim.dependence.CallNode;
import eu.actorsproject.xlim.dependence.DataDependenceGraph;
import eu.actorsproject.xlim.dependence.Location;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueOperator;
import eu.actorsproject.xlim.dependence.StateLocation;

/**
 * An ActionNode is a leaf of the decision tree, which is
 * associated with code that has side effects (ports, state variables).
 * This is an action firing (or what is left of it).
 */
public class ActionNode extends DecisionTree {

	private XlimContainerModule mAction;
	private Map<StateLocation,ValueNode> mOutputMapping;
	private PortSignature mPortSignature;
	private String mDescription;
	
	public ActionNode(XlimContainerModule action) {
		assert(action!=null);
		parseActionCode(action);
	}
	
	@Override
	public PortSignature requiredPortSignature() {
		return mPortSignature;
	}
	
	@Override
	public PortSignature getMode() {
		return mPortSignature;
	}

	@Override
	protected XlimModule getModule() {
		return mAction;
	}
	
	@Override
	public void findActionNodes(List<ActionNode> actionNodes) {
		actionNodes.add(this);
	}

	
	@Override
	protected DecisionTree hoistAvailabilityTests(PortSignature parentSignature, 
			                                      Map<XlimTopLevelPort, XlimOperation> pinAvailMap) {
		// No tests to hoist at the leaves
		return this;
	}

	
	@Override
	protected void findPinAvail(Map<XlimTopLevelPort, XlimOperation> pinAvailMap) {
		// No pinAvails here...	
	}

	@Override
	protected DecisionTree removeRedundantTests(Map<XlimTopLevelPort, Integer> successfulTests) {
		// Check that we have established required token availability 
		assert(allAvailabilityTestsOk(successfulTests));
		// No tests to remove at the leaves
		return this;
	}
	
	private boolean allAvailabilityTestsOk(Map<XlimTopLevelPort, Integer> successfulTests) {
		for (Map.Entry<XlimTopLevelPort, Integer> entry: successfulTests.entrySet()) {
			XlimTopLevelPort port=entry.getKey();
			if(mPortSignature.getPortRate(port)>entry.getValue())
				return false;
		}
		return true;
	}
	
	
	@Override
	protected void generateBlockingWaits(Map<XlimTopLevelPort, Integer> failedTests) {
		// Not a NullNode (nothing to do)
	}

	/**
	 * @return the mapping from state to values at end of ActionNode
	 */
	public Map<StateLocation,ValueNode> getOutputMapping() {		
		return mOutputMapping;
	}
		
	@Override
	public <T extends AbstractValue<T>> DecisionTree foldDecisions(Context<T> context) {
		return this; // found a leaf node
	}

	
	@Override
	public <T extends AbstractValue<T>> void 
		propagateState(DemandContext<T> context,
				       StateEnumeration<T> stateEnumeration) {
		// State propagation has reached a leaf (this ActionNode)
		stateEnumeration.updateState(this,context);
	}
	
	
	@Override
	protected <T extends AbstractValue<T>> 
	boolean createPhase(DemandContext<T> context, 
			                 StateEnumeration<T> stateEnum, 
			                 boolean blockOnNullNode, 
			                 List<DecisionTree> leaves) {
		leaves.add(this);
		return false;  // *not* non-deterministic
	}

	@Override
	protected <T extends AbstractValue<T>> Characteristic 
	printModes(StateEnumeration<T> stateEnum, PortSignature inMode) {
		if (inMode==null) {
			inMode=getMode();
			System.out.println("Mode: "+inMode);
		}
		System.out.print("    ");
		printAction();
		
		Characteristic cAction=stateEnum.printTransitions(this);
		switch (cAction) {
		case STATIC:
			System.out.println("    Static next mode");
			break;
		case STATE_DEPENDENT:
			System.out.println("    State-dependent next mode");
			break;
		case NON_DETERMINISTIC:
			System.out.println("    input-dependent next mode");
			break;
		case EMPTY:
		default:
		}
		
		return cAction;
	}
	
	@Override
	public <T extends AbstractValue<T>> Characteristic 
	printTransitions(DemandContext<T> context, 
			         StateEnumeration<T> stateEnum, 
			         PortSignature inMode, 
			         boolean hasNDParent) {
		if (inMode==null) {
			inMode=getMode();
			System.out.println("    --> Mode: "+inMode);
		}
		System.out.print("        --> ");
		printAction();
		
		return Characteristic.STATIC;
	}
	
	private void printAction() {
		System.out.print("Action:");
		for (XlimBlockElement element: mAction.getChildren()) {
			if (element instanceof XlimOperation) {
				XlimOperation op=(XlimOperation) element;
				XlimTaskModule task=op.getTaskAttribute();
				if (task!=null)
				    System.out.print(" "+task.getName());
			}
		}
		System.out.println();
	}
	
	/**
	 * Initializes this ActionNode by parsing its XLIM code
	 * 
	 * @param action  the XLIM code of an action node
	 */
	private void parseActionCode(XlimContainerModule action) {
		mDescription="";
		mAction=action;
		mOutputMapping=new HashMap<StateLocation,ValueNode>();
		HashMap<XlimTopLevelPort,Integer> portMap=new HashMap<XlimTopLevelPort,Integer>();
		String delimiter="";
		
		// Visit the action code and find the final definitions of state vars/actor ports
		for (XlimBlockElement element: mAction.getChildren()) {
			if (element instanceof XlimOperation) {
				// TODO: we could use BlockElement.Visitor instead, but
				// we have no way of dealing with port rates and complex flow anyway...
				XlimOperation xlimOp=(XlimOperation) element;
				ValueOperator valueOp=xlimOp.getValueOperator();
				for (ValueNode output: valueOp.getOutputValues()) {
					Location location=output.getLocation();
					if (location!=null && location.isStateLocation())
						mOutputMapping.put(location.asStateLocation(), output);
				}
				
				// Add consumption/production rates of action
				XlimTaskModule task=xlimOp.getTaskAttribute();
				if (task!=null) {
					addRates(task, portMap);
					mDescription += delimiter+task.getName();
					delimiter="+";
				}
			}
			else // Don't know how to handle ActionNodes with non-trivial flow-of-control
				throw new IllegalStateException("too complex ActionNode");
		}	
		
		mPortSignature=new PortSignature(portMap);
	}
	
	/**
	 * Adds the port rates of 'task' to 'portMap'
	 * 
	 * @param task     an XLIM task (action)
	 * @param portMap  map containing port rates
	 */
	private void addRates(XlimTaskModule task, Map<XlimTopLevelPort, Integer> portMap) {
		// The ports are among the accessed state of the task
		CallNode callNode=task.getCallNode();
		DataDependenceGraph ddg=callNode.getDataDependenceGraph();
		for (StateLocation carrier: ddg.getModifiedState()) {
			XlimTopLevelPort port=carrier.asActorPort();
			if (port!=null) {
				Integer oldRate=portMap.get(port);
				int newRate=task.getPortRate(port);
				
				if (oldRate!=null)
					newRate+=oldRate;
				portMap.put(port, newRate);
			}
		}
	}

	public String getDescription() {
		return "ActionNode"+getIdentifier()+" ("+mDescription+")";
	}

	/* XmlElement interface */

	@Override
	public String getTagName() {
		return "actionNode";
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return mAction.getChildren();
	}
}
