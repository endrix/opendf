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

package net.sf.opendf.cal.lib_i2;

import net.sf.opendf.cal.i2.Configuration;
import net.sf.opendf.cal.i2.Environment;
import net.sf.opendf.cal.i2.environment.DynamicEnvironmentFrame;
import net.sf.opendf.cal.i2.environment.EnvironmentFactory;
import net.sf.opendf.cal.i2.util.FunctionOf1;
import net.sf.opendf.cal.i2.util.FunctionOf2;

/**
 * 
 * @deprecated
 * 
 * @author jornj
 */
public class BitOps implements EnvironmentFactory {

	public Environment createEnvironment(Environment parent, Configuration configuration) {
		
		DynamicEnvironmentFrame env = new DynamicEnvironmentFrame(parent);
		final Configuration conf = configuration;
		
        env.bind("bitor", new FunctionOf2() {
        	@Override
        	public Object f(Object a, Object b) {
                int aa = conf.intValue(a);
                int bb = conf.intValue(b);
                return conf.createInteger(aa | bb);
            }
        }, null);

        env.bind("bitand", new FunctionOf2() {
        	@Override
        	public Object f(Object a, Object b) {
                int aa = conf.intValue(a);
                int bb = conf.intValue(b);
                return conf.createInteger(aa & bb);
            }
        }, null);

        env.bind("bitxor", new FunctionOf2() {
        	@Override
        	public Object f(Object a, Object b) {
                int aa = conf.intValue(a);
                int bb = conf.intValue(b);
                return conf.createInteger(aa ^ bb);
            }
        }, null);
        
        env.bind("bitnot", new FunctionOf1() {
        	@Override
        	public Object f(Object a) {
                int aa = conf.intValue(a);
                return conf.createInteger(~aa);
        	}
        }, null);
        
        env.bind("rshift", new FunctionOf2() {
        	@Override
        	public Object f(Object a, Object b) {
                int aa = conf.intValue(a);
                int bb = conf.intValue(b);
                return conf.createInteger(aa >> bb);
            }
        }, null);

        env.bind("urshift", new FunctionOf2() {
        	@Override
        	public Object f(Object a, Object b) {
                int aa = conf.intValue(a);
                int bb = conf.intValue(b);
                return conf.createInteger(aa >>> bb);
            }
        }, null);

        env.bind("lshift", new FunctionOf2() {
        	@Override
        	public Object f(Object a, Object b) {
                int aa = conf.intValue(a);
                int bb = conf.intValue(b);
                return conf.createInteger(aa << bb);
            }
        }, null);

        return env;
	}
	

}
