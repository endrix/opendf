package net.sf.opendf.execution.transport;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;

import net.sf.opendf.util.json.JSONLib;

public abstract class AbstractSocketTransport extends AbstractTransport {

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
		socket = createSocket();
		try {
			reader = new InputStreamReader(socket.getInputStream());	// consider buffering
			writer = new OutputStreamWriter(socket.getOutputStream());  // consider buffering
		}
		catch (IOException e) {
			throw new TransportException("Cannot create input/output streams from socket.", this, e);			
		}
	}
	
	abstract protected Socket  createSocket() throws TransportException;
	
	
	protected AbstractSocketTransport() {}

	
	private Socket 			socket;
	
	private Reader			reader;
	private Writer			writer;

	
}
