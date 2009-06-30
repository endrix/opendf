/* 
BEGINCOPYRIGHT X,UC

	Copyright (c) 2009, EPFL
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

package net.sf.opendf.plugin.causation;

/*
import net.sf.caltrop.hades.des.MessageEvent;

import net.sf.caltrop.cal.interpreter.Context;
import net.sf.caltrop.cal.interpreter.environment.Environment;

 * 
 */

import net.sf.opendf.hades.des.schedule.Scheduler;
import net.sf.opendf.hades.des.schedule.SimulationFinalizer;
import net.sf.opendf.hades.cal.CalInterpreter;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import net.sf.opendf.cal.ast.Action;
import net.sf.opendf.cal.ast.Actor;
import net.sf.opendf.cal.i2.Environment;

/**
 * This class implements a Cal interpreter that generates a causation
 * trace as part of the simulation.
 * 
 * @author jwj
 *
 */

public class CalInterpreterCT extends CalInterpreter {

	public CalInterpreterCT(Actor a, Map env) {
		super(a, env);
	}
	
	protected MosesInputChannel  createInputChannel(String name) {
		return new MosesInputChannelCT(name);
	}
	
	protected  MosesOutputChannel createOutputChannel(String name) {
		return new MosesOutputChannelCT(name);
	}

	public void initializeState(double t, Scheduler s) {
		
		if (!causationTraceBuilders.containsKey(s)) {
			// if a CTB has not already been registered for this scheduler, then
			// 1. create one
			try {
				// FIXME: have some mechanism for specifying the location of the trace file.
				ctb = new XmlCausationTraceBuilder(new PrintWriter(new FileOutputStream(path + "/causation_trace.xml")), true);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
			
			// 2. register it for this scheduler
			causationTraceBuilders.put(s, ctb);

			// 3. register a simulation finalizer that does the cleanup
			s.registerSimulationFinalizer(new SimulationFinalizer() {	
				public void finalizeSimulation() {
					if (causationTraceBuilders.containsKey(scheduler)) {
						CausationTraceBuilder ctb = (CausationTraceBuilder)causationTraceBuilders.get(scheduler);
						ctb.endTrace();
						causationTraceBuilders.remove(scheduler);
					}
				}					
			} );
			
			// 4. start trace
			ctb.beginTrace();
		} else {
			ctb = (CausationTraceBuilder)causationTraceBuilders.get(s);
		}
		
		super.initializeState(t, s);
		
		actorName = actor.getName();
		actorID = counter++;
	}
	
	public boolean processEvent(double time) {
		if (delayOutput) {
			delayOutput = false;
			ctb.beginStep();
			ctb.setStepAttribute("interpreter", "Cal");
			ctb.setStepAttribute("actor-name", actorName);
			ctb.setStepAttribute("actor-id", new Long(actorID));
			ctb.setStepAttribute("current-time", new Double(scheduler.currentTime()));
			ctb.setStepAttribute("kind", "postfire");
			ctb.setStepAttribute("action", previousAction);
			flushOutputChannels();
			ctb.beginDependency(previousStep);
			ctb.setDependencyAttribute("kind", "delay");
			ctb.endDependency();
			previousStep = ctb.currentStep();
			ctb.endStep();
			
			scheduleActor();
			return true;
		} else {
			ctb.beginStep();
			ctb.setStepAttribute("interpreter", "Cal");
			ctb.setStepAttribute("actor-name", actorName);
			ctb.setStepAttribute("actor-id", new Long(actorID));
			ctb.setStepAttribute("current-time", new Double(scheduler.currentTime()));
			
			((CausationLoggingEnvironment)actorEnv).resetVarSets();
			Action [] a = eligibleActions[currentState];
			for (int i = 0; i < a.length; i++) {
				ai.actionSetup(a[i]);
				if (ai.actionEvaluatePrecondition()) {
					
					if (a[i].getTag() != null && successorState.length > 1 && previousStep != null) {
						// if this is a tagged action
						// and this actor has a non-trivial state machine
						// and there was a previous step of a tagged action
						// then create a dependency to that action
						ctb.beginDependency(previousStep);
						ctb.setDependencyAttribute("kind", "scheduler");
						ctb.endDependency();
					}
					
					for (int j = 0; j < actor.getActions().length; j++) {
						if (a[i] == actor.getActions()[j]) {
							previousAction = new Integer(j);
							ctb.setStepAttribute("action", previousAction);
							break;
						}
					}
					if (a[i].getTag() != null) {
						ctb.setStepAttribute("action-tag", a[i].getTag().toString());
					}
					
					delayOutput = ai.hasDelay();
					ai.actionStep();
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
					if (delayOutput) {
						// delay outputs by specified amount of time
						scheduleActorDelayed(delay);
						ctb.setStepAttribute("kind", "delayed");
						ctb.setStepAttribute("delay", new Double(delay));
						previousStep = ctb.currentStep();
					} else {
						// nothing to be delayed, flush outputs immediately
						ctb.setStepAttribute("kind", "immediate");
						flushOutputChannels();
						scheduleActor();					
					}
					((CausationLoggingEnvironment)actorEnv).commitReadVars();
					if (successorState.length > 1 && a[i].getTag() != null) {
						// if this actor has a non-trivial scheduler state machine
						// and this action is tagged
						// record the current step
						previousStep = ctb.currentStep();
					}
					ctb.endStep();
					return true;
				} else {
					ai.actionClear();
					rollbackInputChannels();
				}
			}
			ctb.cancelStep();
			return false;			
		}
	}

	protected Environment createActorStateEnvironment(Environment parent/*, Context context*/) {
		return (new CausationLoggingEnvironment(ctb, parent/*, context*/));
	}
	
	
	private CausationTraceBuilder  ctb = null;
	
	private String   actorName;
	private long     actorID;
	private Object   previousStep = null;  // last step completed by this actor
	private Object   previousAction = null;
		
	private static Map causationTraceBuilders = new HashMap();
	
	private static long  counter = 0;
	private static String path;
	
	/**
	 * Set the output
	 * @param newpath New ouput path
	 */
	public static void setPath(String newpath){
		path = newpath;
	}
	
	class MosesInputChannelCT extends MosesInputChannel {
		
		//
		//  override: InputChannel
		//
		public void commit() {
			if (tokensRead > 0) {
				if (lastReader != null) {
					ctb.beginDependency(lastReader);
					ctb.setDependencyAttribute("kind", "port");
					ctb.setDependencyAttribute("port", name);
					ctb.setDependencyAttribute("port-type", "input");
					ctb.setDependencyAttribute("ntokens", new Integer(tokensRead));
					ctb.endDependency();
				}
				lastReader = ctb.currentStep();
				Object step = tokenSteps.get(0);
				int n = 1;
				for (int i = 1; i < tokensRead; i++) {
					Object s = tokenSteps.get(i);
					if (step.equals(s)) {
						n += 1;
					} else {
						ctb.beginDependency(step);
						ctb.setDependencyAttribute("kind", "token");
						ctb.setDependencyAttribute("port", name);
						ctb.setDependencyAttribute("ntokens", new Integer(n));
						ctb.endDependency();	
						step = s;
						n = 1;
					}
				}
				ctb.beginDependency(step);
				ctb.setDependencyAttribute("kind", "token");
				ctb.setDependencyAttribute("port", name);
				ctb.setDependencyAttribute("ntokens", new Integer(n));
				ctb.endDependency();
				tokenSteps.deleteRange(0, tokensRead);
			}
			super.commit();
		}
								

		
		public void	message(Object msg, double time, Object source){
			super.message(msg,time,source);
			Object step = ctb.currentStep();
			tokenSteps.add(step);
		}

		//
		//  Ctor
		//
		MosesInputChannelCT(String name) {
			super(name);
			tokenSteps = new MyArrayList();
		}
				
		private MyArrayList  tokenSteps;
		private Object       lastReader = null;
	}
	
	
	protected class MosesOutputChannelCT extends MosesOutputChannel {
				
		//
		//  override: MosesOutputChannel
		//
		public boolean  flush() {
			Object step = ctb.currentStep();
			if (lastWriter != null && step != null) {
				ctb.beginDependency(lastWriter);
				ctb.setDependencyAttribute("kind", "port");
				ctb.setDependencyAttribute("port", name);
				ctb.setDependencyAttribute("port-type", "output");
				ctb.endDependency();
			}
			lastWriter = step;
			return super.flush();
		}
		
		//
		//  Ctor
		//
		MosesOutputChannelCT(String name) {
			super(name);
		}
		
		//
		//  data
		//
		private Object lastWriter = null;
	}

}
