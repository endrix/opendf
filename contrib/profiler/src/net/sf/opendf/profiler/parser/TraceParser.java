package net.sf.opendf.profiler.parser;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;


import net.sf.opendf.profiler.data.Step;
import net.sf.opendf.profiler.data.Trace;
import net.sf.opendf.util.logging.Logging;

/**
 * 
 * @author jornj
 */
public class TraceParser extends DefaultHandler {
	
	//
	//  TraceParser
	//
	
	public Trace parse() {
		try {
			SAXParser p = SAXParserFactory.newInstance().newSAXParser();
			p.parse(is, this);
			return trace;
		} catch (Exception e) {
            Logging.dbg().throwing("TraceParser", "parse", e);
			throw new RuntimeException(e);
		}
	}
	
	
	//
	//  DefaultHandler
	//
	
	public void startDocument() {
		trace = new Trace();
	}
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		
		try {
			if ("dependency".equals(qName)) {			
				if (currentStep == null)
					throw new RuntimeException("No dependencies outside of step elements.");
				
				Map a = new HashMap();
				for (int i = 0; i < attributes.getLength(); i++) {
					a.put(attributes.getQName(i), attributes.getValue(i));
				}
				Integer src = new Integer((String)a.get("source"));
				Integer dst = currentStep.getID();
				
				int dk = dependencyKind(a);
				if ((dk & dependencyMask) != 0) {
					trace.addDependency(src, dst);
					nDFDeps += 1;
				}
				nDeps += 1;
				return;
			}
			
			if ("step".equals(qName)) {
				
				Map a = new HashMap();
				for (int i = 0; i < attributes.getLength(); i++) {
					a.put(attributes.getQName(i), attributes.getValue(i));
				}
				Integer stepID = new Integer((String)a.get("ID"));
				String actorClassName = (String)a.get("actor-name");
				int actorId = Integer.parseInt((String)a.get("actor-id"));
				
				int action;
				try {
					action = Integer.parseInt((String)a.get("action"));
				} catch (NumberFormatException exc) {
					throw new RuntimeException("Cannot parse action #: '" + a.get("action") + "' of actor '" + actorClassName + "'.", exc);
				}
				
				String kind = (String)a.get("kind");
				String tag = (String)a.get("action-tag");
				
				Step s = new Step(stepID, actorClassName, actorId, action, kind, tag);
				trace.addStep(s);
				if (trace.length() % 10000 == 0) {
					if (trace.length() % 50000 == 0) {
						if (trace.length() % 100000 == 0) {
							System.out.print("#");
						} else {
							System.out.print(":");
						}								
					} else {
						System.out.print("'");
					}
				}
				currentStep = s;
				
				return;
			}
			if ("causation-trace".equals(qName)) {
				
				return;
			}
		}
		catch (Exception e) {
            Logging.dbg().throwing("TraceParser", "startElement", e);
			throw new RuntimeException(e);
		}
	}
	
	
	//
	//  Ctor
	//
	
	public TraceParser (InputStream is, int dependencyMask) {
		this.is = is;
		this.dependencyMask = dependencyMask;
	}
	
	//
	//  private
	// 
	
	private int  dependencyKind(Map attrs) {
		Object kind = attrs.get("kind");
		if (kind == null)
			return DEPS_ALL;			

		if ("token".equals(kind))
			return DEP_TOKEN;
		if ("stateVar".equals(kind)) {
			if ("RW".equals(attrs.get("dir"))) 
				return DEP_STATE_RW;
			else
				return DEP_STATE_WR;
		}
		if ("scheduler".equals(kind))
			return DEP_SCHEDULER;
		if ("port".equals(kind)) {
			if ("output".equals(attrs.get("port-type"))) 
				return DEP_PORT_OUTPUT;
			else
				return DEP_PORT_INPUT;
		}
		
		return DEPS_ALL;
	}
	
	
	private InputStream is;
	private Trace trace = null;
	private Step  currentStep = null;
	private int   dependencyMask;
	
	private int   nDeps = 0;
	private int   nDFDeps = 0;
	
	//
	//  Test
	//
	
	public static void main(String [] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			InputStream s = new FileInputStream(args[i]);
			TraceParser tp = new TraceParser(s, DEPS_DF);
			System.out.println("trace: " + args[i] + "...");
			Trace t = tp.parse();
			System.out.println(" done.");
			System.out.println("   # steps  :  " + t.length());
			System.out.println("   # deps   :  " + tp.nDeps);			
			System.out.println("   # dfdeps :  " + tp.nDFDeps);			
		}
	}
		
	public static final int DEP_TOKEN = 1;
	public static final int DEP_STATE_RW = 2;
	public static final int DEP_STATE_WR = 4;
	public static final int DEP_SCHEDULER = 8;
	public static final int DEP_PORT_INPUT = 16;
	public static final int DEP_PORT_OUTPUT = 32;
	
	public static final int DEPS_ALL = DEP_TOKEN | DEP_STATE_RW | DEP_STATE_WR | DEP_SCHEDULER | DEP_PORT_INPUT | DEP_PORT_OUTPUT;
	public static final int DEPS_DF = DEP_TOKEN | DEP_STATE_WR | DEP_SCHEDULER;
}
