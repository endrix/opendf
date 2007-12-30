package net.sf.opendf.profiler.schedule.test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.opendf.profiler.data.Step;
import net.sf.opendf.profiler.data.Trace;
import net.sf.opendf.profiler.io.TraceParser;
import net.sf.opendf.profiler.schedule.BasicResourceScheduler;
import net.sf.opendf.profiler.schedule.ScheduleOutput;
import net.sf.opendf.profiler.schedule.Scheduler;
import net.sf.opendf.profiler.schedule.SchedulerWithResourceConstraints;
import net.sf.opendf.profiler.schedule.SimpleResourceScheduler;
import net.sf.opendf.profiler.schedule.SimpleScheduler;
import net.sf.opendf.profiler.schedule.XmlScheduleOutput;

/**
 * 
 * @author jornj
 */

public class ScheduleTest {
	
	static void simpleSched(Trace t, PrintWriter pw) {
		ScheduleOutput so = new XmlScheduleOutput(pw);
		Scheduler s = new SimpleScheduler();
		s.schedule(t, so);
	}
	
	static void constraintSched0(Trace t, PrintWriter pw) {
		ScheduleOutput so = new XmlScheduleOutput(pw);
		Scheduler s = new SchedulerWithResourceConstraints(new SimpleResourceScheduler());
		s.schedule(t, so);
	}

	static void constraintSched1(Trace t, PrintWriter pw) {
		ScheduleOutput so = new XmlScheduleOutput(pw);
		Scheduler s = new SchedulerWithResourceConstraints(new BasicResourceScheduler());
		s.schedule(t, so);
	}

	public static void main (String [] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			System.out.print("Basic-1 trace: " + args[i] + "---parsing..."); System.out.flush();
			InputStream s = new FileInputStream(args[i] + ".ct");
			TraceParser tp = new TraceParser(s, TraceParser.DEPS_ALL);
			Trace t = tp.parse();
			System.out.println(" done.");
			s.close();
			
			PrintWriter pw = new PrintWriter(new FileOutputStream(args[i] + "-b1.sched"));
			// PrintWriter pw = new PrintWriter(System.out);

			System.out.print("   scheduling...");
			//simpleSched(t, pw);
			constraintSched1(t, pw);
			System.out.println(" done.");
			pw.flush();
			pw.close();		
		
			System.out.print("Basic-0 trace: " + args[i] + "---parsing..."); System.out.flush();
			s = new FileInputStream(args[i] + ".ct");
			tp = new TraceParser(s, TraceParser.DEPS_ALL);
			t = tp.parse();
			System.out.println(" done.");
			s.close();
			
			pw = new PrintWriter(new FileOutputStream(args[i] + "-b0.sched"));
			// PrintWriter pw = new PrintWriter(System.out);

			System.out.print("   scheduling...");
			//simpleSched(t, pw);
			constraintSched0(t, pw);
			System.out.println(" done.");
			
			pw.flush();
			pw.close();
		}
	}
}
