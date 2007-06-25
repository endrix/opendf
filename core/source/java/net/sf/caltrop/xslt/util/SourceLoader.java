/**
 * 
 */
package net.sf.caltrop.xslt.util;

import java.io.InputStream;

import org.w3c.dom.Node;

public interface SourceLoader {
	String 	extension();
	Node  	load(InputStream s);
}