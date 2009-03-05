package net.sf.opendf.profiler.schedule.data;

/**
 * 
 * @author jornj
 */

public class ResourceConstraint {
	public Object    resourceClassID;
	public int    poolSize;
	
	public ResourceConstraint(Object resourceClassID, int poolSize) {
		this.resourceClassID = resourceClassID;
		this.poolSize = poolSize;
	}
}

