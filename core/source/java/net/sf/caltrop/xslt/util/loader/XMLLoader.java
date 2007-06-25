/**
 * 
 */
package net.sf.caltrop.xslt.util.loader;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;


import net.sf.caltrop.xslt.util.SourceLoader;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import static net.sf.caltrop.util.Util.saxonify;


public abstract class XMLLoader implements SourceLoader {
	abstract public String extension();
	
	public Node load(InputStream s) {
		Document doc;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(s);
			return saxonify(doc);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}