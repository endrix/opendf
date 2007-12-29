package net.sf.opendf.profiler.schedule.test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import net.sf.opendf.profiler.data.Step;
import net.sf.opendf.profiler.data.Trace;
import net.sf.opendf.profiler.parser.TraceParser;

public class StatTrace {

	public static void main (String [] args) throws Exception {
		
		if (args.length % 2 != 0) {
			printUsage();
			return;
		}
		
		// Parse trace
		for (int k = 0; k < args.length; k += 2) {

			System.out.print("Parse trace: " + args[k] + "..."); System.out.flush();
			InputStream s = new FileInputStream(args[k]);
			TraceParser tp = new TraceParser(s, TraceParser.DEPS_ALL);
			Trace t = tp.parse();
			System.out.println(" done."); System.out.flush();
			s.close();

			System.out.print("Stat actionclasses (" + args[k + 1] + ")..."); System.out.flush();
			SortedMap stats = runStat(t);
			PrintWriter pw = new PrintWriter(new FileOutputStream(args[k + 1]));
			pw.println("ActorClass, ActorID, Action, Firings, ActionLabel");
			for (Iterator i = stats.keySet().iterator(); i.hasNext(); ) {
				StatKey key = (StatKey)i.next();
				pw.println("\"" + key.actorClassName + "\", " 
						        + key.actorID + ", " 
						        + key.action + ", " 
						        + stats.get(key).toString() + ", \"" 
						        + (key.tag == null ? "" : key.tag) + "\"");
			}
			pw.close();
			System.out.println(" done."); System.out.flush();			
		}
	}
	
	static  SortedMap runStat(Trace t) {
		SortedMap m = new TreeMap();
		for (Iterator i = t.stepsIterator(); i.hasNext(); ) {
			Integer sid = (Integer)i.next();
			Step s = t.getStep(sid);
			StatKey k = new StatKey(s.getActorClassName(), s.getActorId(), s.getActionId(), s.getTag());
			Long n = (Long)m.get(k);
			long count = (n == null) ? 0 : n.longValue();
			m.put(k, new Long(count + 1));
		}
		
		return m;
	}

	static void printUsage() {
		System.out.println("StatTrace traceFile actionClassStatFile");
	}
	
	static class StatKey implements Comparable {
		public String actorClassName;
		public int    actorID;
		public int    action;
		public String tag;
		
		StatKey(String actorClassName, int actorID, int action, String tag) {
			this.actorClassName = actorClassName;
			this.actorID = actorID;
			this.action = action;
			this.tag = tag;
		}
		
		public int hashCode() {
			return actorClassName.hashCode() + actorID + action;
		}
		
		public boolean equals(Object a) {
			if (a instanceof StatKey) {
				StatKey k = (StatKey)a;
				return (actorID == k.actorID) 
				    && (action == k.action)
				    && (actorClassName.equals(k.actorClassName));
			} else {
				return false;
			}
		}
		
		public int compareTo(Object a) {
			StatKey k = (StatKey)a;
			int n = actorClassName.compareTo(k.actorClassName);
			if (n != 0)
				return n;
			n = actorID - k.actorID;
			if (n != 0)
				return n;
			n = action - k.action;
			return n;
		}
	}
}
