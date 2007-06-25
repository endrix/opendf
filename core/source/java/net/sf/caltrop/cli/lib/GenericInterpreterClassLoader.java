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

package net.sf.caltrop.cli.lib;

import java.io.DataInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

import net.sf.caltrop.hades.models.ModelInterface;
import net.sf.caltrop.util.Logging;



public class GenericInterpreterClassLoader extends ClassLoader {
	
	protected Class findClass(String name) throws ClassNotFoundException {
		
		if (name.equals(this.name) || name.equals(this.actualName)) {
			return this.getModelInterpreterClass();
		}
		
		throw new ClassNotFoundException("Could not find class '" + name + "'.");
	}
	
	
	public Class getModelInterpreterClass() throws ClassNotFoundException {
		
		if (this.interpreterClass != null) {
			return this.interpreterClass;
		}
		
		try {
			Class c = this.defineClass(null, // "net.sf.caltrop.hades.models.GenericModelInterpreter",
					interpreterClassBytes, 0, interpreterClassBytes.length);
			
			Method m = c.getMethod("setModelParameters", new Class[] { Object.class, ModelInterface.class, Object.class, ClassLoader.class });
			m.invoke(null, new Object[] { 
					model,
					modelInterface,
					source,
					topLevelLoader});
			
			this.interpreterClass = c;
			this.actualName = c.getName();
			this.resolveClass(c);
			
			return c;
		} catch (Exception e) {
            Logging.dbg().throwing("GenericInterpreterClassLoader", "getModelInterpreterClass", e);
			throw new ClassNotFoundException("Cannot load interpreter class.", e);
		}
	}
	
	GenericInterpreterClassLoader(ClassLoader parent, String name, ModelInterface mi, Object source, Object model) {
		
		super(parent);
		
		this.name = name;
		this.modelInterface = mi;
		this.source = source;
		this.model = model;
		this.topLevelLoader = parent;
	}
	
	private ClassLoader topLevelLoader;
	
	private String name;
	
	private ModelInterface modelInterface;
	
	private Object source;
	
	private Object model;
	
	private Class interpreterClass = null;
	
	private String actualName = null;	
	
	private static final String GenericModelInterpreterClassFileName = "net/sf/caltrop/hades/models/GenericModelInterpreter.class";

	private static byte[] interpreterClassBytes;
	
	static {
		ClassLoader cl = GenericInterpreterClassLoader.class.getClassLoader();
		if (cl == null)
			cl = getSystemClassLoader();
		InputStream s = cl.getResourceAsStream(GenericModelInterpreterClassFileName);
		if (s == null)
			s = cl.getSystemResourceAsStream(GenericModelInterpreterClassFileName);
		if (s != null) {
			try {
				DataInputStream dis = new DataInputStream(s);
				int n = s.available();
				Logging.dbg().info("GMI length: " + n);
				interpreterClassBytes = new byte[n];
				dis.readFully(interpreterClassBytes);
			} catch (Exception e) {
				interpreterClassBytes = null;
			}
		} else
			interpreterClassBytes = null;
		
		if (interpreterClassBytes == null)
			Logging.user().warning("Could not load generic model interpreter class -- you will not be able to execute compiled model objects.");
	}
	
}
