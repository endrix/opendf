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

package net.sf.caltrop.hades.des.schedule;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.caltrop.hades.des.EventProcessor;

public abstract class AbstractObservableScheduler extends AbstractScheduler 
                                                  implements ObservableScheduler {
	
	private List observers = null;
	
	public void  addSchedulerObserver(SchedulerObserver so) {
		if (observers == null)
			observers = new ArrayList();
		
		if (!observers.contains(so))
			observers.add(so);
	}
	
	public void  removeSchedulerObserver(SchedulerObserver so) {
		if (observers == null)
			return;
		
		observers.remove(so);
		if (observers.size() == 0)
			observers = null;
	}
	
	public boolean  hasSchedulerObserver() {
		return observers != null;
	}
	
	public void     notifyException(Exception e) {
		if (observers == null)
			return;
		
		double tm = currentTime();
		for (Iterator i = observers.iterator(); i.hasNext(); ) {
			SchedulerObserver so = (SchedulerObserver)i.next();
			so.schedulerException(tm, e);
		}
	}
	
	public void     notifySchedule(double time, double precedence, EventProcessor ep) {
		if (observers == null)
			return;
		
		double tm = currentTime();
		for (Iterator i = observers.iterator(); i.hasNext(); ) {
			SchedulerObserver so = (SchedulerObserver)i.next();
			so.schedulerSchedule(tm, time, precedence, ep);
		}
	}
	
	
	public void     notifyUnschedule(EventProcessor ep) {
		if (observers == null)
			return;
		
		double tm = currentTime();
		for (Iterator i = observers.iterator(); i.hasNext(); ) {
			SchedulerObserver so = (SchedulerObserver)i.next();
			so.schedulerUnschedule(tm, ep);
		}
	}
	
	
	public void     notifyExecute(double time, double precedence, EventProcessor ep, 
			boolean weak, boolean result) {
		if (observers == null)
			return;
		
		for (Iterator i = observers.iterator(); i.hasNext(); ) {
			SchedulerObserver so = (SchedulerObserver)i.next();
			so.schedulerExecute(time, precedence, ep, weak, result);
		}
	}
}


