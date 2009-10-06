package net.sf.opendf.profiler.schedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.opendf.profiler.schedule.data.ClassMapping;
import net.sf.opendf.profiler.schedule.data.InstanceMapping;
import net.sf.opendf.profiler.schedule.data.Resource;
import net.sf.opendf.profiler.schedule.data.ResourceConfiguration;
import net.sf.opendf.profiler.schedule.data.ResourceConstraint;
import net.sf.opendf.profiler.data.Action;
import net.sf.opendf.profiler.data.ActionClass;
import net.sf.opendf.profiler.data.Step;

/**
 * 
 * @author jornj
 */
public class BasicResourceScheduler implements ResourceScheduler {

	//
	//  ResourceScheduler
	//
	
	public ResourceRequestResult requestResourceFor(Step s, Time currentTime) {
		
		Action action = new Action(s);
		ResourceRequestResult rrr = null;
	
		// (1) check, whether the action is already associated with a resource
		Set resources = (Set)mapping.get(action);
		if (resources == null) {
			resources = Collections.EMPTY_SET;
		}
		Iterator i = resources.iterator();
		while (i.hasNext()) {
			Resource r = (Resource)i.next();
			if (!revCurrentAssignment.containsKey(r)) {
				// if resource not currently assigned
				assignResource(s, r);
				
				Action a = new Action(s.getActorId(), s.getActorClassName(), s.getActionId());
				List imappings = (List)this.instanceMapping.get(a);
				if (imappings != null) {
					for (Iterator j = imappings.iterator(); j.hasNext(); ) {
						InstanceMapping im = (InstanceMapping)j.next();
						if (r.equals(im.resource)) {
							return new ResourceRequestResult(currentTime.add(im.initiationInterval), currentTime.add(im.latency), r);			
						}
					}
				}
				ClassMapping cm = (ClassMapping)classMapping.get(new ActionClass(s.getActorClassName(), s.getActionId()));
				// assert cm == null || r.classID == cm.resourceClassID; FIXME: allow client-side multiplexing
				return new ResourceRequestResult(currentTime.add(cm == null ? Duration.ONE : cm.initiationInterval), currentTime.add(cm == null ? Duration.ONE : cm.latency), r);			
			}
		}
		
		if (allowClientSideMultiplexing || resources.isEmpty()) {
			// no resource yet for the action
			// or resource is busy and we can multiplex on the client
			rrr = findFreeResource(s, currentTime);
			if (rrr.granted) {
				// resource was found
				assignResource(s, rrr.resource);
				return rrr;
			} else {
				// no resource has been found
				return rrr;
			}
		} else {
			// action has
			//     (a) already an unavailable resource assigned to it, and
			//     (b) client-side multiplexing is disallowed.
			return new ResourceRequestResult();
		}			
	}
	
	public Resource getResource(Step s) {
		return (Resource)currentAssignment.get(s);
	}
	
	public void endInitiation(Step s) {
		Resource r = getResource(s);
		if (r != null)
			unassignResource(s, r);
	}


	public void finishStep(Step s) {
	}
	
	public void writeStatistics(ScheduleOutput so) {
		so.attribute("resource-usage", poolUsed);
		so.attribute("resource-assignments", assignments);
		so.attribute("mapping", mapping);
	}
	
	//
	//  Ctor
	//
	
	
	public BasicResourceScheduler(InstanceMapping [] im, ClassMapping [] m, ResourceConstraint [] c, boolean allowClientSideMultiplexing) {
		this.mapping = new HashMap();
		this.currentAssignment = new HashMap();
		this.revCurrentAssignment = new HashMap();
		this.freeResources = new HashMap();
		this.assignments = new HashMap();
		this.poolUsed = new HashMapWithDefault(new Integer(0));
		
		this.classMapping = new HashMap();
		for (int i = 0; i < m.length; i++) {
			this.classMapping.put(m[i].actionClass, m[i]);
		}
		
		this.instanceMapping = new HashMap();
		for (int i = 0; i < im.length; i++) {
			List s = (List)this.instanceMapping.get(im[i].action);
			if (s == null) {
				s = new ArrayList();
				this.instanceMapping.put(im[i].action, s);
			}
			s.add(im[i]);
		}
		
		
		this.resourceConstraints = new HashMap();
		for (int i = 0; i < c.length; i++) {
			this.resourceConstraints.put(c[i].resourceClassID, new Integer(c[i].poolSize));
		}
		
		this.allowClientSideMultiplexing = allowClientSideMultiplexing;
	}
	
	public BasicResourceScheduler(ResourceConfiguration config) {
		this(config.instanceMapping, config.classMapping, config.resourceConstraint, config.allowClientSideMultiplexing);
	}


	private Map classMapping;
	private Map instanceMapping;
	
	public BasicResourceScheduler() {
		this (new InstanceMapping [0], new ClassMapping[0], new ResourceConstraint[0], true);
	}
	
	
	private ResourceRequestResult  findFreeResource(Step s, Time tm) {
		
		Action a = new Action(s.getActorId(), s.getActorClassName(), s.getActionId());
		List imappings = (List)this.instanceMapping.get(a);
		if (imappings != null) {
			for (Iterator i = imappings.iterator(); i.hasNext(); ) {
				InstanceMapping im = (InstanceMapping)i.next();
				if (!revCurrentAssignment.containsKey(im.resource)) {
					// if resource not currently assigned
					Set free = (Set)freeResources.get(im.resource.classID);
					if (free == null) {
						free = new HashSet();
						freeResources.put(im.resource.classID, free);
						free.add(im.resource);
					}
					return new ResourceRequestResult(tm.add(im.initiationInterval), tm.add(im.latency), im.resource);			
				}
			}
		}
		
		// no instance mappings, or none of the instance-mapped resources were available
		
		ClassMapping cm = (ClassMapping)classMapping.get(new ActionClass(s.getActorClassName(), s.getActionId()));
		
		if (cm == null && imappings == null) {
			cm = new ClassMapping(s.getActorClassName(), s.getActionId(), "DEFAULT--" + s.getActorClassName() + ":" + s.getActionId(), Duration.ONE);
		}
		else if (imappings != null) 
			return new ResourceRequestResult();

		Object resClassID = cm.resourceClassID;
		Set free = (Set)freeResources.get(resClassID);
		if (free == null) {
			free = new HashSet();
			freeResources.put(resClassID, free);
		}
		if (free.isEmpty()) {
			// no resources found that have been previously allocated and are now free
			Integer usedResources = (Integer)poolUsed.get(resClassID);
			Integer maxSize = (Integer)resourceConstraints.get(resClassID);
			if (maxSize != null && usedResources.intValue() >= maxSize.intValue()) {
				// no more resources available---try again later
				return new ResourceRequestResult();
			} else {
				// either no max size, or limit not exceeded
				// therefore: allocate new resource from pool
				Resource r = new Resource(cm.resourceClassID, usedResources.intValue());
				poolUsed.put(resClassID, new Integer(usedResources.intValue() + 1));
				free.add(r);
				assert !revCurrentAssignment.containsKey(r);
				return new ResourceRequestResult(tm.add(cm.initiationInterval), tm.add(cm.latency), r);
			}
		} else {
			// there are free resources---look for resource with minimal multiplexing
			int n = -1;
			Resource r = null;
			for (Iterator i = free.iterator(); i.hasNext(); ) {
				Resource r1 = (Resource)i.next();
				Set users = (Set)assignments.get(r1);
				if (n < 0 || users.size() <= n) {
					n = users.size();
					r = r1;
				}
			}
			assert !revCurrentAssignment.containsKey(r);
			return new ResourceRequestResult(tm.add(cm.initiationInterval), tm.add(cm.latency), r);
		}
	}
	
	private void  assignResource(Step s, Resource r) {
		assert !revCurrentAssignment.containsKey(r);
		
		currentAssignment.put(s,r);
		revCurrentAssignment.put(r,s);
		Action a = new Action(s.getActorId(), s.getActorClassName(), s.getActionId());

		Set resources = (Set)mapping.get(a);
		if (resources == null) {
			resources = new HashSet();
			mapping.put(a, resources);
		}
		resources.add(r);

		Set actions = (Set)assignments.get(r);
		if (actions == null) {
			actions = new HashSet();
			assignments.put(r, actions);
		}
		actions.add(a);
		
		Set free = (Set)freeResources.get(r.classID);
		free.remove(r);
	}
	
	private void  unassignResource(Step s, Resource r) {
		currentAssignment.remove(s);
		revCurrentAssignment.remove(r);
		Set resources = (Set)freeResources.get(r.classID);
		if (resources == null) {
			resources = new HashSet();
			freeResources.put(r.classID, resources);
		}
		resources.add(r);
	}
	
	
	// step --> resource
	private Map  currentAssignment;

	// resource --> step
	private Map  revCurrentAssignment;
	
	// action --> set of resources
	private Map  mapping;
	
	// resource --> set of actions
	private Map  assignments;
	
	// resourceClassID --> set of free resources
	private Map  freeResources;

	// resourceClassID --> max pool size used
	private Map  poolUsed;

	
	
	
	// resourceClassID --> poolSize
	private Map  resourceConstraints;
	
	private boolean  allowClientSideMultiplexing; 
	
	static class HashMapWithDefault extends HashMap {
		
		public Object  get(Object k) {
			Object v = super.get(k);
			return (v == null) ? defaultValue : v;
		}
		
		HashMapWithDefault(Object defaultValue) {
			this.defaultValue = defaultValue;
		}
		
		private Object defaultValue;
	}
}
