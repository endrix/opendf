package net.sf.opendf.hades.execution;

import net.sf.opendf.hades.des.DiscreteEventComponent;
import net.sf.opendf.hades.des.schedule.SimpleScheduler;
import net.sf.opendf.hades.simulation.SequentialSimulatorCallback;


public class ExecutionController {
	
	public void initialize(Transport transport) {}
	
	public void step() {}
	
	public void run() {}
	
	public void runUntil(double time, boolean inclusive) {}
	

	public void token(String port, double time, Object value) {}
	
	public ExecutionController(double t, DiscreteEventComponent dec, ClassLoader classLoader, Transport transport) {
		this.dec = dec;
		scheduler = null;
		this.transport = transport;
	
		scheduler = new SimpleScheduler(classLoader);
		
		initialize(t);
	}
	
	
	
	private void initialize(double t) {
		
		scheduler.initialize();
		dec.initializeState(t, scheduler);
	}

	
	private DiscreteEventComponent	dec;
	private SimpleScheduler 		scheduler;
	private Transport 				transport;
	
	
	
	public final static String commandInitialize = "initialize";
	public final static String commandStep = "step";
	public final static String commandRun = "run";
	public final static String commandRunUntil = "runUntil";
	public final static String commandStats = "stats";
	
	public final static String eventTerminate = "terminate";
	
	public final static String statusAckToken = "ackToken";
	public final static String statusError = "error";
	public final static String statusWarning = "warning";
		
	
	public final static String fieldPacket = ":packet";
	public final static String fieldData = ":data";
	public final static String fieldCommand = ":command";
	public final static String fieldStatus = ":status";
	public final static String fieldMessage = ":message";
	
	
	public final static String packetResponse = "response";
	public final static String packetStatus = "status";
	public final static String packetToken = "token";
	
	public final static String packetCommand = "command";
	public final static String packetEvent = "event";



}
