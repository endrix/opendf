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
import java.util.Vector;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

/**
 * PluginManager is the general plug-ins manager
 * - gets and stores all the available plug-ins
 * - loads plug-ins classes
 * - provides enabling/disabling of plug-ins
 * @author Samuel Keller EPFL
 */
public class PluginManager {

	/**
	 * pluginManager is the global plug-in manager
	 */
	public static PluginManager pluginManager;
	
	/**
	 * priorityList Contains the plug-in kind related priorities informations
	 */
	protected static Map<String,Map<Integer,String>> priorityList;

	/**
	 * pluginList Contains all the available plug-ins list
	 */
	protected static Vector<String> pluginList;

	/**
	 * classList Contains all the available plug-ins class (available only after Factories registration)
	 */
	protected static Map<String,Map<String,Class<?>>> classList;
	
	/**
	 * descriptions 
	 */
	protected static Vector<String> descriptions;
	
	/**
	 * enableList Contains the enable/disable information
	 */
	protected Map<String,Boolean> enableList;
	
	/**
	 * Default constructor
	 **/
	public PluginManager(){
		enableList = new TreeMap<String,Boolean>();
	}

	/**
	 * Get all the plug-ins names
	 * @return All the plug-ins names
	 */
	public static Vector<String> getPluginList() {
		return pluginList;
	}
	
	/**
	 * Get all the plug-ins descriptions
	 * @return All the plug-ins descriptions
	 */
	public static Vector<String> getDescriptions() {
		return descriptions;
	}
	
	/**
	 * getPluginClass sends current Class to be used for construction
	 * according to enabling/disabling of plug-ins (called by new Factories)
	 * @param kind Kind of plug-in required
	 * @param defaultclass Default class to be constructed (no available plug-ins activated)
	 * @return current Class to be used for construction according to enabling/disabling of plug-ins
	 */
	public Class<?> getPluginClass(String kind, Class<?> defaultclass){
		if(priorityList.get(kind)==null)
			return defaultclass;
		for(String plugin:priorityList.get(kind).values()){
			if(getEnable(plugin))
				return classList.get(kind).get(plugin);
		}
		return defaultclass;
	}
	
	/**
	 * setEnable sets if the given plug-in is enabled or not
	 * @param plugin plug-in to set
	 * @param enable enabling/disabling
	 */
	public void setEnable(String plugin, Boolean enable) {
		enableList.put(plugin,enable);
	}

	/**
	 * getEnable return if the given plug-in is enabled or not
	 * @param plugin plug-in to check
	 * @return if the given plug-in is enabled or not
	 */
	public Boolean getEnable(String plugin) {
		Boolean result = null;
		try{
			result = enableList.get(plugin);
		}
		catch(Exception e){}
		if(result!=null)
			return result;
		return false;
	}
	
	/**
	 * Add a new PLug-in in the structure
	 * @param name Plug-in name
	 * @param description Plug-in description
	 */
	private static void addPlugin(String name, String description) {
		pluginList.add(name);
		descriptions.add(description);
	}
	
	/**
	 * Add a Class to a Plug-in
	 * @param name Plug-in name
	 * @param kind Kind of class to add
	 * @param priority Priority to apply in case of multiple matches
	 * @param clazz Class to load
	 */
	private static void addPluginClass(String name, String kind, Integer priority ,Class<?> clazz) {
		Map<String,Class<?>> cl = classList.get(kind);
		Map<Integer,String> prior = priorityList.get(kind);
		if(cl==null)
			cl = new TreeMap<String,Class<?>>();
		if(prior==null)
			prior = new TreeMap<Integer,String>();
		cl.put(name, clazz);
		prior.put(priority, name);
		classList.put(kind, cl);
		priorityList.put(kind, prior);
	}

	
	/**
	 * Static initializations + pre-loading of Plug-ins
	 */
	static{
		pluginManager = new PluginManager();
		pluginList = new Vector<String>();
		descriptions = new Vector<String>();
		priorityList = new TreeMap<String,Map<Integer,String>>();
		classList = new TreeMap<String,Map<String,Class<?>>>();
		// Get the plug-in list
		IConfigurationElement[] contributions = 
			Platform.getExtensionRegistry().getConfigurationElementsFor("net.sf.opendf.eclipse.plugin.OpendfPlugin.odfcoreplugin");
		// Add each plug-in
		for (int i = 0; i < contributions.length; i++) {
			String name = contributions[i].getAttribute("name");
			try {
				// Get the class loader
				PluginClassLoader classloader = 
	               (PluginClassLoader)contributions[i].createExecutableExtension("classloader");
				addPlugin(name, contributions[i].getAttribute("description"));
				
				// Get all the kinds related
				IConfigurationElement[] children = contributions[i].getChildren();
				for(int j = 0; j < children.length; j++){
					String kind = children[j].getAttribute("kind");
					addPluginClass(name, kind, 
							Integer.parseInt(children[j].getAttribute("priority")), 
							classloader.getPluginClass(kind));
				}
			 } catch (CoreException e) {
				 System.out.println("ERROR LOADING PLUGIN");
		            
			 }	
		}
	}
}