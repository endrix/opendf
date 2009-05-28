/**
 * 
 */
package net.sf.opendf.util.source;

import java.io.InputStream;

import org.w3c.dom.Node;
import net.sf.opendf.util.io.SourceStream;

public interface SourceLoader {
	String 	extension();
	Node  	load(SourceStream s);
}