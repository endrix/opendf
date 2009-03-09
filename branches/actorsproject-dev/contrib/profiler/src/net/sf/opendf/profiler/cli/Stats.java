package net.sf.opendf.profiler.cli;

import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import net.sf.opendf.profiler.data.Action;
import net.sf.opendf.profiler.data.ActionClass;
import net.sf.opendf.profiler.data.Step;
import net.sf.opendf.profiler.data.Trace;
import net.sf.opendf.profiler.io.TraceParser;

/**
 * 
 * @author jornj
 */
public class Stats {
	
	public static void main(String [] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			File f = new File(args[i]);
			FileInputStream fis = new FileInputStream(f);
			TraceParser tp = new TraceParser(fis, TraceParser.DEPS_DF);
			Trace t = tp.parse();
			printStats(t, args[i]);
		}
	}
	
	static void printStats(Trace t, String name) {
		System.out.println("Trace statistics for '" + name + "': ");
		System.out.println("  # steps: " + t.length());
		Stats s = createStats(t, name);
		for (Action a : s.actionFreq.keySet()) {
			System.out.println("  Action: " + a + " -- " + s.actionFreq.get(a));
		}
		for (ActionClass ac : s.actionClassFreq.keySet()) {
			System.out.println("  ActionClass: " + ac + " -- " + s.actionClassFreq.get(ac));
		}
	}
	
	public static Stats  createStats(Trace t, String name) {
		SortedMap<Action, Integer> actionFreq = new TreeMap<Action, Integer>(); 
		SortedMap<ActionClass, Integer> actionClassFreq = new TreeMap<ActionClass, Integer>(); 
		for (Integer sid : t.getSteps().keySet()) {
			Step s = t.getStep(sid);
			Action a = s.getAction();
			incrementCount(actionFreq, a);
			ActionClass ac = s.getActionClass();
			incrementCount(actionClassFreq, ac);
		}
		return new Stats(actionFreq, actionClassFreq);
	}
	
	private static void  incrementCount(Map m, Object k) {
		Integer n = (Integer)m.get(k);
		if (n == null) {
			m.put(k, Integer.valueOf(1));
		} else {
			m.put(k, Integer.valueOf(n.intValue() + 1));
		}
	}
	
	
	public Stats(Map<Action, Integer> actionFreq, Map<ActionClass, Integer> actionClassFreq) {
		this.actionFreq = actionFreq;
		this.actionClassFreq = actionClassFreq;
	}
	
	private Map<Action, Integer>  actionFreq;
	private Map<ActionClass, Integer> actionClassFreq;
	
}
