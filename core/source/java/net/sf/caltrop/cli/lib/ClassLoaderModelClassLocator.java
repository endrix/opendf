package net.sf.caltrop.cli.lib;

import java.io.InputStream;

import net.sf.caltrop.cli.ModelClassLocator;

public class ClassLoaderModelClassLocator implements ModelClassLocator {

	public InputStream getAsStream(String name) {
		return loader.getResourceAsStream(name);
	}
	
	public ClassLoaderModelClassLocator(ClassLoader loader) {
		this.loader = loader;
	}
	
	private ClassLoader loader;

}
