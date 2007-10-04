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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

import net.sf.caltrop.cal.ast.Actor;
import net.sf.caltrop.cal.interpreter.util.ASTFactory;
import net.sf.caltrop.cal.util.SourceReader;
import net.sf.caltrop.hades.models.ModelInterface;
import net.sf.caltrop.hades.models.lib.CalModelInterface;
import net.sf.caltrop.util.logging.Logging;
import net.sf.caltrop.util.source.MultiErrorException;
import net.sf.caltrop.util.xml.Util;

import org.w3c.dom.Document;
import org.w3c.dom.Node;


public class CalActorClassFactory extends AbstractCachingGenericInterpreterModelClassFactory {
	
	
	@Override
	protected ModelInterface getModelInterface() {
		return MI;
	}

	@Override
	protected Object readModel(InputStream modelSource)
    {
        Object o = null;
        try
        {
            o = SourceReader.readActor(new InputStreamReader(modelSource), getResourceName());
        }
        catch (MultiErrorException mee)
        {
            throw new RuntimeException(mee);
        }
        
		return o;
	}

	@Override
	protected Object readCachedModel(InputStream cachedModelSource) {
		return SourceReader.readPreprocessedActorML(cachedModelSource);
	}

	@Override
	protected Object readModelWhileCaching(InputStream is, OutputStream os)
    {
        Document doc = null;
        try 
        {
            doc = SourceReader.parseActor(new InputStreamReader(is), getResourceName());
        }
        catch (MultiErrorException mee)
        {
            throw new RuntimeException(mee);
        }
        
        Node a = ASTFactory.preprocessActor(doc);
        
        try
        {
            String result = Util.createXML(a);
            PrintWriter pw = new PrintWriter(os);
            pw.println(result);
            pw.flush();
        }
        catch (Exception te)
        {
            // Compilation succeeded which is all that is necessary
            // for runtime.  If this exception occurs, warn the user
            // and bail out of writing.
            Logging.user().severe("Could not cache actor: " + te.getMessage());
            Logging.user().severe("Processing will continue, but no cache file was written");
        }
        
        Actor actor = ASTFactory.buildPreprocessedActor(a);
        return actor;
	}
	

	
	public CalActorClassFactory() {
		this (null);
	}
	
	public CalActorClassFactory(String cachePath) {
		super (cachePath);
	}

    // A CalModelInterface which does NOT use the moses legacy mode
    // for actors with no package declaration
	private static final ModelInterface MI = new CalModelInterface();

}
