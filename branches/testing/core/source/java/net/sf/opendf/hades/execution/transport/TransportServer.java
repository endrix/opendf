package net.sf.opendf.hades.execution.transport;

public interface TransportServer {

	public void sendPacket(Object p) throws TransportException;
	
	public Object receivePacket() throws TransportException;
	
	public void  createConnection() throws TransportException;
		

}
