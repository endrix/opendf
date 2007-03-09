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

import java.util.*;

/**
 *   A BMP handles the registration and notification protocol of a MessageProducer.
 */

public class BasicMessageProducer implements MessageProducer {
	
	protected Vector	listeners;
	
	public void	    addMessageListener(MessageListener ml) {
		listeners.addElement(ml);
		ml.notifyAddProducer(this);
	}
	
	public void	    removeMessageListener(MessageListener ml) {
		listeners.removeElement(ml);
		ml.notifyRemoveProducer(this);
	}
	
	public void	    notifyMessage(MessageEvent msg) {
		for (int i = 0; i < listeners.size(); i++)
			((MessageListener)listeners.elementAt(i)).message(msg);
	}
	
    public void  notifyControl(ControlEvent ce) {
    	for (Iterator i = this.listeners.iterator(); i.hasNext(); ) {
    		MessageListener l = (MessageListener)i.next();
    		l.control(ce);
    	}
    }
    
    public void control(ControlEvent ce) {
    	// DO NOTHING
    }
	
	public void	    disconnect() {
		while(!listeners.isEmpty()) {
			MessageListener ml = (MessageListener)listeners.firstElement();
			listeners.removeElementAt(0);
			ml.notifyRemoveProducer(this);
		}
	}
	
	
	public BasicMessageProducer() {
		listeners = new Vector();
	}
}


