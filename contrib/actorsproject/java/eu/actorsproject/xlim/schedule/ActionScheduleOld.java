/* 
 * Copyright (c) Ericsson AB, 2010
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

package eu.actorsproject.xlim.schedule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.actorsproject.util.MergeFindPartition;
import eu.actorsproject.util.XmlPrinter;
import eu.actorsproject.xlim.decision2.ActionNode;
import eu.actorsproject.xlim.decision2.DecisionNode;
import eu.actorsproject.xlim.decision2.DecisionTree;
import eu.actorsproject.xlim.decision2.NullNode;
import eu.actorsproject.xlim.decision2.PortSignature;
import eu.actorsproject.xlim.io.ReaderContext;

/**
 * Represents all possible action schedules of the actor
 */
public class ActionScheduleOld extends ActionSchedule {

	private ControlFlowGraph mControlFlowGraph;
	private MergeFindPartition<BasicBlock> mPartition;
	private ReaderContext mOriginalSymbols;
	private StaticActionSchedule mStaticSchedule;
	private boolean mHasStaticSchedule;
	private boolean mIsTimingDependent;
	
	protected ActionScheduleOld(ControlFlowGraph controlFlowGraph,
			                 ReaderContext originalSymbols) {
		mControlFlowGraph=controlFlowGraph;
		mPartition=new MergeFindPartition<BasicBlock>();
		mOriginalSymbols=originalSymbols;
	}
	
	/**
	 * @param controlFlowGraph
	 * @param originalSymbols   a ReaderContext, which allows for symbol look-up
	 *                          in terms of original names (as given in input file).
	 *                          May be null, but then no renaming of identifiers
	 *                          will be done in XLIM print-outs.
	 * @return an action schedule that is constructed from the control-flow graph
	 */
	public static ActionSchedule create(ControlFlowGraph controlFlowGraph,
			                            ReaderContext originalSymbols) {
		ActionScheduleOld result=new ActionScheduleOld(controlFlowGraph, originalSymbols);
		result.createInitialPartition();
		// TODO: substitute mergePhases() for checkPhases() once we got
		// the DecisionTree right (merge them, if there is more than one)
		// result.mergePhases();
		result.checkPhases();
		return result;
	}
	
	/**
	 * @return true if there is a static action schedule
	 */
	public boolean isStatic() {
		return mHasStaticSchedule;
	}
	
	/**
	 * @return true if the actor may terminate (transition to a state,
	 *         from which no further action scheduling is possible);
	 *         false if there is no terminal state (actor will always
	 *         be able to fire, given sufficient input).
	 */
	public boolean mayTerminate() {
		return mControlFlowGraph.getTerminalNode()!=null;
	}
	
	/**
	 * @return true if action selection tests for absense of input,
	 *         which *might* lead to a timing-dependent action schedule
	 *         (which in turn might result in timing-dependent behavior).
	 */
	public boolean isTimingDependent() {
		return mIsTimingDependent;
	}
	
	/**
	 * @return the static action schedule (if one has been found, see isStatic)
	 */
	public StaticActionSchedule getStaticSchedule() {
		
		if (mHasStaticSchedule && mStaticSchedule==null)
			mStaticSchedule=createStaticSchedule();
		return mStaticSchedule;
	}
	
	/**
	 * Prints the action schedule in XML format
	 * 
	 * @param printer  where the XML output is generated
	 */
	public void printActionSchedule(XmlPrinter printer) {
		String mayTerminate=" may-terminate="+yesOrNo(mayTerminate());
		String kind;
		
		if (isTimingDependent()) {
			kind=" kind=\"timing-dependent\"";
		}
		else if (isStatic()) {
			kind=" kind=\"static\"";	
		}
		else {
			kind=" kind=\"dynamic\"";
		}
		
		printer.println("<actionSchedule"+kind+mayTerminate+">");
		if (mHasStaticSchedule) {
			printer.increaseIndentation();
			StaticActionSchedule staticSchedule=getStaticSchedule();
			printer.printElement(staticSchedule.asXmlElement());
			printer.decreaseIndentation();
		}
		printer.println("</actionSchedule>");
	}
	
	/**
	 * Constructs the static action schedule by following the unique successors
	 * of the "phases" (blocks in the partition)
	 * 
	 * @return the static action schedule
	 */
	private StaticActionSchedule createStaticSchedule() {
		// list of subschedules
		List<StaticSubSchedule> phases=new ArrayList<StaticSubSchedule>();
		// mapping from "phase" to index in list
		Map<MergeFindPartition<BasicBlock>.Block, Integer> phaseMap=
			new HashMap<MergeFindPartition<BasicBlock>.Block, Integer>();
		MergeFindPartition<BasicBlock>.Block nextPhase=
			mPartition.find(mControlFlowGraph.getInitialNode());
		
		
		while (nextPhase!=null && phaseMap.containsKey(nextPhase)==false) {
			// Create the StaticPhase
			// TODO: we should also support merging of decision trees (remove assertion)
			assert(nextPhase.size()==1);
			BasicBlock block=nextPhase.iterator().next();
			DecisionTree root=block.getDecisionTree();
			
			StaticPhase phase=new StaticPhase(root.getMode(), block);
			phaseMap.put(nextPhase, phases.size());
			phases.add(phase);
			
			assert(hasStaticSuccessor(nextPhase) || block==mControlFlowGraph.getTerminalNode());
			nextPhase=getStaticSuccessor(nextPhase);
		}
		
		StaticSubSchedule initialSchedule=null;
		StaticSubSchedule repeatedSchedule=null;
		
		if (nextPhase!=null) {
			// This schedule is repeated indefinitely
			int loopHeader=phaseMap.get(nextPhase);
			if (loopHeader!=0) {
				initialSchedule=new StaticSequence(phases.subList(0, loopHeader), 1);
			}
			repeatedSchedule=new StaticSequence(phases.subList(loopHeader, phases.size()), 1);
		}
		else {
			// This is a finite static schedule
			initialSchedule=new StaticSequence(phases, 1);
		}
		
		return new StaticActionSchedule(initialSchedule,repeatedSchedule,mOriginalSymbols);
	}
	
	private String yesOrNo(boolean p) {
		return p? "\"yes\"" : "\"no\"";
	}
	
	
	
	/**
	 * Create a first partition, consisting of the initial node
	 * of the CFG and the successors of all actions 
	 */
	private void createInitialPartition() {
		CheckSuccessor test=new CheckSuccessor();
		BasicBlock initialNode=mControlFlowGraph.getInitialNode();

		// Start with an optimistic assumption
		mHasStaticSchedule=true;    // ->false in CheckSuccessor, mergePhases
		mIsTimingDependent=false;   // ->true  in CheckSuccessor
		
		mPartition.add(initialNode);
		test.check(initialNode);
		
		for (ActionNode action: mControlFlowGraph.getActions()) {
			BasicBlock succ=mControlFlowGraph.getSuccessor(action);
			mPartition.add(succ);
			test.check(succ);
		}
	}
	
	private class CheckSuccessor implements DecisionTree.Visitor<Boolean,Object> {

		/**
		 * Checks the decision tree of a "successor" for
		 * (1) a common portSignature (otherwise there is no static schedule)
		 * (2) tests for absence of tokens (=the schedule may be timing dependent)
		 * 
		 * @param succ  a basic block, which is the succesor
		 *              (of the initial node or an action)
		 */
		void check(BasicBlock succ) {
			DecisionTree root=succ.getDecisionTree();
			
			if (root.getMode()==null) {
				if (succ!=mControlFlowGraph.getTerminalNode()) {
					// the phase has no common PortSignature =no static schedule
					mHasStaticSchedule=false;
					if (root.accept(this, null))
						mIsTimingDependent=true;
				}
			}
		}
		
		public Boolean visitDecisionNode(DecisionNode decision, Object dummy) {
			return decision.getCondition().mayTestTokenAbsence(decision.requiredPortSignature())
			       || decision.getChild(true).accept(this, null)
			       || decision.getChild(true).accept(this, null);
		}

		public Boolean visitActionNode(ActionNode action, Object dummy) {
			return false;
		}

		public Boolean visitNullNode(NullNode node, Object dummy) {
			return false;
		}
	}
	
	/**
	 * Checks if we have a static schedule
	 * TODO: included in mergePhases, remove when no longer used. 
	 */
	private void checkPhases() {
		for (BasicBlock root: mPartition) {
			
			if (root.getDecisionTree().getMode()==null) {
				if (root!=mControlFlowGraph.getTerminalNode()) {
					// the phase has no common PortSignature =no static schedule
					mHasStaticSchedule=false;
					return;
				}
			}
			else {
				MergeFindPartition<BasicBlock>.Block phase=mPartition.find(root);
				if (hasStaticSuccessor(phase)==false) {
					mHasStaticSchedule=false;
					return;
				}
			}
		}
	}
	
	/**
	 * Merge the successors of a "phase" (=a set of basic blocks/decision trees)
	 * if possible (=when all reachable actions have the same token rates).
	 * 
	 * If each "phase" gets a unique successor (all successors were merged),
	 * then there is a static action schedule.
	 */
	private void mergePhases() {
		Set<MergeFindPartition<BasicBlock>.Block> workList =
			new LinkedHashSet<MergeFindPartition<BasicBlock>.Block>();
		
		mHasStaticSchedule=true;
		for (BasicBlock root: mPartition) {
			
			if (root.getDecisionTree().getMode()==null) {
				if (root!=mControlFlowGraph.getTerminalNode()) {
					// the phase has no common PortSignature =no static schedule
					mHasStaticSchedule=false;
				}
			}
			else {
				MergeFindPartition<BasicBlock>.Block phase=mPartition.find(root);
				if (hasStaticSuccessor(phase)==false) {
					// phase has multiple successors, put on workList
					workList.add(phase);
				}
			}
		}
		
		while (workList.isEmpty()==false) {
			MergeFindPartition<BasicBlock>.Block phase=workList.iterator().next();
			
			workList.remove(phase);
			
			if (mergeSuccessors(phase)) {
				// its successors were successfully merged
				assert(hasStaticSuccessor(phase));
				MergeFindPartition<BasicBlock>.Block nextPhase=getStaticSuccessor(phase);
				
				if (hasStaticSuccessor(nextPhase)==false) {
					workList.add(nextPhase);
				}
			}
			else if (hasStaticSuccessor(phase)==false) {
				// Couldn't merge successors = we failed to find a static schedule
				mHasStaticSchedule=false;
			}
		}	
	}
	
	/**
	 * @param phase  a block in the partition
	 * @return       true if there is a unique "successor phase", which follows 'phase'
	 */
	private boolean hasStaticSuccessor(MergeFindPartition<BasicBlock>.Block phase) {
		return getStaticSuccessor(phase)!=null;
	}
	
	
	/**
	 * @param phase  a block in the partition
	 * @return       the unique "successor phase", if any 
	 */
	private MergeFindPartition<BasicBlock>.Block 
	getStaticSuccessor(MergeFindPartition<BasicBlock>.Block phase) {
		
		MergeFindPartition<BasicBlock>.Block result=null;
		
		// Iterate over all actions that are reachable from the roots of the phase
		// Check if there is a common successor phase
		for (BasicBlock root: phase) {
			for (ActionNode action: root.getDecisionTree().reachableActionNodes()) {
				BasicBlock succ=mControlFlowGraph.getSuccessor(action);
				MergeFindPartition<BasicBlock>.Block nextPhase=mPartition.find(succ);
				if (result==null)
					result=nextPhase;
				else if (result!=nextPhase)
					return null; // There are several successors
			}
		}
		return result;
	}
	
	/**
	 * Checks if the successors of 'phase' can be merged (have the same token rates) and,
	 * provided this is the case, merges them.
	 * 
	 * @param phase  a block in the partition
	 * @return       true if successors were merged (=partition was modified).
	 * 
	 * When the method returns false either:
	 * a) The successors differ in token rates, or
	 * b) The successors were already merged (or there is just a single one).
	 */
	private boolean mergeSuccessors(MergeFindPartition<BasicBlock>.Block phase) {
		// First check if we can merge (=if successors have the same port rate)
		PortSignature commonPortSignature=null;
		
		for (BasicBlock root: phase) {
			for (ActionNode action: root.getDecisionTree().reachableActionNodes()) {
				BasicBlock succ=mControlFlowGraph.getSuccessor(action);
				PortSignature portSignature=succ.getDecisionTree().getMode();
				
				if (portSignature==null)
					return false;
				else if (commonPortSignature==null)
					commonPortSignature=portSignature;
				else if (portSignature.equals(commonPortSignature)==false)
					return false;
			}
		}
		
		BasicBlock representer=null;
		boolean changed=false;
		
		for (BasicBlock root: phase) {
			for (ActionNode action: root.getDecisionTree().reachableActionNodes()) {
				BasicBlock succ=mControlFlowGraph.getSuccessor(action);
				
				if (representer==null)
					representer=succ;
				else if (mPartition.merge(representer, succ))
					changed=true;
			}
		}
		
		return changed;
	}
}
