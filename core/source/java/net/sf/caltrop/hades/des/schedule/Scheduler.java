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

/**
 The scheduler drives the execution of DECs. Event processors register for execution and are
 scheduled according to time stamp and precedence. Precedences only affect the ordering of
 events with identical time stamps, lower precedences are schedules earlier.

 It is assumed that max one instance of an event processor is scheduled at any one time.

 @author JWJ
 @see net.sf.caltrop.hades.des.DiscreteEventComponent
 @see net.sf.caltrop.hades.des.EventProcessor
 */

public interface Scheduler {

  /** response to the execute command */
  public final int STEP = 0;
  public final int NOSTEP = 1;
  public final int ABORT = 2;

  /**
   Initializes the scheduler, clearing its event calendar.
   */
  public void initialize();

  /**
   Checks, if at least one event is contained in the event calendar.

   @return true, if event calendar contains an event.
   */
  public boolean hasEvent();

  /**
   Checks, if next event (if there is one) is 'weak' (as defined in net.sf.caltrop.hades.des.EventProcessor).

   @return true, if next event is weak. undefined, if no event exists.
   @see net.sf.caltrop.hades.des.EventProcessor
   */
  public boolean isNextWeak();

  /**
   @return the time stamp of the next event in the calendar.
   */
  public double nextEventTime();

  /**
   The current time is defined to be either (a) the time of the last event processed
   (if currently none is processed), or (b) the time of the current event (if the scheduler is
   processing one).

   @return the current time stamp.
   */
  public double currentTime();

  /**
   Executes the next event. Undefined if there is none.
   */
  public int execute();

  /**
   Schedules the event processor at time tm, with precedence 0. Equivalent to schedule(tm, 0, ep).
   */
  public void schedule(double tm, EventProcessor ep);

  /**
   Schedules the event processor at time tm, with a specific precedence. Lower precedences are
   executed first for identical time stamps.
   */
  public void schedule(double tm, double precedence, EventProcessor ep);

  /**
   Unschedules the event processor.
   */
  public void unschedule(EventProcessor ep);
  
  /**
   * Returns the number of events processed and completed so far in the simulation.
   */
  
  public long  currentEventCount();

  /**
   * Returns the number of string events processed and completed so far in the simulation.
   */
  
  public long  currentStrongEventCount();
  
  /**
   * Adds a postfire handler to be triggered at the end of the current firing 
   * (or the next, if not firing currently). This method does nothing if the handler 
   * has already been added and not triggered since then.
   * 
   * The handler will be removed after it has been triggered, so it needs to be added
   * in order to be triggered again.
   * 
   * @see PostfireHandler
   */
  
  public void  addPostfireHandler(PostfireHandler ph);

  /**
   * Removes the specified postfire handler from the set of postfire handlers to be triggered 
   * next.
   * 
   * @see PostfireHandler
   */
  
  public void  removePostfireHandler(PostfireHandler ph);  
  
  /**
   * Registers a simulation finalizer.
   */  

  public void  registerSimulationFinalizer(SimulationFinalizer sf);

  /**
   * Unregisters a simulation finalizer.
   */
  
  public void  unregisterSimulationFinalizer(SimulationFinalizer sf);
  
  /**
   * Wraps up the simulation and calls all registered simulation finalizers. It is an error
   * for the execute() method to be called after finalizeSimulation has been called.
   */

  public void  finalizeSimulation();

  /**
   * Get the value of the property identified by the key.
   * 
   * @param key The identifying key of the property.
   * @return The value of the property.
   */

  public Object  getProperty(Object key);
  
  /**
   * Set the value of the property identified by the key to the specified value.
   * 
   * @param key The identifying key of the property.
   * @param value The new value of the property.
   * @return The previous value of the property.
   */

  public Object  setProperty(Object key, Object value);
}
