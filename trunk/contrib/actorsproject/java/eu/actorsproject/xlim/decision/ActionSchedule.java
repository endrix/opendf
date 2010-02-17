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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import eu.actorsproject.util.MergeFindPartition;
import eu.actorsproject.util.XmlPrinter;
import eu.actorsproject.xlim.XlimTopLevelPort;

/**
 * Represents all possible action schedules of the actor
 */
public class ActionSchedule {

	private SchedulingPhase mInitialPhase;
	private Map<ActionNode,SchedulingPhase> mPhases;
	private boolean mMayTerminate;
	private boolean mIsDeterministic;
	private boolean mHasStaticSchedule;
	private MergeFindPartition<DecisionTree> mModes;
	
	public ActionSchedule(SchedulingPhase initialPhase) {
		mInitialPhase=initialPhase;
		mPhases=new HashMap<ActionNode,SchedulingPhase>();
		mMayTerminate=false;
		mIsDeterministic=true;
		mHasStaticSchedule=true; // an optimisitic assumption -later to be verified
	}
		
	public SchedulingPhase getInitialPhase() {
		return mInitialPhase;
	}
	
	public SchedulingPhase getPhase(DecisionTree t) {
		return mPhases.get(t);
	}
	
	public void addPhase(ActionNode actionNode, SchedulingPhase phase) {
		mPhases.put(actionNode,phase);
		if (phase.mayTerminate())
			mMayTerminate=true;
		if (phase.isDeterministic()==false)
			mIsDeterministic=false;
		if (phase.hasStaticPortSignature()==null) {
			mHasStaticSchedule=false;
		}
	}
	
	public boolean mayTerminate() {
		return mMayTerminate;
	}
	
	public boolean isDeterministic() {
		return mIsDeterministic;
	}
	
	public boolean hasStaticSchedule() {
		if (mHasStaticSchedule && mModes==null) {
			// Partition leaves to prove that there is a static schedule
			partitionLeaves();
		}
		return mHasStaticSchedule;
	}
	
	public void printStaticSchedule(XmlPrinter printer) {
		Map<DecisionTree,Integer> visited=new HashMap<DecisionTree,Integer>();
		
		// Repeat until we find an infinite loop or termination
		DecisionTree nextRep=getRepresenter(mInitialPhase);
		int phaseId=0;
		while (nextRep!=null) {
			DecisionTree rep=nextRep;
			visited.put(rep, phaseId);
			
			nextRep=getRepresenter(mPhases.get(rep));
			Integer visitedId=visited.get(nextRep);
			int nextId=(visitedId!=null)? visitedId : phaseId+1;
			
			printer.println("<mode id=\""+phaseId+"\" next=\""+nextId+"\">");
			printer.increaseIndentation();
			for (DecisionTree l: mModes.find(rep)) {
				printer.printElement(l);
			}
			printer.decreaseIndentation();
			printer.println("</mode>");
			
			if (nextId<=phaseId)
				break;
			++phaseId;
		}
		
		if (nextRep==null) {
			printer.println("<mode id=\""+phaseId+"\" terminate=\"yes\"/>");
		}
	}
	
	public LinkedList<Integer> getPortPattern(String portName) {
		Map<DecisionTree,Integer> visited=new HashMap<DecisionTree,Integer>();
		LinkedList<Integer> pattern = new LinkedList<Integer>();
		// Repeat until we find an infinite loop or termination
		DecisionTree nextRep=getRepresenter(mInitialPhase);
		int phaseId=0;
		while (nextRep!=null) {
			boolean portFoundInPhase = false;
			DecisionTree rep=nextRep;
			visited.put(rep, phaseId);
			
			nextRep=getRepresenter(mPhases.get(rep));
			Integer visitedId=visited.get(nextRep);
			int nextId=(visitedId!=null)? visitedId : phaseId+1;
			
			PortSignature ps = mPhases.get(rep).hasStaticPortSignature();
			if (ps != null) {
				for (XlimTopLevelPort port : ps.getPorts()) {
					if (port.getSourceName().equals(portName)) {
						pattern.add(ps.getPortRate(port));
						portFoundInPhase = true;
					}
				}			
			}
			if (!portFoundInPhase) {
				pattern.add(0);
			}
			if (nextId<=phaseId)
				break;
			++phaseId;
		}		
		return pattern;
	}

	private DecisionTree getRepresenter(SchedulingPhase phase) {
		DecisionTree aLeaf=phase.getLeaves().get(0);
		Set<DecisionTree> mode=mModes.find(aLeaf);
		
		if (mode!=null)
			return mode.iterator().next();
		else
			return null;
	}
	
	private void partitionLeaves() {
		// We currently require the following things:
		// a) that each phase hase a static port signature (actions in same "mode")
		//    so that its leaves can be merged into a common phase.
		// b) that there is a common "next" mode, corresponding to the merged leaves
		//    This is the trycky part: although their port signatures match, their
		//    future trajectory doesn't necessarily match. We test this property by
		//    repreatedly merging the leaves of the "next" mode until we have a complete, 
		//    successful partition or we fail to merge.
		//
		// TODO: this procedure only detects a subset of possible static schedules
		// In particular it fails to deal with fixed iterations (i.e CSDF) 
		mModes=new MergeFindPartition<DecisionTree>();
		mModes.addAll(mPhases.keySet());

		// Start by (a) merging the leaves of phases with static port signature
		boolean success=mergeLeaves(mInitialPhase);
		for (SchedulingPhase phase: mPhases.values()) {
			success|=mergeLeaves(phase);
			if (!success)
				break;
		}
		
		// Then do (b): revisit all the phases of the partition and verify 
		// that the future trajectory of all merged phases is the same.
		if (success) {
			for (DecisionTree actionNode: mPhases.keySet()) {
				Set<DecisionTree> mode=mModes.find(actionNode);
				if (mergeNextMode(mode)==false) {
					success=false;
					break;
				}
			}
		}
		
		if (success==false) {
			mHasStaticSchedule=false;
			mModes=null;
		}
	}
	
	private boolean mergeLeaves(SchedulingPhase phase) {
		if (phase.hasStaticPortSignature()!=null) {
			// Leaves can be merged, they have the same signature
			mModes.mergeAll(phase.getLeaves());
			return true;
		}
		else {
			// a terminal phase can be part of a static schedule
			return phase.alwaysTerminates();
		}
	}
	
	private boolean mergeNextMode(Iterable<DecisionTree> mode) {
		
		while (true) {
			Iterator<DecisionTree> pAction=mode.iterator();
			SchedulingPhase nextPhase=mPhases.get(pAction.next());

			if (nextPhase.mayTerminate()) {
				// If one phase may terminate, then all phases must terminate 
				// (otherwise the future trajectory is not the same).
				for (DecisionTree actionNode: mode) {
					nextPhase=mPhases.get(actionNode);
					if (nextPhase.alwaysTerminates()==false)
						return false;
				}
				return true;
			}
			else {
				// Check that all "next" phases have the same port signature
				// and merge their leaves if this is the case.
				
				DecisionTree aLeaf=nextPhase.getLeaves().get(0);
				PortSignature signature=nextPhase.hasStaticPortSignature();
				assert(signature!=null);

				while (pAction.hasNext()) {
					nextPhase=mPhases.get(pAction.next());

					if (nextPhase.mayTerminate())
						return false;  // the first phase does not terminate

					if (signature.equals(nextPhase.hasStaticPortSignature())==false)
						return false;  // signatures do not match
				}

				// All port signatures are the same, merge the leaves of the "next" phases
				// Since we have already merged the leaves of each phase, it is sufficient
				// to merge one representer from each "next" phase.
				boolean changed=false;
				for (DecisionTree actionNode: mode) {
					nextPhase=mPhases.get(actionNode);
					DecisionTree anotherLeaf=nextPhase.getLeaves().get(0);
					if (mModes.merge(aLeaf,anotherLeaf))
						changed=true;
				}

				if (changed) {
					// Continue merging the leaves of the resulting, new, mode
					mode=mModes.find(aLeaf);
				}
				else {
					// merge complete and successful
					return true;
				}
			}
		}
	}	
}
