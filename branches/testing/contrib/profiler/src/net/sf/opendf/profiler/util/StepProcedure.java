package net.sf.opendf.profiler.util;

import java.util.Set;

import net.sf.opendf.profiler.data.Step;
import net.sf.opendf.profiler.data.Trace;

public interface StepProcedure {

	public void  run(Trace t, Step s);
	public Set   successors(Trace t, Step s);
}
