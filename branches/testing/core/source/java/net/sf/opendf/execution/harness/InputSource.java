package net.sf.opendf.execution.harness;

import java.io.InputStream;

public interface InputSource {

	void close();
	
	boolean hasToken();
	Object  nextToken();
	double  nextInputTime();

	interface Factory {
		InputSource  create(InputStream s);
	}
}
