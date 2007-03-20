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

import java.io.*;
import java.util.logging.*;

import net.sf.caltrop.util.Logging;

/**
 * VersionInfo provides a common facility for reporting the current
 * version of the tool.

 * <p>Created: Fri Dec 15 15:47:14 2006
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id:$
 */
public class VersionInfo
{
    private static final String VERSION_FILE = "version.txt";

    public static void printVersion ()
    {
        String msg = buildVersion();
        Logging.user().log(Logging.FORCE, buildVersion());
    }

    public static void printVersion (PrintStream os)
    {
        os.println(buildVersion());
    }

    public static String buildVersion ()
    {
        StringBuffer version = new StringBuffer();
        version.append("Version: ");
        
        DataInputStream is = new DataInputStream(VersionInfo.class.getClassLoader().getResourceAsStream(VERSION_FILE));
        if (is == null) {
        	version.append("Error: Cannot find version.txt.");
        } else {
        	int cnt = 0;
        	byte[] data = new byte[1024];
        	try
        	{
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                while (cnt >= 0)
                {
                    cnt = is.read(data);
                    if (cnt > 0) baos.write(data, 0, cnt);
                }
                version.append(baos.toString());
        	}
        	catch (IOException ioe)
        	{
        		version.append("Error: " + ioe.getMessage());
        	}
        }

        return version.toString();
    }
    
}
