package net.sf.opendf.execution.transport;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;

public class SocketTransportClient extends AbstractSocketTransport implements Transport {

	@Override
	protected Socket  createSocket() throws TransportException {
		try {
			return new Socket(host, port);
		}
		catch (IOException e) {
			throw new TransportException("Error creating connection to host '" + host + "' on port " + port + ".", this, e);			
		}		
	}
	
	public SocketTransportClient(String host, int port) {
		this.host = host;
		this.port = port;
		
	}
	
	private String 	host;
	private int		port;
}
