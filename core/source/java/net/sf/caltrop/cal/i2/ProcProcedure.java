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

package net.sf.caltrop.cal.i2;

import net.sf.caltrop.cal.i2.environment.EnvironmentFrame;
import net.sf.caltrop.cal.i2.environment.LazyEnvironmentFrame;
import net.sf.caltrop.cal.i2.types.Type;
import net.sf.caltrop.cal.interpreter.ast.Decl;
import net.sf.caltrop.cal.interpreter.ast.ExprProc;
import net.sf.caltrop.cal.interpreter.ast.Statement;

/**
 * A ProcProcedure is a procedure object resulting from the evaluation of a proc expression.
 *
 * @author Jörn W. Janneck <janneck@eecs.berkeley.edu>
 */

public class ProcProcedure implements Procedure {

	public void call(int n, Executor executor) {
 
		String [] params = proc.getParameters();
        if (params.length != n)
            throw new InterpreterException("Illegal number of arguments. Got "
                                          + n + ", expected " + params.length + ".");
                
		Object [] values = new Object[n];
		Type [] types = new Type[n];

		for (int i = 0; i < n; i++) {
			values[i] = executor.getValue(i);
			types[i] = null;  // TYPEFIXME
		}
		executor.pop(n);
		
		Environment e = new EnvironmentFrame(this.env, params, values, types);
		
		Decl [] decls = proc.getDecls();
		
		if (decls != null && decls.length > 0) {
			e = new LazyEnvironmentFrame(e, decls, executor);
		}
		
		for (Statement s : this.proc.getBody()) {
			executor.execute(s, e);
		}
    }

    public ProcProcedure(Environment env, ExprProc proc) {
        this.proc = proc;
        this.env = env;
        
    }

    private ExprProc proc;
    private Environment env;
}
