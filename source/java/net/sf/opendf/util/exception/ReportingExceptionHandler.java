
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

package net.sf.opendf.util.exception;

import net.sf.opendf.util.logging.Logging;
import net.sf.opendf.cal.parser.CalParserException;
import net.sf.opendf.nl.parser.NLParserException;
import net.sf.opendf.util.source.ParserErrorException;
import net.sf.opendf.util.source.GenericError;
import net.sf.opendf.util.source.MultiErrorException;
import net.sf.opendf.cal.interpreter.InterpreterException;
import net.sf.opendf.util.exception.*;

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
public class ReportingExceptionHandler extends UnravelingExceptionHandler
{

    protected ExceptionHandler[] getHandlers ()
    {
        return handlers;
    }
    
    private static final ExceptionHandler handlers[] = {

        
        new DbgTypedExceptionHandler() 
        {
            protected Class getHandledClass() { return InterpreterException.class; }
            public boolean handle (Throwable t)
            {
                super.handle(t);
                Logging.user().severe("Error during interpreting: " + t.getMessage());
                return true;
            }
        },
        
        new DbgTypedExceptionHandler() 
        {
            protected Class getHandledClass() { return net.sf.opendf.cal.i2.InterpreterException.class; }
            public boolean handle (Throwable t)
            {
                super.handle(t);
                Logging.user().severe("Error during interpreting: " + t.getMessage());
                return true;
            }
        },
        
        new DbgTypedExceptionHandler() 
        {
            protected Class getHandledClass() { return ClassNotFoundException.class; }
            public boolean handle (Throwable t)
            {
                super.handle(t);
                Logging.user().severe("Error during loading: " + t.getMessage());
                return true;
            }
        },
        
        new DbgTypedExceptionHandler() 
        {
            protected Class getHandledClass() { return CalParserException.class; }
            public boolean handle (Throwable t)
            {
                super.handle(t);
                Logging.user().severe("Error during parsing of CAL source: " + t.getMessage());
                return true;
            }
        },
        
        new DbgTypedExceptionHandler() 
        {
            //protected Class getHandledClass() { return ParserErrorException.class; }
            protected Class getHandledClass() { return MultiErrorException.class; }
            public boolean handle (Throwable t)
            {
                super.handle(t);
                ((MultiErrorException)t).logTo(Logging.user());
                return true;
            }
        },
        
        new DbgTypedExceptionHandler() 
        {
            protected Class getHandledClass() { return NLParserException.class; }
            public boolean handle (Throwable t)
            {
                super.handle(t);
                Logging.user().severe("Error during parsing of Network Language source: " + t.getMessage());
                return true;
            }
        },
        
        new DbgTypedExceptionHandler() 
        {
            protected Class getHandledClass() { return net.sf.saxon.trans.DynamicError.class; }
            public boolean handle (Throwable t)
            {
                super.handle(t);
                // Nothing to be gained by reporting to user
                //Logging.user().severe("XSLT Transformation error: " + ((net.sf.saxon.trans.DynamicError)t).getMessage());
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
        
        // Handle only the exact class RuntimeException generically.
        // Allow all other (previously unhandled) runtime exceptions
        // to fall through so that they are not silently ignored.
        new ExactTypeExceptionHandler() 
        {
            protected Class getHandledClass() { return RuntimeException.class; }
            public boolean handle (Throwable t)
            {
                Logging.dbg().info(t.getClass() + " " + t.getStackTrace()[0].toString() + "\n\t" + t.getMessage());
                //Do nothing for user unless this is the top level of
                // the stack trace
                if (t.getCause() == null)
                {
                    Logging.user().info("Exceptional event: " + t.getMessage());
                }
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
            String stackTop = t.getStackTrace().length == 0 ? "no stack trace available":t.getStackTrace()[0].toString();
            Logging.dbg().info(t.getClass() + " " + stackTop + "\n\t" + t.getMessage());

            return false;
        }
        
    }
    
}
