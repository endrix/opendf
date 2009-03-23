/* 
BEGINCOPYRIGHT X,UC

	Copyright (c) 2009, EPFL
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

package net.sf.opendf.plugin;

import java.util.Map;

import net.sf.opendf.cal.ast.Actor;
import net.sf.opendf.hades.cal.CalInterpreter;
import net.sf.opendf.hades.cal.CalInterpreterIT;

/**
 * InterpreterFactory is the factory for Interpreters
 * it creates when requested the Interpreters according to enabled plug-ins 
 * 
 * @author Samuel Keller EPFL
 */

public class InterpreterFactory extends PluginFactory {
	
	/**
	 * interpreterfactory is the global factory of Interpreters
	 */
	public static InterpreterFactory interpreterfactory;
	
	public static String kind = "Interpreter";

	/**
	 * InterpreterFactory constructor takes a PluginManager as parameter that enable/disable plug-ins
	 * @param manager The PluginManager for enabling/disabling plug-ins
	 */
	public InterpreterFactory(PluginManager manager){
		super(CalInterpreter.class, manager, kind);
	}

	/**
	 * newInterpreter creates a new Interpreter according the enable/disable plug-ins
	 * @param a Basic Actor
	 * @param env Environment
	 * @return New Interpreter
	 */
	public CalInterpreterIT newInterpreter(Actor a, Map env) {
		try{
			return (CalInterpreterIT) getPluginClass().getConstructor(Actor.class, Map.class).newInstance(a,env);
		}
		catch(Exception e){
			return null;
		}
	}

	/**
	 * getNewClassLoader sends CalInterpreterIT ClassLoader to PluginManager for new Interpreter construction
	 * @return CalInterpreterIT ClassLoader
	 */
	public ClassLoader getNewClassLoader(){
		return CalInterpreterIT.class.getClassLoader();
	}
	
	/**
	 * Static initializations
	 */
	static{
		interpreterfactory = new InterpreterFactory(PluginManager.pluginManager);
	}
}