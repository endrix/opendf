package net.sf.opendf.profiler.apps;

import java.io.File;
import java.io.FileInputStream;

import net.sf.opendf.profiler.data.Trace;
import net.sf.opendf.profiler.parser.TraceParser;

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
	}

}
