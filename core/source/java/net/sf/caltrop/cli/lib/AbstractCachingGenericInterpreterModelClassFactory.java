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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import net.sf.caltrop.util.Logging;
import net.sf.caltrop.util.MultiErrorException;

abstract public class AbstractCachingGenericInterpreterModelClassFactory extends AbstractGenericInterpreterModelClassFactory {
	
	/**
	 * Read the model object from an input stream, while producing the cached data as output.
	 * 
	 * @param is The input stream containing the original model data.
	 * @param os The output stream to which the cached data is to be written.
	 * @return The object representing the model.
	 */
	abstract protected Object     	readModelWhileCaching(InputStream is, OutputStream os) throws MultiErrorException;

	/**
	 * Construct the model from the cached data.
	 * 
	 * @param is The input stream containing the cached data.
	 * @return The object representing the model.
	 */
	abstract protected Object		readCachedModel(InputStream is);
		
	public Class createClass(String name, ClassLoader topLevelLoader, InputStream source) throws ClassNotFoundException {
		//
		// FIXME: Caching TBD.
		//
		return super.createClass(name, topLevelLoader, source);
	}
	
	protected AbstractCachingGenericInterpreterModelClassFactory(String cachePath) {
		this.cachePath = null;
		if (cachePath != null) {
			try {
				File f = new File(cachePath);
				if (f.exists() && f.isDirectory()) {
					this.cachePath = cachePath;				
				} else {
					Logging.dbg().warning("Cannot cache at specified location: " + cachePath);
				}
			}
			catch (Exception e) {}
		}
	}

	private String cachePath;
}
