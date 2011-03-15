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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import eu.actorsproject.util.MergeFindPartition;
import eu.actorsproject.util.Pair;
import eu.actorsproject.util.XmlPrinter;
import eu.actorsproject.xlim.decision2.ActionNode;
import eu.actorsproject.xlim.decision2.DecisionNode;
import eu.actorsproject.xlim.decision2.DecisionTree;
import eu.actorsproject.xlim.decision2.NullNode;
import eu.actorsproject.xlim.io.ReaderContext;

public class ActionScheduleWithLoops extends ActionSchedule {

	private ControlFlowGraph mControlFlowGraph;
	private LoopTree mLoopTree;
	private ReaderContext mOriginalSymbols;

	private Map<Loop,PhasePartition> mLoopPartitions=new HashMap<Loop,PhasePartition>();
	private StaticActionSchedule mStaticSchedule;
	private boolean mIsTimingDependent;

	private TimingDependenceCheck mTimingDependenceCheck=new TimingDependenceCheck();
	
	protected ActionScheduleWithLoops(ControlFlowGraph controlFlowGraph,
			                  LoopTree loopTree,
			                  ReaderContext originalSymbols) {
		mControlFlowGraph=controlFlowGraph;
		mLoopTree=loopTree;
		mOriginalSymbols=originalSymbols;
		
		// Optimistic assumptions
		mIsTimingDependent=false;
	}
	
	/**
	 * @param controlFlowGraph
	 * @param loopTree
	 * @param originalSymbols   a ReaderContext, which allows for symbol look-up
	 *                          in terms of original names (as given in input file).
	 *                          May be null, but then no renaming of identifiers
	 *                          will be done in XLIM print-outs.
	 * @return an action schedule that is constructed from the control-flow graph
	 */
	public static ActionScheduleWithLoops create(ControlFlowGraph controlFlowGraph,
			                            LoopTree loopTree,
			                            ReaderContext originalSymbols) {
		ActionScheduleWithLoops result=new ActionScheduleWithLoops(controlFlowGraph, loopTree, originalSymbols);
		result.offLineScheduling();
		return result;
	}
	
	/**
	 * @return true if there is a static action schedule
	 */
	public boolean isStatic() {
		return mStaticSchedule!=null;
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
	 * @return true if action selection tests for absence of input,
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
		return mStaticSchedule;
	}
	
	/**
	 * Prints the action schedule in XML format
	 * 
	 * @param printer  where the XML output is generated
	 */
	public void printActionSchedule(XmlPrinter printer) {
		String mayTerminate=" may-terminate="+(mayTerminate()? "\"yes\"" : "\"no\"");
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
		if (mStaticSchedule!=null) {
			printer.increaseIndentation();
			printer.printElement(mStaticSchedule.asXmlElement());
			printer.decreaseIndentation();
		}
		printer.println("</actionSchedule>");
	}
	
	private StaticActionSchedule offLineScheduling() {
		partitionLoops();
		
		PhasePartition phasePart=new PhasePartition(mControlFlowGraph.getInitialNode());
		if (phasePart.createPartition()) {
			// Successfully partitioned the control-flow graph
			List<StaticSubSchedule> phases=new ArrayList<StaticSubSchedule>();
			int nextPhase=phasePart.createStaticSchedule(phases);
			StaticSubSchedule initialSchedule=null;
			StaticSubSchedule repeatedSchedule=null;
			
			if (nextPhase<phases.size()) {
				// The schedule is repeated indefinitely
				if (nextPhase!=0) {
					initialSchedule=createStaticSequence(phases.subList(0, nextPhase));
				}
				repeatedSchedule=createStaticSequence(phases.subList(nextPhase, phases.size()));
			}
			else {
				// This is a finite static schedule
				initialSchedule=createStaticSequence(phases);
			}
			
			mStaticSchedule=new StaticActionSchedule(initialSchedule,repeatedSchedule,mOriginalSymbols);
		}
		return mStaticSchedule;
	}
	
	private StaticSubSchedule createStaticSequence(List<StaticSubSchedule> phases) {
		if (phases.isEmpty())
			return null;
		else if (phases.size()==1)
			return phases.get(0);  // This is a trivial sequence
		else
			return new StaticSequence(phases, 1);  // One repeatition of the sequence
	}
	private void partitionLoops() {
		List<Loop> allLoops=mLoopTree.getAllLoops();
		
		// allLoops are sorted in rPostOrder: outer loops before their inner loops
		// by reverseing the order, inner loops are processed prior to their surrounding loops
		int i=allLoops.size();
		while (i>0) {
			Loop loop=allLoops.get(--i);
			
			if (loop.hasConstantTripCount()
				&& loop.getExits().size()==1) {
				PhasePartition phasePart=new PhasePartition(loop);
			
				if (phasePart.createPartition()) {
					mLoopPartitions.put(loop,phasePart);
				}
			}
		}
	}
	
	
	
	/**
	 * Represents a partition of "phases" (subgraphs of the control-flow graph). These subgraphs
	 * have the form of decision trees with actions at the leaves and two actions are in the same
	 * block of the partition if they might be fired in the same phase/step of a schedule.
	 * 
	 * Two phases are merged only if they have the same token rates, which means that a static
	 * schedule can be generated given successful partitioning of the phases.
	 * 
	 * Separate partitions are determined for loops with constant trip-count. Such loops are
	 * represented as distinct phases in their surrounding region of the control-flow graph.
	 */
	private class PhasePartition {
		private Region mInitialRegion;
		private Set<BasicBlock> mLoopBody;
		private Map<Region,Set<Region>> mSuccessors;
		private MergeFindPartition<Region> mPartition;
		private Loop mLoop;
		private BasicBlock mExit;
		private Region mSubstituteForExit;
		
		/**
		 * @param initialBlock  the initial node of the control-flow-graph
		 * 
		 * Creates a partition of the phases in the control-flow graph
		 */
		PhasePartition(BasicBlock initialBlock) {
			mLoop=null;
			mInitialRegion=asSuccessor(initialBlock);
		}
		
		/**
		 * @param loop  a loop with static trip-count
		 * 
		 * Creates a partition of the phases in a loop 
		 */
		PhasePartition(Loop loop) {
			mLoop=loop;
			mInitialRegion=loop.getHeader();
			findExitAndSubstitution();
			findLoopBody();
		}
		
		boolean createPartition() {
			return createInitialPartition() && checkPhases();
		}
		
		int createStaticSchedule(List<StaticSubSchedule> result) {
			List<StaticSubSchedule> phases;
			
			// Schedule of a loop is put in a locally allocated list
			// Schedule of the entire control-flow graph is put directly into result
			if (mLoop!=null)
				phases=new ArrayList<StaticSubSchedule>();
			else
				phases=result;
			
			// mapping from "phase" to index in list
			Map<MergeFindPartition<Region>.Block, Integer> phaseMap=
				new HashMap<MergeFindPartition<Region>.Block, Integer>();
			// next phase to add to the schedule
			MergeFindPartition<Region>.Block nextPhase=mPartition.find(mInitialRegion);
			// first phase of the loop body
			int startOfBody=-1;
			
			while (nextPhase!=null && phaseMap.containsKey(nextPhase)==false) {
				// Create the StaticPhase
				// TODO: we should also support merging of decision trees (remove assertion)
				assert(nextPhase.size()==1);
				Region region=nextPhase.iterator().next();
				int index=phases.size();
				
				phaseMap.put(nextPhase, index);
				
				// Keep track of the start of the loop body
				// Note that a phase either has all its leaves or none of them in the loop body
				if (mLoop!=null && startOfBody<0 && mLoopBody.contains(getALeaf(nextPhase)))
					startOfBody=index;
				
				if (region.isLoop()) {
					PhasePartition phasePart=mLoopPartitions.get(region.asLoop());
					
					phasePart.createStaticSchedule(phases);
				}
				else {
					BasicBlock b=region.asBasicBlock();
					DecisionTree root=b.getDecisionTree();
					
					if (b!=mControlFlowGraph.getTerminalNode())
						phases.add(new StaticPhase(root.getMode(), b));
				}
				
				nextPhase=getASuccessor(nextPhase);  // the phase has a static successor
			}
			
			if (mLoop!=null) {
				// Resulting loop schedule is a repeated schedule + an optional final schedule
				long tripCount=mLoop.getTripCount();
				
				if (startOfBody<=0)
					tripCount=tripCount+1;  // entire schedule before the test

				if (tripCount>0) {
					StaticSubSchedule repeatedSchedule=new StaticSequence(phases, (int) tripCount);
					result.add(repeatedSchedule);
				}
				
				if (startOfBody>0) {
					StaticSubSchedule finalSchedule=(startOfBody==1)?
						phases.get(0) : new StaticSequence(phases.subList(0, startOfBody),1);
					result.add(finalSchedule);
				}
				return result.size();
			}
			else {
				// Return the index of the next phase, which is either the header of an infinite loop
				// or the end of a finite schedule
				return (nextPhase==null)? result.size() : phaseMap.get(nextPhase);
			}
		}
		
		/**
		 * Partitions the flow graph/loop into regions, such that there are actions or loops 
		 * at the "end" of each block of the partition.
		 * Also determines the successors of each block. 
		 * @return true iff each of the blocks has a fixed token rate.
		 */
		private boolean createInitialPartition() {
			Queue<Region> workList=new ArrayDeque<Region>();
			boolean staticallySchedulable=true;
			
			mSuccessors=new HashMap<Region,Set<Region>>();
			mPartition=new MergeFindPartition<Region>();
			
			/*
			 * Create initial partiton
			 */
			mPartition.add(mInitialRegion);
			workList.add(mInitialRegion);
			while (workList.isEmpty()==false) {
				Region phase=workList.remove();
				
				if (checkMode(phase)==false)
					staticallySchedulable=false;
				
				if (phase.isLoop()) {
					BasicBlock exitTo=phase.asLoop().getExits().get(0).getSecond();
					Region succ=asSuccessor(exitTo);
					if (succ!=null) {
						addSuccessor(phase,succ);
						if (mPartition.add(succ)) {
							workList.add(succ);
						}
					}
				}
				else {
					DecisionTree root=phase.asBasicBlock().getDecisionTree();
					for (ActionNode action: root.reachableActionNodes()) {
						
						if (mLoop==null || mLoop.contains(mControlFlowGraph.getNode(action))) {
							// action is in the same loop 
							Region succ=asSuccessor(mControlFlowGraph.getSuccessor(action));
							if (succ!=null) {
								// the successor of the action is also in the same loop iteration
								addSuccessor(phase, succ);
								if (mPartition.add(succ)) {
									workList.add(succ);
								}
							}
						}
					}
				}
			}
			return staticallySchedulable;
		}
		
				
		/**
		 * Sets mExit and mSubstututeForExit, so that the exit node (which has successors outside the loop)
		 * is not used as a phase representer
		 */
		private void findExitAndSubstitution() {
			Pair<BasicBlock,BasicBlock> exit=mLoop.getExits().get(0);
			BasicBlock exitTo=exit.getSecond();
			
			mExit=exit.getFirst();
			for (BasicBlock succ: mExit.getSuccessors()) {
				if (succ!=exitTo)
					mSubstituteForExit=succ;
			}
		}
		
		/**
		 * @param phase  the basic block, which dominates a phase 
		 * @return       a BasicBlock or a Loop, which represents the phase
		 * 
		 * This makes statically schedulable inner loops appear as single nodes
		 * in the context of an outer loop/region
		 */
		private Region asSuccessor(BasicBlock phase) {
			Loop loop=mLoopTree.getLoop(phase);
			
			if (loop!=null
				&& loop.getHeader()==phase
				&& (mLoop==null || mLoop!=loop && mLoop.contains(loop))
				&& mLoopPartitions.containsKey(loop)) {
				// phase is the entry of an inner loop, which is statically schedulable
				return loop;
			}
			else if (mLoop==null || mLoop.contains(phase)){
				// 'phase' represents itself (using a BasicBlock)
				if (phase==mExit)
					return mSubstituteForExit;
				else
					return phase;
			}
			else {
				// null represents exits from the loop
				return null; 
			}
		}
		
		private boolean addSuccessor(Region fromRegion, Region toRegion) {
			Set<Region> successors=mSuccessors.get(fromRegion);
			if (successors==null) {
				successors=new LinkedHashSet<Region>();
				mSuccessors.put(fromRegion, successors);
			}
			return successors.add(toRegion);
		}
		
		/**
		 * @param r  a region
		 * @return   true iff the action nodes of the region share a common mode/port signature
		 */
		private boolean checkMode(Region r) {
			if (r.isBasicBlock()) {
				DecisionTree root=r.asBasicBlock().getDecisionTree();
			
				if (root.getMode()==null) {
					if (r!=mControlFlowGraph.getTerminalNode()) {
						// the phase has no common PortSignature =no static schedule
						if (root.accept(mTimingDependenceCheck, null))
							mIsTimingDependent=true;
						return false;
					}
				}
			}
			
			return true;
		}
		/**
		 * Checks if we have a static schedule
		 */
		private boolean checkPhases() {
			for (MergeFindPartition<Region>.Block phase: mPartition.getBlocks()) {
				if (hasStaticSuccessor(phase)==false)
					return false;
				
				if (mLoop!=null && checkLoopPhase(phase)==false)
					return false;
			}
			
			return true;
		}
		
		/**
		 * @param phase  a phase of a loop's schedule
		 * @return true if all actions/loops in the phase have the same iteration count
		 */
		private boolean checkLoopPhase(MergeFindPartition<Region>.Block phase) {
			// Check that the actions/loops of the phase either appear prior to the iteration test/exit
			// or after (in the loop body).
			// This is necessary since the header is executed one time more than the body.
			boolean inHeader=false;
			boolean inBody=false;
					
			for (Region r: phase) {
				if (r.isLoop()) {
					if (mLoopBody.contains(r.getHeader()))
						inBody=true;
					else
						inHeader=true;
				}
				else {
					DecisionTree root=r.asBasicBlock().getDecisionTree();
					
					for (ActionNode action: root.reachableActionNodes()) {
						BasicBlock b=mControlFlowGraph.getNode(action);
						
						if (mLoop.contains(b)) {
							if (mLoopBody.contains(b))
								inBody=true;
							else
								inHeader=true;
						}
					}
				}
			}
			
			// All loops/actions either in header or in body (but not both)
			return !(inHeader && inBody);
		}

		/**
		 * Create the set of basic blocks, which are in the loop body
		 */
		private void findLoopBody() {
			BasicBlock header=mLoop.getHeader();
			BasicBlock exit=mLoop.getExits().get(0).getFirst();
			
			mLoopBody=new HashSet<BasicBlock>();
			//  add the exit so that the traversal doesn't pass exit (exit dominates body)
			mLoopBody.add(exit);
			for (BasicBlock backEdge: mLoop.getBackEdges()) {
				visitLoopBody(backEdge);
			}
		}
		
		private void visitLoopBody(BasicBlock from) {
			if (mLoopBody.add(from)) {
				for (BasicBlock pred: from.getPredecessors()) {
					visitLoopBody(pred);
				}
			}
		}
				
		/**
		 * @param phase  a block in the partition
		 * @return       true if there is a unique successor (or no successor =end of schedule)
		 *               false if there are multiple, distinct successors  
		 */
		private boolean hasStaticSuccessor(MergeFindPartition<Region>.Block phase) {
			MergeFindPartition<Region>.Block staticSuccessor=getASuccessor(phase);
			
			// Iterate over all actions that are reachable from the roots of the phase
			// Check if there is a common successor phase
			for (Region r: phase) {
				Set<Region> successors=mSuccessors.get(r);
				if (successors==null) {
					// empty successor
					if (staticSuccessor!=null)
						return false;
				}
				else {
					for (Region succ: successors) {
						MergeFindPartition<Region>.Block nextPhase=mPartition.find(succ);
						if (staticSuccessor!=nextPhase)
							return false; // There are several successors
					}
				}
			}
			return true;
		}
			
		
		/**
		 * @param phase  a block in the partition
		 * @return       a successor of 'phase' (the static successor if there is only one)
		 */
		private MergeFindPartition<Region>.Block getASuccessor(MergeFindPartition<Region>.Block phase) {
			Region r=phase.iterator().next();
			Set<Region> successors=mSuccessors.get(r);
			if (successors==null || successors.isEmpty())
				return null;
			else
				return mPartition.find(successors.iterator().next());
		}
		
		/**
		 * @param phase  a block in the partition
		 * @return       a BasicBlock that is a leaf of the phase, either the basic block of an ActionNode
		 *               or the header of an inner loop
		 */
		private BasicBlock getALeaf(MergeFindPartition<Region>.Block phase) {
			Region r=phase.iterator().next();
			if (r.isLoop())
				return r.getHeader();
			else {
				DecisionTree root=r.asBasicBlock().getDecisionTree();
				
				for (ActionNode action: root.reachableActionNodes()) {
					BasicBlock b=mControlFlowGraph.getNode(action);
					
					if (mLoop==null || mLoop.contains(b)) {
						return b;
					}
				}
				
				return null; // no leaf found
			}
		}
	}
	
	
	/**
	 * Checks if the decision tree tests for absence of tokens (=the schedule may be timing dependent)
	 */

	private class TimingDependenceCheck implements DecisionTree.Visitor<Boolean,Object> {
		
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
}
