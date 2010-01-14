package net.sf.opendf.hades.execution;

public interface Transport {

	public void response(Object data) throws TransportException;

	public void status(Object data) throws TransportException;

	public void token(String port, double time, Object value);

	
	public interface Callback {
		
		void command(String kind, Object data);
		void token(String port, double time, Object value);
	}
	
	public interface Factory {
		Transport  create(Callback cb) throws TransportException;
	}

}
