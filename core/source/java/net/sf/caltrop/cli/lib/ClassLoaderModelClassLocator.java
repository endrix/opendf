package net.sf.caltrop.cli.lib;

import java.io.InputStream;

import net.sf.caltrop.cli.ModelClassLocator;
import net.sf.caltrop.util.Logging;

public class ClassLoaderModelClassLocator implements ModelClassLocator {

	public InputStream getAsStream(String name) {
		InputStream s = loader.getResourceAsStream(name);
		Logging.dbg().info("ClassLoaderModelClassLocator: Locating '" + name + "' " + (s == null ? "failed" : "succeeded") + ".");
		return s;
	}
	
	public ClassLoaderModelClassLocator(ClassLoader loader) {
		this.loader = loader;
	}
	
	private ClassLoader loader;

}
