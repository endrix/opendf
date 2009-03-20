package net.sf.opendf.profiler.schedule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.opendf.profiler.schedule.data.Resource;
import net.sf.opendf.profiler.data.Step;
import net.sf.opendf.profiler.data.Trace;

/**
 * 
 * @author jornj
 */
public class SchedulerWithResourceConstraints implements Scheduler {
	
	public void  schedule(Trace t, ScheduleOutput so) {

		long n = 0;
		// stepsWaiting = new ArrayList();
		stepsWaiting = new LinkedList();
		stepsExecuting = new TimedQueue();

		so.start();

		Time currentTime = Time.ZERO;

		Set initialSteps = t.initialSteps();
		addWaitingSteps(currentTime, initialSteps);

		while ((!stepsWaiting.isEmpty()) || (!stepsExecuting.isEmpty()) ) {
			
			assert stepsExecuting.isEmpty() || !stepsExecuting.nextTimeStamp().isBefore(currentTime);
			
			if ((!stepsExecuting.isEmpty()) && !currentTime.isBefore(stepsExecuting.nextTimeStamp())) {
				// dequeue next executing step
				ExecutingStep s = (ExecutingStep)stepsExecuting.dequeueNext();
				switch (s.state) {
				case ExecutingStep.STATE_INITIATING:
					// step has reached the end of its initiation interval
					resourceScheduler.endInitiation(s.step);
					s.state = ExecutingStep.STATE_FINISHING;
					if (s.reinitiationTime.isBefore(s.completionTime)) {
						stepsExecuting.add(s.completionTime, s);
						break;
					}
					// FALLTHROUGH
				case ExecutingStep.STATE_FINISHING:
					resourceScheduler.finishStep(s.step);
					
					finishTime = s.completionTime;

//					n += 1;
//					if (n % 10000 == 0) {
//						if (n % 50000 == 0) {
//							if (n % 100000 == 0) {
//								System.out.print("#");
//							} else {
//								System.out.print(":");
//							}								
//						} else {
//							System.out.print("'");
//						}
//					}
					
					// ... add all new initial steps after removing this one to the waiting queue 
					addWaitingSteps(currentTime, t.removeInitialStep(s.step));
					break;
				default:
				}
			} else {
				// find waiting step that can be executed
				int i = 0;
				WaitingStep stepFound = null;
				ResourceScheduler.ResourceRequestResult rrr = null;
				while (stepFound == null && i < stepsWaiting.size()) {
					WaitingStep ws = (WaitingStep)stepsWaiting.get(i);
					rrr = resourceScheduler.requestResourceFor(ws.step, currentTime);
					if (rrr.granted) {
						assert rrr.resource != null;
						stepFound = ws;
					} else {
						i += 1;
					}
				}
				if (stepFound != null) {
					// if step was found...
					stepsWaiting.remove(i);
					//if (stepsWaiting.size() > 0 && (stepsWaiting.size() % 1000 == 0))
					//	System.out.println("   " + stepsWaiting.size() + "waiting");
					stepsExecuting.add(rrr.reinitiationTime, new ExecutingStep(rrr.reinitiationTime, rrr.completionTime, rrr.resource, stepFound.step));
					reportStep(so, currentTime, stepFound, rrr);					
				} else {
					// no waiting step was found
					if (stepsExecuting.isEmpty()) 
						throw new RuntimeException("Deadlock at time " + currentTime + ".");
					assert !stepsExecuting.isEmpty();
					
					currentTime = stepsExecuting.nextTimeStamp();
				}
			}
		}
		resourceScheduler.writeStatistics(so);
		so.finish(finishTime);
	}
	
	private Time finishTime = Time.ZERO;
		
	public SchedulerWithResourceConstraints(ResourceScheduler rs) {
		this.resourceScheduler = rs;
	}
	
	
	private void  addWaitingSteps(Time currentTime, Set s) {
		for (Iterator i = s.iterator(); i.hasNext(); ) {
			Object v = i.next();
			stepsWaiting.add(new WaitingStep(currentTime, (Step)v));
		}		
	}
	
	private void  reportStep(ScheduleOutput so, Time currentTime, WaitingStep ws, ResourceScheduler.ResourceRequestResult rrr) {
		so.beginStep(ws.step.getID(), currentTime, currentTime.durationUntil(rrr.completionTime));
		
		so.attribute("resource-class", rrr.resource.classID);
		so.attribute("resource-instance", new Integer(rrr.resource.instanceID));
		so.attribute("enabled-since", ws.enablingTime);
		
		so.endStep();
	}
	
	
	
	
	private List 	   stepsWaiting;
	private TimedQueue stepsExecuting;
	
	private ResourceScheduler  resourceScheduler;
	
	static class WaitingStep {
		public Time  enablingTime;
		public Step  step;
		
		public WaitingStep(Time enablingTime, Step s) {
			this.enablingTime = enablingTime;
			step = s;
		}
	}
	
	static class ExecutingStep {
		public int      state;
		public Step     step;
		public Resource resource;
		public Time     reinitiationTime;
		public Time     completionTime;
		
		public final static int   STATE_INITIATING = 1;
		public final static int   STATE_FINISHING = 2;
		
		public ExecutingStep(Time reinitiationTime, Time completionTime, Resource r, Step s) {
			this.reinitiationTime = reinitiationTime;
			this.completionTime = completionTime;
			resource = r;
			step = s;
			state = STATE_INITIATING;
		}
	}
	
}


