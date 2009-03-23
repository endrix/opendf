package net.sf.opendf.util.io;

import java.io.InputStream;

public interface StreamLocator {
	
	InputStream  getAsStream(String name) ;
}
