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


package net.sf.caltrop.hades.simulation;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import net.sf.caltrop.hades.des.DiscreteEventComponent;
import net.sf.caltrop.hades.des.schedule.AbstractObservableScheduler;
import net.sf.caltrop.hades.des.schedule.Scheduler;
import net.sf.caltrop.hades.des.schedule.SchedulerObserver;
import net.sf.caltrop.hades.des.schedule.SimpleScheduler;
import net.sf.caltrop.hades.util.NullInputStream;
import net.sf.caltrop.hades.util.NullOutputStream;

/**
 *  This generic sequential simulator class is a simple discrete event component interface
 *  that provides callbacks for handling its input and output.
 *  It simply executes its discrete event component step by step. It can be used instead of a full-fledged
 *  distributed simulation engine when only sequential execution is desired.
 *  Apart from that, it serves as an illustration of some of the concepts in this package.
 *
 *  @see Simulation.DiscreteEventComponent
 *  @see Simulation.SequentialSimulatorCallback
 *
 * @version 22-08-00 Rob add listener methods
 * @version 17-1-07 JTK added (OutputStream) cast to System.out in conditionals - compilation error?!?
 */

public class SequentialSimulator {
	
	protected Scheduler scheduler;
	protected DiscreteEventComponent component;
	protected double time;
	protected SequentialSimulatorCallback callback;
	
//	
//	sequential simulator
//	
	
	public double currentTime() {
		return time;
	}
	
	public void finalizeSimulation() {
		scheduler.finalizeSimulation();
	}
	
	/**
	 * if my default scheduler is observable then add a listener
	 */
	public void addSchedulerObserver(SchedulerObserver so) {
		if (scheduler instanceof AbstractObservableScheduler)
			((AbstractObservableScheduler) scheduler).addSchedulerObserver(so);
	}
	
	/**
	 * if my default scheduler is observable then remove a listener
	 */
	public void removeSchedulerObserver(SchedulerObserver so) {
		if (scheduler instanceof AbstractObservableScheduler)
			((AbstractObservableScheduler) scheduler).removeSchedulerObserver(so);
	}
	
	public boolean hasEvent() {
		if (scheduler.hasEvent())
			return true;
		if (callback.hasInputBefore(Double.POSITIVE_INFINITY))
			return true;
		return false;
	}
	
	public boolean isNextWeak() {return scheduler.isNextWeak();
	}
	
	/**
	 Execute component up to time maxTime
	 */
	public void run(double maxTime) {
		while (currentTime() < maxTime && (callback.hasInputBefore(maxTime) || scheduler.nextEventTime() <= maxTime))
			step(maxTime);
	}
	
	public int step() {
		return step(Double.POSITIVE_INFINITY);
	}
	
	/**
	 Execute one step of the component, if this is not later than maxTime
	 */
	public int step(double maxTime) {
		time = Math.min(maxTime, scheduler.nextEventTime());
		while (callback.hasInputBefore(time)) {
			callback.nextInput();
			time = Math.min(maxTime, scheduler.nextEventTime());
		}
		if (scheduler.hasEvent()) {
			if (scheduler.nextEventTime() <= maxTime) {
				return scheduler.execute();
			} else {
				return Scheduler.NOSTEP;
			}
		} else {
			return Scheduler.NOSTEP;
		}
	}
	
	public  Object setProperty(Object key, Object value) {
		return scheduler.setProperty(key, value);
	}
	
	public  Object  getProperty(Object key) {
		return scheduler.getProperty(key);
	}
	
//	
//	aux
//	
	
	private void initialize(double t) {
		
		scheduler.initialize();
		component.initializeState(t, scheduler);
		time = t;
		//  send initial input messages
		while (callback.hasInputBefore(time)) {
			callback.nextInput();
			time = scheduler.nextEventTime();
		}
	}
	
//	ctor
//	changed by Yan, 12/01/04
//	for adding a parameter "boolean useInteractiveScheduler" to enable interactiveScheduler
//	
	
	public SequentialSimulator(double t, DiscreteEventComponent dec, SequentialSimulatorCallback sscb, Map properties, boolean useInteractiveScheduler) {
		component = dec;
		callback = sscb;
		scheduler = null;
	
		callback.connect(component);
		scheduler = new SimpleScheduler();
		
		for (Iterator i = properties.keySet().iterator(); i.hasNext(); ) {
			Object k = i.next();
			this.setProperty(k, properties.get(k));
		}
		initialize(t);
	}
		
	public SequentialSimulator(double t, DiscreteEventComponent dec, SequentialSimulatorCallback sscb, boolean useInteractiveScheduler) {
		this(t, dec, sscb, Collections.EMPTY_MAP, useInteractiveScheduler);
	}

	public SequentialSimulator(DiscreteEventComponent dec, SequentialSimulatorCallback sscb) {
		this(0, dec, sscb, false);
	}
	
	public SequentialSimulator(double t, DiscreteEventComponent dec,
			InputStream inStream, OutputStream outStream,
			boolean useInteractiveScheduler) {
		this(t, dec, new StreamIOCallback(inStream, outStream), useInteractiveScheduler);
	}
	
	public SequentialSimulator(double t, DiscreteEventComponent dec,
			InputStream inStream, OutputStream outStream, Map properties,
			boolean useInteractiveScheduler) {
		this(t, dec, new StreamIOCallback(inStream, outStream), properties, useInteractiveScheduler);
	}
	
	public SequentialSimulator(double t, DiscreteEventComponent dec,
			InputStream inStream, OutputStream outStream) {
		this(t, dec, new StreamIOCallback(inStream, outStream), false);
	}
	
	public SequentialSimulator(double t, DiscreteEventComponent dec,
			InputStream inStream, OutputStream outStream, Map properties) {
		this(t, dec, new StreamIOCallback(inStream, outStream), properties, false);
	}
	
	public SequentialSimulator(DiscreteEventComponent dec, InputStream inStream, OutputStream outStream) {
		this(0, dec, inStream, outStream);
	}
	
	public SequentialSimulator(DiscreteEventComponent dec, InputStream inStream, OutputStream outStream, Map properties) {
		this(0, dec, inStream, outStream, properties);
	}
	
	public SequentialSimulator(double t, DiscreteEventComponent dec, Reader inReader, Writer outWriter) {
		this(t, dec, new StreamIOCallback(inReader, outWriter), false);
	}
	
	public SequentialSimulator(DiscreteEventComponent dec, Reader inReader, Writer outWriter) {
		this(0, dec, inReader, outWriter);
	}
	
	public SequentialSimulator(double t, DiscreteEventComponent dec, String inFile, String outFile) throws IOException, FileNotFoundException {

		this(t, dec, 
				(inFile == null) ? new NullInputStream() :
	                   (".".equals(inFile)) ? System.in :
	                	   new FileInputStream(inFile),
	       		(outFile == null) ? new NullOutputStream() :
	 	                   (".".equals(outFile)) ? (OutputStream) System.out :
	 	                	   new FileOutputStream(outFile));
	}
	
	public SequentialSimulator(double t, DiscreteEventComponent dec, String inFile, String outFile, Map properties) throws IOException, FileNotFoundException {
		
		this(t, dec, 
				(inFile == null) ? new NullInputStream() :
	                   (".".equals(inFile)) ? System.in :
	                	   new FileInputStream(inFile),
	       		(outFile == null) ? new NullOutputStream() :
	 	                   (".".equals(outFile)) ? (OutputStream) System.out :
	 	                	   new FileOutputStream(outFile),
    	        properties);
	}
	
	public SequentialSimulator(DiscreteEventComponent dec, String inFile, String outFile, Map properties) throws IOException, FileNotFoundException {
		this(0, dec, inFile, outFile, properties);
	}

	public SequentialSimulator(DiscreteEventComponent dec, String inFile, String outFile) throws IOException, FileNotFoundException {
		this(0, dec, inFile, outFile, Collections.EMPTY_MAP);
	}
	
	public SequentialSimulator(double t, DiscreteEventComponent dec, String inFile, String outFile, Map properties, boolean useInteractiveScheduler) throws IOException,
	FileNotFoundException {

		this(t, dec, 
				(inFile == null) ? new NullInputStream() :
	                   (".".equals(inFile)) ? System.in :
	                	   new FileInputStream(inFile),
	       		(outFile == null) ? new NullOutputStream() :
	 	                   (".".equals(outFile)) ? (OutputStream) System.out :
	 	                	   new FileOutputStream(outFile),
    	        properties, useInteractiveScheduler);
	}
	
	public SequentialSimulator(double t, DiscreteEventComponent dec, String inFile, String outFile, boolean useInteractiveScheduler) throws IOException,
	FileNotFoundException {
		this (t, dec, inFile, outFile, Collections.EMPTY_MAP, useInteractiveScheduler);
	}	
}
