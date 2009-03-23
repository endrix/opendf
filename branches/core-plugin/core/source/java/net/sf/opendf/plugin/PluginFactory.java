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

/**
 * PluginFactory is the abstract class to be used to create new Factories
 * it provides interaction to PluginManager and priorities support
 * @author Samuel Keller EPFL
 */

public abstract class PluginFactory {

	/**
	 * manager is the current PluginManager used for enabling/disabling of plug-ins
	 */
	private PluginManager manager;

	/**
	 * defaultclass stores the default class to be constructed (no available plug-ins activated) 
	 */
	private Class<?> defaultclass;
	
	/**
	 * kind of plug-in
	 */
	private String kind;

	/**
	 * PluginFactory unique constructor to be called by new Factories
	 * @param defaultclass Default class to be constructed (no available plug-ins activated) 
	 * @param manager PluginManager used for enabling/disabling of plug-ins
	 * @param kind Kind of plug-in
	 */
	public PluginFactory(Class<?> defaultclass,PluginManager manager, String kind){
		this.manager = manager;
		this.defaultclass = defaultclass;
		this.kind = kind;
	}

	/**
	 * getPluginClass sends current Class to be used for construction
	 * according to enabling/disabling of plug-ins (called by new Factories)
	 * @return current Class to be used for construction according to enabling/disabling of plug-ins
	 */
	protected Class<?> getPluginClass() {
		return manager.getPluginClass(kind, defaultclass);		
	}
	
	/**
	 * getNewClassLoader sends Object Interfaces ClassLoader to PluginManager for new Object construction
	 * (called by PluginManager, defined in the new Factories)
	 * @return Object Interfaces ClassLoader to PluginManager for new Object construction
	 */
	public abstract ClassLoader getNewClassLoader();
}