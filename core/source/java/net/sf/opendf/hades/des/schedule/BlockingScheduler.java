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

import net.sf.caltrop.hades.des.EventProcessor;


public class BlockingScheduler extends AbstractScheduler implements Scheduler, EventProcessor {
	
	private double nextEventTime;
	private Scheduler parent;
	private Scheduler inner;
	
	protected BlockingScheduler(Scheduler parent, Scheduler inner) {
		this.parent = parent;
		this.inner = inner;
		nextEventTime = inner.nextEventTime();
	}
	
	public BlockingScheduler(Scheduler parent) {
		this(parent, new SimpleScheduler());
	}
	
	public void initialize() {
		inner.initialize();
	}
	
	public boolean hasEvent() {
		return inner.hasEvent();
	}
	
	public boolean isNextWeak() {
		return inner.isNextWeak();
	}
	
	public double nextEventTime() {
		return nextEventTime;
	}
	
	public double currentTime() {
		return inner.currentTime();
	}
	
	public long currentEventCount() {
		return inner.currentEventCount();
	}
	
	public long currentStrongEventCount() {
		return inner.currentStrongEventCount();
	}
	
	public int execute() {
		return inner.execute();
	}
	
	public void schedule(double tm, EventProcessor ep) {
		schedule(tm, 0, ep);
	}
	
	public void schedule(double tm, double precedence, EventProcessor ep) {
		inner.schedule(tm, precedence, ep);
		double t = inner.nextEventTime();
		if (t != nextEventTime) {
			parent.schedule(tm, precedence, this);
			nextEventTime = t;
		}
	}
	
	public void unschedule(EventProcessor ep) {
		inner.unschedule(ep);
		double tm = inner.nextEventTime();
		if (tm != nextEventTime) {
			if (inner.hasEvent())
				parent.schedule(tm, this);
			else
				parent.unschedule(this);
			nextEventTime = tm;
		}
	}
	
	public boolean processEvent(double time) {
		boolean ret = false;
		while (inner.nextEventTime() <= time) {
			if (inner.execute() == Scheduler.STEP) {
				ret = true;
			}
		}
		nextEventTime = inner.nextEventTime();
		return ret;
	}
	
	public boolean isWeakEvent() {
		return inner.isNextWeak();
	}
	
}
