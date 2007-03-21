package net.sf.caltrop.cli;

import java.io.InputStream;

public interface ModelClassLocator {
	
	InputStream  getAsStream(String name) ;
}
