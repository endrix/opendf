package net.sf.opendf.hades.execution.transport;

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

import net.sf.opendf.hades.execution.controller.ExecutionController;
import net.sf.opendf.util.json.JSONLib;

public class SocketTransportServer implements TransportServer {

	@Override
	public void sendPacket(Object packet) throws TransportException {
		try {
			JSONLib.write(packet, writer);
		}
		catch (IOException e) {
			throw new TransportException("Error writing packet.", this, e);			
		}
	}

	@Override
	public Object receivePacket() throws TransportException {
		try {
			return JSONLib.read(reader);
		}
		catch (IOException e) {
			throw new TransportException("Error reading packet.", this, e);			
		}
	}

	@Override
	public void createConnection() throws TransportException {
		try {
			socket = serverSocket.accept();
			reader = new InputStreamReader(socket.getInputStream());	// consider buffering
			writer = new OutputStreamWriter(socket.getOutputStream());  // consider buffering
		}
		catch (IOException e) {
			throw new TransportException("Error accepting connection on server socket " + socketNo + ".", this, e);			
		}
	}
	
	protected SocketTransportServer(int socket) throws TransportException {
		this.socketNo = socket;
		try {
			serverSocket = new ServerSocket(socket);
		}
		catch (IOException e) {
			throw new TransportException("Error creating server socket at " + socketNo + ".", this, e);
		}
	}
	
	private Socket 			socket;
	private ServerSocket	serverSocket;
	private int  			socketNo;
	
	private Reader			reader;
	private Writer			writer;
	
}
