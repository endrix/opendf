// TypedContext.java
// Xilinx Confidential
// Copyright (c) 2005 Xilinx Inc

// 2005-08-08 DBP Created from caltrop/dev/source/java/caltrop/interpreter/util/DefaultContext.java

package net.sf.caltrop.xslt.cal;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.caltrop.cal.interpreter.Context;
import net.sf.caltrop.cal.interpreter.Function;
import net.sf.caltrop.cal.interpreter.InterpreterException;
import net.sf.caltrop.cal.interpreter.Procedure;
import net.sf.caltrop.cal.interpreter.environment.Environment;
import net.sf.caltrop.cal.interpreter.environment.HashEnvironment;
import net.sf.caltrop.cal.interpreter.java.ClassObject;
import net.sf.caltrop.cal.interpreter.java.MethodObject;

/**
 *  @author Jörn W. Janneck <janneck@eecs.berkeley.edu>
 */

public class TypedContext implements Context {

    //
    //  implement: Context
    //

    ////////////  Simple Data Objects

    public Object createNull() {
    	return new TypedObject(null, Type.typeNull);
    }

    public boolean  isNull(Object o) {
        return Type.nameNull.equals(((TypedObject)o).getType().getName());
    }

    public Object createBoolean(boolean b) {
        return b ? boolTRUE : boolFALSE;
    }

    public boolean isBoolean(Object o) {
        return Type.nameBool.equals(((TypedObject)o).getType().getName());
    }

    public boolean booleanValue(Object b) {
   		return ((Boolean)((TypedObject)b).getValue()).booleanValue();
    }

    public Object createInteger(int n) {
    	BigInteger v = BigInteger.valueOf(n);
    	return createInteger(v, v.bitLength() + 1, true);
    }
    
    public Object createInteger(String s) {
    	String s0 = s.trim();
    	int radix;
    	String digits;
    	if (s0.startsWith("0x")) {
    		radix = 16;
    		digits = s0.substring(2);
    	} else if (s0.startsWith("0") && ! "0".equals(s0)) {
    		radix = 8;
    		digits = s0.substring(1);
    	} else {
    		radix = 10;
    		digits = s0;
    	}
    	BigInteger v = new BigInteger(digits, radix);
    	return createInteger(v, v.bitLength() + 1, true);
    }
    
    public Object  createInteger(BigInteger n) {
    	return createInteger(n, n.bitLength() + 1, true);
    }
    
    public Object  createInteger(BigInteger n, int nBits, boolean signed) {
    	if (n.bitLength() + 1 > nBits)  // FIXME: assume signed
    		throw new RuntimeException("Cannot create '" + n + "' as integer " + nBits + "-bit integer (has " + n.bitLength() + " bits).");
		Type t = Type.create(Type.nameInt, 
   			  Collections.EMPTY_MAP, 
   			  Collections.singletonMap(Type.vparSize, new Integer(Math.max(nBits, 1))));
		return new TypedObject(t, n);
    }

    public boolean isInteger(Object o) {
        return Type.nameInt.equals(((TypedObject)o).getType().getName());
    }

    public int intValue(Object o) {
    	return asBigInteger(o).intValue();
    }

    public BigInteger  asBigInteger(Object o) {
    	return (BigInteger)((TypedObject)o).getValue();
    }
    
    public int getIntegerLength(Object o) {
    	Type t = getType(o);
    	if (t.getValueParameters() != null) {
    		Object sz = t.getValueParameters().get(Type.vparSize);
    		if (sz != null)
    			return ((Integer)sz).intValue();
    	}
    	return getMinimalIntegerLength(o);
    }
    
    public int getMinimalIntegerLength(Object o) {
    	return asBigInteger(o).bitLength();
    }
    
    public boolean isSignedInteger(Object o) {
    	return true;
    }
    
    public Object createReal(String s) {
        return createReal(Double.parseDouble(s));
    }

	public Object createReal(double v) {
		return new TypedObject(Type.typeReal, new Double(v));
	}

    public boolean isReal(Object o) {
        return Type.nameReal.equals(((TypedObject)o).getType().getName());
    }

    public double realValue(Object o) {
    	return ((Double)((TypedObject)o).getValue()).doubleValue();
    }

    public Object createCharacter(char c) {
    	throw new UnsupportedTypeException("TypedContext: Characters not supported.");
    }

    public boolean isCharacter(Object o) {
    	return false;
    }

    public char charValue(Object o) {
    	throw new UnsupportedTypeException("TypedContext: Characters not supported.");
    }

    public Object createString(String s) {
        return new TypedObject(Type.typeString, s);
    }

    public boolean isString(Object o) {
        return Type.nameString.equals(((TypedObject)o).getType().getName());
    }

    public String stringValue(Object o) {
    	return (String)((TypedObject)o).getValue();
    }


    ////////////  Collections

    public Object createList(List a) {
    	Type elementType = lubTypeOfObjects(a);
    	
		Type listType = Type.create(Type.nameList,
		                         Collections.singletonMap(Type.tparType, elementType),
		                         Collections.singletonMap(Type.vparSize, new Integer(a.size())));

    	return new TypedObject(listType, a);
    }

    public boolean isList(Object o) {
        return Type.nameList.equals(getTypeName(o));
    }

    public List getList(Object o) {
        return (List)getValue(o);
    }

    public Object createSet(Set s) {
    	throw new UnsupportedTypeException("TypedContext: Sets not supported.");
    }

    public boolean isSet(Object o) {
    	return false;
    }

    public Set getSet(Object o) {
    	throw new UnsupportedTypeException("TypedContext: Sets not supported.");
    }

    public Object createMap(Map m) {
    	throw new UnsupportedTypeException("TypedContext: Maps not supported.");
    }

    public boolean isMap(Object o) {
    	return false;
    }

    public Map getMap(Object a) {
    	throw new UnsupportedTypeException("TypedContext: Maps not supported.");
    }

    public Object applyMap(Object map, Object arg) {
    	throw new UnsupportedTypeException("TypedContext: Maps not supported.");
    }

    public boolean isCollection(Object o) {
    	return isList(o);
    }

    public Collection getCollection(Object a) {
    	return getList(a);
    }


    ////////////  Functional and procedural closures

    public Object createFunction(Function f) {
    	return new TypedObject(Type.typeFunction, f);
    }

    public boolean isFunction(Object a) {
    	return getValue(a) instanceof Function;
    }

    public Object applyFunction(Object function, Object[] args) {
        return ((Function) getValue(function)).apply(args);
    }

    public Object createProcedure(Procedure p) {
    	return new TypedObject(Type.typeProcedure, p);
    }

    public boolean isProcedure(Object o) {
    	return getValue(o) instanceof Procedure;
    }

    public void callProcedure(Object procedure, Object[] args) {
        ((Procedure) getValue(procedure)).call(args);
    }


    ////////////  Class

    public Object createClass(Class c) {
        return new TypedObject(Type.typeClass, new ClassObject(c, this));
    }

    public boolean isClass(Object o) {
        return getValue(o) instanceof ClassObject;
    }

    public Class getJavaClass(Object o) {
        return ((ClassObject)getValue(o)).getClassObject();
    }

    public Class getJavaClassOfObject(Object o) {
        return this.toJavaObject(o).getClass();
    }


    ////////////  Misc

    public Environment  newEnvironmentFrame(Environment env) {
    	return new HashEnvironment(env, this);  // FIXME: change to type-aware environment
    }
    
    ////////////  Misc

    public Object selectField(Object tcomposite, String fieldName) {
        // check if the name of e is a Field in the enclosingObject. if so, return that value. otherwise,
        // assume it's a method.
    	Object composite = getValue(tcomposite);
        Class c = composite.getClass();
        Field f;
        try {
            f = c.getField(fieldName);
            return this.fromJavaObject(f.get(composite));
        } catch (NoSuchFieldException nsfe1) {
            // maybe the enclosing object is a Class?
            if (this.isClass(tcomposite)) {
                try {
                    f = this.getJavaClass(tcomposite).getField(fieldName);
                    return this.fromJavaObject(f.get(null));
                } catch (NoSuchFieldException nsfe2) {
                    return new TypedObject(Type.typeMethod, 
     		               new MethodObject(this.getJavaClass(tcomposite), fieldName, this));
                } catch (IllegalAccessException iae) {
                    throw new InterpreterException("Tried to access field " + fieldName +
                            " in " + composite.toString(), iae);
                }
            } 
            // assume it's a method.
            return new TypedObject(Type.typeMethod, 
            		               new MethodObject(composite, fieldName, this));
        } catch (IllegalAccessException iae) {
            throw new InterpreterException("Tried to access field " + fieldName + " in " + composite, iae);
        }

    }
    
    public void modifyField(Object tcomposite, String fieldName, Object value) {
        // check if the name of e is a Field in the enclosingObject. if so, return that value. otherwise,
        // assume it's a method.
    	Object composite = getValue(tcomposite);
        Class c = composite.getClass();
        Field f;
        try {
            f = c.getField(fieldName);
            f.set(composite, this.toJavaObject(value));
        } catch (NoSuchFieldException nsfe1) {
            // maybe the enclosing object is a Class?
            if (this.isClass(tcomposite)) {
                try {
                    f = this.getJavaClass(tcomposite).getField(fieldName);
                    f.set(null, this.toJavaObject(value));
                } catch (NoSuchFieldException nsfe2) {
                    throw new InterpreterException("Tried to access field " + fieldName +
                            " in " + composite.toString() + ": field does not exist.", nsfe2);
                } catch (IllegalAccessException iae) {
                    throw new InterpreterException("Tried to access field " + fieldName +
                            " in " + composite.toString() + ": access denied.", iae);
                }
           } else {
               throw new InterpreterException("Tried to access field " + fieldName +
                       " in " + composite.toString() + ".");
           }
        } catch (IllegalAccessException iae) {
            throw new InterpreterException("Tried to access field " + fieldName + " in " + composite, iae);
        }    	
    }

    public Object fromJavaObject(Object o) {
        // When the object is already a TypedObject, simply return
        // it. 
        if (o instanceof TypedObject)
        {
            return o;
        }
        
    	if (o instanceof Boolean) {
    		return createBoolean(((Boolean)o).booleanValue());
    	}
    	if (o instanceof Integer) {
    		return createInteger(((Integer)o).intValue());
    	}
    	if (o instanceof Double) {
    		return createReal(((Double)o).doubleValue());
    	}
    	if (o instanceof String) {
    		return createString((String)o);
    	}
        if (o == null)
        {
            return createNull();
        }
    	
    	return new TypedObject(Type.typeANY, o);
    }

    public Object toJavaObject(Object v) {
    	Object o = getValue(v);
        if (o instanceof ClassObject)
            return this.getJavaClass(o);
        else
            return o;
    }

    public Object  cond(Object c, Thunk v1, Thunk v2) {
    	Object v = ((TypedObject)c).getValue();
    	if (v instanceof Boolean) {
    		return ((Boolean)v).booleanValue() ? v1.value() : v2.value();
    	} else {
    		TypedObject t1 = (TypedObject)v1.value();
    		TypedObject t2 = (TypedObject)v2.value();
    		return new TypedObject(Type.lub(t1.getType(), t2.getType()), UNDEFINED);
    	}
    }

    
    //  subinterface: DataStructureManipulator

    public Object getLocation(Object structure, Object[] location) {
    	
    	if (isUndefined(structure)) {
    		Type t = ((TypedObject)structure).getType();
    		for (int i = 0; i < location.length; i++) {
    			t = (Type)t.getTypeParameters().get(Type.tparType);
    		}
    		return new TypedObject(t, UNDEFINED);
    	}
   
    	TypedObject a = (TypedObject)structure;
    	Type t = a.getType();
    	for (int i = 0; i < location.length; i ++) {
    		if (!isList(a))
    			throw new IllegalArgumentException("Expected " + location.length + "-dimensional list structure: " + structure);
			t = (Type)a.getType().getTypeParameters().get(Type.tparType);
			if (isUndefined(location[i]))
				return new TypedObject(t, UNDEFINED);
    		if (!isInteger(location[i]))
    			throw new IllegalArgumentException("List indices must all be integers: " + location);
    		
    		a = (TypedObject)getList(a).get(this.intValue(location[i]));
    	}
    	return a;
    }

    public void setLocation(Object structure, Object[] location, Object value) {
    	TypedObject a = (TypedObject)structure;
    	for (int i = 0; i < location.length - 1; i ++) {
    		if (!isList(a))
    			throw new IllegalArgumentException("Expected " + location.length + "-dimensional list structure: " + structure);
    		if (!isInteger(location[i]))
    			throw new IllegalArgumentException("List indices must all be integers: " + location);
    		
    		a = (TypedObject)getList(a).get(this.intValue(location[i]));
    	}
		if (!isList(a))
			throw new IllegalArgumentException("Expected " + location.length + "-dimensional list structure: " + structure);
        getList(a).set(this.intValue(location[location.length - 1]), value);
    }

    //
    //  static
    //

    public final static Context theContext = new TypedContext();
    
    public static TypedObject  createTypedObject(Type t, Object v) {
    	
    	return new TypedObject(t, v);
    }
    
    // FIXME: extend for more types---e.g., lists
    
    public static TypeCheckResult checkTypes(Type ts, Type td) {
    	if (ts.equals(td)) {
    		return new TypeCheckResult(TypeCheckResult.resIdentical, "Types are identical.");
    	}
    	if (ts.getName().equals(td.getName())) {
    		if (Type.nameInt.equals(ts.getName())) {
        		int asz = ((Number)ts.getValueParameters().get(Type.vparSize)).intValue();
        		int bsz = ((Number)td.getValueParameters().get(Type.vparSize)).intValue();
        		if (asz == bsz) {
        			// FIXME: allow for signed integers
            		return new TypeCheckResult(TypeCheckResult.resIdentical, "Integer types are identical.");
        		} else if (asz < bsz) {
            		return new TypeCheckResult(TypeCheckResult.resAssignable, "Integer types are different, but assignable.");        			
        		} else {
            		return new TypeCheckResult(TypeCheckResult.resIncompatible, "Integer types are not assignable---precision is lost converting " + asz + " bits to " + bsz + " bits.");        			
        		} 
    		} else {
        		return new TypeCheckResult(TypeCheckResult.resIncompatible, "Incompatible types: " + ts.getName() + " and " + td.getName());        			
    		}
    	} else {
    		return new TypeCheckResult(TypeCheckResult.resIncompatible, "Incompatible types: " + ts.getName() + " and " + td.getName());
    	}
    }
    
    public static class TypeCheckResult {
    	public int     result;
    	public String  message;
    	
    	public final static int  resIdentical = 0;
    	public final static int  resAssignable = 1;
    	public final static int  resIncompatible = -1;
    	
    	public TypeCheckResult(int result, String message) {
    		this.result = result;
    		this.message = message;
    	}
    }
    
    public static boolean isUndefined(Object a) {
    	return isUndefined((TypedObject)a);
    }

    public static boolean isUndefined(TypedObject a) {
    	return a.getValue() == UNDEFINED;
    }

    //
    //  ctor
    //

    public TypedContext() {
    }    
    


    //
    //  private
    //
    
    public static Object  getValue(Object o) {
    	return ((TypedObject)o).getValue();
    }
    
    public static Type  getType(Object o) {
    	return ((TypedObject)o).getType();
    }
    
    private String  getTypeName(Object o) {
    	return getType(o).getName();
    }
    
    private Type  lubTypeOfObjects(Collection a) {
    	if (a.isEmpty())
    		return null;

    	Iterator i = a.iterator();
    	TypedObject to = (TypedObject)i.next();
    	Type t = to.getType();
    	while (i.hasNext()) {
    		to = (TypedObject)i.next();
    		Type t1 = Type.lub(t, to.getType());
    		if (t1 == null)
    			return null;
    		t = t1;
    	}
    	return t;
    }
    

    static class UnsupportedTypeException extends RuntimeException
    {
        public UnsupportedTypeException (String msg) { super(msg); }
    }
    
       
    public  final static Object      UNDEFINED = new Object();
    public  final static TypedObject boolTRUE = new TypedObject(Type.typeBool, Boolean.TRUE);
    public  final static TypedObject boolFALSE = new TypedObject(Type.typeBool, Boolean.FALSE);
    public  final static TypedObject boolUNDEFINED = new TypedObject(Type.typeBool, UNDEFINED);
}
