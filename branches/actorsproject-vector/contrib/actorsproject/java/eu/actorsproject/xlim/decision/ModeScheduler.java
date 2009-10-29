package eu.actorsproject.xlim.decision;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.actorsproject.util.XmlPrinter;
import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.XlimLoopModule;
import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.absint.AbstractValue;
import eu.actorsproject.xlim.absint.Evaluator;
import eu.actorsproject.xlim.absint.Interval;
import eu.actorsproject.xlim.absint.IntervalConstraintEvaluator;
import eu.actorsproject.xlim.absint.IntervalDomain;
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
 * The ModeScheduler class supports transformation from "action scheduler"
 * to "mode scheduler".
 * In terms of the decision tree, a "mode" is a subtree whose leaves are
 * actions with the same port signature (plus possible "null" nodes). 
 * 
 * The transformation from the original decision tree consists in:
 * (1) Finding the "modes" (subtrees). The original decision tree is in effect
 *     cut into a "mode-selection tree" with "modes" as its leaves and per-mode
 *     action schedulers with action nodes as leaves.
 * (2) Identifying the "initial mode", given the initial state of the actor.
 *     If initial mode selection is input dependent, an artificial mode is created. 
 * (3) Now the decision tree can be rotated: given that we know the mode we're
 *     in we can start at its action scheduler. Then, the mode-selection tree
 *     can be used to identify the next mode.
 * (4) Rather than just duplicating the mode-selection tree for every action node, 
 *     it is specialized w.r.t. the knowledge of state that is collected by 
 *     abstract interpretation. 
 */
public class ModeScheduler {

	private ActionSchedulerParser mParser=new ActionSchedulerParser();
	private DecisionTree mDecisionTree;
	private DependenceSlice mDecisionSlice;
	private HashMap<CallNode,DependenceSlice> mActionSlices=
		new HashMap<CallNode,DependenceSlice>();
	private ArrayList<ActionNode> mActionNodes=
		new ArrayList<ActionNode>();
	private HashMap<ActionNode,DependenceSlice> mActionNodeSlices=
		new HashMap<ActionNode,DependenceSlice>();
	private Set<XlimStateVar> mRelevantState=new HashSet<XlimStateVar>();
	private IntervalDomain mIntervalDomain=new IntervalDomain(new Evaluator());
	
	/**
	 * @param design the XLIM-representation of the actor
	 * 
	 * Creates the mode-selection tree, the per-mode action schedulers,
	 * and the program slices required to analyze the mode-selection tree and
	 * the state updates performed by the ActionNodes.
	 */
	public void create(XlimDesign design) {
		XlimTaskModule actionScheduler=design.getActionScheduler();
		
		// Form the decision tree of the action scheduler
		mDecisionTree=mParser.parseXlim(actionScheduler);
				
		// Find the program slice, required to determine "next" action
		XlimLoopModule schedulingLoop=mParser.findSchedulingLoop(actionScheduler);
		XlimModule schedulingLoopBody=schedulingLoop.getBodyModule(); 
		sliceDecisionTree(schedulingLoopBody);
		
		// Create the collection of "relevant state", which is tracked
		// and the program slices of the actions
		updateRelevantState();
		sliceActions(design);		
		
		// Create the action-node slices 
		mDecisionTree.findActionNodes(mActionNodes);
		sliceActionNode(schedulingLoopBody);
		
		// Enumerate state space
		StateEnumeration<Interval> stateEnum=enumerateStateSpace(design);
		printResult(stateEnum);
	}
	
	/**
	 * @param schedulingLoopBody body of the loop in the action scheduler
	 * 
	 * Creates the program slice, which is needed to analyze the mode-selection tree:
	 * the conditions of the decision nodes are the values of interest (output values)
	 */
	private void sliceDecisionTree(XlimModule schedulingLoopBody) {
		mDecisionSlice=new ModuleDependenceSlice("mode-selection", 
				                                     mActionSlices,
				                                     schedulingLoopBody);
		mDecisionTree.createDependenceSlice(mDecisionSlice);
	}
	
	/**
	 * @return true if new state variables were added to mRelevantState
	 */
	private boolean updateRelevantState() {
		boolean changed=false;
		for (ValueNode input: mDecisionSlice.getInputValues()) {
			Location location=input.actsOnLocation();
			// We are in trouble if there is other inputs than state vars/ports
			assert(location!=null && location.isStateLocation());
			if (location.hasSource()) {
				XlimStateVar stateVar=location.getSource().asStateVar();
				assert(stateVar!=null);
				if (mRelevantState.add(stateVar))
					changed=true;
			}
		}
		return changed;
	}
	
	/**
	 * @param design the XLIM-representation of the actor
	 * 
	 * Creates the program slices of the actions: the updated state variables are
	 * the values of interest (output values) of the slices.
	 * 
	 * Depends on mRelevantState, the piece of state needed by the mode-selector
	 * Resulting slices are put in the mActionSlices map, which is shared by all
	 * program slices. 
	 */
	private void sliceActions(XlimDesign design) {
		XlimTaskModule actionScheduler=design.getActionScheduler();
		CallGraph callGraph=design.getCallGraph();
		
		for (CallNode callNode: callGraph.postOrder()) {
			XlimTaskModule action=callNode.getTask();
			
			if (action!=actionScheduler) {
				DependenceSlice actionSlice=new DependenceSlice(action.getName(), 
						                                        mActionSlices);
				// Add relevant outputs to slice
				DataDependenceGraph ddg=callNode.getDataDependenceGraph();
				Iterable<ValueNode> outputs=ddg.getModifiedOutputValues();
				addRelevantOutputs(actionSlice, outputs);
				
				mActionSlices.put(callNode, actionSlice);
			}
		}
		
		// TODO: form closure of relevant state (w.r.t. actions)?
		// Additional state may be needed to compute the updated state...
		// Not that simple though:
		// * We need to propagate new relevant state from ActionNodes, via the
		//   calls in the action nodes, to Actions, back to ActionNodes
		// * Further propagation in the "per mode" decision trees which we
		//   currently haven't dealt with (do we need to?), up to the "mode selector"
		// In a sneaky version we can perhaps assume that ActionNodes are just
		// action call + fsm state update?
	}
	
	/**
	 * @param schedulingLoopBody body of the loop in the action scheduler
	 * 
	 * Creates program slices of the action nodes (typically action call + fsm state update):
	 * the updated state variables (including a possible fsm state variable) are the values
	 * of interest (output values) of the slices.
	 * 
	 * Depends on mActionNodes, the collection of all action nodes,
	 *            mRelevantState, the projection of state that we track
	 * Resulting slices are owned by the ActionNodes   
	 */
	private void sliceActionNode(XlimModule schedulingLoopBody) {
		for (ActionNode actionNode: mActionNodes) {
			String sliceName="ActionNode "+ actionNode.getIdentifier();
			DependenceSlice slice=new ModuleDependenceSlice(sliceName, 
					                                        mActionSlices,
					                                        schedulingLoopBody);
			mActionNodeSlices.put(actionNode,slice);
			
			Iterable<ValueNode> outputs=actionNode.getOutputMapping().values();
			addRelevantOutputs(slice, outputs);
		}
	}
	
	/**
	 * Adds outputs to slice corresponding to state vars in mRelevantState
	 *  
	 * @param slice    a dependence slice
	 * @param outputs  output values of state carriers (in slice)
	 */
	private void addRelevantOutputs(DependenceSlice slice, 
			                        Iterable<ValueNode> outputs) {
		for (ValueNode outputValue: outputs) {
			Location location=outputValue.actsOnLocation();
			
			if (location!=null && mRelevantState.contains(location))
				slice.add(outputValue);
		}
	}
	
	
	/**
	 * Creates Evaluatable objects for all actions, which is required
	 * to do the same for ActionNodes.
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
	
	
	private StateEnumeration<Interval> enumerateStateSpace(XlimDesign design) {
		
		// Create Evaluatable objects for actions, ActionNodes and the decision tree
		SliceSorter sliceSorter=new SliceSorter();
		sortActionSlices(design, sliceSorter);
		Map<ActionNode,DependenceComponent> actionNodeComponents=
			sortActionNodeSlices(sliceSorter);
		IntervalConstraintEvaluator constraintEvaluator=
			new IntervalConstraintEvaluator();
		
		// do the StateEnumeration
		StateEnumeration<Interval> stateEnumeration=
			new StateEnumeration<Interval>(mDecisionTree,
					                       actionNodeComponents,
					                       mDecisionSlice.getInputValues(),
					                       mIntervalDomain,
					                       constraintEvaluator);
		stateEnumeration.enumerateStateSpace();
		return stateEnumeration;
	}
	
//	private <T extends AbstractValue<T>> void printResult(StateEnumeration<T> stateEnumeration) {
//		String delimiter="";
//		System.out.print("Relevant slice of state: ");
//		
//		for (XlimStateVar stateVar: mRelevantState) {
//			String name=stateVar.getSourceName();
//			if (name==null)
//				name=stateVar.getUniqueId();
//			System.out.print(delimiter+name);
//			delimiter=",";
//		}
//		System.out.println();
//
//		switch (mDecisionTree.printModes(stateEnumeration)) {
//		case STATIC:
//			System.out.println("Static progression of modes (subset of CSDF)");
//			break;
//		case STATE_DEPENDENT:
//			System.out.println("State-dependent selection of next mode (CSDF or DDF)");
//			break;
//		case NON_DETERMINISTIC:
//			System.out.println("Input-dependent selection of next mode (may be non-deterministic)");
//			break;
//		}
//	}
	
	private <T extends AbstractValue<T>> void printResult(StateEnumeration<T> stateEnumeration) {
		ActionSchedule actionSchedule=stateEnumeration.createActionSchedule();
		if (actionSchedule.isDeterministic())
			if (actionSchedule.hasStaticSchedule())
				System.out.println("Static progression of modes (subset of CSDF)");
			else
				System.out.println("State-dependent selection of next mode (CSDF or DDF)");
		else
			System.out.println("Input-dependent selection of next mode (may be non-deterministic)");
		
		if (actionSchedule.mayTerminate())
			System.out.println("Schedule may terminate");
		else
			System.out.println("Schedule repeats indefinitely");
		
		if (actionSchedule.isDeterministic() && actionSchedule.hasStaticSchedule()) {
			System.out.println("\n<!-- static schedule -->");
			XmlPrinter printer=new XmlPrinter(System.out);
			actionSchedule.printStaticSchedule(printer);
		}
	}

}
