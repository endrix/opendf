/**
 * 
 */
package net.sf.opendf.nl.util;

import static net.sf.opendf.util.xml.Util.saxonify;

import java.io.InputStream;

import net.sf.opendf.util.source.SourceLoader;

import org.w3c.dom.Node;


public class NLLoader implements SourceLoader {

	public String extension() {
		return "nl";
	}

	public Node load(InputStream s) {
		return saxonify(Lib.readNL(s));
	}		
}