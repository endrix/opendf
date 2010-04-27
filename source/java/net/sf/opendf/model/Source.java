package net.sf.opendf.model;

import java.util.Map;

import org.w3c.dom.Node;

public interface Source {
	
	boolean isAtomic();
	
	Kind	getSourceKind();
	
	Object	getSource();
	
	Loader	getLoader();
	
	public enum Kind {JAVA, CALML, XDF, XNL};
}
