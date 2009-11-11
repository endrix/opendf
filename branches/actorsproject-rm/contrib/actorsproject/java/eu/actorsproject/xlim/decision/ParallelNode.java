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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.absint.AbstractValue;
import eu.actorsproject.xlim.absint.Context;
import eu.actorsproject.xlim.absint.DemandContext;
import eu.actorsproject.xlim.dependence.DependenceSlice;

/**
 * A ParallelNode groups nodes of a decision tree that are evaluated
 * in parallel (in Xlim they are enclosed in a "mutex" module).
 */
public class ParallelNode extends DecisionTree {

	private ArrayList<DecisionTree> mChildren;
	private PortSignature mPortSignature, mMode;
	private boolean mModeHasBeenSet;
	
	public static DecisionTree create(DecisionTree t1, DecisionTree t2) {
		if (t1==null)
			return t2;
		else if (t2==null)
			return t1;
		else {
			ParallelNode p1;
			if (t1 instanceof ParallelNode) {
				p1=(ParallelNode) t1;
			}
			else {
				p1=new ParallelNode(t1);
			}
			
			p1.add(t2);
			return p1;
		}
	}
	
	private ParallelNode(DecisionTree t1) {
		mChildren=new ArrayList<DecisionTree>();
		mChildren.add(t1);
	}

	private void add(DecisionTree t) {
		if (t instanceof ParallelNode)
			mChildren.addAll(((ParallelNode) t).mChildren);
		else
			mChildren.add(t);
		// adding a tree affects the port signature: invalidate it
		mPortSignature=null;
		if (mMode!=null) {
			mModeHasBeenSet=false;
			mMode=null;
		}
	}
	
	@Override
	public PortSignature requiredPortSignature() {
	
		if (mPortSignature==null) {
			PortSignature result=mChildren.get(0).requiredPortSignature();
			
			for (int i=1; i<mChildren.size() && result.isEmpty()==false; ++i) {
				DecisionTree root=mChildren.get(i);
				PortSignature s=root.requiredPortSignature();
				result=PortSignature.intersect(result, s);
			}
			mPortSignature=result;
		}
		
		return mPortSignature;
	}
	
	
	@Override
	public PortSignature getMode() {
		if (mModeHasBeenSet==false) {
			PortSignature result=mChildren.get(0).getMode();
			
			for (int i=1; i<mChildren.size() && result!=null; ++i) {
				DecisionTree root=mChildren.get(i);
				PortSignature m=root.getMode();
				if (m==null || m.equals(result)==false)
					result=null;
			}	
			mMode=result;
			mModeHasBeenSet=true;
		}
		
		return mMode;
	}

	@Override
	protected XlimModule getModule() {
		// ParallelNode corresponds to several modules
		throw new UnsupportedOperationException("ParallelNode.getModule");
	}
	
	
	@Override
	public void findActionNodes(List<ActionNode> actionNodes) {
		for (DecisionTree root: mChildren)
			root.findActionNodes(actionNodes);
	}

	@Override
	protected void findPinAvail(Map<XlimTopLevelPort, XlimOperation> pinAvailMap) {
		for (DecisionTree root: mChildren)
			root.findPinAvail(pinAvailMap);
	}
	
	@Override
	protected DecisionTree hoistAvailabilityTests(PortSignature parentSignature, 
			                                      Map<XlimTopLevelPort, XlimOperation> pinAvailMap) {
		// We have not implemented any scheme for code generation from "mutex" modules
		// (which imply parallel evaluation of guard conditions). Use the modified SSA-
		// generator instead (which uses nested if-then-else constructs instead)
		
		// In principle, we could implement hoisting, but we won't be able to test it...
		throw new UnsupportedOperationException("\"mutex\" modules not supported");
	}

	
	@Override
	protected DecisionTree removeRedundantTests(Map<XlimTopLevelPort, Integer> successfulTests) {
		for (int i=0; i<mChildren.size(); ++i) {
			DecisionTree root=mChildren.get(i);
			root=root.removeRedundantTests(successfulTests);
			mChildren.set(i, root);
		}
		return this;
	}

	
	@Override
	protected void generateBlockingWaits(Map<XlimTopLevelPort, Integer> failedTests) {
		for (DecisionTree root: mChildren)
			root.generateBlockingWaits(failedTests);
	}

	@Override
	public void createDependenceSlice(DependenceSlice slice) {
		for (DecisionTree root: mChildren)
			root.createDependenceSlice(slice);
	}
	
	@Override
	public <T extends AbstractValue<T>> DecisionTree foldDecisions(Context<T> context) {
		DecisionTree uniqueSubtree=null;
		
		// Look for a unique non-null subtree that can be deduced
		for (DecisionTree root: mChildren) {
			DecisionTree t=root.foldDecisions(context);
			if (t.isNullNode()==null)
				if (uniqueSubtree==null)
					uniqueSubtree=t;
				else
					return this; // no unique root, return this node
		}
		
		if (uniqueSubtree!=null)
			return uniqueSubtree;
		else
			return this; // no unique root, return this node
	}
	

	@Override
	public <T extends AbstractValue<T>> void 
		propagateState(DemandContext<T> context, 
				       StateEnumeration<T> stateEnumeration) {
		for (DecisionTree root: mChildren) {
			root.propagateState(context, stateEnumeration);
		}
	}

	
	@Override
	protected <T extends AbstractValue<T>> 
	boolean createPhase(DemandContext<T> context, 
			                 StateEnumeration<T> stateEnum, 
			                 boolean blockOnNullNode, 
			                 List<DecisionTree> leaves) {
		boolean isNonDeterministic=false;
		for (DecisionTree root: mChildren) {
			if (root.createPhase(context, stateEnum, blockOnNullNode, leaves))
				isNonDeterministic=true;
		}
		return isNonDeterministic;
	}

	@Override
	protected <T extends AbstractValue<T>> Characteristic printModes(StateEnumeration<T> stateEnum, PortSignature inMode) {
		if (inMode==null) {
			inMode=getMode();
			if (inMode!=null)
				System.out.println("Mode: "+inMode);
		}
		Characteristic cMax=Characteristic.EMPTY;
		for (DecisionTree child: mChildren) {
			Characteristic cChild=child.printModes(stateEnum, inMode);
			if (cChild.compareTo(cMax)>0)
				cMax=cChild;
		}
		return cMax;
	}
	
	@Override
	public <T extends AbstractValue<T>> Characteristic 
		printTransitions(DemandContext<T> context, 
				         StateEnumeration<T> stateEnumeration, 
				         PortSignature inMode, 
				         boolean blockOnNullNode) {
		if (inMode==null) {
			inMode=getMode();
			if (inMode!=null)
				System.out.println("    --> Mode: "+inMode);
		}
		
		Characteristic cMax=Characteristic.EMPTY; 
		for (DecisionTree child: mChildren) {
			Characteristic cChild=
				child.printTransitions(context, stateEnumeration, inMode, blockOnNullNode);
			if (cChild.compareTo(cMax)>0)
				cMax=cChild;
		}
		return cMax;
	}
	/* XmlElement interface */

	@Override
	public String getTagName() {
		return "parallelNode";
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return mChildren;
	}
}
