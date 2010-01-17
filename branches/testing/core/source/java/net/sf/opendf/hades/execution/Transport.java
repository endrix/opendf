package net.sf.opendf.hades.execution;

public interface Transport {

	public void sendPacket(Object p) throws TransportException;
	
	public Object receivePacket() throws TransportException;
	
	public void  createConnection() throws TransportException;
		

}
