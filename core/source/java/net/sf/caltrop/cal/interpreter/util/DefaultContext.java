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


import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.caltrop.cal.interpreter.Context;
import net.sf.caltrop.cal.interpreter.Function;
import net.sf.caltrop.cal.interpreter.InterpreterException;
import net.sf.caltrop.cal.interpreter.Procedure;
import net.sf.caltrop.cal.interpreter.environment.DataStructureManipulator;
import net.sf.caltrop.cal.interpreter.environment.Environment;
import net.sf.caltrop.cal.interpreter.environment.HashEnvironment;
import net.sf.caltrop.cal.interpreter.java.ClassObject;
import net.sf.caltrop.cal.interpreter.java.MethodObject;

/**
 *  @author Jörn W. Janneck <janneck@eecs.berkeley.edu>
 */

public class DefaultContext implements Context {

    //
    //  implement: Context
    //

    ////////////  Simple Data Objects

    public Object createNull() {
        return null;
    }

    public boolean  isNull(Object o) {
        return o == null;
    }

    public Object createBoolean(boolean b) {
        return b ? Boolean.TRUE : Boolean.FALSE;
    }

    public boolean isBoolean(Object o) {
        return o instanceof Boolean;
    }

    public boolean booleanValue(Object b) {
        return ((Boolean)b).booleanValue();
    }

    public Object createInteger(int n) {
        return new Integer(n);
    }

    public Object createInteger(String s) {
        // FIXME: handle big integers.
    	if (s.startsWith("0x")) {
    		return new Integer(Integer.parseInt(s.substring(2), 16));
    	} else if (s.startsWith("0") && ! "0".equals(s)) {
    		return new Integer(Integer.parseInt(s.substring(1), 8));
    	} else {
    		return new Integer(s);
    	}
    }
    
	public Object createInteger(BigInteger n) {
		return createInteger(n, n.bitLength() + 1, true);
	}
    
    public Object createInteger(BigInteger n, int nBits, boolean signed) {
    	return new Integer(n.intValue()); // FIXME: see above
    }

    public boolean isInteger(Object o) {
        return o instanceof Integer;
    }

    public int intValue(Object o) {
        return ((Number) o).intValue();
    }

    public BigInteger  asBigInteger(Object o) {
    	return BigInteger.valueOf(((Number)o).longValue());
    }

    public int getIntegerLength(Object o) {
    	return 32;
	}

	public int getMinimalIntegerLength(Object o) {
		BigInteger b = asBigInteger(o);
		return b.bitLength() + 1;
	}

	public boolean isSignedInteger(Object o) {
		return true;
	}

	public Object createReal(String s) {
        return new Double(s);
    }

    public Object createReal(double v) {
        return new Double(v);
    }

    public boolean isReal(Object o) {
        return o instanceof Double;
    }

    public double realValue(Object o) {
        return ((Double) o).doubleValue();
    }

    public Object createCharacter(char c) {
        return new Character(c);
    }

    public boolean isCharacter(Object o) {
        return o instanceof Character;
    }

    public char charValue(Object o) {
        return ((Character)o).charValue();
    }

    public Object createString(String s) {
        return s;
    }

    public boolean isString(Object o) {
        return o instanceof String;
    }

    public String stringValue(Object o) {
        return (String) o;
    }


    ////////////  Collections

    public Object createList(List a) {
        return a;
    }

    public boolean isList(Object o) {
        return o instanceof List;
    }

    public List getList(Object o) {
        return ((List) o);
    }

    public Object createSet(Set s) {
        return s;
    }

    public boolean isSet(Object o) {
        return o instanceof Set;
    }

    public Set getSet(Object o) {
        return (Set)o;
    }

    public Object createMap(Map m) {
        return m;
    }

    public boolean isMap(Object o) {
        return o instanceof Map;
    }

    public Map getMap(Object a) {
        return (Map)a;
    }

    public Object applyMap(Object map, Object arg) {
        return ((Map) map).get(arg);
    }

    public boolean isCollection(Object o) {
        return o instanceof Collection;
    }

    public Collection getCollection(Object a) {
        return (Collection)a;
    }


    ////////////  Functional and procedural closures

    public Object createFunction(Function f) {
        return f;
    }

    public boolean isFunction(Object a) {
        return a instanceof Function;
    }

    public Object applyFunction(Object function, Object[] args) {
        return ((Function) function).apply(args);
    }

    public Object createProcedure(Procedure p) {
        return p;
    }

    public boolean isProcedure(Object o) {
        return o instanceof Procedure;
    }

    public void callProcedure(Object procedure, Object[] args) {
        ((Procedure) procedure).call(args);
    }


    ////////////  Class

    public Object createClass(Class c) {
        return new ClassObject(c, this);
    }

    public boolean isClass(Object o) {
        return o instanceof ClassObject;
    }

    public Class getJavaClass(Object o) {
        return ((ClassObject)o).getClassObject();
    }

    public Class getJavaClassOfObject(Object o) {
    	if (o == null) {
    		return Object.class;
    	} else {
    		return o.getClass();
    	}
    }


    ////////////  Environment
    
    public Environment  newEnvironmentFrame(Environment env) {
    	return new HashEnvironment(env, this);
    }


    ////////////  Misc

    public Object selectField(Object composite, String fieldName) {
        // check if the name of e is a Field in the enclosingObject. if so, return that value. otherwise,
        // assume it's a method.
        Class c = composite.getClass();
        Field f;
        try {
            f = c.getField(fieldName);
            return this.fromJavaObject(f.get(composite));
        } catch (NoSuchFieldException nsfe1) {
            // maybe the enclosing object is a Class?
            if (this.isClass(composite)) {
            	Class javaClass = this.getJavaClass(composite);
                try {
                    f = javaClass.getField(fieldName);
                    return this.fromJavaObject(f.get(composite));
                } catch (NoSuchFieldException nsfe2) {
                    // assume it's a method.
                    return new MethodObject(this.toJavaObject(composite), fieldName, this);
                } catch (IllegalAccessException iae) {
                    throw new InterpreterException("Tried to access field " + fieldName +
                            " in " + composite.toString(), iae);
                }
            }
            // assume it's a method.
            return new MethodObject(this.toJavaObject(composite), fieldName, this);
        } catch (IllegalAccessException iae) {
            throw new InterpreterException("Tried to access field " + fieldName + " in " + composite, iae);
        }
    }
    
    public void  modifyField(Object composite, String fieldName, Object value) {
        // check if the name of e is a Field in the enclosingObject. if so, return that value. otherwise,
        // assume it's a method.
        Class c = composite.getClass();
        Field f;
        try {
            f = c.getField(fieldName);
            f.set(composite, this.toJavaObject(value));
        } catch (NoSuchFieldException nsfe1) {
            // maybe the enclosing object is a Class?
            if (this.isClass(composite)) {
            	Class javaClass = this.getJavaClass(composite);
                try {
                    f = javaClass.getField(fieldName);
                    f.set(composite, this.toJavaObject(value));
                } catch (NoSuchFieldException nsfe2) {
                    throw new InterpreterException("Tried to access field " + fieldName +
                            " in " + composite.toString() + ": field does not exist.", nsfe2);
                } catch (IllegalAccessException iae) {
                    throw new InterpreterException("Tried to access field " + fieldName +
                            " in " + composite.toString() + ": access denied.", iae);
                }
            }
        } catch (IllegalAccessException iae) {
            throw new InterpreterException("Tried to access field " + fieldName + " in " + composite, iae);
        }
    	
    }

    public Object fromJavaObject(Object o) {
        return o;
    }

    public Object toJavaObject(Object o) {
        if (o instanceof ClassObject)
            return this.getJavaClass(o);
        else
            return o;
    }
    
    public Object cond(Object c, Thunk v1, Thunk v2) {
		return booleanValue(c) ? v1.value() : v2.value();
	}

    //  subinterface: DataStructureManipulator


	public Object getLocation(Object structure, Object[] location) {
        return dsm.getLocation(structure, location);
    }

    public void setLocation(Object structure, Object[] location, Object value) {
        dsm.setLocation(structure, location, value);
    }

    //
    //  static
    //

    public final static Context theContext = new DefaultContext();

    //
    //  ctor
    //

    public DefaultContext() {
        this(new DefaultDataStructureManipulator());
    }

    public DefaultContext(DataStructureManipulator dsm) {
        this.dsm = dsm;
    }


    //
    //  private
    //

    private DataStructureManipulator  dsm;

}
