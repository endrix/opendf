package net.sf.opendf.profiler.io;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;


import net.sf.opendf.profiler.data.AttributeCarrier;
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
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		
		try {
			Map<String, String> a = new HashMap<String, String>();
			for (int i = 0; i < attributes.getLength(); i++) {
				a.put(attributes.getQName(i), attributes.getValue(i));
			}

			if ("dependency".equals(qName)) {			
				if (currentStep == null)
					throw new RuntimeException("No dependencies outside of step elements.");
				
				Integer src = new Integer(a.get("source"));
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
				
				Integer stepID = new Integer(a.get("ID"));
				String actorClassName = a.get("actor-name");
				int actorId = Integer.parseInt(a.get("actor-id"));
				
				int action;
				try {
					action = Integer.parseInt(a.get("action"));
				} catch (NumberFormatException exc) {
					throw new RuntimeException("Cannot parse action #: '" + a.get("action") + "' of actor '" + actorClassName + "'.", exc);
				}
				
				String kind = a.get("kind");
				String tag = a.get("action-tag");
				
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
			
			if ("attribute".equals(qName)) {
				AttributeCarrier ac = (currentStep == null) ? trace : currentStep;
				currentHandler = new AttributeHandler(ac, a.get("name"));
				return;
			}
			
			if ("Integer".equals(qName)) {
				currentHandler = new AtomicObjectHandler(currentHandler, Integer.parseInt(a.get("value")));
				return;
			}
			
			if ("Real".equals(qName)) {
				currentHandler = new AtomicObjectHandler(currentHandler, Double.valueOf(a.get("value")));
				return;
			}
			
			if ("String".equals(qName)) {
				currentHandler = new AtomicObjectHandler(currentHandler, a.get("value"));
				return;
			}
			
			if ("List".equals(qName)) {
				currentHandler = new ListHandler(currentHandler);
				return;
			}
			
			if ("Set".equals(qName)) {
				currentHandler = new SetHandler(currentHandler);
				return;
			}
			
			if ("Map".equals(qName)) {
				currentHandler = new MapHandler(currentHandler);
				return;
			}
			
			if ("Mapping".equals(qName)) {
				currentHandler = new MappingHandler(currentHandler);
				return;
			}
			
			currentHandler = new DummyHandler(currentHandler, qName);
			
		}
		catch (Exception e) {
            Logging.dbg().throwing("TraceParser", "startElement", e);
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) {
		if (currentHandler != null) {
			currentHandler = currentHandler.handleEnd();
		}
		
		if ("step".equals(qName)) {
			currentStep = null;
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
	
	private Object  currentObject = null;
	private ObjectHandler currentHandler = null;
	
	interface ObjectHandler {
		void handleChild(Object child);
		ObjectHandler handleEnd();
	}
	
	static class AttributeHandler implements ObjectHandler {

		public void handleChild(Object child) {
			ac.setAttribute(attr, child);
		}

		public ObjectHandler handleEnd() {
			return null;
		}
		
		AttributeHandler(AttributeCarrier ac, String attr) {
			this.ac = ac;
			this.attr = attr;
		}
		
		private AttributeCarrier ac;
		private String attr;
	}
	
	static class ListHandler implements ObjectHandler {

		public void handleChild(Object child) {
			list.add(child);
		}

		public ObjectHandler handleEnd() {
			parent.handleChild(list);
			return parent;
		}
		
		ListHandler(ObjectHandler parent) {
			this.parent = parent;
			this.list = new ArrayList();
		}

		private ObjectHandler parent;
		private List list;
	}
	
	static class SetHandler implements ObjectHandler {

		public void handleChild(Object child) {
			set.add(child);
		}

		public ObjectHandler handleEnd() {
			parent.handleChild(set);
			return parent;
		}
		
		SetHandler(ObjectHandler parent) {
			this.parent = parent;
			this.set = new HashSet();
		}

		private ObjectHandler parent;
		private Set set;
	}
	
	static class MapHandler implements ObjectHandler {

		public void handleChild(Object child) {
			Mapping m = (Mapping)child;
			map.put(m.k, m.v);
		}

		public ObjectHandler handleEnd() {
			parent.handleChild(map);
			return parent;
		}
		
		MapHandler(ObjectHandler parent) {
			this.parent = parent;
			this.map = new HashMap();
		}
		
		static class Mapping { 
			Object k; Object v; 
			Mapping (Object k, Object v) {
				this.k = k;
				this.v = v;
			}
		}

		private ObjectHandler parent;
		private Map map;
	}
	
	class MappingHandler implements ObjectHandler {

		public void handleChild(Object child) {
			switch(n) {
			case 0: k = child; break;
			case 1: v = child; break;
			default: break;
			}
			n += 1;
		}

		public ObjectHandler handleEnd() {
			parent.handleChild(new MapHandler.Mapping(k, v));
			return parent;
		}
		
		MappingHandler(ObjectHandler parent) {
			this.parent = parent;
		}
		
		int n = 0;
		private ObjectHandler parent;
		private Object k;
		private Object v;
	}
	
	static class AtomicObjectHandler implements ObjectHandler {
		
		public void handleChild(Object child) {
		}

		public ObjectHandler handleEnd() {
			parent.handleChild(v);
			return parent;
		}
		
		AtomicObjectHandler(ObjectHandler parent, Object v) {
			this.parent = parent;
			this.v = v;
		}
		
		private ObjectHandler parent;
		private Object v;
	}
	
	static class DummyHandler implements ObjectHandler {

		public void handleChild(Object child) {
		}

		public ObjectHandler handleEnd() {
			return parent;
		}
		
		DummyHandler(ObjectHandler parent, String element) {
			this.parent = parent;
			Logging.user().warning("Parsing trace - encountered undefined tag: '" + element + "'.");
		}
		
		private ObjectHandler parent;
	}
	
	
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
