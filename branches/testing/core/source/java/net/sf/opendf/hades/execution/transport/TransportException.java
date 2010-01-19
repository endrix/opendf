package net.sf.opendf.hades.execution.transport;

public class TransportException extends Exception {
	
	public TransportServer  getTransport() {
		return transport;
	}
	
	
	public TransportException (String message, TransportServer transport, Throwable cause) {
		super("Transport exception: '" + message + " ' on transport [" + transport + "].", cause);
		
		this.transport = transport;
		this.message = message;
	}
			
	private TransportServer transport;
	private String message;
}
