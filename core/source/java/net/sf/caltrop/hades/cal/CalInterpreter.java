/* 
BEGINCOPYRIGHT X,UC
	
	Copyright (c) 2007, Xilinx Inc.
	Copyright (c) 2003, The Regents of the University of California
	All rights reserved.
	
	Redistribution and use in source and binary forms, 
	with or without modification, are permitted provided 
	that the following conditions are met:
	- Redistributions of source code must retain the above 
	  copyright notice, this list of conditions and the 
	  following disclaimer.
	- Redistributions in binary form must reproduce the 
	  above copyright notice, this list of conditions and 
	  the following disclaimer in the documentation and/or 
	  other materials provided with the distribution.
	- Neither the names of the copyright holders nor the names 
	  of contributors may be used to endorse or promote 
	  products derived from this software without specific 
	  prior written permission.
	
	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
	CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
	INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
	MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
	DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
	CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
	SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
	NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
	LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
	HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
	CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
	OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
	SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
	
ENDCOPYRIGHT
*/

package net.sf.caltrop.hades.cal;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.sf.caltrop.cal.ast.Action;
import net.sf.caltrop.cal.ast.Actor;
import net.sf.caltrop.cal.ast.Decl;
import net.sf.caltrop.cal.ast.Expression;
import net.sf.caltrop.cal.ast.PortDecl;
import net.sf.caltrop.cal.ast.QID;
import net.sf.caltrop.cal.ast.Transition;
import net.sf.caltrop.cal.ast.TypeExpr;
import net.sf.caltrop.cal.interpreter.Context;
import net.sf.caltrop.cal.interpreter.ExprEvaluator;
import net.sf.caltrop.cal.interpreter.InputChannel;
import net.sf.caltrop.cal.interpreter.InputPort;
import net.sf.caltrop.cal.interpreter.OutputChannel;
import net.sf.caltrop.cal.interpreter.OutputPort;
import net.sf.caltrop.cal.interpreter.Procedure;
import net.sf.caltrop.cal.interpreter.environment.CacheEnvironment;
import net.sf.caltrop.cal.interpreter.environment.Environment;
import net.sf.caltrop.cal.interpreter.util.DefaultPlatform;
import net.sf.caltrop.cal.interpreter.util.ImportHandler;
import net.sf.caltrop.cal.interpreter.util.ImportMapper;
import net.sf.caltrop.cal.interpreter.util.ImportUtil;
import net.sf.caltrop.cal.interpreter.util.Platform;
import net.sf.caltrop.cal.interpreter.util.PriorityUtil;
import net.sf.caltrop.cal.interpreter.util.Types;
import net.sf.caltrop.hades.des.AbstractDiscreteEventComponent;
import net.sf.caltrop.hades.des.AbstractMessageListener;
import net.sf.caltrop.hades.des.BasicMessageProducer;
import net.sf.caltrop.hades.des.ControlEvent;
import net.sf.caltrop.hades.des.EventProcessor;
import net.sf.caltrop.hades.des.MessageEvent;
import net.sf.caltrop.hades.des.schedule.PostfireHandler;
import net.sf.caltrop.hades.des.schedule.Scheduler;
import net.sf.caltrop.hades.des.schedule.SimulationFinalizer;
import net.sf.caltrop.hades.des.util.Attributable;
import net.sf.caltrop.hades.des.util.LocationMap;
import net.sf.caltrop.hades.des.util.OutputBlockRecord;
import net.sf.caltrop.hades.des.util.StateChangeEvent;
import net.sf.caltrop.hades.des.util.StateChangeListener;
import net.sf.caltrop.hades.des.util.StateChangeProvider;
import net.sf.caltrop.util.logging.Logging;



/**
 * 
 * @author jwj@acm.org
 */

public class CalInterpreter extends AbstractDiscreteEventComponent
implements EventProcessor, LocationMap, StateChangeProvider { 
	
	private static final Object LAST_FIRED_AT = "lastFiredAt";//JTK

	//
	// LocationMap
	//
	
	private Map locationMap = new HashMap();
	
	public Object getContent(Object location) {
		return locationMap.get(location);
	}
	
	public Set locations() {
		return locationMap.keySet();
	}
	
	//
	//  DEC
	//
	
	public void initializeState(double t, Scheduler s) {
		
		scheduler = s;
		
		setupAssertionHandling();
		
		setupPortTypeChecking();
		
		warnBigBuffers = -1;
		String bufferSizeWarning = System.getProperty("CalBufferWarning");
		if (bufferSizeWarning != null) {
			try {
			warnBigBuffers = Integer.parseInt(bufferSizeWarning);
			}
			catch (Exception exc) {}
		}
		
		ignoreBufferBounds = false;
		String ignoreBufferBoundString = System.getProperty("CalBufferIgnoreBounds");
		if (ignoreBufferBoundString != null && ignoreBufferBoundString.trim().toLowerCase().equals("true")) {
			ignoreBufferBounds = true;
		}
		
		bufferBlockRecord = false;
		String bufferBlockRecordString = System.getProperty("CalBufferBlockRecord");
		if (bufferBlockRecordString != null && bufferBlockRecordString.trim().toLowerCase().equals("true")) {
			bufferBlockRecord = true;
		}
		
		String platformName = System.getProperty("CalPlatform");
		if (platformName == null) {
			myPlatform = defaultPlatform;
		} else {
			try {
				myPlatform = (Platform)this.getClass().getClassLoader().loadClass(platformName).newInstance();
			}
			catch (Exception exc) {
				throw new RuntimeException("Cannot load platform class: " + platformName, exc);
			}
		}

		myContext = myPlatform.context();
		
		ImportHandler [] importHandlers = myPlatform.getImportHandlers(CalInterpreter.class.getClassLoader());
		ImportMapper [] importMappers = myPlatform.getImportMappers();
				
		Environment newEnv = ImportUtil.handleImportList(myPlatform.createGlobalEnvironment(),
				importHandlers,
				actor.getImports(), importMappers);
		outsideEnv = new EnvironmentWrapper(this.parentEnv, newEnv, myContext);
		
		Decl[] decls = actor.getStateVars();
		Environment constantEnv = myContext.newEnvironmentFrame(outsideEnv);
		constantEnv.bind("this", this);
		// disallow writing to cached environment
		Environment cachedEnv = new CacheEnvironment(constantEnv, false, myContext);
		this.actorEnv = createActorStateEnvironment(cachedEnv, myContext);
		if (decls != null) {
			ExprEvaluator eval = new ExprEvaluator(myContext, this.actorEnv);
			for (int i = 0; i < decls.length; i++) {
				String var = decls[i].getName();
				Expression valExpr = decls[i].getInitialValue();
				boolean isStateVariable = decls[i].isAssignable() || decls[i].isMutable();
				
				// Note: this assumes that declarations are
				// ordered by eager dependency
				
				Object value = (valExpr == null) ? myContext.createNull() : eval.evaluate(valExpr);
				if (isStateVariable)
					this.actorEnv.bind(var, value);
				else
					constantEnv.bind(var, value);
			}
		}
		
		ai = new ActorInterpreter(actor, myContext,
				this.actorEnv, inputPortMap, outputPortMap);
		
		executeInitializer();
		
		firingCount = 0;
		delayOutput = false;
		blockedOutputChannels = new HashSet();
		hasTraceVar = this.actorEnv.localBindings().containsKey(traceVarName);
		hasNDTrackerVar = this.actorEnv.localBindings().containsKey(nondeterminismTrackerVarName);

		checkInvariants();
		checkFinalizer(constantEnv);
		scheduleActor();		
	}
	
	private void checkFinalizer(Environment env) {
		if (env.localBindings().containsKey(FINALIZE_PROCEDURE)) {
			final Object finalizeProcedure = env.localBindings().get(FINALIZE_PROCEDURE);
			if (finalizeProcedure instanceof Procedure) {
				scheduler.registerSimulationFinalizer(new SimulationFinalizer(){
					public void finalizeSimulation() {
						Object[] args = new Object[0];
						((Procedure)finalizeProcedure).call(args);
					}
				});
			}
		}
	}

	protected  Environment  createActorStateEnvironment(Environment parent, Context context) {
		return context.newEnvironmentFrame(parent);
	}
	
	private void executeInitializer() {
		
		Action[] actions = actor.getInitializers();
		for (int i = 0; i < actions.length; i++) {
			// Note: could we perhaps reuse environment?
			ai.actionSetup(actions[i]);
			if (ai.actionEvaluatePrecondition()) {
				ai.actionStep();
				ai.actionComputeOutputs();
				ai.actionClear();
				return;
			} else {
				ai.actionClear();
			}
		}
	}
	
	public boolean isInitialized() {
		return scheduler != null;
	}
	
	//
	//  EventProcessor
	//
	
	protected int  eventProcessingState;
	
	protected final int  epsActionSelection = 1;
	protected final int  epsOutput = 2;
	protected final int  epsDelayedOutput = 99;
	
	
	public boolean isWeakEvent() {
		return false;
	}
	
	public boolean processEvent(double time) {
		
		if (!blockedOutputChannels.isEmpty()) {
			flushBlockedOutputChannels();
			scheduleActor();
			return true;
		} else if (delayOutput) {
			delayOutput = false;
			flushOutputChannels();
			scheduleActor();
			nVoidFirings = 0;			
			return true;
		} else {
			Action [] a = eligibleActions[currentState];
			for (int i = 0; i < a.length; i++) {
				try {
				ai.actionSetup(a[i]);
				if (ai.actionEvaluatePrecondition()) {

					if (hasNDTrackerVar) {
						ai.actionClear();
						rollbackInputChannels();

						List activeActions = new ArrayList();
						for (int j = 0; j < a.length; j++) {
							ai.actionSetup(a[j]);
							if (ai.actionEvaluatePrecondition()) {
								activeActions.add(a[j]);
							}
							ai.actionClear();
							rollbackInputChannels();
						}
						
						List enabledActions = new ArrayList();
						for (int j = 0; j < activeActions.size(); j++) {
							Action a1 = (Action)activeActions.get(j);
							boolean enabled = true;
							for (int k = j - 1; enabled && k >= 0 ; k--) {
								Action a2 = (Action)activeActions.get(k);
								if (PriorityUtil.isOrdered(a2, a1, actor.getPriorities())) {
									enabled = false;
								}
							}
							if (enabled) {
								enabledActions.add(a1);
							}
						}
						if (enabledActions.size() != 1) {
							String msg = "nondeterminism";
//							String msg = "ND Warning: actor '" 
//									       + actor.getName() 
//									       + "', at step #"
//									       + (firingCount + 1)
//									       + ", multiple enabled actions: ";
							for (int j = 0; j < enabledActions.size(); j++) {
								if (j > 0)
									msg += ", ";
								Action ea = (Action)enabledActions.get(j);
								msg += ea.getID();
							}
							Logging.user().warning(msg);
						}
						
						
						ai.actionSetup(a[i]);
						ai.actionEvaluatePrecondition(); //discard return value
					}
					
					nVoidFirings = 0;
					firingCount += 1;
					if (hasTraceVar) {
						Object v = this.actorEnv.get(traceVarName);
						if (myContext.isBoolean(v) && myContext.booleanValue(v)) {
							Logging.user().info("<trace actor='" 
									           + actor.getName() 
									           + "' action='"
									           + a[i].getID()
									           + "'>"
									           + "    " + actionDescription(a[i])
									           + "\n    states: " + ((ndaStateSets != null) ? ndaStateSets[currentState].toString() : "<empty>") 
									           + "\n    position: " + i
									           + "\n    eligible actions:" + actionDescription(a)
									           + "</trace>");
						}
					}
					delayOutput = ai.hasDelay();
					
					// actionsFired.add(a[i].getTag()==null ? "unnamed action" : a[i].getTag().toString()); //JTK
					checkActionPreconditions(ai);
					ai.actionStep();
					checkActionPostconditions(ai);
					/* Computing the delay AFTER the step allows us to take into account 
					 * the action code, and use all knowledge we have at the end of an 
					 * action firing. It also allows us to use old-constructs as part of the
					 * delay expression.
					 * <p>
					 * FIXME: make sure to amend the documentation accordingly.
					 */
					double delay = ai.computeDelay();
					ai.actionComputeOutputs();
					ai.actionClear(); // sets .currentAction() to null
					currentState = successorState[currentState][i];
					commitInputChannels();
					checkInvariants();
					if (delayOutput) {
						// delay outputs by specified amount of time
						scheduleActorDelayed(delay);
					} else {
						// nothing to be delayed, flush outputs immediately
						flushOutputChannels();
						scheduleActor();					
					}
					animationPostfireHandler.modifiedAnimationState();					
					return true;
				} else {
					ai.actionClear();
					rollbackInputChannels();
				}
				} catch (Exception exc) {
					throw new RuntimeException("Error in actor '" + actor.getName() + "', action " + a[i].getID() + " (" + a[i].getAttribute("text-begin-line") + "-" + a[i].getAttribute("text-end-line") + ").", exc);
				}
			}
			nVoidFirings += 1;
			return false;			
		}
	}
	
	private String  actionDescription(Action a) {
		return ""
        + a.getID() 
        + " (" + a.getAttribute("text-begin-line") 
        + "-" + a.getAttribute("text-end-line") + ")"
        + ((a.getTag() != null) ? "---action tag: " + a.getTag().toString() : "");
	}
	
	private String  actionDescription(Action [] a) {
		StringBuffer s = new StringBuffer();
		for (int i = 0; i < a.length; i++) {
			if (i != 0) {
				s.append(", ");
			}
			s.append("[");
			s.append(actionDescription(a[i]));
			s.append("]");
		}
		return s.toString();
	}
	
	protected void commitInputChannels() {
		for (Iterator i = inputPortMap.keySet().iterator(); i.hasNext(); ) {
			Object k = i.next();
			MosesInputChannel mic = (MosesInputChannel) inputPortMap.get(k);
			mic.commit();
		}
	}
	
	protected void rollbackInputChannels() {
		for (Iterator i = inputPortMap.keySet().iterator(); i.hasNext(); ) {
			Object k = i.next();
			MosesInputChannel mic = (MosesInputChannel) inputPortMap.get(k);
			mic.rollback();
		}
	}
	
	protected void flushOutputChannels() {
		for (Iterator i = outputPortMap.keySet().iterator(); i.hasNext(); ) {
			Object k = i.next();
			MosesOutputChannel moc = (MosesOutputChannel)outputPortMap.get(k);
			if (moc.flush()) {
				blockedOutputChannels.add(moc);
			}
		}
	}
	
	protected void flushBlockedOutputChannels() {
		Set stillBlocked = new HashSet();
		for (Iterator i = blockedOutputChannels.iterator(); i.hasNext(); ) {
			MosesOutputChannel moc = (MosesOutputChannel)i.next();
			if (moc.flush()) {
				stillBlocked.add(moc);
			}
		}
		blockedOutputChannels = stillBlocked;
	}
	
	protected void blockActor(String port) {
		if (hasTraceVar) {
			Object v = this.actorEnv.get(traceVarName);
			if (myContext.isBoolean(v) && myContext.booleanValue(v)) {
				Logging.user().info("<block actor='" 
						           + actor.getName() 
						           + "' port='"
						           + port
						           + "'/>");
			}
		}

		if (!bufferBlockRecord)
			return;

		blockedStep = scheduler.currentEventCount();
		blockedTime = scheduler.currentTime();
		Collection<OutputBlockRecord> obr = getOBR();
		if (!obr.contains(myOBR))
			obr.add(myOBR);
	}
	
	protected void unblockActor(String port) {
		if (hasTraceVar) {
			Object v = this.actorEnv.get(traceVarName);
			if (myContext.isBoolean(v) && myContext.booleanValue(v)) {
				Logging.user().info("<unblock actor='" 
						           + actor.getName() 
						           + "' port='"
						           + port
						           + "'/>");
			}
		}

		if (!bufferBlockRecord)
			return;
		
		Collection<OutputBlockRecord> obr = getOBR();
		obr.remove(myOBR);
	}
	
	private Collection<OutputBlockRecord> getOBR() {
		Collection<OutputBlockRecord> obr = (Collection<OutputBlockRecord>)scheduler.getProperty("OutputBlockRecords"); 
		if (obr == null) {
			obr = new HashSet<OutputBlockRecord>();
			scheduler.setProperty("OutputBlockRecords", obr);
		}
		return obr;
	}
	
	//
	//  CalInterpreter
	//
	
	public CalInterpreter(Actor a, Map outsideEnv) {
		
		actor = a;
		
		this.parentEnv = outsideEnv;
		
		PortDecl[] pd = actor.getInputPorts();
		inputPortMap = new HashMap();
		for (int i = 0; i < pd.length; i++) {
			MosesInputChannel mic = createInputChannel(pd[i].getName());
			inputPortMap.put(pd[i].getName(), mic);
			inputs.addConnector(pd[i].getName(), mic);
		}
		
		pd = actor.getOutputPorts();
		outputPortMap = new HashMap();
		for (int i = 0; i < pd.length; i++) {
			MosesOutputChannel moc = createOutputChannel(pd[i].getName());
			outputPortMap.put(pd[i].getName(), moc);
			outputs.addConnector(pd[i].getName(), moc);
		}
		
		scheduler = null;
		ai = null;
		actions = PriorityUtil.prioritySortActions(actor);
		
		actionCoverSets = PriorityUtil.computeActionPriorities(actor);
		
		setupDFA();
		
		//location map stuff
		locationMap.put(new Integer(0), this);
		animationPostfireHandler = new AnimationPostfireHandler();
	}
	
	protected  MosesInputChannel createInputChannel(String name) {
		return new MosesInputChannel(name);
	}
	
	protected  MosesOutputChannel createOutputChannel(String name) {
		return new MosesOutputChannel(name);
	}
	
	
	/**
	 * Produce the actor state variables as a map from the variable names to their
	 * values.
	 */
	
	public Map  getActorStateVariables() {
		return actorEnv.localBindings();
	}
	
	/**
	 * Produce the current state of the actor scheduler FSM. Note that in general 
	 * this state corresponds to a set of states, since the FSM may be non-deterministic. 
	 * For deterministic FSMs, the returned set will always contain exactly one element.
	 * <p>
	 * The elements of the set are the names of the states of the FSM, as String objects. 
	 */
	
	public Set getSchedulerState() {
        if (ndaStateSets == null) {
            return Collections.EMPTY_SET;
        } else {
            return Collections.unmodifiableSet(ndaStateSets[currentState]);
        }
    }
	
	public Map  getInputQueues() {
		Map m = new HashMap();
		for (Iterator i = inputPortMap.keySet().iterator(); i.hasNext(); ) {
			Object k = i.next();
			m.put(i, ((MosesInputChannel)inputPortMap.get(i)).getTokenList());
		}
		return m;
	}
	
	/**
	 * Produce the environment containing all variables with actor scope, and in
	 * particular the actor state variables.
	 */
	
	public Environment  getActorEnvironment() {
		return actorEnv;
	}
	
	public long  getFiringCount() {
		return firingCount;
	}
	
	public String  getName() {
		return actor.getName() + "@" + this.hashCode();
	}
	
	//
	//  private
	//
	
	
	protected double  computeSchedulePriority() {
//		if (tokensQueued == 0) 
//			return 10000;
//		else return -(double)tokensQueued;
		
		
		if (tokensQueued == 0)
			return 101;
		
		double prio = (nVoidFirings > 99) ? 99 : nVoidFirings;
		
		prio += (tokensQueued == 0) ? 1.0 : (1.0/tokensQueued);
		
		prio += rand.nextDouble() - 0.5;
		
		return prio;
	}
	
	private Random rand = new Random();
	
	protected void scheduleActor() {
		if (!delayOutput && blockedOutputChannels.isEmpty()) {
			scheduler.schedule(scheduler.currentTime(), computeSchedulePriority(), this);
		}
	}
		
	protected void scheduleActorDelayed(double delay) {
		scheduler.schedule(scheduler.currentTime() + delay, computeSchedulePriority(), this);
	}
	
	protected void  scheduleActorForOutputFlushing() {
		double schedulePriority = computeSchedulePriority();
		scheduler.schedule(scheduler.currentTime(), schedulePriority, this);
	}
	
	
	/**
	 * Convert the nondeterministic schedule automaton into a deterministic one (DFA).
	 * The data to represent the DFA is stored in a few private members.
	 * 
	 * @see #currentState
	 * @see #eligibleActions
	 * @see #successorState
	 */
	private void    setupDFA() {
		currentState = 0;
		if (actor.getScheduleFSM() == null) {
			// generate trivial DFA in case there is no schedule fsm.
			//    1. only one state
			//    2. all actions are eligible
			//    3. the successor state is always the same
			eligibleActions = new Action [][] {actions};
			successorState = new int [1] [actions.length];
			for (int i = 0; i < actions.length; i++) {
				successorState[0][i] = 0;
			}
			return;
		}
		
		Set stateSets = new HashSet();
		// put initial state into set of state sets
		Set initialState = Collections.singleton(actor.getScheduleFSM().getInitialState());
		stateSets.add(initialState);
		int previousSize = 0;
		// iterate until fixed-point, i.e. we cannot reach any new state set
		while (previousSize != stateSets.size()) {
			previousSize = stateSets.size();
			// for each action...
			for (int i = 0; i < actions.length; i ++) {
				Set nextStates = new HashSet();
				// ... compute the set of states that can be reached through it... 
				for (Iterator j = stateSets.iterator(); j.hasNext(); ) {
					Set s = (Set) j.next();
					if (isEligibleAction(s, actions[i])) {
						nextStates.add(computeNextStateSet(s, actions[i]));
					}
				}
				// ... and add them to the state set
				stateSets.addAll(nextStates);
			}
		}
		
		// The set of all reachable state sets is the state space of the NDA. 
		ndaStateSets = (Set []) new ArrayList(stateSets).toArray(new Set[stateSets.size()]);
		// Make sure the initial state is state 0.
		for (int i = 0; i < ndaStateSets.length; i++) {
			if (ndaStateSets[i].equals(initialState)) {
				Set s = ndaStateSets[i];
				ndaStateSets[i] = ndaStateSets[0];
				ndaStateSets[0] = s;
			}
		}
		
		eligibleActions = new Action [ndaStateSets.length] [];
		successorState = new int [ndaStateSets.length] [];
		// For each state set (i.e. each NDA state), identify the eligible actions,
		// and also the successor state set (i.e. the successor state in the NDA).
		for (int i = 0; i < ndaStateSets.length; i++) {
			List ea = new ArrayList();
			List ss = new ArrayList();
			for (int j = 0; j < actions.length; j++) {
				if (isEligibleAction(ndaStateSets[i], actions[j])) {
					ea.add(actions[j]);
					ss.add(computeNextStateSet(ndaStateSets[i], actions[j]));
				}
			}
			eligibleActions[i] = (Action []) ea.toArray(new Action[ea.size()]);
			List ds = Arrays.asList(ndaStateSets);  // so we can use List.indexOf()
			successorState[i] = new int [ss.size()];
			// locta the NDA successor state in array
			for (int j = 0; j < ss.size(); j++) {
				successorState[i][j] = ds.indexOf(ss.get(j));
				
				// must be in array, because we iterated until reaching a fixed point.
				assert successorState[i][j] >= 0;
			}
		}
	}
	
	private boolean isEligibleAction(Set css, Action a) {
		QID tag = a.getTag();
		if (tag != null && css != null) {
			if (actor.getScheduleFSM() == null)
				return true;
			Transition[] ts = actor.getScheduleFSM().getTransitions();
			for (int i = 0; i < ts.length; i++) {
				Transition t = ts[i];
				if (css.contains(t.getSourceState())
						&& isPrefixedByTagList(tag, t.getActionTags())) {
					
					return true;
				}
			}
			return false;
		} else {
			return true;
		}
	}
	
	private Set computeNextStateSet(Set css, Action a) {
		if (css == null) {
			return null;
		}
		if (a == null || a.getTag() == null) {
			return css;
		}
		
		Set ns = new HashSet();
		QID tag = a.getTag();
		Transition[] ts = actor.getScheduleFSM().getTransitions();
		for (int i = 0; i < ts.length; i++) {
			Transition t = ts[i];
			if (css.contains(t.getSourceState())
					&& isPrefixedByTagList(tag, t.getActionTags())) {
				
				ns.add(t.getDestinationState());
			}
		}
		return ns;
	}
	
	private boolean isPrefixedByTagList(QID tag, QID[] tags) {
		for (int j = 0; j < tags.length; j++) {
			if (tags[j].isPrefixOf(tag)) {
				return true;
			}
		}
		return false;
	}
	
	private void  setupPortTypeChecking() {
		final String actorName = actor.getName();
		
		String typeChecking = System.getProperty("EnableTypeChecking");
		if (typeChecking != null) {
			typeChecking = typeChecking.trim().toLowerCase();
		}
		
		boolean checkTypes = "true".equals(typeChecking) || "on".equals(typeChecking);

		for (PortDecl pd : actor.getInputPorts()) {
			final String portName = pd.getName();
			final TypeExpr te = pd.getType();
			TokenListener tl = null;
			if (checkTypes && te != null) {
				tl = new TokenListener () {
					public void notify(Object token) {
						Types.TypeCheckResult tcr = Types.checkType(te, token, myContext, actorEnv);
						switch (tcr) {
						case NO: 
							Logging.user().severe("Type check failed on input port '" + portName 
								                + "' of actor '" + actorName + "'. Value: " + token);
							break;
						case UNDECIDED: 
							Logging.user().warning("Type check inconclusive on input port '" + portName 
			                                    + "' of actor '" + actorName + "'. Value: " + token);
							break;
						default:
							break;
						}
					}
				};
			}
			inputPortMap.get(portName).setTokenListener(tl);
		}

		for (PortDecl pd : actor.getOutputPorts()) {
			final String portName = pd.getName();
			final TypeExpr te = pd.getType();
			TokenListener tl = null;
			if (checkTypes && te != null) {
				tl = new TokenListener () {
					public void notify(Object token) {
						Types.TypeCheckResult tcr = Types.checkType(te, token, myContext, actorEnv);
						switch (tcr) {
						case NO: 
							Logging.user().severe("Type check failed on output port '" + portName 
								                + "' of actor '" + actorName + "'. Value: " + token);
							break;
						case UNDECIDED: 
							Logging.user().warning("Type check inconclusive on output port '" + portName 
			                                    + "' of actor '" + actorName + "'. Value: " + token);
							break;
						default:
							break;
						}
					}
				};
			}
			outputPortMap.get(portName).setTokenListener(tl);
		}

	}
	
	private void  setupAssertionHandling() {
		String assertions = System.getProperty("EnableAssertions");
		if (assertions != null) {
			assertions = assertions.trim().toLowerCase();
		}
		checkAssertions = false;
		if ("true".equals(assertions) || "on".equals(assertions)) {
			checkAssertions = true;
			AssertionHandler ah = (AssertionHandler) scheduler.getProperty("CalAssertionHandler");
			if (ah == null) {
				ah = new LoggingAssertionHandler();
				scheduler.setProperty("CalAssertionHandler", ah);
			}
			assertionHandler = ah;
		}
	}
	
	private void  checkInvariants() {
		if (!checkAssertions) 
			return;
		Expression [] invariants = actor.getInvariants();
		if (invariants == null)
			return;
		ExprEvaluator eval = new ExprEvaluator(myContext, this.actorEnv);
		for (int i = 0; i < invariants.length; i++) {
			Object res = eval.evaluate(invariants[i]);
			if (!myContext.booleanValue(res)) {
				reportAssertionFailure("Invariant #" + i, "Failed invariant.");
			}
		}
	}
	
	private void  checkActionPreconditions(ActorInterpreter ai) {
		if (!checkAssertions)
			return;
		Action a = ai.currentAction();
		Expression [] pc = a.getPreconditions();
		if (pc == null) 
			return;
		for (int i = 0; i < pc.length; i++) {
			Object res = ai.evaluateExpression(pc[i]);
			if (!myContext.booleanValue(res)) {
				reportAssertionFailure("Action precondition " + a.getID() + "." + i, "Failed precondition.");
			}
		}
		
	}
	
	private void  checkActionPostconditions(ActorInterpreter ai) {
		if (!checkAssertions)
			return;
		Action a = ai.currentAction();
		Expression [] pc = a.getPostconditions();
		if (pc == null) 
			return;
		for (int i = 0; i < pc.length; i++) {
			Object res = ai.evaluateExpression(pc[i]);
			if (!myContext.booleanValue(res)) {
				reportAssertionFailure("Action postcondition " + a.getID() + "." + i, "Failed postcondition.");
			}
		}
		
	}
	
	private void  reportAssertionFailure(String assertionLocus, String message) {
		AssertionHandler ah = (AssertionHandler)scheduler.getProperty("CalAssertionHandler");
		if (ah == null)
			return;
		
		ah.assertionFailed(this, assertionLocus, getFiringCount(), null, message);
	}
	
	//
	//  data
	//
	
	
	protected Actor actor;
	protected Action[] actions;
	protected Map actionCoverSets;
	
	protected boolean checkAssertions = false;
	protected AssertionHandler assertionHandler = null;
	
	private Map parentEnv;
	
	/**
	 * This environment contains all variables defined outside the actor, including the 
	 * bindings for the actor parameters. All bindings in this environment should be 
	 * treated as immutable.
	 * <p>
	 * The reason for this environment to be a member variable is that it is 
	 * built in the constructor, while the actor state variables will not be initialized
	 * until the call to initialize.
	 */
	protected Environment outsideEnv;
	
	/**
	 * This environment contains all bindings with actor scope. The local frame contains 
	 * the actor state variables.
	 */
	protected Environment actorEnv;
	private   boolean     hasTraceVar;
	private   boolean     hasNDTrackerVar;
	private   int         warnBigBuffers;
	private   boolean     ignoreBufferBounds;
	private   boolean     bufferBlockRecord;
	private final static String traceVarName = "_CAL_traceOutput";
	private final static String nondeterminismTrackerVarName = "_CAL_trackND";
	private final static String FINALIZE_PROCEDURE = "__CAL_Finalize";
	protected Scheduler scheduler;
	protected ActorInterpreter ai;
	
	private OutputBlockRecord myOBR = new OutputBlockRecord() {
		public String getComponentName() {
			return getName();
		}
		
		public Collection<String> getBlockedOutputConnectors() {
			List<String> ports = new ArrayList<String>();
			for (MosesOutputChannel moc : blockedOutputChannels) {
				ports.add(moc.getName());
			}
			return ports;
		}
		
		public Map<String, Collection<Object>> getBlockingSourceMap() {
			Map<String, Collection<Object>> sources = new HashMap<String, Collection<Object>>();
			for (MosesOutputChannel moc : blockedOutputChannels) {
				sources.put(moc.getName(), Collections.EMPTY_SET); // to shut up the compiler. this class has got to go. JWJ
	  		}
			return sources;
		}
		
		public long getStepNumber() {
			return blockedStep;
		}
		
		public double getTime() {
			return blockedTime;
		}
	};
	
	protected AnimationPostfireHandler animationPostfireHandler = new AnimationPostfireHandler();
	
	/**
	 * 
	 */
	
	protected boolean delayOutput = false;
	protected Set<MosesOutputChannel> blockedOutputChannels = new HashSet<MosesOutputChannel>();
	protected long blockedStep;
	protected double blockedTime;
	
	/**
	 * The current scheduler state in the NDA. Each state represents a (non-empty) set 
	 * of states of the original scheduler FSM.
	 */
	protected int        currentState;
	
	/**
	 * Contains for each state an array of Actions (topologically sorted to respect the
	 * priority order) that are eligible in that state.
	 */
	protected Action[][] eligibleActions;  // #states x #actions (per state) 
	
	/**
	 * Identifies for each state, and each action eligible in that state, the follow-up
	 * state.
	 */
	protected int   [][] successorState;   // #states x #eligible actions
	
	/**
	 * Identifies the set of original schedule FSM states for each state in the NDA.
	 */
	protected Set   []   ndaStateSets;	// not used in simulation, but might be 
	// useful when animating
	
	protected Map<String, MosesInputChannel> inputPortMap;
	protected Map<String, MosesOutputChannel> outputPortMap;
	
	private int tokensQueued;
	private int  nVoidFirings = 0;
	private long firingCount = 0;
	
	protected Platform myPlatform = null;
	protected Context myContext = null;
	
	protected final static Platform defaultPlatform = DefaultPlatform.thePlatform;
	protected final static Context  defaultContext  = defaultPlatform.context();
	
	
	//
	//  StateChangeProvider
	//
	
	private Set listeners = null;
	
	private Object lastState;
	
	
	public boolean hasStateChangeListeners() {
		return listeners == null || listeners.isEmpty();
	}
	
	public void addStateChangeListener(StateChangeListener listener) {
		if (listeners == null)
			listeners = new HashSet();
		
		listeners.add(listener);
	}
	
	public void removeStateChangeListener(StateChangeListener listener) {
		if (listeners == null) {
			return;
		}
		listeners.remove(listener);
		if (listeners.isEmpty())
			listeners = null;
	}
	
	public void requestStateInformation() {
		sendStateChangeEvent(new StateChangeEvent(this, "CalActorState", Double.NaN,
			null, lastState == null ? this.getStateInformation() : lastState));
	}
	
	
	public void notifyStateChange(double t, Object a) {
		if (listeners == null) {
			return;
		}
		try {
			StateChangeEvent sce;
			sce = new StateChangeEvent(this, "CalActorState", t, lastState, a);
			lastState = a;
			sendStateChangeEvent(sce);
		} catch (Throwable e) {}
	}
	
	public Object getStateInformation() {
		Map stateMap = new HashMap();
		//tokenMap
		Map tokenMap = new HashMap();
		for (Iterator i = inputPortMap.keySet().iterator(); i.hasNext(); ) {
			Object k = i.next();
			MosesInputChannel mic = (MosesInputChannel) inputPortMap.get(k);
			tokenMap.put(mic.getName(), mic.getTokenList());
		}
		stateMap.put("tokenMap", tokenMap);
		
		stateMap.put("currentEvent", new Long(scheduler.currentStrongEventCount()));
		stateMap.put("currentTime", new Double(scheduler.currentTime()));
		stateMap.put("actorState", getActorStateVariables());
		stateMap.put("schedulerState", getSchedulerState());
		stateMap.put("actionsFired", getActionsFired());
		//action status
		List el = Arrays.asList(eligibleActions[currentState]);
		Map actionAssociations = new HashMap();
		Set enabledActions = new HashSet();
		for (int i = 0; i < actor.getActions().length; i++) {
			Action a = actor.getActions()[i];
			int state = 0;
			if (el.contains(a)) {
				state++;
				ai.actionSetup(a);
				if (ai.actionEvaluateTokenAvailability()) {
					state++;
					if (ai.actionEvaluateGuard()) {
						enabledActions.add(a);
						state++;						
					}
				}    
				//clean up after evaluation
				ai.actionClear();
				rollbackInputChannels();
			}
			
			Set cover = new HashSet((Set)actionCoverSets.get(a));
			cover.retainAll(enabledActions);
			boolean isCovered = !cover.isEmpty();

			//info = label, state, begin-line, begin-col, end-line, end-col
//			Object[] info = {"[" + Utility.arrayToString(a.getInputPatterns()) + "] ==> ["
//					+ Utility.arrayToString(a.getOutputExpressions()) + "]", new Integer(state), a.getAttribute("text-begin-line"), a.getAttribute("text-begin-col"), 
//					a.getAttribute("text-end-line"), a.getAttribute("text-end-col")};
			Object [] info = {new Integer(state), new Boolean(isCovered)};
			actionAssociations.put(a, info);
		}
		//clean up after evaluation
		stateMap.put("actionMap", actionAssociations);
		return stateMap;
	}
	
	//
	// event number handling
	//
	
	private List actionsFired = new ArrayList();
	private List getActionsFired() {
		return actionsFired;
	}

	private void sendStateChangeEvent(StateChangeEvent sce) {
		for (Iterator i = listeners.iterator(); i.hasNext(); ) {
			StateChangeListener scl = (StateChangeListener)i.next();
			scl.stateChange(sce);
		}
	}
	
	protected void resetEvents(double t) {
		notifyStateChange(t, this.getStateInformation());
	}
	
	//
	//  inner classes
	// 
	
	protected class MosesInputChannel  extends AbstractMessageListener implements InputPort, InputChannel, Attributable {
		
		//
		//  InputPort
		//
		
		public String getName() {
			return name;
		}
		
		public boolean isMultiport() {
			return false;
		}
		
		public int width() {
			return 1;
		}
		
		public InputChannel getChannel(int n) {
			if (n != 0) {
				throw new RuntimeException("Getting channel >0 from SingleInputPort.");
			}
			return this;
		}
		
		//
		//  InputChannel
		//
		
		public boolean hasAvailable(int n) {
			return n <= tokens.size();
		}
		
		public Object get(int n) {
			if (hasAvailable(n + 1)) {
				tokensRead = Math.max(tokensRead, n + 1);
				return tokens.get(n);
			} else {
				throw new RuntimeException("CalInterpreter '" + actor.getName() + "': Read token beyond end of queue.");
			}
		}
		
		public void commit() {
			boolean full = bufferFull();
			tokens.deleteRange(0, tokensRead);
			tokensQueued -= tokensRead;
			if (warnBigBuffers > 0 && tokens.size() > warnBigBuffers)
				Logging.user().warning("Channel '" + actor.getName() + "." + name + "', big queue (R): " +  (tokens.size() + tokensRead) + " - " + tokensRead);
			tokensRead = 0;
			animationPostfireHandler.modifiedAnimationState();
			if (full && !bufferFull()) {
				notifyControl(CE_UNBLOCK);								
			}
		}
		
		public void rollback() {
			tokensRead = 0;
		}
		
		// 
		// Attributable
		//
		
		public boolean  set(Object name, Object value) {
			if (attrBufferSize.equals(name) && value instanceof Integer) {
				bufferSize = ((Integer)value).intValue();
				if (bufferSize == 0)
					bufferSize = 1;
				return true;
			}
			return false;
		}
		
		//
		//  AbstractMessageListener
		//
		
	    public void	message(MessageEvent evt) {
			tokens.add(evt.value);
			try {
				if (listener != null) {
					listener.notify(evt.value);
				}
			} catch (Exception e) {e.printStackTrace(); System.out.println("Uh-oh");}
			tokensQueued += 1;
			scheduleActor();
			animationPostfireHandler.modifiedAnimationState();
			if (warnBigBuffers > 0 && tokens.size() > warnBigBuffers)
				Logging.user().warning("Channel '" + actor.getName() + "." + name + "', big queue (W): " + tokensQueued);
			if (bufferFull()) {
				notifyControl(CE_BLOCK);
			}
	    }
	   
		//
		//  MosesInputChannel
		//
		
		public MosesInputChannel(String name) {
			tokensRead = 0;
			this.name = name;
			tokens = new MyArrayList();
			callbacks = new MyArrayList();
			bufferSize = -1;
			listener = null;
		}
		
		public List  getTokenList() {
			return Collections.unmodifiableList(tokens);
		}
		
		protected boolean bufferFull() {
			if (ignoreBufferBounds)
				return false;
			return bufferSize > 0 && tokensQueued >= bufferSize;
		}
		
		public void  setTokenListener(TokenListener tl) {
			listener = tl;
		}
		
		//
		//  data
		//
		
		protected MyArrayList tokens;
		protected MyArrayList callbacks;
		protected int tokensRead;
		protected String name;
		protected int  bufferSize;
		
		protected TokenListener listener;
		
		private final String attrBufferSize = "bufferSize";

		protected final ControlEvent CE_BLOCK = new ControlEvent(this, ControlEvent.BLOCK);
		protected final ControlEvent CE_UNBLOCK = new ControlEvent(this, ControlEvent.UNBLOCK);
	}

	protected class MosesOutputChannel extends BasicMessageProducer implements OutputPort, OutputChannel {
		
		//
		//  BasicMessageProducer -- override
		//
		
		public void  control(ControlEvent ce) {
			if (ce.data == ControlEvent.BLOCK) {
				blocked = true;
				blockActor(getName());
			} else if (ce.data == ControlEvent.UNBLOCK) {
				if (blocked) {
					blocked = false;
					unblockActor(getName());
					scheduleActorForOutputFlushing();
				} else {
					Logging.dbg().warning(CalInterpreter.this.getName() + "." + this.getName() + ": Received unblocking event without blocking event.");
				}
			} else
				Logging.dbg().warning("Received unidentified control event.");
		}
		
		//
		//  OutputPort
		//
		
		public OutputChannel getChannel(int n) {
			if (n != 0) {
				throw new RuntimeException("Getting channel >0 from SingleOutputPort.");
			}
			return this;
		}
		
		public String getName() {
			return name;
		}
		
		public boolean isMultiport() {
			return false;
		}
		
		public int width() {
			return 1;
		}
		
		//
		//  OutputChannel
		//
		
		public void put(Object a) {
			try {
				if (listener != null) {
					listener.notify(a);
				}
			} catch (Exception e) {}
			tokens.add(a);
		}
		
		//
		//  MosesOutputChannel
		//
		
		/**
		 * @return True, if blocked.
		 */
		
		public boolean  flush() {
			int n = 0;
			while (!blocked && n < tokens.size()) {
				Object a = tokens.get(n);
				super.notifyMessage(new MessageEvent(CalInterpreter.this, scheduler.currentTime(), a));
				n += 1;
			}
			tokens.deleteRange(0, n);
			return blocked;
		}
		
		public void  setTokenListener(TokenListener tl) {
			listener = tl;
		}
				
		public MosesOutputChannel(String name) {
			this.name = name;
			this.blocked = false;
			listener = null;
		}
		
		protected TokenListener  listener;
		protected String name;
		protected boolean  blocked;
		protected MyArrayList tokens = new MyArrayList();
	}
	
	interface TokenListener {
		void notify(Object token);
	}
	
	/**
	 * Creating this subclass of ArrayList is unfortunate but necessary, because we want to use the
	 * {@link ArrayList#removeRange removeRange} method of {@link ArrayList}, and it 
	 * happens to be protected for some reason.
	 */
	
	protected static class MyArrayList extends ArrayList {
		public void deleteRange(int a, int b) {
			removeRange(a, b);
		}
		
		public MyArrayList() { super(); }
	}
	
	/**
	 * An animationPostfireHandler records the relevant (to the animation) changes to 
	 * the CalInterpreter during a firing, and registers itself with the scheduler as a
	 * PostfireHandler. During the postfire phase, it emits the corresponding animation
	 * events.
	 * 
	 * @see PostfireHandler
	 * 
	 */

	protected class AnimationPostfireHandler implements PostfireHandler {
		
		//
		//  PostfireHandler
		//
		
		public void postfire() {
			notifyStateChange(scheduler.currentTime(), getStateInformation());
		}
		
		//
		//  APH
		//
		
		public void  modifiedAnimationState() {
			if (listeners != null)
				scheduler.addPostfireHandler(this);
		}
		
		//
		//  Ctor
		//
		
		//
		//  data
		//
		
	}
	
	/**
	 * 
	 */
	
	public interface AssertionHandler {
		
		void assertionFailed (Object component, Object location, long step, Object assertion, String message);
	}
	
	class LoggingAssertionHandler implements AssertionHandler {

		public void assertionFailed(Object component, Object location, long step, Object assertion, String message) {
			String s = "Assertion failed. --- ";
			
			s += "[at: " + component + "::" + location + ", during step #" + step + "]";
			
			if (message != null) {
				s += ": " + message;
			}
									
			Logging.user().warning(s);
		}
	}
	
	
		
	
	static public final String  CAL_PLATFORM = "CalPlatform";

}
