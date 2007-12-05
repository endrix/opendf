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

package net.sf.caltrop.xslt.cal.lib;

import java.math.BigInteger;
import java.util.Collections;

import net.sf.caltrop.cal.interpreter.Context;
import net.sf.caltrop.cal.interpreter.environment.Environment;
import net.sf.caltrop.cal.interpreter.environment.EnvironmentFactory;
import net.sf.caltrop.xslt.cal.AbstractBinarySBFunction;
import net.sf.caltrop.xslt.cal.AbstractUnarySBFunction;
import net.sf.caltrop.xslt.cal.Type;
import net.sf.caltrop.xslt.cal.TypedObject;

/**
 * 
 *
 * 
 * @author jornj
 */
public class BitOps implements EnvironmentFactory {

	public Environment createEnvironment(Environment parent, Context _context) {
		
		Environment env = _context.newEnvironmentFrame(parent);
		final Context context = _context;
		
        env.bind("bitnot", context.createFunction(new AbstractUnarySBFunction() {

        	protected TypedObject  doValueFunction(TypedObject a) {
        		if (context.isInteger(a)) {
        			return new TypedObject(a.getType(),
        					context.asBigInteger(a).not());
        		} else
        			throw new RuntimeException("Cannot bitnot.");
        	}

        	protected Type doTypeFunction(Type t) {
				return Type.create(Type.nameInt, 
						Collections.EMPTY_MAP, 
						Collections.singletonMap(Type.vparSize, new Integer(32)));
        	}
        }));

        env.bind("bitor", context.createFunction(new AbstractBinarySBFunction() {

    	    protected TypedObject  doValueFunction(TypedObject a, TypedObject b) {
    	    	if (context.isInteger(a) && context.isInteger(b)) {
    	    		BigInteger n = context.asBigInteger(a).or(context.asBigInteger(b));
    	    		return (TypedObject)context.createInteger(n, n.bitLength() + 1, true);
    	    	} else
    	    		throw new RuntimeException("Cannot bitor. (" + a + "<" + ((a == null) ? "NULL" : a.getType()) + ">, " 
    	    				                                   + b + "<" + ((b == null) ? "NULL" : b.getType()) + ">");
    	    }

    	    protected Type doTypeFunction(Type a, Type b) {
        		if (Type.nameInt.equals(a.getName()) && Type.nameInt.equals(b.getName())) {
        				int w1 = ((Integer)a.getValueParameters().get(Type.vparSize)).intValue();
        				int w2 = ((Integer)b.getValueParameters().get(Type.vparSize)).intValue();
        				return Type.create(Type.nameInt, 
        						Collections.EMPTY_MAP, 
        						Collections.singletonMap(Type.vparSize, new Integer(Math.max(w1, w2))));
        		} else {
        			throw new RuntimeException("Cannot bitor types: " + a + ", " + b + ".");
        		}
    	    }
        }));

        env.bind("bitand", context.createFunction(new AbstractBinarySBFunction() {

    	    protected TypedObject  doValueFunction(TypedObject a, TypedObject b) {
    	    	if (context.isInteger(a) && context.isInteger(b)) {
    	    		BigInteger n = context.asBigInteger(a).and(context.asBigInteger(b));
    	    		return (TypedObject)context.createInteger(n, n.bitLength() + 1, true);
    	    	} else
    	    		throw new RuntimeException("Cannot bitand. (" + a + "<" + ((a == null) ? "NULL" : a.getType()) + ">, " 
    	    				                                   + b + "<" + ((b == null) ? "NULL" : b.getType()) + ">");
    	    }

    	    protected Type doTypeFunction(Type a, Type b) {
        		if (Type.nameInt.equals(a.getName()) && Type.nameInt.equals(b.getName())) {
        				int w1 = ((Integer)a.getValueParameters().get(Type.vparSize)).intValue();
        				int w2 = ((Integer)b.getValueParameters().get(Type.vparSize)).intValue();
        				return Type.create(Type.nameInt, 
        						Collections.EMPTY_MAP, 
        						Collections.singletonMap(Type.vparSize, new Integer(Math.max(w1, w2))));
        		} else {
        			throw new RuntimeException("Cannot bitand types: " + a + ", " + b + ".");
        		}
    	    }
        }));
        
        env.bind("bitxor", context.createFunction(new AbstractBinarySBFunction() {

    	    protected TypedObject  doValueFunction(TypedObject a, TypedObject b) {
    	    	if (context.isInteger(a) && context.isInteger(b)) {
    	    		BigInteger n = context.asBigInteger(a).xor(context.asBigInteger(b));
    	    		return (TypedObject)context.createInteger(n, n.bitLength() + 1, true);
    	    	} else
    	    		throw new RuntimeException("Cannot bitxor. (" + a + "<" + ((a == null) ? "NULL" : a.getType()) + ">, " 
    	    				                                   + b + "<" + ((b == null) ? "NULL" : b.getType()) + ">");
    	    }

    	    protected Type doTypeFunction(Type a, Type b) {
        		if (Type.nameInt.equals(a.getName()) && Type.nameInt.equals(b.getName())) {
        				int w1 = ((Integer)a.getValueParameters().get(Type.vparSize)).intValue();
        				int w2 = ((Integer)b.getValueParameters().get(Type.vparSize)).intValue();
        				return Type.create(Type.nameInt, 
        						Collections.EMPTY_MAP, 
        						Collections.singletonMap(Type.vparSize, new Integer(Math.max(w1, w2))));
        		} else {
        			throw new RuntimeException("Cannot bitxor types: " + a + ", " + b + ".");
        		}
    	    }
        }));

        env.bind("rshift", context.createFunction(new AbstractBinarySBFunction() {

    	    protected TypedObject  doValueFunction(TypedObject a, TypedObject b) {
    	    	if (context.isInteger(a) && context.isInteger(b)) {
    	    		BigInteger n = context.asBigInteger(a).shiftRight(context.intValue(b));
    	    		return (TypedObject)context.createInteger(n, n.bitLength() + 1, true);
    	    	} else
    	    		throw new RuntimeException("Cannot rshift. (" + a + "<" + ((a == null) ? "NULL" : a.getType()) + ">, " 
    	    				                                   + b + "<" + ((b == null) ? "NULL" : b.getType()) + ">");
    	    }

    	    protected Type doTypeFunction(Type a, Type b) {
    	    	return a;
    	    }
        }));

        env.bind("urshift", context.createFunction(new AbstractBinarySBFunction() {

    	    protected TypedObject  doValueFunction(TypedObject a, TypedObject b) {
    	    	if (context.isInteger(a) && context.isInteger(b)) {
    	    		BigInteger n = context.asBigInteger(a).shiftRight(context.intValue(b));
    	    		return (TypedObject)context.createInteger(n, n.bitLength() + 1, true);
    	    	} else
    	    		throw new RuntimeException("Cannot urshift. (" + a + "<" + ((a == null) ? "NULL" : a.getType()) + ">, " 
    	    				                                   + b + "<" + ((b == null) ? "NULL" : b.getType()) + ">");
    	    }

    	    protected Type doTypeFunction(Type a, Type b) {
    	    	return a;
    	    }
        }));

        env.bind("lshift", context.createFunction(new AbstractBinarySBFunction() {

    	    protected TypedObject  doValueFunction(TypedObject a, TypedObject b) {
    	    	if (context.isInteger(a) && context.isInteger(b)) {
    	    		BigInteger n = context.asBigInteger(a).shiftLeft(context.intValue(b)).and(BigInteger.valueOf(0xffffffff));
    	    		return (TypedObject)context.createInteger(n, n.bitLength() + 1, true);
    	    	} else
    	    		throw new RuntimeException("Cannot lshift. (" + a + "<" + ((a == null) ? "NULL" : a.getType()) + ">, " 
    	    				                                   + b + "<" + ((b == null) ? "NULL" : b.getType()) + ">");
    	    }

        	protected Type doTypeFunction(Type a, Type b) {
				return Type.create(Type.nameInt, 
						Collections.EMPTY_MAP, 
						Collections.singletonMap(Type.vparSize, new Integer(32)));
        	}
        }));


        return env;
	}
	

}
