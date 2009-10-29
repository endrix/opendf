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

import java.util.HashMap;
import java.util.Set;


import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.XlimIfModule;
import eu.actorsproject.xlim.XlimLoopModule;
import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimPhiNode;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTestModule;
import eu.actorsproject.xlim.dependence.Location;
import eu.actorsproject.xlim.dependence.PhiOperator;
import eu.actorsproject.xlim.dependence.SideEffectPhiOperator;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueOperator;
import eu.actorsproject.xlim.dependence.ValueUsage;

/**
 * Used with code motion to achieve "as-late-as-possible" evaluation
 * of expressions.
 */
public class LatestEvaluationAnalysis {

	protected LatestEvaluationTraversal mTaskTraversal=new LatestEvaluationTraversal();
	
	public CodeMotionPlugIn analyze(XlimDesign design) {
		LatestEvaluationResult result=new LatestEvaluationResult();
		for (XlimTaskModule task: design.getTasks()) {
			mTaskTraversal.traverse(task, new LatestEvaluationArg(task, result));
		}
		return result;
	}
	
	public CodeMotionPlugIn analyze(XlimTaskModule task) {
		LatestEvaluationResult result=new LatestEvaluationResult();
		mTaskTraversal.traverse(task, new LatestEvaluationArg(task, result));
		return result;
	}
}

class LatestEvaluationResult implements CodeMotionPlugIn {

	private HashMap<ValueOperator,XlimModule> mLatest=
		new HashMap<ValueOperator, XlimModule>();
	
	@Override
	public XlimModule insertionPoint(XlimOperation op) {
		return latest(op.getValueOperator());
	}
	
	XlimModule latest(ValueOperator op) {
		return mLatest.get(op);
	}
		
	boolean noLater(ValueOperator op, XlimModule module) {
		XlimModule m=mLatest.get(op);
		if (m!=null)
			module=module.leastCommonAncestor(m);
		if (m!=module) {
			mLatest.put(op, module);
			return true;
		}
		else
			return false;
	}
}


class LatestEvaluationArg {

	KilledStateProperty mKilled;
	LatestEvaluationResult mResult;
	
	LatestEvaluationArg(XlimTaskModule task, LatestEvaluationResult result) {
		mKilled=KilledStateProperty.analyze(task);
		mResult=result;
	}	
	
	boolean noLater(XlimOperation op, XlimModule module) {
		return mResult.noLater(op.getValueOperator(), module);
	}
	
	boolean noLater(ValueUsage use, XlimModule module) {
		ValueOperator def=use.getValue().getDefinition();
		if (def!=null)
			return mResult.noLater(def,module);
		else
			return false;
	}

	XlimModule computeLatest(XlimOperation op) {
		XlimModule earliest=op.getParentModule();
		XlimModule latest=mResult.latest(op.getValueOperator());
					
		// Better safe than sorry, in this version we neither move state updates
		// nor things with "volatile" side-effects (such as pinWaits)
		if (latest==null || op.modifiesLocation() || op.isRemovable()==false) {
			// if (latest==null)
			//  System.out.println("// LatestEvaluationAnalysis: no latest point: "+op);
			// else
			//   System.out.println("// LatestEvaluationAnalysis: not moved: "+op);
			latest=earliest;
		}
		
		// If op accesses state, we must take care not to move beyond state updates
		Iterable<? extends ValueNode> inputs=null;
		if (op.dependsOnLocation()) {
			inputs=op.getValueOperator().getInputValues();
		}
		
		// Hoist out of loops and above state modifications
		assert(earliest.leastCommonAncestor(latest)==earliest);
		XlimModule m=latest;
		XlimModule child=null;
		while (m!=earliest) {
			if (m instanceof XlimLoopModule) {
				// System.out.println("// LatestEvaluationAnalysis: "+op+" hoisted out of loop "+m);
				latest=m;
			}
			else if (inputs!=null && child!=null) {
				// The set of values killed on the path from 'm' to 'child'
				Set<Location> killed=mKilled.killedInParent(child);
				for (ValueNode value: inputs) {
					Location location=value.actsOnLocation();
					if (location!=null && killed.contains(location)) {
						// System.out.println("// LatestEvaluationAnalysis: "+op+"hoisted above definition of "+location);
						latest=m;
						break;
					}
				}
			}
			child=m;
			m=m.getParentModule();
		}
		
		// System.out.println("// LatestEvaluationAnalysis: LATEST("+op+")="+latest);
		noLater(op, latest);
		return latest;
	}
}

class LatestEvaluationTraversal extends BottomUpXlimTraversal<Object,LatestEvaluationArg> {

	@Override
	protected Object traverseTestModule(XlimTestModule m, LatestEvaluationArg arg) {
		ValueUsage decision=m.getValueUsage();
		arg.noLater(decision, m);
		
		return super.traverseTestModule(m, arg);
	}

	@Override
	protected Object traverseIfModule(XlimIfModule m, LatestEvaluationArg arg) {
		traverseStatePhiNodes(m.getStatePhiOperators(),arg);
		super.traverseIfModule(m, arg);
		return null;
	}
	
	protected Object traverseLoopModule(XlimLoopModule m, LatestEvaluationArg arg) {
		super.traverseLoopModule(m, arg);
		traverseStatePhiNodes(m.getStatePhiOperators(),arg);
		return null;
	}	

	private void traverseStatePhiNodes(Iterable<? extends SideEffectPhiOperator> phiOperators,
				                  LatestEvaluationArg arg) {
		for (SideEffectPhiOperator phi: phiOperators) {
			handlePhiOperator(phi,arg);
		}
	}

	@Override
	protected Object handlePhiNode(XlimPhiNode phi, LatestEvaluationArg arg) {
		handlePhiOperator(phi.getValueOperator(), arg);
		return null;
	}
	
	private void handlePhiOperator(PhiOperator phi, LatestEvaluationArg arg) {
		// Arguments of phi-nodes must be available at end predecessor modules:
		// "then-module" or loop pre-header for first argument
		// "else-module" or loop body for second argument
		ValueUsage use0=phi.getUsedValue(0);
		ValueUsage use1=phi.getUsedValue(1);
		arg.noLater(use0, phi.usedInModule(use0));
		arg.noLater(use1, phi.usedInModule(use1));
	}
	
	@Override
	protected Object handleOperation(XlimOperation op, LatestEvaluationArg arg) {
		XlimModule latest=arg.computeLatest(op);
		
		// Propagate latest to the values used by 'op'
		for (ValueUsage use: op.getValueOperator().getUsedValues()) {
			arg.noLater(use, latest);
			// boolean changed=arg.noLater(use, latest);
			// if (changed)
			//	System.out.println("// LatestEvaluationAnalysis: "+use.getValue().getUniqueId()
			//			            +" <= LATEST("+op+")");
		}
		
		return null;
	}
}
