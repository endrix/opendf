/**
 * 
 */
package net.sf.caltrop.xslt.util.loader;

import static net.sf.caltrop.util.Util.saxonify;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import net.sf.caltrop.cal.interpreter.util.SourceReader;
import net.sf.caltrop.xslt.util.SourceLoader;

import org.w3c.dom.Document;
import org.w3c.dom.Node;


public class CalLoader implements SourceLoader {

	public String extension() {
		return "cal";
	}

	public Node load(InputStream s) {
		Reader r = new InputStreamReader(s);
		try {
			Document doc = SourceReader.parseActor(r);
			return saxonify(doc);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}		
}