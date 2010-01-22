package net.sf.opendf.execution.transport;

public interface Transport {

	public void sendPacket(Object p) throws TransportException;
	
	public Object receivePacket() throws TransportException;
	
	public void  createConnection() throws TransportException;
	
	public boolean  createConnection(int retries, long retryDelay);
		

}
