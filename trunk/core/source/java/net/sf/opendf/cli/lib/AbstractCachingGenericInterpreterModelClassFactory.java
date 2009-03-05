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

package net.sf.opendf.cli.lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import net.sf.opendf.util.io.BufferingInputStream;
import net.sf.opendf.util.logging.Logging;

abstract public class AbstractCachingGenericInterpreterModelClassFactory extends AbstractGenericInterpreterModelClassFactory {
	
	/**
	 * Read the model object from an input stream, while producing the cached data as output.
	 * 
	 * @param is The input stream containing the original model data.
	 * @param os The output stream to which the cached data is to be written.
	 * @return The object representing the model.
	 */
	abstract protected Object     	readModelWhileCaching(InputStream is, OutputStream os);

	/**
	 * Construct the model from the cached data.
	 * 
	 * @param is The input stream containing the cached data.
	 * @return The object representing the model.
	 */
	abstract protected Object		readCachedModel(InputStream is);
		
	@Override
	public Class createClass(String name, ClassLoader topLevelLoader, InputStream source) throws ClassNotFoundException {

		if (!caching()) {
			return super.createClass(name, topLevelLoader, source);
		}
		
		try {
			Logging.dbg().fine("ACGIModelClassFactory:: Reading class " + name + "...");

			Object model = null;
			synchronized (topLevelLoader) {
				BufferingInputStream bis = new BufferingInputStream(source);
				MessageDigest md = MessageDigest.getInstance(DigestType);
				DigestInputStream dis = new DigestInputStream(bis, md);
				while (dis.read() >= 0)
					;
				byte [] digest = md.digest();
				
				InputStream cache = getCache(name, digest);
				if (cache == null) {
					bis.resetToStart();
					OutputStream os = createCache(name, digest);
					model = readModelWhileCaching(bis, os);
					bis.close();
					os.close();
					installCache(name);
					Logging.dbg().fine("ACGIModelClassFactory:: Class " + name + " cached.");
				} else {
					bis.close();
					model = readCachedModel(cache);
					cache.close();
					Logging.dbg().fine("ACGIModelClassFactory:: Reading cached version of " + name + ".");
				}
			}
			GenericInterpreterClassLoader cl = new GenericInterpreterClassLoader(topLevelLoader, name, this.getModelInterface(), 
					null, model);
			Class c = cl.getModelInterpreterClass();
			Logging.dbg().fine("ACGIModelClassFactory:: Class " + name + " loaded.");
			return c;
		}
		catch (Exception exc) {
			exc.printStackTrace();
			Logging.dbg().severe("ACGIModelClassFactory:: ERROR loading class " + name + ".");
			throw new ClassNotFoundException("Could not create class '" + name +  "'.", exc);
		}
	}
	
	protected AbstractCachingGenericInterpreterModelClassFactory(String cachePath) {
		this.cacheDir = null;
		if (cachePath != null) {
			try {
				File f = new File(cachePath);
				if (f.exists() && f.isDirectory()) {
					this.cacheDir = f;				
				} else {
					Logging.dbg().warning("Cannot cache at specified location: " + cachePath);
				}
			}
			catch (Exception e) {}
		}
	}
	
	protected boolean  caching() {
		return cacheDir != null;
	}
	
	protected InputStream  getCache(String name, byte [] digest) {
		
		try {
			byte [] cachedDigest = readDigest(name);
			if (equalDigests(digest, cachedDigest)) {
				File f = getCacheFile(name);
				if (!f.exists())
					return null;
				else 
					return new FileInputStream(f);
			} else {
				return null;
			}
		}
		catch (Exception whatever) {
			return null;
		}
	}
	
	protected OutputStream  createCache(String name, byte [] digest) throws IOException {
		File f = getCacheTempFile(name);
		if (f.exists())
			f.delete();
		OutputStream s = new FileOutputStream(f);
		writeDigest(name, digest);
		return s;
	}
	
	protected void  installCache(String name) {
		File fDigest = getDigestFile(name);
		File fTempDigest = getDigestTempFile(name);
		File fCache = getCacheFile(name);
		File fTempCache = getCacheTempFile(name);
		
		if (fDigest.exists()) {
			fDigest.delete();
		}
		if (fCache.exists()) {
			fCache.delete();
		}
		
		if (fTempCache.exists() && fTempCache.exists()) {
			fTempCache.renameTo(fCache);
			fTempDigest.renameTo(fDigest);
		}
	}
	
	protected byte [] readDigest(String name) throws IOException {
		File f = getDigestFile(name);
		if (! f.exists())
			return null;
		InputStream s = new FileInputStream(f);
		byte [] b = new byte[(int)f.length()];
		s.read(b);
		s.close();
		return b;
	}
	
	protected void  writeDigest(String name, byte [] digest) throws IOException {
		File f = getDigestTempFile(name);
		if (f.exists())
			f.delete();
		OutputStream s = new FileOutputStream(f);
		s.write(digest);
		s.close();
	}
	
	protected boolean  equalDigests(byte [] d1, byte [] d2) {
		if (d1.length != d2.length) {
			return false;
		}
		
		for (int i = 0; i < d1.length; i++) {
			if (d1[i] != d2[i])
				return false;
		}
		return true;
	}
	
	protected File  getDigestFile(String name) {
		return new File(cacheDir.getAbsolutePath() + File.separator + name + DigestSuffix);
	}

	protected File  getDigestTempFile(String name) {
		return new File(cacheDir.getAbsolutePath() + File.separator + name + DigestTempSuffix);
	}

	protected File  getCacheFile(String name) {
		return new File(cacheDir.getAbsolutePath() + File.separator + name);
	}

	protected File  getCacheTempFile(String name) {
		return new File(cacheDir.getAbsolutePath() + File.separator + name + CacheTempSuffix);
	}
	

	private File cacheDir;
	
	private final String DigestType = "SHA-512";

	private final String DigestSuffix = "--digest";
	private final String DigestTempSuffix = "--digest-temp";
	private final String CacheTempSuffix = "--temp";
}
