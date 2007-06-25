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


package net.sf.caltrop.hades.des;


import java.util.Iterator;

import net.sf.caltrop.hades.des.schedule.Scheduler;

/**
 *  A wrapper to be used around a DiscreteEventComponent. A VDEC allows scaling and offsetting the time
 *  axis of its embedded DEC wrt. to the surrounding DEC, thus providing a 'virtual' view of the time scales.
 *
 *  
 *  CAUTION: This might NOT work if components exchange information as to their respective absolute local clock.
 *
 *  @see DiscreteEventComponent
 */

public class VirtualDiscreteEventComponent extends AbstractDiscreteEventComponent {
	
	private DiscreteEventComponent		dec;
	
	private boolean		    isStopped;
	private double		    localTime;
	private double		    globalTime;
	private double		    timeScale;
	
	//
	//  DEC
	//
	
	
	public void   disconnect() {
		inputs.disconnect();
	}
	
	public void   initializeState(double t, Scheduler s) {
		dec.initializeState(toLocal(t), s); // YYY
	}
	
	public boolean  isInitialized() { return dec.isInitialized(); }
	
	
	
	//
	//  VDEC
	//
	
	public void   start(double gtm) {
		if (!isStopped)
			throw new vdecException();
		
		globalTime = gtm;
		isStopped = false;
	}
	
	public void   stop(double gtm) {
		if (isStopped)
			throw new vdecException();
		
		localTime = toLocal(gtm);
		globalTime = gtm;
		isStopped = true;
	}
	
	public void   setScale(double gtm, double r) {
		localTime = toLocal(gtm);
		globalTime = gtm;
		timeScale = r;
	}
	
	public double getScale() {
		return timeScale;
	}
	
	
	//
	//  ctor
	//
	
	public VirtualDiscreteEventComponent(DiscreteEventComponent c) {
		this(c, 0);
	}
	
	public VirtualDiscreteEventComponent(DiscreteEventComponent c, double gtm) {
		this(c, gtm, 1);
	}
	
	public VirtualDiscreteEventComponent(DiscreteEventComponent c, double gtm, double scale) {
		dec = c;
		localTime = 0;
		globalTime = gtm;
		timeScale = scale;
		isStopped = false;
		
		InputConnectors in = dec.getInputConnectors();
		Iterator ks = in.keySet().iterator();
		while (ks.hasNext()) {
			String k = (String)ks.next();
			MessageProducer producer = new BasicMessageProducer();
			producer.addMessageListener(in.getConnector(k));
			MessageListener listener = new vdecInMessageListener(producer);
			inputs.addConnector(k, listener);
		}
		
		OutputConnectors out = dec.getOutputConnectors();
		ks = out.keySet().iterator();
		while (ks.hasNext()) {
			String k = (String)ks.next();
			MessageProducer producer = new BasicMessageProducer();
			outputs.addConnector(k, producer);
			MessageListener listener = new vdecOutMessageListener(producer);
			out.getConnector(k).addMessageListener(listener);
		}
		
	}
	
	
	//
	//  time management
	//
	
	private class vdecInMessageListener extends AbstractMessageListener {
		
		private MessageProducer	producer;
		
		public void message(MessageEvent msg) {
			producer.notifyMessage(new MessageEvent(this, toLocal(msg.time), msg.value, msg.sd));
		}
		
		public void control(ControlEvent ce) {
			producer.notifyControl(ce);
		}
		
		vdecInMessageListener(MessageProducer mp) {
			producer = mp;
		}
	}
	
	private class vdecOutMessageListener extends AbstractMessageListener {
		
		private MessageProducer	producer;
		
		public void message(MessageEvent msg) {
			producer.notifyMessage(new MessageEvent(this, toGlobal(msg.time), msg.value, msg.sd));
		}
		
		public void control(ControlEvent ce) {
			producer.notifyControl(ce);
		}
		
		vdecOutMessageListener(MessageProducer mp) {
			producer = mp;
		}
	}
	
	private double  toLocal(double gtm) {
		double t = gtm - globalTime;
		
		if (t == 0)
			return localTime;
		else
			return localTime + ((gtm - globalTime) / timeScale);
	}
	
	private double  toGlobal(double ltm) {
		return globalTime + ((ltm - localTime) * timeScale);
	}
	
	//  
	//  exceptions
	//
	
	static class vdecException extends RuntimeException {
	}
	
}




