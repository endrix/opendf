package net.sf.opendf.hades.execution.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.opendf.hades.des.DiscreteEventComponent;
import net.sf.opendf.hades.des.schedule.SimpleScheduler;
import net.sf.opendf.hades.execution.transport.PacketConstants;
import net.sf.opendf.hades.execution.transport.TransportServer;
import net.sf.opendf.hades.execution.transport.TransportException;
import net.sf.opendf.hades.simulation.SequentialSimulatorCallback;

/**
 * The ExecutionController is the top-level supervisor of a dataflow execution on a computing node. It 
 * is intended to support two related usage scenarios:
 * (1) Testing.
 * (2) Distributed execution.
 * 
 * In the case of testing, the entire system is run under a single ExecutionController. The transport is
 * used to feed test input sequences to the execution and to receive the output produced. It can also
 * be used to control the execution of the system with respect to the tokens fed into it. In that scenario,
 * the other end of the transport constitutes the "test harness", and is responsible for retrieving or
 * generating the test sequences and storing or evaluating the generated output.
 * 
 * @author jwj
 *
 */

public class ExecutionController implements PacketConstants {
	
	//  commands
	
	public Object initialize(double t) {
		
		scheduler = new SimpleScheduler(classLoader);		
		scheduler.initialize();
		dec.initializeState(t, scheduler);		

		Map m = new HashMap();
		m.put(fieldPacket, packetResponse);
		m.put(fieldResponse, responseInitialized);
		m.put(fieldActive, scheduler.hasEvent());
		m.put(fieldTime, scheduler.currentTime());
		m.put(fieldNextEventTime, scheduler.hasEvent() ? scheduler.nextEventTime() : scheduler.currentTime());
	
		return m;
	}
	
	public Object step() {
		if (scheduler == null)
			throw new RuntimeException("System has not been initialized.");

		boolean didStep = false;
		if (scheduler.hasEvent()) {
			scheduler.execute();
			didStep = true;
		}

		Map m = new HashMap();
		m.put(fieldPacket, packetResponse);
		m.put(fieldResponse, responseDone);
		m.put(fieldNSteps, didStep ? 1 : 0);
		m.put(fieldActive, scheduler.hasEvent());
		m.put(fieldTime, scheduler.currentTime());
		m.put(fieldNextEventTime, scheduler.hasEvent() ? scheduler.nextEventTime() : scheduler.currentTime());
		
		return m;
	}
	
	public Object runComplete() {
		return runUntil(Double.POSITIVE_INFINITY, true);
	}
	
	public Object runUntil(double time, boolean inclusive) {

		if (scheduler == null)
			throw new RuntimeException("System has not been initialized.");

		long nSteps = 0;

		while (scheduler.hasEvent() && (inclusive ? scheduler.nextEventTime() <= time : scheduler.nextEventTime() < time)) {
			if (asynchronousTokens)
				flushPendingTokens();
			scheduler.execute();
			nSteps += 1;
		}

		Map m = new HashMap();
		m.put(fieldPacket, packetResponse);
		m.put(fieldResponse, responseDone);
		m.put(fieldNSteps, nSteps);
		m.put(fieldActive, scheduler.hasEvent());
		m.put(fieldTime, scheduler.currentTime());
		m.put(fieldNextEventTime, scheduler.hasEvent() ? scheduler.nextEventTime() : scheduler.currentTime());

		return m;
	}
	
	public Object terminateExecution() {
		running = false;

		Map m = new HashMap();
		m.put(fieldPacket, packetResponse);
		m.put(fieldResponse, responseTerminated);

		return m;
	}

	
	//  asynchronous calls

	public void token(Token t) {
		dec.getInputConnectors().getConnector(t.port).message(t.value, t.time, null);
	}
	
	public void abort() {
		running = false;
		Map m = new HashMap();
		m.put(fieldPacket, packetStatus);
		m.put(fieldStatus, statusTerminated);
		try {
			transport.sendPacket(m);
		}
		catch (TransportException e) {}
	}

	
	//  admin calls
	
	public void  startMainLoop(boolean newThread) {

		running = true;
		if (newThread) {
			new Thread(new Runnable() {
				public void run() {
					mainLoop();
				}
			}).start();
		} else {
			mainLoop();
		}
	}
	
	public void  stopMainLoop() {
		terminateExecution();
	}
	
	public void  connect() throws TransportException {
		transport.createConnection();
	}
	
	public void  startInputThread() {
		
	}
	
	public void  stopInputThread() {
		
	}
	
	

	//  ctor

	public ExecutionController(double t, DiscreteEventComponent dec, ClassLoader classLoader, 
			                   TransportServer transport, boolean asynchronousTokens) {
		this.dec = dec;
		scheduler = null;
		this.transport = transport;
		this.classLoader = classLoader;
		this.asynchronousTokens = asynchronousTokens;
	}
	
	
	private void  mainLoop() {
		while (running) {
			while (running && command == null) {
				try {
					this.wait();
				}
				catch (InterruptedException e) {
				}
			}
			if (running) {			// this means we have a command
				assert command != null;
				flushPendingTokens();
				Object response = command.execute();
				command = null;		// do not reset command until execution complete
				try {
					transport.sendPacket(response);
				}
				catch (TransportException e) {}   // FIXME
			}
		}
	}

	synchronized public void setCommand(Command c) {
		if (command != null)
			throw new RuntimeException("Execution already busy --- cannot overlap command execution.");
		
		command = c;
		this.notifyAll();
	}
	
	synchronized public void sendInputToken(String port, double time, Object value) {
		
	}
		
	synchronized private void flushPendingTokens() {
		if (pendingTokens.isEmpty())
			return;
		
		for (Token t : pendingTokens) {
			token(t);
		}
	}
	
	private List<Token> pendingTokens = new ArrayList<Token>();	
	private Command command = null;
	private boolean  running = true;

	
	private ClassLoader				classLoader;
	private DiscreteEventComponent	dec;
	private SimpleScheduler 		scheduler;
	private TransportServer 				transport;
	private boolean					asynchronousTokens;
	
	private InputLoop				inputLoop;
	
	
	interface Command {
		Object execute();
	}
	
	static class Token {
		public String port;
		public double time;
		public Object value;
		
		public Token(String port, double time, Object value) {
			this.port = port;
			this.time = time;
			this.value = value;
		}			
	}

	class InputLoop implements Runnable {

		@Override
		public void run() {
			while (running) {
				Object v = null;
				try {
					v = transport.receivePacket();
				}
				catch (TransportException e) {} // FIXME
				
				if (v instanceof Map) {
					final Map m = (Map)v;
					
					String tPacket = (String)m.get(fieldPacket);
					if (packetCommand.equals(tPacket)) {
						String cmd = (String)m.get(fieldCommand);
						if (commandInitialize.equals(cmd)) {
							final double tm = getDouble(m, fieldTime);
							setCommand(new Command() {
								@Override
								public Object execute() {
									return initialize(tm);
								}
							});
						} else if (commandRun.equals(cmd)) {
							setCommand(new Command() {
								@Override
								public Object execute() {
									return runComplete();
								}
							});							
						} else if (commandRunUntil.equals(cmd)) {
							final double tm = getDouble(m, fieldTime);
							setCommand(new Command() {
								@Override
								public Object execute() {
									return runUntil(tm, false);
								}
							});							
						} else if (commandStep.equals(cmd)) {
							setCommand(new Command() {
								@Override
								public Object execute() {
									return step();
								}
							});							
						} else if (commandTerminate.equals(cmd)) {
							setCommand(new Command() {
								@Override
								public Object execute() {
									return terminateExecution();
								}
							});							
						}
						
					} else if (packetToken.equals(tPacket)) {
						String port = (String)m.get(fieldPort);
						double time = ((Number)m.get(fieldTime)).doubleValue();
						Object value = collectionToValue(m.get(fieldValue), m.get(fieldType));
						sendInputToken(port, time, value);
					} else {
						// FIXME
					}
				}
			}
		}
		
		public void terminate() {
			running = false;
		}
		
		private boolean running = true;
		
	}


	/**
	 * Convert a data structure from its "packetized" form (which is based on basic types, lists, and maps) to a proper "internal" 
	 * data structure that can be used by the interpreter.
	 * 
	 * @param a  The data structure in packetized form.
	 * @param type  The type of the data structure (also in packetized form).
	 * @return The data structure in its internal form.
	 */

	static Object collectionToValue(Object a, Object type) {
		return a;			// FIXME
	}
		
	static double getDouble(Map m, Object k) {
		return ((Number)m.get(k)).doubleValue();
	}

	static String getString(Map m, Object k) {
		return (String)m.get(k);
	}

	static Object getValue(Map m, Object k, Object tk) {
		return collectionToValue(m.get(k), m.get(tk));
	}

}
