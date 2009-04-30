/**
 * 
 */
package net.sf.opendf.util.source;

import static net.sf.opendf.util.xml.Util.saxonify;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.opendf.util.io.SourceStream;

import org.w3c.dom.Document;
import org.w3c.dom.Node;


public abstract class XMLLoader implements SourceLoader {
	abstract public String extension();
	
	public Node load(SourceStream s) {
		Document doc;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(s.getInputStream());
			return saxonify(doc);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}