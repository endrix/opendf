/**
 * 
 */
package net.sf.opendf.nl.util;

import static net.sf.opendf.util.xml.Util.createXML;
import static net.sf.opendf.util.xml.Util.saxonify;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.logging.Level;

import net.sf.opendf.cal.util.SourceReader;
import net.sf.opendf.util.io.SourceStream;
import net.sf.opendf.util.logging.Logging;
import net.sf.opendf.util.source.SourceLoader;

import org.w3c.dom.Node;


public class NLLoader implements SourceLoader {

	public String extension() {
		return "nl";
	}

	public Node load(SourceStream s) {
		//return saxonify(Lib.readNL(s.getInputStream()), s.getFilename());
		Node n = saxonify(Lib.readNL(s.getInputStream()), s.getFilename());
        String xmlRes = createXML(n);
        try{
          PrintWriter upw = new PrintWriter(new FileOutputStream("/Users/johan/Documents/CAL/work/MPEG4_SP_Decoder/XDF_sample.xml"));
          upw.print(xmlRes);
          upw.flush();
        }  catch (Exception e){
        	System.out.println("Could not output XM: debug sample");
       	}
		return n;
	}		
}