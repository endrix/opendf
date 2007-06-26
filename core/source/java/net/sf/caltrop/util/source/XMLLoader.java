/**
 * 
 */
package net.sf.caltrop.util.source;

import static net.sf.caltrop.util.xml.Util.saxonify;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;


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