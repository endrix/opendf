
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

package net.sf.caltrop.cli;

import net.sf.caltrop.util.logging.Logging;
import net.sf.caltrop.cal.parser.CalParserException;
import net.sf.caltrop.nl.parser.NLParserException;
import net.sf.caltrop.util.source.ParserErrorException;
import net.sf.caltrop.util.source.GenericError;
import net.sf.caltrop.util.source.MultiErrorException;
import net.sf.caltrop.cal.i2.InterpreterException;
import net.sf.caltrop.cal.i2.UndefinedVariableException;
import net.sf.caltrop.cal.i2.UndefinedInterpreterException;
import net.sf.caltrop.util.exception.*;

import java.util.*;

/**
 * The ReportingExceptionHandler uses the Logging infrastructure to
 * generate meaningful and well formatted messages as a result of a
 * caught exception.
 *
 * <p>Created: Tue Oct 02 15:58:28 2007
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id:$
 */
public class SimRuntimeExceptionHandler implements ExceptionHandler
{
    private int numErrors = 0;

    public int getErrorCount ()
    {
        return this.numErrors;
    }
    
    
    // Traverse back up the exception causation stack
    public boolean process (Throwable t)
    {
        this.numErrors += 1;
        
        while (t != null)
        {
            for (int i=0; i < handlers.length; i++)
            {
                if (handlers[i].process(t))
                    break;
            }
            
            t = t.getCause();
        }
        return true;
    }

    private static ExceptionHandler handlers[] = {
        
        new DbgTypedExceptionHandler() 
        {
            protected Class getHandledClass() { return UndefinedVariableException.class; }
            public boolean handle (Throwable t)
            {
                super.handle(t);
                Logging.user().info("Undefined variable: " + t.getMessage());
                return true;
            }
        },
        
        new DbgTypedExceptionHandler() 
        {
            protected Class getHandledClass() { return InterpreterException.class; }
            public boolean handle (Throwable t)
            {
                super.handle(t);
                Logging.user().info("Simulation error: " + t.getMessage());
                return true;
            }
        },

        // just in case....
        new DbgTypedExceptionHandler() 
        {
            protected Class getHandledClass() { return net.sf.caltrop.cal.interpreter.InterpreterException.class; }
            public boolean handle (Throwable t)
            {
                super.handle(t);
                Logging.user().info("Simulation error: " + t.getMessage());
                return true;
            }
        },

        new DbgTypedExceptionHandler() 
        {
            protected Class getHandledClass() { return IndexOutOfBoundsException.class; }
            public boolean handle (Throwable t)
            {
                super.handle(t);
                Logging.user().info("Index value out of range: " + t.getMessage());
                return true;
            }
        },
        
        new DbgTypedExceptionHandler() 
        {
            protected Class getHandledClass() { return IllegalArgumentException.class; }
            public boolean handle (Throwable t)
            {
                super.handle(t);
                Logging.user().info("Illegal argument: " + t.getMessage());
                return true;
            }
        },
        
        new DbgTypedExceptionHandler() 
        {
            protected Class getHandledClass() { return LocatableException.class; }
            public boolean handle (Throwable t)
            {
                super.handle(t);
                if (!LocatableException.Internal.class.isAssignableFrom(t.getClass()))
                {
                    Logging.user().severe("Error found in: " + ((LocatableException)t).getLocation());
                }
                else
                {
                    Logging.dbg().warning("Error found in: " + ((LocatableException)t).getLocation());
                }
                
                return true;
            }
        },
        
        new DbgTypedExceptionHandler() 
        {
            protected Class getHandledClass() { return LocatableException.class; }
            public boolean handle (Throwable t)
            {
                super.handle(t);
                Logging.user().severe("Unknown interpreter error: " + t.getMessage());
                return true;
            }
        },
        
        // Handle only the exact class RuntimeException generically.
        // Allow all other (previously unhandled) runtime exceptions
        // to fall through so that they are not silently ignored.
        new ExactTypeExceptionHandler() 
        {
            protected Class getHandledClass() { return RuntimeException.class; }
            public boolean handle (Throwable t)
            {
                Logging.dbg().info(t.getClass() + " " + t.getStackTrace()[0].toString() + "\n\t" + t.getMessage());
                //Do nothing for user
                return true;
            }
        },

        // As the final default, report out ALL unhandled exceptions.
        new DbgTypedExceptionHandler() 
        {
            protected Class getHandledClass() { return Throwable.class; }
            public boolean handle (Throwable t)
            {
                super.handle(t);
                // Report the unhandled exception to the user
                Logging.user().info("Unhandled exceptional event: " + t.getMessage() + "(" + t.getClass() + ")");
                return true;
            }
        },
        
    };
    

    // Log to the debug stream
    private abstract static class DbgTypedExceptionHandler extends TypedExceptionHandler
    {
        public boolean handle (Throwable t)
        {
            Logging.dbg().info(t.getClass() + " " + t.getStackTrace()[0].toString() + "\n\t" + t.getMessage());
            return false;
        }
        
    }
    
}