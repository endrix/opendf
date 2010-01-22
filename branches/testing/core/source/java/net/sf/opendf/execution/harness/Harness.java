package net.sf.opendf.execution.harness;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import net.sf.opendf.execution.harness.io.BytesTokenSink;
import net.sf.opendf.execution.harness.io.BytesTokenSource;
import net.sf.opendf.execution.harness.io.TimedJSONTokenSink;
import net.sf.opendf.execution.harness.io.TimedJSONTokenSource;
import net.sf.opendf.execution.harness.io.UntimedJSONTokenSink;
import net.sf.opendf.execution.harness.io.UntimedJSONTokenSource;
import net.sf.opendf.execution.transport.Transport;
import net.sf.opendf.execution.transport.TransportException;

import static net.sf.opendf.execution.transport.PacketConstants.*;
import static net.sf.opendf.util.json.JSONLib.*;

public class Harness {
	
	
	public void  execute1() {
		Status s = getStatus();
		
		
		
	}

	
	public Harness(HarnessConfiguration hc) {
		
		config = hc;
	}

	
	
	private void  outputToken(String port, Token token) {
		try {
			if (sinks.get(port).token(token)) {
				// FIXME --- an error occurred
			}
		}
		catch (Exception e)  {}  // FIXME
	}
	
	private Status  getStatus() {
		Map m = new HashMap();
		m.put(fieldPacket, packetCommand);
		m.put(fieldCommand, commandStatus);
		Object r = sendCommand(m);
		if (r instanceof Map && responseStatus.equals(((Map)r).get(fieldResponse))) {
			Map sr = (Map)r;
			Status s = new Status();
			s.hasEvent = getBoolean(m.get(fieldActive), false);
			s.currentTime = getDouble(m.get(fieldTime), -1);
			s.nextEventTime = getDouble(m.get(fieldNextEventTime), -1);
			s.nSteps = getLong(m.get(fieldNSteps), -1);
			
			return s;
		} else
			throw new RuntimeException("Cannot get status packet.");
	}
	
	class Status {
		boolean   	hasEvent;
		double		currentTime;
		double		nextEventTime;
		long		nSteps;
	}
	
	
	private void  statusMessage(Object message) {
		try {
			write(message, statusLog);
		}
		catch (IOException e) {} // FIXME
	}
	
	private Map<String, TokenSink>	sinks;
	
	private Transport				transport;
	private HarnessConfiguration 	config;
	
	private Writer					commandLog;
	private Writer					responseLog;
	private Writer					statusLog;

	
	private Object  sendCommand(Object cmdPacket) {
		if (commandBusy) 
			throw new RuntimeException("Cannot overlap command execution.");
		commandBusy = true;
		response = null;
		try {
			transport.sendPacket(cmdPacket);
			while (response == null) {
				try {
					wait(100);
				}
				catch (InterruptedException e) {}  // FIXME
			}
			Object r = response;
			try {
				write(cmdPacket, commandLog);
				write(r, responseLog);
			}
			catch (IOException e) {}  // FIXME
			return r;
		}
		catch (TransportException e) {
			throw new RuntimeException("Error sending command packet.", e);
		}
		finally {
			response = null;
			commandBusy = false;			
		}
	}
	
	private Object 					response = null;
	private boolean					commandBusy = false;
	
	private boolean 				receiverRunning;

	
	static boolean  getBoolean(Object v, boolean defaultValue) {
		try {
			return ((Boolean)v).booleanValue();
		}
		catch (Exception e) {
			return defaultValue;
		}
	}

	static long  getLong(Object v, long defaultValue) {
		try {
			return ((Number)v).longValue();
		}
		catch (Exception e) {
			return defaultValue;
		}
	}

	static double  getDouble(Object v, double defaultValue) {
		try {
			return ((Number)v).doubleValue();
		}
		catch (Exception e) {
			return defaultValue;
		}
	}

	private void  receiverLoop() {
		
		while (receiverRunning) {
			Object packet = null;
			try {
				packet = transport.receivePacket();
			}
			catch (TransportException e) {}    // FIXME
			
			if (packet instanceof Map) {
				Map m = (Map) packet;
				String tPacket = (String)m.get(fieldPacket);
				
				if (packetResponse.equals(tPacket)) {					
					response = tPacket;
					notifyAll();
				} else if (packetToken.equals(tPacket)) {
					outputToken((String)m.get(fieldPort), new Token(getDouble(m.get(fieldTime), -1), m.get(fieldData), getLong(m.get(fieldStep), -1)));
				} else if (packetStatus.equals(tPacket)) {
					statusMessage(m);
				} else {
					
				}
			}
		}
	}

	
	static private Map<String, TokenSource.Factory> sourceFormats;
	static {
		sourceFormats = new HashMap<String, TokenSource.Factory>();
		
		sourceFormats.put("timedJSON", new TimedJSONTokenSource.Factory());
		sourceFormats.put("untimedJSON", new UntimedJSONTokenSource.Factory());
		sourceFormats.put("bytes", new BytesTokenSource.Factory());
	}

	static private Map<String, TokenSink.Factory> sinkFormats;
	static {
		sinkFormats = new HashMap<String, TokenSink.Factory>();
		
		sinkFormats.put("timedJSON", new TimedJSONTokenSink.Factory());
		sinkFormats.put("untimedJSON", new UntimedJSONTokenSink.Factory());
		sinkFormats.put("bytes", new BytesTokenSink.Factory());
	}
	
	
}
