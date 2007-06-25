/**
 * 
 */
package net.sf.caltrop.xslt.util.loader;

import java.io.InputStream;

import net.sf.caltrop.nl.util.Lib;
import net.sf.caltrop.xslt.util.SourceLoader;

import org.w3c.dom.Node;
import static net.sf.caltrop.util.Util.saxonify;


public class NLLoader implements SourceLoader {

	public String extension() {
		return "nl";
	}

	public Node load(InputStream s) {
		return saxonify(Lib.readNL(s));
	}		
}