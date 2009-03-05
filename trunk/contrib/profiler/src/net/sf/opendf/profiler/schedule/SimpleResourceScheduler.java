package net.sf.opendf.profiler.schedule;

import net.sf.opendf.profiler.schedule.data.Resource;
import net.sf.opendf.profiler.data.Step;

/**
 * 
 * @author jornj
 */
public class SimpleResourceScheduler implements ResourceScheduler {
	
	//
	//  ResourceScheduler
	//

	public ResourceRequestResult requestResourceFor(Step s, Time currentTime) {
		return new ResourceRequestResult(currentTime.add(Duration.ONE), currentTime.add(Duration.ONE), new Resource("11", 1));
	}

	public Resource getResource(Step s) {
		return new Resource("11", 1);
	}
	
	public void endInitiation(Step s) {}

	public void finishStep(Step s) {
	}
	
	public void writeStatistics(ScheduleOutput so) {
		
	}
	
	
	//
	//  Ctor
	//
	
	public SimpleResourceScheduler() {
	}

}
