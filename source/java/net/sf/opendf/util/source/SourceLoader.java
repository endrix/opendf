/**
 * 
 */
package net.sf.opendf.util.source;

import net.sf.opendf.util.io.SourceStream;

import org.w3c.dom.Node;

public interface SourceLoader {
	String 	extension();
	Node  	load(SourceStream s);
}
