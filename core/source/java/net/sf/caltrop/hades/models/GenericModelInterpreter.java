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

package net.sf.caltrop.hades.models;


import java.util.Collections;
import java.util.Map;

import net.sf.caltrop.hades.des.AbstractDiscreteEventComponent;
import net.sf.caltrop.hades.des.DiscreteEventComponent;
import net.sf.caltrop.hades.des.schedule.Scheduler;



public class GenericModelInterpreter extends AbstractDiscreteEventComponent {
	
	//
	// DEC
	//

	public void initializeState(double t, Scheduler s) {
		dec.initializeState(t, s);
	}

	public boolean isInitialized() {
		return dec.isInitialized();
	}

	//
	//  Ctor
	//
	
	public GenericModelInterpreter() {
		this (Collections.EMPTY_MAP);
	}

	public GenericModelInterpreter(Object pars) {
		dec = instantiate((Map) pars, loader);
		this.inputs = dec.getInputConnectors();
		this.outputs = dec.getOutputConnectors();
	}
	
	//
	// Misc
	//

	public String toString() {
		String name = modelInterface.getName(modelSource);
		return (name == null ? "Component without name" : name) + "@" + Integer.toHexString(hashCode());
	}

	private DiscreteEventComponent		dec;
	
	/******************************************************
	 * This is the static part of the GMI                 *
	 *****************************************************/

	protected static DiscreteEventComponent instantiate(Map env, ClassLoader loader) {
		return modelInterface.instantiate(modelSource, env, locationMap, loader);
	}

	public static void setModelParameters(Object source, ModelInterface mi, Object sourceRep, ClassLoader classloader) {
		try {
			modelSource = source;
			modelInterface = mi;
			locationMap = mi.createLocationMap(source);
			sourceRepresentation = sourceRep;
			loader = classloader;
		} catch (Exception e) {
			throw new RuntimeException("Could not initialize compiled model class.", e);
		}	  
	}

	private static Object modelSource;

	private static Map locationMap;

	private static ModelInterface modelInterface;

	private static Object sourceRepresentation;
	
	private static ClassLoader loader;

}