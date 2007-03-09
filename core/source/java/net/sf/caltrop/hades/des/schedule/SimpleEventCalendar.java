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

import java.util.*;

import net.sf.caltrop.hades.des.*;


public class SimpleEventCalendar implements EventCalendar {

  private SortedSet      events  = new TreeSet();
  private Map            eventEntry = new HashMap();
  private Event          nextEvent = null;
  private double         currentTm = 0;

  //
  // EventCalendar
  //

  public void     initialize() {
    events.clear();
    eventEntry.clear();
    nextEvent = null;
  }

  public void     schedule(double time, double precedence, EventProcessor processor) {
    unschedule(processor);

    Event e = new Event(time, precedence, processor);
    eventEntry.put(processor, e);

    if (nextEvent == null) {
      nextEvent = e;
    } else if (e.compareTo(nextEvent) < 0) {
      events.add(nextEvent);
      nextEvent = e;
    } else {
      events.add(e);
    }
  }

  public void unschedule(EventProcessor processor) {
    if (nextEvent == null)
      return;
    Event e = (Event)eventEntry.get(processor);
    if (e == null)
      return;
	
    if (nextEvent == e) {
      if (events.isEmpty()) {
	nextEvent = null;
      } else {
	nextEvent = (Event)events.first();
	events.remove(nextEvent);
      }
    } else
      events.remove(e);
    eventEntry.remove(processor);
  }

  public boolean  isEmpty() {
    return nextEvent == null;
  }

  public boolean  isNextWeak() {
    return (nextEvent != null) ? nextEvent.isWeak() : false;
  }

  public double   nextTime() {
    if (isEmpty())
      return Double.POSITIVE_INFINITY;
    else
      return nextEvent.getTime();
  }

  public double   currentTime() { return currentTm; }
    
  public boolean  execute() {
    Event e = nextEvent;

    if (events.isEmpty()) {
      nextEvent = null;
    }
    else {
      nextEvent = (Event)events.first();
      events.remove(nextEvent);
    }
    eventEntry.remove(e.getProcessor());

    currentTm = e.getTime();
    return e.execute();
  }

  public double  nextPrecedence() {
    if (isEmpty())
      return 0;
    else
      return nextEvent.getPrecedence();
  }

  public EventProcessor  nextEventProcessor() {
    if (isEmpty())
      return null;
    else
      return nextEvent.getProcessor();
  }
  
  public int  size() {
	  return events.size();
  }

  //
  // ctor
  //

  public SimpleEventCalendar() {}


  //
  // Event 
  //

  static class Event implements Comparable {

    private static int counter = 0;

    private double time;
    private double precedence;
    private EventProcessor entity;

    private int hashCode;

    public Event(double time, double precedence, EventProcessor entity) { 
      this.time = time; this.precedence = precedence; this.entity = entity;
      this.hashCode = counter ++;
    }

    public boolean execute() { return entity.processEvent(time); }
    public double  getTime() { return time; }
    public double  getPrecedence() { return precedence; }
    public boolean isWeak() { return entity.isWeakEvent(); }
    public EventProcessor getProcessor() { return entity; }

    public int compareTo(Object a) {
      if (!(a instanceof Event)) throw new RuntimeException("Cannot compare event to something else.");

      Event e = (Event)a;
      if (time < e.time)
	return -1;
      if (time > e.time)
	return 1;
      if (precedence < e.precedence)
	return -1;
      if (precedence > e.precedence)
	return 1;
      if (hashCode < e.hashCode)
	return -1;
      if (hashCode > e.hashCode)
	return 1;
      return 0;
    }

    public int hashCode() { return hashCode; }

    public boolean equals(Object a) {
      if (this == a)
	return true;
      if (!(a instanceof Event)) return false;

      Event e = (Event)a;
      return (time == e.time) && (entity.hashCode() == e.entity.hashCode());
    }
    /*MN debug
    public String toString() {
      return new String(entity+" @ "+time);
    }
    */
  }
}


