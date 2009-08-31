package net.sf.opendf.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import net.sf.opendf.cal.interpreter.util.ASTFactory;
import net.sf.opendf.cal.util.SourceReader;
import net.sf.opendf.util.logging.Logging;
import net.sf.opendf.util.source.MultiErrorException;
import net.sf.opendf.util.xml.Util;
import net.sf.opendf.util.exception.ReportingExceptionHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class Cal2CalML {

	public static void main (String [] args)
	{

		for (int i = 0; i < args.length; i++)
		{
			files.add(args[i]);
		}

		try
		{
			doCompiling();
		}
		catch (Exception e) // in 'main'
		{
			(new ReportingExceptionHandler()).process(e);
			System.exit(-1);
		}
	}

	private static void doCompiling () throws MultiErrorException, IOException, ParserConfigurationException, SAXException, TransformerException
	{
		for (String s : files) {
			String fn = s.trim();
			String baseName;

			Logging.user().info("Translating " + fn + ": ");

			if (fn.endsWith(suffixCAL)) {	
				baseName = fn.substring(0, fn.length() - suffixCAL.length());
			} else {
				baseName = fn;
			}
			
			File inFile = new File(fn);
			if (!inFile.exists()) {
				throw new RuntimeException("Cannot locate source file: " + fn);
			}

			Logging.user().fine("read ");
			InputStream is = new FileInputStream(inFile);

			Node doc = SourceReader.parseActor(new InputStreamReader(is));
			is.close();

			Logging.user().fine("transform ");
			String result = Util.createXML(doc);

			Logging.user().fine("write ");
			OutputStream os = new FileOutputStream(baseName + suffixCALML);
			PrintWriter pw = new PrintWriter(os);
			pw.println(result);
			pw.close();
			Logging.user().info("done.");
		}		
	}

	static private String  destinationDir = null;
	static private List<String> files = new ArrayList<String>();

	final static String suffixCAL = ".cal";
	final static String suffixCALML = ".calml";
}

