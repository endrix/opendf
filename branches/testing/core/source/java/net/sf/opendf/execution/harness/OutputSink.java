package net.sf.opendf.execution.harness;

import java.io.OutputStream;
import java.net.URL;

public interface OutputSink {
	
	void  open();
	void  close();
	
	void  token(double time, Object value);
	
	interface Factory {
		OutputSink  create(OutputStream s);
	}
}
