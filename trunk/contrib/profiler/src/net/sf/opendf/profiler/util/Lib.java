package net.sf.opendf.profiler.util;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.List;

import net.sf.opendf.profiler.data.Dependency;
import net.sf.opendf.profiler.data.Step;
import net.sf.opendf.profiler.data.Trace;
import net.sf.opendf.profiler.data.Action;
import net.sf.opendf.profiler.data.ActionClass;

/**
 * 
 * @author jornj
 */
public class Lib {
	
	public static void writeTrace(Trace t, PrintWriter pw) {
		pw.println("<causation-trace>");
		for (Iterator i = t.stepsIterator(); i.hasNext(); ) {
			Integer n = (Integer)i.next();
			Step s = t.getStep(n);
			writeStep(s, pw);
		}
		pw.println("</causation-trace>");
		pw.flush();
	}

	private static void writeStep(Step s, PrintWriter pw) {
		pw.println("<step ID='" + s.getID() 
				+ "' actor-name='" + s.getActorClassName()
				+ "' actor-id='" + s.getActorId() 
				+ "' action='" + s.getActionId() + "'>");
		for (Iterator i = s.preset().iterator(); i.hasNext(); ) {
			Integer n = (Integer)i.next();
			pw.println("<dependency source='" + n + "'/>");
		}
		Map a = s.attributes();
		for (Object k : a.keySet()) {
			Object v = a.get(k);
			pw.println("<attribute name='" + k + "'>");
			writeObject(pw, v);
			pw.println("</attribute>");
		}
		pw.println("</step>");
	}
	
	private static void  writeObject(PrintWriter pw, Object v) {
		if (v instanceof Integer) {
			pw.println("  <Integer value='" + v.toString() + "'/>");	
		} else if (v instanceof Number) {
			pw.println("  <Real value='" + v + "'/>");
		} else if (v instanceof String) {
			pw.println("  <String value='" + v + "'/>");
		} else if (v instanceof List) {
			pw.println("  <List>");
			for (Object e : (List)v) {
				writeObject(pw, e);
			}
			pw.println("  </List>");
		} else if (v instanceof Set) {
			pw.println("  <Set>");
			for (Object e : (Set)v) {
				writeObject(pw, e);
			}
			pw.println("  </Set>");
		} else if (v instanceof Map) {
			pw.println("  <Map>");
			for (Object k : ((Map)v).keySet()) {
				pw.println("  <Mapping>");
				writeObject(pw, k);
				writeObject(pw, ((Map)v).get(k));
				pw.println("  </Mapping>");				
			}
			pw.println("  </Map>");
		} else if (v instanceof Action) {
			Action a = (Action)v;
			pw.println("  <Action actorID='" + a.actorID +  "' actionID='" + a.actionClass.action + "' actorClass='" + a.actionClass.actorClassName + "'/>");
		} else if (v instanceof ActionClass) {
			ActionClass ac = (ActionClass)v;
			pw.println("  <ActionClass actionID='" + ac.action + "' actorClass='" + ac.actorClassName + "'/>");
		} else {
			if (v == null) {
				pw.println("  <Null/>");
			} else {
				pw.println("  <Object class='" + v.getClass().getName() + "' toString='" + v.toString() + "'/>");
			}
		}
	}
	
	public static void  inputSequentializeTrace(Trace t, String sourceClassName, int sourceID) {
		
		int nInput = 0;
		
		for (Iterator i = t.stepsIterator(); i.hasNext(); ) {
			Step s = (Step)i.next();
			if (sourceClassName.equals(s.getActorClassName()) && (sourceID == s.getActorId())) {
				nInput += 1;
				Integer n = new Integer(nInput);
				s.setAttribute(attrSink, valYes);
				s.setAttribute(attrSeqNumber, n);
				addAbsentAttributeToDescendents(t, s, attrSeqNumber, new Integer(nInput));
			}
		}
	}
	
	public static void  outputSequentializeTrace(Trace t, String destClassName, int destID) {
		
		int nOutput = 0;
		
		for (Iterator i = t.stepsIterator(); i.hasNext(); ) {
			Step s = (Step)i.next();
			if (destClassName.equals(s.getActorClassName()) && (destID == s.getActorId())) {
				nOutput += 1;
				Integer n = new Integer(nOutput);
				s.setAttribute(attrSink, valYes);
				s.setAttribute(attrSeqNumber, n);
				addAbsentAttributeToAncestors(t, s, attrSeqNumber, n);
			}
		}
	}
			
	public static void  iterate(Trace t, Step s, StepProcedure p) {
		Set ids = new HashSet();
		ids.add(p.successors(t, s));
		while (!ids.isEmpty()) {
			Integer id = (Integer)ids.iterator().next();
			Step next = t.getStep(id);
			ids.addAll(p.successors(t, next));
			ids.remove(id);

			p.run(t, next);
		}
	}
	
	public static void addAbsentAttributeToAncestors(Trace t, Step s, final Object a, final Object v) {
		
		iterate(t, s, new StepProcedure () {
			public void run(Trace t, Step s) {
				s.setAttribute(a, v);
			}
			public Set successors(Trace t, Step s) {
				if (s.hasAttribute(a))
					return Collections.EMPTY_SET;
				else
					return s.preset();
			}
		});
	}
	
	public static void addAbsentAttributeToDescendents(Trace t, Step s, final Object a, final Object v) {
		
		iterate(t, s, new StepProcedure () {
			public void run(Trace t, Step s) {
				s.setAttribute(a, v);
			}
			public Set successors(Trace t, Step s) {
				if (s.hasAttribute(a))
					return Collections.EMPTY_SET;
				else
					return s.postset();
			}
		});
	}
	
	public final static String attrSeqNumber = "seq-number";
	public final static String attrSink = "sink";

	public final static String valYes = "yes";
}
