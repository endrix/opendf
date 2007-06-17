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

package net.sf.caltrop.cal.i2.environment;

import java.util.HashMap;
import java.util.Map;

import net.sf.caltrop.cal.i2.Environment;
import net.sf.caltrop.cal.i2.UndefinedInterpreterException;
import net.sf.caltrop.cal.i2.UndefinedVariableException;


/**
 * This environment automatically creates a new local binding for any variable assignment that fails
 * on its parent environment. If it does not have a parent environment, it creates a local binding for
 * every newly assigned variable.
 *
 *  @author Jörn W. Janneck <janneck@eecs.berkeley.edu>
 */

public class AutoBindEnvironment extends DynamicEnvironmentFrame {

	public long setByName(Object var, Object value) {
		int res = localSet(var, value);
		
		if (res >= 0) {
			return makePos(0, res);
		} else {
			switch (res) {
			case (int)NOPOS:
				return NOPOS;
			case (int) UNDEFINED:
				if (parent == null) {
					return makePos(0, bind(var, value, null));
				}
				try {
					long pres = parent.setByName(var, value);
					if (pres >= 0) {
						return makePos(posFrame(pres) + 1, posVar(pres));
					} else {
						return pres;
					}
				}
				catch (Exception exc) {
					return makePos(0, bind(var, value, null));
				}
			default:
 				throw new UndefinedInterpreterException("Bad return value from localSet: " + res);
			}
		}
	}
	
	public void setByPosition(int frame, int varPos, Object value) {
		assert frame >= 0;
		assert varPos >= 0;
		
		if (frame == 0) {
			localSetByPos(varPos, value);
		} else  {
			assert parent != null;

			parent.setByPosition(frame - 1, varPos, value);
		}
	}
	
	public Map localBindings() { 
		Map m = new HashMap();
		for (int i = 0; i < vars.size(); i++) {
			m.put(vars.get(i), values.get(i));
		}
		return m;
	}


    //
    //  ctor
    //

    public AutoBindEnvironment() {
        this(null);
    }

    public AutoBindEnvironment(Environment parent) {
        super(parent);
    }

}
