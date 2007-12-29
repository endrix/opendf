package net.sf.opendf.profiler.schedule.data;

import net.sf.opendf.profiler.data.Action;
import net.sf.opendf.profiler.schedule.data.Resource;
import net.sf.opendf.profiler.schedule.Duration;

public class InstanceMapping {

	public Action       action;
	public Resource     resource;
	public Duration     latency;
	public Duration     initiationInterval;
	public double       weight;
	
	public InstanceMapping(Action action, Resource resource, Duration latency, Duration initiationInterval, double weight) {
		this.action = action;
		this.resource = resource;
		this.latency = latency;		
		this.initiationInterval = initiationInterval;
		this.weight = weight;
	}
	public InstanceMapping(Action action, Object resourceClassID, int resourceID, Duration latency, Duration initiationInterval, double weight) {
		this(action, new Resource(resourceClassID, resourceID), latency, initiationInterval, weight);
	}
	
	public InstanceMapping(Action action, Object resourceClassID, int resourceID, Duration latency, double weight) {
		this (action, resourceClassID, resourceID, latency, latency, weight);
	}
	
	public InstanceMapping(Action action, Object resourceClassID, int resourceID, Duration latency, Duration initiationInterval) {
		this (action, resourceClassID, resourceID, latency, initiationInterval, 1);
	}
	
	public InstanceMapping(Action action, Object resourceClassID, int resourceID, Duration latency) {
		this (action, resourceClassID, resourceID, latency, 1);
	}
	
	public InstanceMapping(int actorID, String actorClassName, int action, Object resourceClassID, int resourceID, Duration latency, Duration initiationInterval) {
		this(new Action(actorID, actorClassName, action), resourceClassID, resourceID, latency, initiationInterval);
	}
	
	public InstanceMapping(int actorID, String actorClassName, int action, Object resourceClassID, int resourceID, Duration latency) {
		this(new Action(actorID, actorClassName, action), resourceClassID, resourceID, latency);
	}
	
	public InstanceMapping(int actorID, String actorClassName, int action, Object resourceClassID, int resourceID, long latency, long initiationInterval) {
		this(actorID, actorClassName, action, resourceClassID, resourceID, new Duration(latency), new Duration(initiationInterval));
	}

	public InstanceMapping(int actorID, String actorClassName, int action, Object resourceClassID, int resourceID, long latency) {
		this(actorID, actorClassName, action, resourceClassID, resourceID, new Duration(latency));
	}
}
