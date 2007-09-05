/* 
BEGINCOPYRIGHT X
	
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
	- Neither the name of the copyright holder nor the names 
	  of its contributors may be used to endorse or promote 
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

package net.sf.caltrop.hades.network;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.caltrop.hades.des.AbstractDiscreteEventComponent;
import net.sf.caltrop.hades.des.AbstractMessageListener;
import net.sf.caltrop.hades.des.BasicMessageListener;
import net.sf.caltrop.hades.des.BasicMessageProducer;
import net.sf.caltrop.hades.des.DiscreteEventComponent;
import net.sf.caltrop.hades.des.MessageEvent;
import net.sf.caltrop.hades.des.MessageHandler;
import net.sf.caltrop.hades.des.MessageListener;
import net.sf.caltrop.hades.des.MessageProducer;
import net.sf.caltrop.hades.des.schedule.Scheduler;

import net.sf.caltrop.util.source.ParserErrorException;
import net.sf.caltrop.util.source.LoadingErrorException;
import net.sf.caltrop.util.source.LoadingErrorRuntimeException;


public class Network extends AbstractDiscreteEventComponent {
	
	//
	//  DEC
	//

	public void initializeState(double t, Scheduler s) {
		
		try {
			creator.createNetwork(this, t, s, env, loader);
			
			for (Iterator i = processes.iterator(); i.hasNext(); ) {
				DiscreteEventComponent dec = (DiscreteEventComponent) i.next();
				dec.initializeState(t, s);
			}
			initialized = true;
			}
        catch (RuntimeException re)
        {
            // Allows detailed runtime exceptions created during
            // parsing to pass through to where they are handled
            throw re;
        }
		catch (Exception e) {
			throw new RuntimeException("Failed to initialize network: " + e.getMessage());
		}
	}
	
	public boolean isInitialized() {
		return initialized;
	}
	
	
	//
	//  Network
	//

	
	public int addProcess(DiscreteEventComponent p) {
		
		int n = processes.size();
		
		processes.add(p);
		return n;
	}

	public void addConnection(DiscreteEventComponent src, String srcConnector,
			DiscreteEventComponent dst, String dstConnector) {
		MessageListener listener;
		MessageProducer producer;
		
		if (src == null) {
			NetworkInput in = (NetworkInput) inputs.getConnector(srcConnector);
			if (in == null) {
				throw new RuntimeException("Undefined network input port: " + srcConnector + "(network '" + this.getName() + "')");
			}
			producer = in.getProducer();
		} else {
			producer = src.getOutputConnectors().getConnector(srcConnector);
			if (producer == null) {
				throw new RuntimeException("Undefined output port '" + srcConnector 
						                 + "' of actor '" + src.getName() + "'");
			}
		}
		
		if (dst == null) {
			NetworkOutput out = (NetworkOutput) outputs.getConnector(dstConnector);
			if (out == null) {
				throw new RuntimeException("Undefined network output port: " + dstConnector + "(network '" + this.getName() + "')");
			}
			listener = out.getListener();
		} else {
			listener = dst.getInputConnectors().getConnector(dstConnector);
			if (listener == null) {
				throw new RuntimeException("Undefined input port '" + dstConnector 
						                 + "' of actor '" + dst.getName() + "'");
			}
		}
		
		if (producer == null) {
			throw new RuntimeException("Cannot locate source: [" + src + "::" + srcConnector + "]");
		}
		if (listener == null) {
			throw new RuntimeException("Cannot locate destination: [" + dst + "::" + dstConnector + "]");
		}
		
		producer.addMessageListener(listener);
	}
	
	
	//
	//  Ctor
	//
	
	public Network(Creator creator, Set inputPorts, Set outputPorts, Map env, ClassLoader loader) {
		
		this.processes = new ArrayList();
		this.env = env;
		this.creator = creator;
		this.initialized = false;
		this.loader = loader;
		
		for (Iterator i = inputPorts.iterator(); i.hasNext();) {
			inputs.addConnector((String) i.next(), new NetworkInput());
		}
		
		for (Iterator i = outputPorts.iterator(); i.hasNext();)
			outputs.addConnector((String) i.next(), new NetworkOutput());
	}

	
	
	private Creator creator;
	private boolean initialized;
	private List  processes;
	private Map env;
	private ClassLoader loader;
	
	
	/**
	 * The Creator generates the network when 
	 */
	
	public interface Creator {
		void  createNetwork(Network n, double t, Scheduler s, Map env, ClassLoader loader);
	}
	
	private static class NetworkInput extends AbstractMessageListener {
		
		MessageProducer producer = new BasicMessageProducer();
		
		public void message(MessageEvent msg) {
			producer.notifyMessage(msg);
		}
		
		public MessageProducer getProducer() {
			return producer;
		}
		
	}
	
	private static class NetworkOutput extends BasicMessageProducer implements MessageHandler {
		
		MessageListener listener;
		
		public void handleMessage(MessageEvent msg) {
			notifyMessage(msg);
		}
		
		public MessageListener getListener() {
			return listener;
		}
		
		NetworkOutput() {
			listener = new BasicMessageListener(this);
		}
	}
}
