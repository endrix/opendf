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

package net.sf.caltrop.cal.lib;

import net.sf.caltrop.cal.interpreter.Context;
import net.sf.caltrop.cal.interpreter.Function;
import net.sf.caltrop.cal.interpreter.InterpreterException;
import net.sf.caltrop.cal.interpreter.environment.Environment;
import net.sf.caltrop.cal.interpreter.environment.EnvironmentFactory;
import net.sf.caltrop.cal.interpreter.environment.HashEnvironment;

/**
 * 
 * @author jornj
 */
public class BitOps implements EnvironmentFactory {

	public Environment createEnvironment(Environment parent, Context _context) {
		
		Environment env = new HashEnvironment(parent, _context);
		final Context context = _context;
		
        env.bind("bitor", context.createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                	int a = context.intValue(args[0]);
                	int b = context.intValue(args[1]);
                	return context.createInteger(a | b);
                } catch (Exception ex) {
                    throw new InterpreterException("Function 'bitor': Cannot apply.", ex);
                }
            }

            public int arity() {
                return 2;
            }
        }));

        env.bind("bitand", context.createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                	int a = context.intValue(args[0]);
                	int b = context.intValue(args[1]);
                	return context.createInteger(a & b);
                } catch (Exception ex) {
                    throw new InterpreterException("Function 'bitand': Cannot apply.", ex);
                }
            }

            public int arity() {
                return 2;
            }
        }));
        
        env.bind("bitxor", context.createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                	int a = context.intValue(args[0]);
                	int b = context.intValue(args[1]);
                	return context.createInteger(a ^ b);
                } catch (Exception ex) {
                    throw new InterpreterException("Function 'bitxor': Cannot apply.", ex);
                }
            }

            public int arity() {
                return 2;
            }
        }));
        
        env.bind("bitnot", context.createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                	int a = context.intValue(args[0]);
                	return context.createInteger(~a);
                } catch (Exception ex) {
                    throw new InterpreterException("Function 'bitnot': Cannot apply.", ex);
                }
            }

            public int arity() {
                return 1;
            }
        }));
        
        env.bind("rshift", context.createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                	int a = context.intValue(args[0]);
                	int b = context.intValue(args[1]);
                	return context.createInteger(a >> b);
                } catch (Exception ex) {
                    throw new InterpreterException("Function 'rshift': Cannot apply.", ex);
                }
            }

            public int arity() {
                return 2;
            }
        }));

        env.bind("urshift", context.createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                	int a = context.intValue(args[0]);
                	int b = context.intValue(args[1]);
                	return context.createInteger(a >>> b);
                } catch (Exception ex) {
                    throw new InterpreterException("Function 'urshift': Cannot apply.", ex);
                }
            }

            public int arity() {
                return 2;
            }
        }));

        env.bind("lshift", context.createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                	int a = context.intValue(args[0]);
                	int b = context.intValue(args[1]);
                	return context.createInteger(a << b);
                } catch (Exception ex) {
                    throw new InterpreterException("Function 'lshift': Cannot apply.", ex);
                }
            }

            public int arity() {
                return 2;
            }
        }));

        return env;
	}
	

}
