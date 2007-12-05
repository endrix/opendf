/* 
BEGINCOPYRIGHT X,ETH
	
	Copyright (c) 1999, Computer Engineering and Communication Networks Lab (TIK)
 	                    Swiss Federal Institute of Technology (ETH) Zurich, Switzerland	
	Copyright (c) 2007, Xilinx Inc.
 	All rights reserved.
	
	Redistribution and use in source and binary forms, 
	with or without modification, are permitted provided 
	that the following conditions are met:
	- Redistributions of source code must retain the above 
	  copyright notice, this list of conditions and the 
	  following disclaimer.
	- Redistributions in binary form must reproduce the 
	  above copyright notice, this list of conditions and 
	  the following disclaimer in the documentation and/or 
	  other materials provided with the distribution.
	- Neither the names of the copyright holders nor the names 
	  of contributors may be used to endorse or promote 
	  products derived from this software without specific 
	  prior written permission.
	
	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
	CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
	INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
	MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
	DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
	CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
	SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
	NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
	LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
	HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
	CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
	OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
	SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
	
ENDCOPYRIGHT
*/

package net.sf.opendf.hades.des.schedule;

import net.sf.opendf.hades.des.EventProcessor;
import net.sf.opendf.util.logging.Logging;
import java.util.logging.Level;

public class SimpleScheduler extends AbstractObservableScheduler {
	
	private EventCalendar events = new SimpleEventCalendar(); // YYY
	private long currentEventCount;
	private long currentStrongEventCount;
	
	
	public void initialize() {
		currentEventCount = currentStrongEventCount = 0;
		events.initialize();
	}
	
	public boolean hasEvent() {return!events.isEmpty();
	}
	
	public boolean isNextWeak() {return events.isNextWeak();
	}
	
	public double nextEventTime() {return events.nextTime();
	}
	
	public double currentTime() {return events.currentTime();
	}
	
	public long  currentEventCount() {
		return currentEventCount;
	}
	
	public long  currentStrongEventCount() {
		return currentStrongEventCount;
	}
	
	public int execute() {
//		System.out.println("Ex 1: " + events.size());
		boolean ret;
		try {
			boolean weak = events.isNextWeak();
			if (hasSchedulerObserver()) {
				double tm = events.nextTime();
				double prec = events.nextPrecedence();
				EventProcessor ep = events.nextEventProcessor();
				
				ret = events.execute();

				notifyExecute(tm, prec, ep, weak, ret);
			} else {
				ret = events.execute();
			}
			currentEventCount += 1;
			if (!weak)
				currentStrongEventCount += 1;
			
			postfire();
		} catch (Exception e) {
            if (Logging.dbg().isLoggable(Level.FINE))
            {
                e.printStackTrace();
            }
            
			notifyException(e);
//			System.out.println("Ex E: " + events.size());
			return Scheduler.NOSTEP;
		}
//		System.out.println("Ex 2: " + events.size());
		return ret ? Scheduler.STEP : Scheduler.NOSTEP;
	}
	
	public void schedule(double tm, EventProcessor ep) {schedule(tm, 0, ep);
	}
	
	public void schedule(double tm, double precedence, EventProcessor ep) {
		events.schedule(tm, precedence, ep);
		if (hasSchedulerObserver())
			notifySchedule(tm, precedence, ep);
	}
	
	public void unschedule(EventProcessor ep) {
		events.unschedule(ep);
		if (hasSchedulerObserver())
			notifyUnschedule(ep);
	}
	
	public SimpleScheduler() {}
}
