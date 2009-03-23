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

package net.sf.opendf.cli;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.opendf.cli.lib.ActorCClassFactory;
import net.sf.opendf.cli.lib.CalActorClassFactory;
import net.sf.opendf.cli.lib.UCalActorClassFactory;
import net.sf.opendf.cli.lib.CalMLActorClassFactory;
import net.sf.opendf.cli.lib.NLClassFactory;
import net.sf.opendf.cli.lib.XDFClassFactory;
import net.sf.opendf.cli.lib.XNLClassFactory;
import net.sf.opendf.util.io.ClassLoaderStreamLocator;
import net.sf.opendf.util.io.DirectoryStreamLocator;
import net.sf.opendf.util.io.StreamLocator;
import net.sf.opendf.util.logging.Logging;


public class SimulationClassLoader extends ClassLoader {
	
	//
	//  ClassLoader
	//
	
	protected Class findClass(String name) throws ClassNotFoundException
    {
		
		Logging.dbg().fine("SimulationClassLoader:: searching: " + name);
		Class c = (Class) loadedClasses.get(name);
		if (c != null) {
			Logging.dbg().fine("SimulationClassLoader:: found again: " + name);
			return c;
		}

        // NOTE: The class loading here must mimic the behavior used
        // in net.sf.opendf.cal.xml.Util.ClasspathURIResolver so that the XSLT
        // transforms (ie synthesis path) and simulation behavior match.
		
		String baseName = name.replace('.', File.separatorChar);
		
		for (String suffix : modelClassExtensions) {
			String resName = baseName + "." + suffix;
			InputStream s = this.getModelClassAsStream(resName);
			if (s != null) {
				Logging.dbg().info("SimulationClassLoader:: Found class " + name + " at resource " + resName);

				ModelClassFactory mcf = modelClassFactories.get(suffix);

                c = mcf.createClass(name, this.topLevelClassLoader, s);
				this.loadedClasses.put(name, c);
                Logging.user().config("Loaded " + resName + " for simulation");
				return c;
			}
		}

		Logging.dbg().warning("SimulationClassLoader:: not found: " + name);
		throw new ClassNotFoundException("SimulationClassLoader: " + name);
	}
	
	//
	//  Ctor
	//
	
	public SimulationClassLoader(ClassLoader parent) {
		this (parent, new String [] {"."}, null);
	}
	
	public SimulationClassLoader(ClassLoader parent, String cachePath) {
		this (parent, new String [] {"."}, cachePath);
	}
	
	public SimulationClassLoader(ClassLoader parent, String [] modelPath) {
		this (parent, modelPath, null);
	}
		
	public SimulationClassLoader(ClassLoader parent, String [] modelPath, String cachePath) {
			
		super(parent);
		
		this.topLevelClassLoader = this;

//		addModelClassFactory("pcalml", new PCalMLActorClassFactory());
		addModelClassFactory("cal", new CalActorClassFactory(cachePath));
		addModelClassFactory("ucal", new UCalActorClassFactory(cachePath));
		addModelClassFactory("actorc", new ActorCClassFactory(cachePath));
		addModelClassFactory("ac", new ActorCClassFactory(cachePath));
		addModelClassFactory("calml", new CalMLActorClassFactory()); 
		addModelClassFactory("nl", new NLClassFactory());
		addModelClassFactory("xnl", new XNLClassFactory());
		addModelClassFactory("xdf", new XDFClassFactory());		

		for (String s : modelPath) {
			locators.add(new DirectoryStreamLocator(s));
		}
		locators.add(new ClassLoaderStreamLocator(this));
	}
	
	private InputStream  getModelClassAsStream(String name) {
		for (StreamLocator mcl : locators) {
			InputStream s = mcl.getAsStream(name);
			if (s != null)
				return s;
		}
		return null;
	}
		
	private ClassLoader topLevelClassLoader;	
	private Map<String, Class> loadedClasses = new HashMap<String, Class>();
	private Map<String, ModelClassFactory> modelClassFactories = new HashMap<String, ModelClassFactory>();	
	private List<String> modelClassExtensions = new ArrayList<String>();

	private void  addModelClassFactory(String ext, ModelClassFactory mcf) {
		if (modelClassExtensions.contains(ext)) {
			throw new RuntimeException("ModelClassFactory extension '" + ext + "' already present.");
		}
		
		modelClassExtensions.add(ext);
		modelClassFactories.put(ext, mcf);
	}
		
	private List<StreamLocator>  locators = new ArrayList<StreamLocator>();
	
}
