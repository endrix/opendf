package net.sf.opendf.profiler.schedule;

import net.sf.opendf.profiler.schedule.data.Resource;
import net.sf.opendf.profiler.data.Step;

/**
 *  
 * @author jornj
 */
public interface ResourceScheduler {
	
	
	ResourceRequestResult  requestResourceFor(Step s, Time currentTime);
	
	Resource  getResource(Step s);
	
	void      endInitiation(Step s);
	
	void      finishStep(Step s);
	
	void      writeStatistics(ScheduleOutput so);
	
	static public class ResourceRequestResult {
		public boolean  granted;
		public Time     reinitiationTime;
		public Time     completionTime;
		public Resource resource;
		
		public ResourceRequestResult(Time reinitiationTime, Time completionTime, Resource resource) {
			this(true, reinitiationTime, completionTime, resource);
		}
		
		public ResourceRequestResult() {
			this(false, null, null, null);
		}
		
		private ResourceRequestResult(boolean granted, Time reinitiationTime, Time completionTime, Resource resource) {
			this.granted = granted;
			this.reinitiationTime = reinitiationTime;
			this.completionTime = completionTime;
			this.resource = resource;
		}
	}
	
	

}
