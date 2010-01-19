package net.sf.opendf.execution.harness;

import java.io.IOException;

public interface InputSource {

	void open() throws IOException;
	void close();
	
	boolean hasToken();
	Object  nextToken();
	double  nextInputTime();
	
}
