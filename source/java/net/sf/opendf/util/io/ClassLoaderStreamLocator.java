package net.sf.opendf.util.io;

import java.io.InputStream;

import net.sf.opendf.util.logging.Logging;

public class ClassLoaderStreamLocator implements StreamLocator {

	public InputStream getAsStream(String name) {
		InputStream s = loader.getResourceAsStream(name);
		Logging.dbg().info("ClassLoaderStreamLocator: Locating '" + name + "' " + (s == null ? "failed" : "succeeded") + ".");
		return s;
	}
	
	public ClassLoaderStreamLocator(ClassLoader loader) {
		this.loader = loader;
	}
	
	private ClassLoader loader;

}
