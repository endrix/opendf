package net.sf.opendf.profiler.cli;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

import net.sf.opendf.profiler.data.Step;
import net.sf.opendf.profiler.data.Trace;
import net.sf.opendf.profiler.io.TraceParser;
import net.sf.opendf.profiler.util.Lib;

public class AnnotateOutputDependency {

	public static void main(String [] args) throws Exception {
		System.out.println("Reading trace '" + args[0] + "'...");
		TraceParser tp = new TraceParser(new FileInputStream(args[0]), TraceParser.DEPS_DF);
		Trace t = tp.parse();
		
		int actorID = Integer.parseInt(args[2]);
		int actionID = Integer.parseInt(args[3]);
		
		System.out.println("\nAnnotating...");
		
		int n = annotateTrace(t, actorID, actionID);
		System.out.println("\nOutput actions: " + n);
		
		FileOutputStream os = new FileOutputStream (args[1]);
		PrintWriter pw = new PrintWriter(os);
		System.out.println("\nWriting trace '" + args[1] + "'...");
		Lib.writeTrace(t, pw);
		pw.close();
		os.close();
		System.out.println("Done.");
	}
	
	public static int  annotateTrace(Trace t, int actorID, int actionID) {
		int n = 0;
		
		for (Integer sid : t.getSteps().keySet()) {
			Step s = t.getStep(sid);
			if (s.getActorId() == actorID && s.getActionId() == actionID) {
				s.setAttribute("outputAction", n);
				annotateBackward(t, s, "outputDependency", n);
				n += 1;
			}
		}
		return n;
	}
	
	private static void annotateBackward(Trace t, Step s, String attr, Object v) {
		Set<Integer> d = new HashSet<Integer>();
		Set<Integer> dnew = new HashSet<Integer>();
		dnew.addAll(s.preset());
		while (! dnew.isEmpty()) {
			Set<Integer> dnext = new HashSet<Integer>();
			for (Integer id : dnew) {
				if (! d.contains(id)) {
					Step snew = t.getStep(id);
					if (! snew.hasAttribute(attr)) {
						d.add(id);
						dnext.addAll(t.getStep(id).preset());
					}
				}
			}
			dnew = dnext;
		}
		
		for (Integer id : d) {
			Step s1 = t.getStep(id);
			s1.setAttribute(attr, v);
		}
	}

}
