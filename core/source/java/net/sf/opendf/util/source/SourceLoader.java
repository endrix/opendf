/**
 * 
 */
package net.sf.caltrop.util.source;

import java.io.InputStream;

import org.w3c.dom.Node;

public interface SourceLoader {
	String 	extension();
	Node  	load(InputStream s);
}