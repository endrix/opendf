package net.sf.opendf.profiler.schedule;

import net.sf.opendf.profiler.data.Trace;

/**
 * 
 * @author jornj
 */
public interface Scheduler {
	
	void schedule(Trace t, ScheduleOutput so);

}
