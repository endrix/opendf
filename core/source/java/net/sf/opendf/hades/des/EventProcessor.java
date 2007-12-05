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

package net.sf.opendf.hades.des;

/**
  Event processors are called by the scheduler to execute events. Every 'active' object must implement
  this interface.

  @author JWJ
  @see net.sf.opendf.hades.des.schedule.Scheduler
  */

public interface EventProcessor {

  /**
    Executes whatever action is associated to this event. It returns an indication whether the system 
    state was affected by this event. The system state is an abstractly defined entity -- it relates to
    the concept of 'state' of the respective model of computation. The return value may, in general, not
    be exact. In cases where the event processor cannot determine whether relevant changes occurred, it should
    return 'true' by default. This is always a safe value.

    The return value should not affect the behavior of the simulated system -- it is used to speed up 
    animation etc.

    @return true, if the system state has changed in the course of the execution of this event.
    */
  public boolean processEvent(double time);

  /**
    Determines, whether the event currently scheduled is 'weak'. An event is defined to be weak if it is 
    guaranteed not to change the system state. If this operation returns true, processEvent() should return
    false. If it returns false, processEvent() may returns any value.

    This method is used to be able to recognize 'administrative' events up front, so all bookkeeping can be 
    performed before the next 'real' event.

    @return true, if this event is weak.
    */
  public boolean isWeakEvent();
}


