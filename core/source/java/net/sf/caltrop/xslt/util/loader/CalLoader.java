/**
 * 
 */
package net.sf.caltrop.xslt.util.loader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import net.sf.caltrop.cal.interpreter.util.SourceReader;
import net.sf.caltrop.util.Util;
import net.sf.caltrop.xslt.util.SourceLoader;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import static net.sf.caltrop.util.Util.saxonify;


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