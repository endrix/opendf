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


package net.sf.caltrop.util;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The Logging class is a factory which provides static methods for
 * accessing {@link Logger} instances for various user/developer
 * output streams.  Static methods allow easy access to these loggers
 * from anywhere in the code base.
 *
 * <p>Created: Wed Jan 03 10:37:04 2007
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: Logging.java 40 2007-01-10 21:17:38Z imiller $
 */
public class Logging
{

    // Debug logger.  Used by developers for insight into the
    // workings/errors in the tool
    private static Logger DBG = null;

    // User logger.  Used for user interactions
    private static Logger USER = null;

    /** A Logger {@link Level} which forces output to happen
     * regardless of the specified level. */
    public static final Level FORCE = new XCALLoggingLevel("FORCE", Level.OFF.intValue());

    /**
     * Returns a non-null Logger instance that is used for output of
     * tool debugging messages.
     *
     * @return a non-null {@link Logger}
     */
    public static Logger dbg ()
    {
        if (DBG == null)
        {
            init();
        }
        return DBG;
    }
    
    /**
     * Returns a non-null Logger instance that is used for output of
     * messages intended for consumption by the user.
     *
     * @return a non-null {@link Logger}
     */
    public static Logger user ()
    {
        if (USER == null)
        {
            init();
        }
        return USER;
    }

    public static void setUserLevel (Level level)
    {
        if (USER == null)
            init();
        USER.setLevel(level);
    }
    
    public static void setDbgLevel (Level level)
    {
        if (DBG == null)
            init();
        DBG.setLevel(level);
    }
    
    /**
     * Initialization is deferred until the first call to access a
     * Logger so that we avoid a race condition with the reading of
     * the logging properties.
     */
    private static void init ()
    {
        DBG = Logger.getLogger("xcal.debug");
        USER = Logger.getLogger("xcal.user");

        // Check to see if we have explicit setting of the default
        // levels.  If not, then set some appropriate levels now
        if (System.getProperty("java.util.logging.config.file") == null &&
            System.getProperty("java.util.logging.config.class") == null)
        {
            if (DBG.getLevel() == null)
                DBG.setLevel(Level.SEVERE);
            if (USER.getLevel() == null)
                USER.setLevel(Level.INFO);
        }
        
        // The parent Loggers are the root which, by default, have the
        // ConsoleHandler defined which sends all output to std err.
        USER.setUseParentHandlers(false);
        USER.addHandler(new FlushedStreamHandler(System.out, new BasicLogFormatter()));

        DBG.setUseParentHandlers(false);
        DBG.addHandler(new FlushedStreamHandler(System.out, new DebugLogFormatter(true, "dbg")));
    }

    /**
     * Sub-class allowing us to specify the name and level
     */
    private static class XCALLoggingLevel extends Level
    {
        private XCALLoggingLevel (String name, int level)
        {
            super(name, level);
        }
    }
}
