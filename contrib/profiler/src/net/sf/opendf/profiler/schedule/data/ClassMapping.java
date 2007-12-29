package net.sf.opendf.profiler.schedule.data;

import net.sf.opendf.profiler.data.ActionClass;
import net.sf.opendf.profiler.schedule.Duration;

/**
 * 
 * @author jornj
 */

public class ClassMapping {
	public ActionClass  actionClass;
	public Object	    resourceClassID;
	public Duration     latency;
	public Duration     initiationInterval;
	public double       weight;
	
	public ClassMapping(ActionClass actionClass, Object resourceClassID, Duration latency, Duration initiationInterval, double weight) {
		this.actionClass = actionClass;
		this.resourceClassID = resourceClassID;
		this.latency = latency;		
		this.initiationInterval = initiationInterval;
		this.weight = weight;
	}
	
	public ClassMapping(ActionClass actionClass, Object resourceClassID, Duration latency, double weight) {
		this (actionClass, resourceClassID, latency, latency, weight);
	}
	
	public ClassMapping(ActionClass actionClass, Object resourceClassID, Duration latency, Duration initiationInterval) {
		this (actionClass, resourceClassID, latency, initiationInterval, 1);
	}
	
	public ClassMapping(ActionClass actionClass, Object resourceClassID, Duration latency) {
		this (actionClass, resourceClassID, latency, 1);
	}
	
	public ClassMapping(String actorClassName, int action, Object resourceClassID, Duration latency, Duration initiationInterval) {
		this(new ActionClass(actorClassName, action), resourceClassID, latency, initiationInterval);
	}
	
	public ClassMapping(String actorClassName, int action, Object resourceClassID, Duration latency) {
		this(new ActionClass(actorClassName, action), resourceClassID, latency);
	}
	
	public ClassMapping(String actorClassName, int action, Object resourceClassID, long latency, long initiationInterval) {
		this(actorClassName, action, resourceClassID, new Duration(latency), new Duration(initiationInterval));
	}

	public ClassMapping(String actorClassName, int action, Object resourceClassID, long latency) {
		this(actorClassName, action, resourceClassID, new Duration(latency));
	}

}

