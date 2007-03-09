/* 
BEGINCOPYRIGHT X,UC
	
	Copyright (c) 2007, Xilinx Inc.
	Copyright (c) 2003, The Regents of the University of California
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

package net.sf.caltrop.cal.interpreter.util;

import java.util.ArrayList;


/**
 * @author Christopher Chang <cbc@eecs.berkeley.edu>
 */
public final class Utility {
	
	public static int getTabDepth() {
        return tabdepth;
    }
	
    public static void increaseTabDepth(int num) {
        tabdepth = tabdepth + num;
        tabs = makeTabs(tabdepth);
    }
    
    public static void decreaseTabDepth(int num) {
        tabdepth = tabdepth - num;
        tabs = makeTabs(tabdepth);
    }
    
    public static String getHeadingTabs() {
        return makeTabs(tabdepth - 1);
    }
    
    public static String arrayToString(Object[] objs) {
        String s = "";

        for (int i = 0; i < objs.length; i++) {
            s = s + tabs + objs[i].toString() + "\n";
        }
        return tabs + s.trim();
    }
    
    private static int tabdepth = 0;
    private static String tabs = "";
    private static String makeTabs(int numtabs) {
        String tabs = "";
        for (int i = 0; i < numtabs; i++) {
            tabs = tabs + "\t";
        }
        return tabs;
    }
}
