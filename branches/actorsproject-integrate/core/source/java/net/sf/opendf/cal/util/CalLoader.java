/**
 * 
 */
package net.sf.opendf.cal.util;

import static net.sf.opendf.util.xml.Util.saxonify;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import net.sf.opendf.util.source.SourceLoader;
import net.sf.opendf.util.io.SourceStream;

import org.w3c.dom.Document;
import org.w3c.dom.Node;


public class CalLoader implements SourceLoader {

	public String extension() {
		return "cal";
	}

	public Node load(SourceStream s) {
		Reader r = new InputStreamReader(s.getInputStream());
		try {
			Node doc = SourceReader.parseActor(r);
			return saxonify(doc, s.getFilename());
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}		
}
