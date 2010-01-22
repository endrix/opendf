package net.sf.opendf.execution.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import net.sf.opendf.execution.controller.ExecutionController;
import net.sf.opendf.util.json.JSONLib;

public class SocketTransportServer extends AbstractSocketTransport implements Transport {

	@Override
	protected Socket createSocket() throws TransportException {
		try {
			return serverSocket.accept();
		}
		catch (IOException e) {
			throw new TransportException("Error accepting connection on server socket " + socketNo + ".", this, e);			
		}
	}
	
	public SocketTransportServer(int socket) throws TransportException {
		super();
		this.socketNo = socket;
		try {
			serverSocket = new ServerSocket(socket);
		}
		catch (IOException e) {
			throw new TransportException("Error creating server socket at " + socketNo + ".", this, e);
		}
	}
	
	private ServerSocket	serverSocket;
	private int  			socketNo;	
}
