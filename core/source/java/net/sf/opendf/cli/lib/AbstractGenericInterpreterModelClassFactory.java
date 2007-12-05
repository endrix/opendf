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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import net.sf.opendf.cli.ModelClassFactory;
import net.sf.opendf.hades.models.ModelInterface;
import net.sf.opendf.util.logging.Logging;
import net.sf.opendf.util.exception.LocatableException;

/**
 * This class builds a model class from a generic interpreter, which is loaded by the {@link GenericInterpreterClassLoader},
 * and which is given model data and a {@link ModelInterface}.
 * 
 * Implementors of this class only need to specify a way to produce an object representing the model from an InputStream,
 * and a model interface.
 * 
 * @author jornj
 * @see GenericInterpreterClassLoader
 * @see ModelInterface
 *
 */

public abstract class AbstractGenericInterpreterModelClassFactory implements ModelClassFactory {

	private String resName = "unknown";
    
	/**
	 * Read the model from aa input stream, and construct an object representing it.
	 * 
	 * @param modelSource The input stream containing the model data.
	 * @return An object representing the model.
	 * @throws IOException
	 */
	abstract protected Object  readModel(InputStream modelSource) throws IOException;

    protected String getResourceName ()
    {
        return this.resName;
    }
    
	/**
	 * Return the model interface corresponding to the models read by this factory.
	 * 
	 * @return The model interface.
	 */
	abstract protected ModelInterface  getModelInterface();

	public Class createClass(String name, ClassLoader topLevelLoader, InputStream source) throws ClassNotFoundException
    {
        this.resName = name;
        
        Logging.dbg().fine("AGIModelClassFactory:: Reading class " + name + "...");
        Object model = null;
        try
        {
            model = readModel(source);
        }
        catch (Exception e)
        {
            throw new LocatableException(e, name);
//             throw new LoadingErrorException(ioe.getMessage(),
//                 new MultiErrorException(ioe.getMessage(), Collections.singletonList(
//                                             new GenericError(ioe.getMessage(), "", -1, -1))
//                 ));
        }
//         catch (MultiErrorException ge)
//         {
//             Logging.dbg().throwing("ASGImcf","createClass",ge);
//             throw new LoadingErrorException("Errors found in " + name + ": " + ge.getMessage(), ge);
//         }
        
        GenericInterpreterClassLoader cl = new GenericInterpreterClassLoader(topLevelLoader, name, this.getModelInterface(), 
            null, model);
        Class c = cl.getModelInterpreterClass();
        Logging.dbg().fine("AGIModelClassFactory:: Class " + name + " loaded.");
        return c;
	}

}
