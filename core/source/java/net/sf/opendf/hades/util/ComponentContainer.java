/* 
BEGINCOPYRIGHT X,ETH
	
	Copyright (c) 1999, Computer Engineering and Communication Networks Lab (TIK)
 	                    Swiss Federal Institute of Technology (ETH) Zurich, Switzerland	
	Copyright (c) 2007, Xilinx Inc.
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


package net.sf.opendf.hades.util;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.opendf.hades.des.AbstractDiscreteEventComponent;
import net.sf.opendf.hades.des.AbstractMessageListener;
import net.sf.opendf.hades.des.BasicMessageProducer;
import net.sf.opendf.hades.des.DelegationMessageListener;
import net.sf.opendf.hades.des.DiscreteEventComponent;
import net.sf.opendf.hades.des.EventProcessor;
import net.sf.opendf.hades.des.MessageListener;
import net.sf.opendf.hades.des.MessageProducer;
import net.sf.opendf.hades.des.schedule.PostfireHandler;
import net.sf.opendf.hades.des.schedule.Scheduler;
import net.sf.opendf.hades.des.schedule.SimpleScheduler;
import net.sf.opendf.hades.des.schedule.SimulationFinalizer;

public class ComponentContainer
extends AbstractDiscreteEventComponent
implements Scheduler, EventProcessor {
	
	//
	//  EventProcessor
	//
	
	public boolean processEvent(double tm) {
		return execute() == Scheduler.STEP;
	}
	
	public boolean   isWeakEvent() {
		return isNextWeak();
	}
	
	
	//
	//  Scheduler
	//
	
	public void     initialize() {
		local.initialize();
	}
	
	public boolean  hasEvent() {
		return local.hasEvent();
	}
	
	public boolean  isNextWeak() {
		return local.isNextWeak();
	}
	
	public double   nextEventTime() {
		return local.nextEventTime();
	}
	
	public double   currentTime() {
		return local.currentTime();
	}
	
	public long currentEventCount() {
		return local.currentEventCount();
	}
	
	public long currentStrongEventCount() {
		return local.currentStrongEventCount();
	}
	
	public int execute() {
		if (isExecuting)
			throw new RuntimeException("Cannot execute reentrantly.");
		if (!local.hasEvent())
			return Scheduler.NOSTEP; // unexpected situation
		double tm = local.nextEventTime();
		
		isExecuting = true;
		
		int ret = local.execute();
		
		isExecuting = false;
		
		postfire();
		
		if (!local.hasEvent())
			outDeadlock.notifyMessage(null, tm, this);
		else
			scheduler.schedule(local.nextEventTime(), this);
		
		return ret;
	}
	
	public void     schedule(double tm, EventProcessor ep) {
		schedule(tm, 0, ep);
	}
	
	public void     schedule(double tm, double precedence, EventProcessor ep) {
		
		double  oldTm = local.nextEventTime();
		boolean alive = local.hasEvent();
		
		local.schedule(tm, precedence, ep);
		
		if (!isExecuting) {
			if (!alive)
				outAlive.notifyMessage(null, tm, this);
				
			double newTm = local.nextEventTime();
			if (newTm != oldTm)
				scheduler.schedule(newTm, precedence, this);
		}
	}
	
	public void     unschedule(EventProcessor ep) {
		
		double t = local.nextEventTime();
		boolean alive = local.hasEvent();
		
		local.unschedule(ep);
		
		if (!isExecuting) {
			boolean stillAlive = local.hasEvent();
			if (alive && !stillAlive)
				outDeadlock.notifyMessage(null, scheduler.currentTime(), this);
			
			if (stillAlive) {
				double newTm = local.nextEventTime();
				if (t != newTm)
					scheduler.schedule(newTm, this);
			} else {
				if (alive)
					scheduler.unschedule(this);
			}
		}
	}
	
	public void  registerSimulationFinalizer(SimulationFinalizer sf) {
		simulationFinalizers.add(sf);
	}
	
	public void  unregisterSimulationFinalizer(SimulationFinalizer sf) {
		simulationFinalizers.remove(sf);
	}
	
	public void  finalizeSimulation() {
		for (Iterator i = simulationFinalizers.iterator(); i.hasNext(); ) {
			SimulationFinalizer sf = (SimulationFinalizer)i.next();
			sf.finalizeSimulation();
		}
	}
	
	public void addPostfireHandler(PostfireHandler ph) {
		postfireHandlers.add(ph);
	}

	public void removePostfireHandler(PostfireHandler ph) {
		postfireHandlers.remove(ph);
	}

	protected void postfire() {
		if (postfireHandlers.isEmpty())
			return;
	
		for (Iterator i = postfireHandlers.iterator(); i.hasNext(); ) {
			PostfireHandler ph = (PostfireHandler)i.next();
			ph.postfire();
		}
		postfireHandlers.clear();
	}
	
	public Object getProperty(Object key) {
		if (properties == null)
			return null;
		return properties.get(key);
	}
	
	public Object setProperty(Object key,Object value) {
		if (properties == null)
			properties = new HashMap();
		return properties.put(key, value);
	}
	
	public ClassLoader getClassLoader() {
		return local.getClassLoader();
	}
	

	private Map  properties = null;

	private List simulationFinalizers = new ArrayList();

	private Set postfireHandlers = new HashSet();

	
	
	//
	//  DEC
	//
	
	public void initializeState(double t, Scheduler scheduler) {
		this.scheduler = scheduler;
		
		local = new SimpleScheduler();
		local.initialize();
		
		isExecuting = false;
		
		dec.initializeState(t, this);
	}
	
	public boolean isInitialized() { return scheduler != null; }
	
	
	//
	//  ctor
	//
	
	
	public ComponentContainer(DiscreteEventComponent dec) {
		this.dec = dec;
		
		for (Iterator i = dec.getInputConnectors().keySet().iterator(); i.hasNext(); ) {
			String nm = (String)i.next();
			MessageListener ml = dec.getInputConnectors().getConnector(nm);
			inputs.addConnector(nm, new DelegationMessageListener(ml));
		}
		
		for (Iterator i = dec.getOutputConnectors().keySet().iterator(); i.hasNext(); ) {
			String nm = (String)i.next();
			MessageProducer mp = dec.getOutputConnectors().getConnector(nm);
			
			MessageProducer producer = new BasicMessageProducer();
			outputs.addConnector(nm, producer);
			mp.addMessageListener(new OutputListener(producer));
		}
		
		outDeadlock = new BasicMessageProducer();
		outputs.addConnector("Deadlocked", outDeadlock);
		outAlive = new BasicMessageProducer();
		outputs.addConnector("Alive", outAlive);
	}
	
	//
	// I/O
	//
	
	private static class OutputListener extends AbstractMessageListener {
		
		private MessageProducer	producer;
		
		public void message(Object msg, double time, Object source) {
			producer.notifyMessage(msg, time, source);
		}
		
		OutputListener(MessageProducer mp) {
			producer = mp;
		}
	}
	
	
	//
	//  data
	//
	
	private DiscreteEventComponent   dec;
	private Scheduler                local;
	private Scheduler                scheduler;
	
	private MessageProducer          outDeadlock;
	private MessageProducer          outAlive;
	
	private boolean                  isExecuting;
}



