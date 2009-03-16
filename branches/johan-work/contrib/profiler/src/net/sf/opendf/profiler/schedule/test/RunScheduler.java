package net.sf.opendf.profiler.schedule.test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.opendf.profiler.schedule.data.ResourceConfiguration;
import net.sf.opendf.profiler.data.Step;
import net.sf.opendf.profiler.data.Trace;
import net.sf.opendf.profiler.io.TraceParser;
import net.sf.opendf.profiler.schedule.BasicResourceScheduler;
import net.sf.opendf.profiler.schedule.ResourceScheduler;
import net.sf.opendf.profiler.schedule.ScheduleOutput;
import net.sf.opendf.profiler.schedule.Scheduler;
import net.sf.opendf.profiler.schedule.SchedulerWithResourceConstraints;
import net.sf.opendf.profiler.schedule.Time;
import net.sf.opendf.profiler.schedule.XmlScheduleOutput;

/**
 * 
 * @author jornj
 */

public class RunScheduler {
	
	static void schedule(Trace t, PrintWriter pw, ResourceConfiguration config) {
		ScheduleOutput so = new XmlScheduleOutput(pw) {
			public void finish(Time tm) {
				super.finish(tm);
				System.out.print(" <finish at: " + tm.toString() + "> ");
			}
		};
		ResourceScheduler rs = new BasicResourceScheduler(config);
		Scheduler s = new SchedulerWithResourceConstraints(rs);
		s.schedule(t, so);
	}

	public static void main (String [] args) throws Exception {
		if (args.length == 0) {
			printUsage();
			return;
		}
		if (args.length % 4 == 1) {
			System.out.println("Number of arguments must be multiple of 4.");
			printUsage();
			return;
		}

		for (int i = 0; i < args.length; i += 4) {
			System.out.println("Trace: " + args[i]);
			
			System.out.print("  Parsing trace... "); 
			int flags = Integer.parseInt(args[i + 1]);
			InputStream s = new FileInputStream(args[i]);
			TraceParser tp = new TraceParser(s, flags);
			Trace t = tp.parse();
			System.out.println("done.");
			s.close();
			
			System.out.print("  Reading resource configuration... "); 
			s = new FileInputStream(args[i + 2]);
			ResourceConfiguration rc = ResourceConfiguration.createConfiguration(s);
			System.out.println("done.");
			s.close();
			
			
			PrintWriter pw = new PrintWriter(new FileOutputStream(args[i + 3]));

			System.out.print("  Scheduling... ");
			schedule(t, pw, rc);
			System.out.println("done.");
			
			System.out.print("  Writing to output file '" + args[i + 3] + "'... ");			
			pw.close();		
			System.out.println("done.");
		}
	}
	
	private static void printUsage() {
		System.out.println("RunSchedule trace1 flags1 config1 outfile1... traceN flagsN configN outfileN");
	}
}
