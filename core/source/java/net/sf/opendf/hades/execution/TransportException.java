package net.sf.opendf.hades.execution;

public class TransportException extends Exception {
	
	public TransportException (String message, Transport transport, Throwable cause) {
		super("Transport exception: '" + message + " ' on transport [" + transport + "].", cause);
		
		this.transport = transport;
		this.message = message;
	}
			
	private Transport transport;
	private String message;
}
