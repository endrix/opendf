package net.sf.caltrop.util.io;

import java.io.InputStream;

public interface StreamLocator {
	
	InputStream  getAsStream(String name) ;
}
