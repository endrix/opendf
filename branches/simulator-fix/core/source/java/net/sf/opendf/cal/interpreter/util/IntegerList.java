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

package net.sf.opendf.cal.interpreter.util;



import java.util.AbstractList;

import net.sf.opendf.cal.interpreter.Context;


/**
@author Jörn W. Janneck <janneck@eecs.berkeley.edu>
@version $Id: IntegerList.java 52 2007-01-22 15:56:51Z imiller $
@since Ptolemy II 3.1
*/

public class IntegerList extends AbstractList {

    public IntegerList(Context context, int a, int b) {
        assert a <= b;

        _context = context;
        _a = a;
        _b = b;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    public Object get(int n) {
        if (_a + n > _b) {
            throw new IndexOutOfBoundsException(
                _a + " + " +  n + " is greater than " + _b);
        }
        return _context.createInteger(_a + n);
    }

    public int  size() {
        return (_b - _a) + 1;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    private Context _context;
    private int  _a;
    private int  _b;
}
