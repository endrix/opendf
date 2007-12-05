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

import java.util.Iterator;
import java.util.Vector;

import net.sf.opendf.util.logging.Logging;

/**
 *   An AML handles the registration protocol, but leaves the actual message handling unspecified.
 */

public abstract class AbstractMessageListener implements MessageListener {
	
	protected Vector messageProducers;
	
	public void	notifyAddProducer(MessageProducer mp) {
		messageProducers.addElement(mp);
		if (messageProducers.size() > 1)
			Logging.user().warning("FAN-IN");
	}
	
	public void	notifyRemoveProducer(MessageProducer mp) {
		messageProducers.removeElement(mp);
	}
	
	public abstract void  message(MessageEvent evt);
	
	public void control(ControlEvent ce) {}
	
	public void notifyControl(ControlEvent ce) {
		for (Iterator i = messageProducers.iterator(); i.hasNext(); ) {
			MessageProducer p = (MessageProducer)i.next();
			p.control(ce);
		}
	}
	
	public void	disconnect() {
		while (!messageProducers.isEmpty()) {
			MessageProducer mp = (MessageProducer)messageProducers.firstElement();
			mp.removeMessageListener(this);
		}
	}
	
	public AbstractMessageListener() {
		messageProducers = new Vector();
	}
}



