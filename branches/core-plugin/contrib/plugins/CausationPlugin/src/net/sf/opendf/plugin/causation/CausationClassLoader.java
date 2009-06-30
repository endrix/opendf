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

package net.sf.opendf.plugin.causation;

import javax.swing.JFileChooser;

import net.sf.opendf.plugin.InterpreterFactory;
import net.sf.opendf.plugin.PluginClassLoader;

/**
 * CausationClassLoader gives the CalInterpreterCT class pointer
 * @author Samuel Keller EPFL
 */
public class CausationClassLoader implements PluginClassLoader {
	
	private JFileChooser dirchooser = new JFileChooser();
	
	/**
	 * Default constructor
	 */
	public CausationClassLoader(){
		dirchooser.setCurrentDirectory(new java.io.File("."));
        dirchooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        dirchooser.setAcceptAllFileFilterUsed(false);
        dirchooser.setDialogTitle("Select output folder");
	}
	
	/**
	 * Return if the "Interpreter" class is requested CalInterpreterCT class
	 * @param kind Kind of core Plug-in requested
	 * @return CalInterpreterCT class pointer
	 */
	public Class<?> getPluginClass(String kind){
		if(kind.equals(InterpreterFactory.kind))
			return CalInterpreterCT.class;
		return null;
	}
	
	/**
	 * Open output folder selection on activation
	 * @param active New status of activation
	 */
	public void activate(boolean active){
		if(active){
			String path = "";
			if (dirchooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
				path = dirchooser.getSelectedFile().getPath();
			CalInterpreterCT.setPath(path);
			XmlCausationTraceBuilder.setPath(path);
		}
	}

}
