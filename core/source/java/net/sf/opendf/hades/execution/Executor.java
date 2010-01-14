package net.sf.opendf.hades.execution;

import java.util.Iterator;
import java.util.Map;

import net.sf.opendf.hades.des.DiscreteEventComponent;
import net.sf.opendf.hades.des.schedule.SimpleScheduler;
import net.sf.opendf.hades.simulation.SequentialSimulatorCallback;

public class Executor {
	
	
	private DiscreteEventComponent dec;
	private SimpleScheduler scheduler;
	
	public Executor(double t, DiscreteEventComponent dec, ClassLoader classLoader) {
		this.dec = dec;
		scheduler = null;
	
		scheduler = new SimpleScheduler(classLoader);
		
		initialize(t);
	}
	
	
	private void initialize(double t) {
		
		scheduler.initialize();
		dec.initializeState(t, scheduler);
	}
}
