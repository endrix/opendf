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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sf.caltrop.hades.des.EventProcessor;


public class VerySimpleEventCalendar implements EventCalendar {

  private double    currentTm = 0;
  private List      events  = new LinkedList();

  //
  // EventCalendar
  //

  public void     initialize() {
    events.clear();
  }

  public void     schedule(double time, double precedence, EventProcessor processor) {

    unschedule(processor);


    int i = 0;
    while(i < events.size() && time <= ((Event)events.get(i)).getTime())
      i += 1;

    events.add(i, new Event(time, processor));
  }

  public void unschedule(EventProcessor processor) {

    for (Iterator i = events.iterator(); i.hasNext(); ) {
      Event e = (Event)i.next();
      if (e.getProcessor() == processor)
	  i.remove();
    }
  }

  public boolean  isEmpty() {
    return events.isEmpty();
  }

  public boolean  isNextWeak() { return false; }  // YYY

  public double   nextTime() {
    if (isEmpty())
      return Double.POSITIVE_INFINITY;
    else
      return ((Event)events.get(0)).getTime();
  }

  public double   currentTime() { return currentTm; }
    
  public boolean     execute() {
    Event e = (Event)events.get(0);
    events.remove(0);

    currentTm = e.getTime();
    return e.execute();
  }

  public double  nextPrecedence() {
    if (isEmpty())
      return 0;
    else
      return ((Event)events.get(0)).getPrecedence();
  }

  public EventProcessor  nextEventProcessor() {
    if (isEmpty())
      return null;
    else
      return ((Event)events.get(0)).getProcessor();
  }
  
  public int  size() {
	  return events.size();
  }

  //
  // ctor
  //

  public VerySimpleEventCalendar() {}


  //
  // Event 
  //

  static class Event {

    private double time;
    private EventProcessor entity;

    public Event(double time, EventProcessor entity) { this.time = time; this.entity = entity; }

    public boolean   execute() { return entity.processEvent(time); }
    public double getTime() { return time; }
    public double getPrecedence() { return 0; }
    public EventProcessor getProcessor() { return entity; }

    public int compareTo(Object a) {
      if (!(a instanceof Event)) throw new RuntimeException("Cannot compare event to something else.");

      Event e = (Event)a;
      if (time < e.time)
	return -1;
      if (time > e.time)
	return 1;
      if (entity.hashCode() < e.entity.hashCode())
	return -1;
      if (entity.hashCode() > e.entity.hashCode())
	return 1;
      return 0;
    }

    public boolean equals(Object a) {
      if (this == a)
	return true;
      if (!(a instanceof Event)) return false;

      Event e = (Event)a;
      return (time == e.time) && (entity.hashCode() == e.entity.hashCode());
    }
  }
}


