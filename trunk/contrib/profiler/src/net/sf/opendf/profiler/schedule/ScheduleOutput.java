package net.sf.opendf.profiler.schedule;

/**
 * 
 * @author jornj
 */

public interface ScheduleOutput {

	void start();
	void executeStep(Object stepID, Time start, Duration duration);
	void beginStep(Object stepID, Time start, Duration duration);
	void endStep();
	void attribute(Object key, Object value);
	void finish(Time tm); 
}
