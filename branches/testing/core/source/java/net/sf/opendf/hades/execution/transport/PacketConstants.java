package net.sf.opendf.hades.execution.transport;

/**
 * These are the constants used in the construction of JSON-formatted packets. These are the packets formats:
 * 
 * Command ::= {
 * 				":packet" = "command",
 * 				":command" = "initialize" | "step" | "run" | "runUntil" | "terminate",
 * 				":time" = <number>   // for initialize and runUntil packets 
 * }
 * 
 * 
 * 
 * 
 * @author jwj
 *
 */

public interface PacketConstants {

	
	public final static String fieldPacket = ":packet";
	public final static String fieldCommand = ":command";
	public final static String fieldData = ":data";
	public final static String fieldStatus = ":status";
	public final static String fieldMessage = ":message";
	public final static String fieldActive = ":active";
	public final static String fieldNSteps = ":nSteps";
	public final static String fieldResponse = ":response";
	public final static String fieldPort = ":port";
	public final static String fieldTime = ":time";
	public final static String fieldNextEventTime = ":nextEventTime";
	public final static String fieldValue = ":value";
	public final static String fieldType = ":type";

	
	public final static String packetResponse = "response";
	public final static String packetStatus = "status";
	public final static String packetToken = "token";
	public final static String packetCommand = "command";
	public final static String packetEvent = "event";

	
	public final static String commandInitialize = "initialize";
	public final static String commandStep = "step";
	public final static String commandRun = "run";
	public final static String commandRunUntil = "runUntil";
	public final static String commandStats = "stats";
	public final static String commandTerminate = "terminate";
		
	public final static String eventTerminate = "terminate";
	
	public final static String statusAckToken = "ackToken";
	public final static String statusError = "error";
	public final static String statusWarning = "warning";
	public final static String statusTerminated = "terminated";
	
	public final static String responseInitialized = "initialized";
	public final static String responseTerminated = "terminated";
	public final static String responseDone = "done";
	
}
