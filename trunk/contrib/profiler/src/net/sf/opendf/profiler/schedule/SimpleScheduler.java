package net.sf.opendf.profiler.schedule;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.opendf.profiler.data.Step;
import net.sf.opendf.profiler.data.Trace;

/**
 * 
 * @author jornj
 */
public class SimpleScheduler implements Scheduler {

	public void schedule(Trace t, ScheduleOutput so) {
		so.start();
		
		Set s = t.initialSteps();
		int n = 0;
		while (!s.isEmpty()) {
			Set s1 = new HashSet();
			for (Iterator i = s.iterator(); i.hasNext(); ) {
				Step a = (Step)i.next();
				so.executeStep(a.getID(), new Time(n), new Duration(1));
				s1.addAll(t.removeInitialStep(a));
			}
			s = s1;
			n += 1;
		}
		
		so.finish(Time.ZERO);
	}
}
