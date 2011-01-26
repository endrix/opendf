package eu.actorsproject.xlim.schedule;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.actorsproject.util.XmlPrinter;
import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.XlimLoopModule;
import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.absint.Evaluator;
import eu.actorsproject.xlim.absint.Interval;
import eu.actorsproject.xlim.absint.IntervalConstraintEvaluator;
import eu.actorsproject.xlim.absint.IntervalDomain;
import eu.actorsproject.xlim.decision2.ActionNode;
import eu.actorsproject.xlim.decision2.ActionSchedulerParser;
import eu.actorsproject.xlim.decision2.Condition;
import eu.actorsproject.xlim.decision2.DecisionNode;
import eu.actorsproject.xlim.decision2.DecisionTree;
import eu.actorsproject.xlim.decision2.NullNode;
import eu.actorsproject.xlim.decision2.PortSignature;
import eu.actorsproject.xlim.dependence.CallGraph;
import eu.actorsproject.xlim.dependence.CallNode;
import eu.actorsproject.xlim.dependence.DataDependenceGraph;
import eu.actorsproject.xlim.dependence.DependenceComponent;
import eu.actorsproject.xlim.dependence.DependenceSlice;
import eu.actorsproject.xlim.dependence.Location;
import eu.actorsproject.xlim.dependence.ModuleDependenceSlice;
import eu.actorsproject.xlim.dependence.SliceSorter;
import eu.actorsproject.xlim.dependence.ValueNode;

/**
 * The Classifier analyzes the behavor of one actor. It produces the following results:
 * (1) The decision tree, which corresponds to the "action scheduler" of an (XLIM) actor.
 * (2) The abstract state, which reaches each action of the actor (stateEnumeration)
 * (3) The control-flow graph, in which actions and decisions form the vertices.
 * (4) The action schedule, which represents all possible sequences of action firings.
 */
public class Classifier {

	private XlimDesign mDesign;
	// TODO: we probably want to generalize/parameterize Classifyer,
	// in which case we need the AbstractDomain as a constructor parameter
	private IntervalDomain mIntervalDomain=new IntervalDomain(new Evaluator());
	
	private DecisionTree mDecisionTree;
	private DependenceSlice mDecisionSlice;
	private HashMap<CallNode,DependenceSlice> mActionSlices=
		new HashMap<CallNode,DependenceSlice>();
	private HashMap<ActionNode,DependenceSlice> mActionNodeSlices=
		new HashMap<ActionNode,DependenceSlice>();
	private Map<ActionNode,DependenceComponent> mActionNodeComponents;

	private WideningGenerator mWideningGenerator;
	
	private StateEnumeration<Interval> mStateEnumeration;
	private ControlFlowGraph mControlFlowGraph;
	private LoopAnalysis mLoopAnalysis;
	private ActionSchedule mActionSchedule;
	
	/**
	 * @param design XlimDesign element of the actor to be classified
	 */
	public Classifier(XlimDesign design) {
		mDesign=design;
	}
	
	/**
	 * @return the XlimDesign element of the actor
	 */
	public XlimDesign getDesign() {
		return mDesign;
	}
	
	/**
	 * @return the DecisionTree that represents the action scheduler of the actor 
	 */
	public DecisionTree getDecisionTree() {
		if (mDecisionTree==null) {
			// Creates mDecisionTree, mDecisionSlice, mActionSlices and mActionNodeSlices
			createDecisionTreeAndSlice();
		}
		return mDecisionTree;
	}
	
	/**
	 * @return the result of state enumeration (an abstraction of the state, which reaches
	 *         each action of the actor)
	 */
	public StateEnumeration<Interval> getStateEnumeration() {
		if (mStateEnumeration==null) {
			DecisionTree decisionTree=getDecisionTree();
			
			// Enumerate state space
			mStateEnumeration=enumerateStateSpace(decisionTree);
		}
		return mStateEnumeration;
	}
	
	/**
	 * @return the ControlFlowGraph that models the behavior of the actor
	 */
	public ControlFlowGraph getControlFlowGraph() {
		if (mControlFlowGraph==null) {
			StateEnumeration<Interval> stateEnum=getStateEnumeration();
			
			// Construct the control-flow-graph
			// mIntervalDomain.setTrace(true);
			mControlFlowGraph=stateEnum.buildFlowGraph();
		}
		return mControlFlowGraph;
	}
	
	/**
	 * @return the result of loop analysis
	 */
	public LoopAnalysis getLoopAnalysis() {
		if (mLoopAnalysis==null) {
			// Make sure the decision tree (+slices) and the control-flow graph are created
			getDecisionTree();
			getControlFlowGraph();
			
			// XmlPrinter printer=new XmlPrinter(System.out);
			// printer.printElement(mControlFlowGraph);
			
			mLoopAnalysis = new LoopAnalysis(mControlFlowGraph, mActionNodeComponents, mDecisionSlice.getInputValues());
			mLoopAnalysis.analyze();
		}
		return mLoopAnalysis;
	}
	
	/**
	 * @return the ActionSchedule, which is computed from the actor's ControlFlowGraph
	 *         (the action schedule represents all sequences of action firings)
	 */
	public ActionSchedule getActionSchedule() {
		if (mActionSchedule==null) {
			ControlFlowGraph cfg=getControlFlowGraph();
			
			mActionSchedule=ActionScheduleOld.create(cfg, mDesign.getSymbolTable());
		}
		return mActionSchedule;
	}
	
	
	public ActionSchedule createActionScheduleWithLoopAnalysis() {
		if (mActionSchedule==null) {
			ControlFlowGraph cfg=getControlFlowGraph();
			LoopTree loops=getLoopAnalysis().getLoopTree();
			
			mActionSchedule=ActionScheduleWithLoops.create(cfg, loops, mDesign.getSymbolTable());
		}
		return mActionSchedule;
	}
	
	/**
	 * Creates the DecisionTree of the actor
	 * 
	 * In addition (to creating mDecisionTree), this method is responsible for the creation of
	 * mDecisionSlice, mActionSlices and mActionNodeSlices
	 */
	private void createDecisionTreeAndSlice() {
		assert(mDecisionTree==null);
		
		XlimTaskModule actionScheduler=mDesign.getActionScheduler();
		
		// Form the decision tree of the action scheduler
		ActionSchedulerParser parser=new ActionSchedulerParser();
		mDecisionTree=parser.parseXlim(actionScheduler);
		
		// Find all conditions of the decision tree
		BagOfConditions conditions=new BagOfConditions(mDecisionTree);
		
		// Find the program slice, required to determine "next" action
		XlimLoopModule schedulingLoop=parser.findSchedulingLoop(actionScheduler);
		XlimModule schedulingLoopBody=schedulingLoop.getBodyModule(); 
		mDecisionSlice=new ModuleDependenceSlice("mode-selection", 
                mActionSlices,
                schedulingLoopBody);
		for (Condition cond: conditions) {
			mDecisionSlice.add(cond.getValue());
		}

		// Compute the set of input tokens that are required to test the conditions
		// The pinPeek operators, which do look-ahead on inputs, are the source of such requirement
		Map<ValueNode,PortSignature> inputDependence=new InputDependence().inputDependence(mDecisionSlice);
		
		// Reorder conjunctions so that potentially timing-dependent tests are performed at the end
		mDecisionTree.accept(new ConditionReordering(), inputDependence);
		
		// Create the maps of program slices for actions and ActionNodes
		mActionSlices=new HashMap<CallNode,DependenceSlice>();
		mActionNodeSlices=new HashMap<ActionNode,DependenceSlice>();
		
		// Create the collection of "relevant state", which is tracked
		Set<XlimStateVar> relevantState=new HashSet<XlimStateVar>();
		updateRelevantState(mDecisionSlice.getInputValues(), relevantState);
		sliceActions(relevantState);		
		
		// Create the action-node slices 
		sliceActionNodes(schedulingLoopBody,relevantState);
		
		// new XmlPrinter(System.out).printDocument(mDecisionTree);
		mWideningGenerator = new WideningGenerator(mDecisionSlice.getInputValues(), mIntervalDomain);
		mWideningGenerator.processDecisionSlice(mDecisionSlice);
	}
	
	/**
	 * Reorders conditions in a first attempt to avoid unnecessarily checking for absence of input
	 * (this relates to identification of Kahn-style, dynamic actors).
	 * It does so by changing the order of terms in conjunctions. 
	 */
	private class ConditionReordering implements DecisionTree.Visitor<Object, Map<ValueNode,PortSignature>> {
		
		public Object visitActionNode(ActionNode action, Map<ValueNode,PortSignature> inputDependence) {
			return null; // Do nothing: leaf node
		}

		public Object visitNullNode(NullNode node, Map<ValueNode,PortSignature> inputDependence) {
			return null; // Do nothing: leaf node
		}
		
		public Object visitDecisionNode(DecisionNode decision, Map<ValueNode,PortSignature> inputDependence) {
			decision.getCondition().reorderTerms(decision.requiredPortSignature(), inputDependence);
			decision.getChild(true).accept(this, inputDependence);
			decision.getChild(false).accept(this, inputDependence);
			return null;
		}				
	}


	/**
	 * Creates the program slices of the actions: the updated state variables are
	 * the values of interest (output values) of the slices.
	 * 
	 * @param relevantState  the piece of state needed by the mode-selector
	 * 
	 * Resulting slices are put in the mActionSlices map, which is shared by all
	 * program slices. 
	 */
	private void sliceActions(Set<XlimStateVar> relevantState) {
		XlimTaskModule actionScheduler=mDesign.getActionScheduler();
		CallGraph callGraph=mDesign.getCallGraph();
		List<CallNode> callNodes=callGraph.postOrder();
		
		// Create dependence slices for all CallNodes, except the action scheduler (i.e. the actions)
		for (CallNode callNode: callNodes) {
			XlimTaskModule action=callNode.getTask();
		
			if (action!=actionScheduler) {
				DependenceSlice actionSlice=new DependenceSlice(action.getName(), 
					                                            mActionSlices);
				mActionSlices.put(callNode, actionSlice);
			}
		}
		
		// Form the closure of relevant state w.r.t. actions:
		// * Determine the required inputs, given the required outputs (relevant state)
		// * Update relevant state if additional state variables are required
		// * Iterate over all actions until a "stable" round has been made (one in which
		//   no state variables were added).
		boolean stable=false;
		CallNode lastChange=null;
		int i=0;
		
		while (!stable) {
			if (i<callNodes.size()) {
				CallNode callNode=callNodes.get(i++);
				DependenceSlice actionSlice=mActionSlices.get(callNode);
			
				if (actionSlice!=null) {
					// Add relevant outputs to slice
					DataDependenceGraph ddg=callNode.getDataDependenceGraph();
					Iterable<ValueNode> outputs=ddg.getModifiedOutputValues();
					if (addRelevantOutputs(actionSlice, outputs, relevantState)) {
						// relevantState was updated, at least one more round
						lastChange=callNode;
					}
					else if (lastChange==callNode) {
						// A complete, stable round has been made
						stable=true;
					}
				}
				// else: callNode is the action scheduler, which has no action slice
			}
			else if (lastChange!=null) { 
				// restart 
				i=0;
			}
			else {
				// No updates in first round
				stable=true;
			}
		}
	}
	
	/**
	 * @param schedulingLoopBody body of the loop in the action scheduler
	 * @param relevantState  the piece of state needed by the mode-selector
	 * 
	 * Creates program slices of the action nodes (typically action call + fsm state update):
	 * the updated state variables (including a possible fsm state variable) are the values
	 * of interest (output values) of the slices.
	 * 
	 * Resulting slices are put in the mActionNodeSlices map.
	 */
	private void sliceActionNodes(XlimModule schedulingLoopBody, Set<XlimStateVar> relevantState) {
		for (ActionNode actionNode: mDecisionTree.reachableActionNodes()) {
			String sliceName="ActionNode "+ actionNode.getIdentifier();
			DependenceSlice slice=new ModuleDependenceSlice(sliceName, 
					                                        mActionSlices,
					                                        schedulingLoopBody);
			mActionNodeSlices.put(actionNode,slice);
			
			Iterable<ValueNode> outputs=actionNode.getOutputMapping().values();
			addRelevantOutputs(slice, outputs, relevantState);
			
			// Make sure the inputs of each action node is also an input of the decision slice
			for (ValueNode input: slice.getInputValues())
				mDecisionSlice.add(input);
		}
	}

	/**
	 * @param inputValues    the input values (state vars and ports) needed by a slice
	 * @param relevantState  the piece of state needed by the mode-selector
	 * 
	 * @return true if new state variables were added to relevantState
	 */
	private boolean updateRelevantState(Iterable<ValueNode> inputValues, Set<XlimStateVar> relevantState) {
		boolean changed=false;
		for (ValueNode input: inputValues) {
			Location location=input.getLocation();
			// We are in trouble if there is other inputs than state vars/ports
			assert(location!=null && location.isStateLocation());
			if (location.hasSource()) {
				XlimStateVar stateVar=location.getSource().asStateVar();
				assert(stateVar!=null);
				if (relevantState.add(stateVar))
					changed=true;
			}
		}
		return changed;
	}
		
	/**
	 * Adds outputs to slice corresponding to state vars in mRelevantState
	 *  
	 * @param slice    a dependence slice
	 * @param outputs  output values of state carriers (in slice)
	 * @param relevantState  the piece of state needed by the mode-selector
	 * @return true if the set of relevant state was updated
	 */
	private boolean addRelevantOutputs(DependenceSlice slice, 
			                        Iterable<ValueNode> outputs,
			                        Set<XlimStateVar> relevantState) {
		boolean inputAdded=false;
		
		for (ValueNode outputValue: outputs) {
			Location location=outputValue.getLocation();
			
			if (location!=null && relevantState.contains(location))
				if (slice.add(outputValue))
					inputAdded=true;
		}
		
		return (inputAdded && updateRelevantState(slice.getInputValues(), relevantState));
	}
		
	private StateEnumeration<Interval> enumerateStateSpace(DecisionTree decisionTree) {
		// Create Evaluatable objects for actions, ActionNodes and the decision tree
		SliceSorter sliceSorter=new SliceSorter();
		sortActionSlices(mDesign, sliceSorter);
		IntervalConstraintEvaluator constraintEvaluator=
			new IntervalConstraintEvaluator();
		
		mActionNodeComponents=sortActionNodeSlices(sliceSorter);
		
		// do the StateEnumeration
		StateEnumeration<Interval> stateEnumeration=
			new StateEnumeration<Interval>(decisionTree,
					                       mActionNodeComponents,
					                       mDecisionSlice.getInputValues(),
					                       mIntervalDomain,
					                       constraintEvaluator,
					                       mWideningGenerator.getWideningOperators());
		stateEnumeration.enumerateStateSpace();
		return stateEnumeration;
	}
	
	/**
	 * Creates Evaluatable objects for all actions, which is required
	 * to do the same (create Evaluatables) for ActionNodes.
	 * 
	 * @param design       an actor class
	 * @param sliceSorter  the delegate that actually creates the Evaluatables
	 */
	private void sortActionSlices(XlimDesign design, SliceSorter sliceSorter) {
		XlimTaskModule actionScheduler=design.getActionScheduler();
		CallGraph callGraph=design.getCallGraph();
		
		for (CallNode callNode: callGraph.postOrder()) {
			XlimTaskModule action=callNode.getTask();
			
			if (action!=actionScheduler) {
				DependenceSlice slice=mActionSlices.get(callNode);
				sliceSorter.topSort(callNode, slice);
			}
		}
	}
	
	private Map<ActionNode,DependenceComponent> sortActionNodeSlices(SliceSorter sliceSorter) {
		Map<ActionNode,DependenceComponent> result=
			new HashMap<ActionNode,DependenceComponent>();
		
		for (Map.Entry<ActionNode,DependenceSlice> entry: mActionNodeSlices.entrySet()) {
			ActionNode actionNode=entry.getKey();
			DependenceSlice slice=entry.getValue();
			DependenceComponent component=sliceSorter.topSort(slice);
			
			result.put(actionNode, component);
		}
		
		return result;
	}
}
