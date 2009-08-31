package net.sf.opendf.profiler.cli;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import net.sf.opendf.profiler.data.Step;
import net.sf.opendf.profiler.data.Trace;
import net.sf.opendf.profiler.io.TraceParser;

public class ReportOutputAnalysis {

	public static void main(String[] args) throws Exception {
		System.out.println("Reading trace '" + args[0] + "'...");
		TraceParser tp = new TraceParser(new FileInputStream(args[0]), TraceParser.DEPS_DF);
		Trace t = tp.parse();
		
		int actorID = Integer.parseInt(args[1]);
		int actionID = Integer.parseInt(args[2]);
		
		SortedMap<Integer, Double> latency = new TreeMap<Integer, Double>();
		Map<Integer, Long> cost = new HashMap<Integer, Long>();
		
		for (Integer id : t.getSteps().keySet()) {
			Step s = t.getStep(id);
			if (s.hasAttribute("outputAction")) {
				latency.put((Integer)s.getAttribute("outputAction"), (Double)s.getAttribute("endTime"));
			}
			if (s.hasAttribute("outputDependency")) {
				accumulate(cost, s.getAttribute("outputDependency"), 1);
			}
		}
		
		System.out.println("Writing report '" + args[3] + "'...");
		PrintWriter pw = new PrintWriter(new FileOutputStream(args[3]));
		pw.println("output, latency, cost");
		for (Integer n : latency.keySet()) {
			pw.println("" + n + ", " + latency.get(n) + ", " + cost.get(n));
		}
		pw.close();
		Set<Integer> s = new HashSet<Integer>();
		s.addAll(cost.keySet());
		s.removeAll(latency.keySet());
		if (! s.isEmpty())
			System.out.println("WARNING: Some cost keys unaccounted for. (" + s + ")");		
		System.out.println("Done.");		
	}
	
	static private void accumulate(Map m, Object k, long n) {
		Long v = (Long) m.get(k);
		if (v == null) {
			v = Long.valueOf(0);
		}
		m.put(k, v.longValue() + n);
	}
	
	

}
