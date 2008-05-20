/**
 * 
 */
package net.sf.opendf.util.source;

import static net.sf.opendf.util.xml.Util.saxonify;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;

import net.sf.opendf.cal.util.SourceReader;
import net.sf.opendf.util.io.ClassLoaderStreamLocator;
import net.sf.opendf.util.io.StreamLocator;
import net.sf.opendf.util.xml.Util;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class DDLLoader extends XMLLoader {

	public Node load(InputStream s) {
		try {
			Node n = super.load(s);
			Node res = Util.applyTransforms(n, convertDDL);
			return res;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String extension() {
		return "ddl";
	}
	
	private static Transformer [] convertDDL;
	static {
        StreamLocator locator = new ClassLoaderStreamLocator(DDLLoader.class.getClassLoader());
		convertDDL = Util.getTransformersAsResources(new String [] {
								"net.sf.opendf.transforms.DDL2XDF.xslt"}, locator);
	}
}