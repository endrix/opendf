package net.sf.caltrop.cal.i2.configuration;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.caltrop.cal.ast.ExprIf;
import net.sf.caltrop.cal.ast.ExprLiteral;
import net.sf.caltrop.cal.ast.ExprVariable;
import net.sf.caltrop.cal.ast.StmtAssignment;
import net.sf.caltrop.cal.i2.Configuration;
import net.sf.caltrop.cal.i2.Environment;
import net.sf.caltrop.cal.i2.Evaluator;
import net.sf.caltrop.cal.i2.ObjectSink;
import net.sf.caltrop.cal.i2.OperandStack;
import net.sf.caltrop.cal.i2.UndefinedInterpreterException;
import net.sf.caltrop.cal.i2.UndefinedVariableException;
import net.sf.caltrop.cal.i2.java.ClassObject;
import net.sf.caltrop.cal.i2.java.MethodObject;
import net.sf.caltrop.cal.interpreter.InterpreterException;

public class DefaultUntypedConfiguration implements Configuration {
	
	public Object createInteger(int n) {
		return BigInteger.valueOf(n);
	}

	public void addListElement(Object list, Object element) {
		((List)list).add(element);
	}

	public void addMapping(Object map, Object key, Object value) {
		((Map)map).put(key, value);
	}

	public void addSetElement(Object set, Object element) {
		((Set)set).add(element);
	}

	public void assign(Object value, Environment env, StmtAssignment stmt) {
		
		try {
			long loc = stmt.getVariableLocation();
			if (loc < 0) {
				loc = env.setByName(stmt.getVar(), value);
				stmt.setVariableLocation(loc);
			} else {
				env.setByPosition(loc, value);
			}
		}
		catch (Exception e) {
			throw new UndefinedVariableException(stmt.getVar(), e);
		}
	}

	public void assign(Object value, Environment env, StmtAssignment stmt,
			int nIndices, OperandStack stack) {
		
		Object structure;
		try {
			long loc = stmt.getVariableLocation();
			if (loc < 0) {
				loc = env.lookupByName(stmt.getVar(), tmpSink);
				stmt.setVariableLocation(loc);
				structure = tmp;
			} else {
				structure = env.getByPosition(loc);
			}
		}
		catch (Exception e) {
			throw new UndefinedVariableException(stmt.getVar(), e);
		}

		if (structure instanceof Map) {
            if (nIndices != 1)
                throw new IllegalArgumentException("Maps expect 1 index, got: " + nIndices + ".");
            ((Map)structure).put(stack.getValue(0), value);
            stack.pop();
        } else if (structure instanceof List) {
        	Object a = structure;
        	for (int i = 0; i < nIndices - 1; i ++) {
        		if (!(a instanceof List))
        			throw new IllegalArgumentException("Expected " + nIndices + "-dimensional list structure: " + structure);
        		Object index = stack.getValue(i);
        		try {
        			int idx = intValue(index);
            		
            		a = ((List)a).get(idx);
        		}
        		catch (NumberFormatException e) {
        			throw new IllegalArgumentException("List indices must all be integers: " + index, e);
        		}
        		catch (IndexOutOfBoundsException e) {
        			throw e;
        		}
        	}
            ((List)a).set(intValue(stack.getValue(nIndices - 1)), value);
            stack.pop(nIndices);
        } else {
            throw new RuntimeException("Unknown data structure: " + structure);
        }
	}

	public void assignField(Object value, Environment env, StmtAssignment stmt,
			String field) {
		throw new RuntimeException("Field assignment NYI.");
	}

	public boolean booleanValue(Object v) {
		return ((Boolean)v).booleanValue();
	}
	
	public int intValue(Object v) {
		return ((Number)v).intValue();
	}

	public Object cond(Evaluator evaluator, ExprIf expr) {
		if (booleanValue(evaluator.valueOf(expr.getCondition()))) {
			return evaluator.valueOf(expr.getThenExpr());
		} else {
			return evaluator.valueOf(expr.getElseExpr());
		}
	}

	public Object createClassObject(Class c) {
		return new ClassObject(c, this);
	}

	public Object createEmptyList() {
		return new ArrayList();
	}

	public Object createEmptyMap() {
		return new HashMap();
	}

	public Object createEmptySet() {
		return new HashSet();
	}
	
	public Object createList(List list) {
		return list;
	}
	
	public List getList(Object v) {
		return (List)v;
	}

	public Object createLiteralValue(ExprLiteral literal) {
        switch (literal.getKind()) {
        case ExprLiteral.litNull:
        	return null;
        case ExprLiteral.litTrue:
        	return Boolean.TRUE;
        case ExprLiteral.litFalse:
        	return Boolean.FALSE;
        case ExprLiteral.litChar:
        	return Character.valueOf(literal.getText().charAt(0));
        case ExprLiteral.litInteger:
        	// FIXME: handle big integers
        	String s = literal.getText();
        	if (s.startsWith("0x")) {
        		return new BigInteger(s.substring(2), 16);
        	} else if (s.startsWith("0") && ! "0".equals(s)) {
        		return new BigInteger(s.substring(1), 8);
        	} else {
        		return new BigInteger(s);
        	}
        case ExprLiteral.litReal:
        	return Double.valueOf(literal.getText());
        case ExprLiteral.litString:
        	return literal.getText();
        default:
            throw new UndefinedInterpreterException("Unknown type in ExprLiteral.");
        }
	}

	public void indexInto(Object structure, int nIndices, OperandStack stack) {
        if (structure instanceof Map) {
            if (nIndices != 1)
                throw new IllegalArgumentException("Maps expect 1 index, got: " + nIndices + ".");
            stack.replaceWithResult(1, ((Map)structure).get(stack.getValue(0)));
        } else if (structure instanceof List) {
        	Object a = structure;
        	for (int i = 0; i < nIndices; i ++) {
        		if (!(a instanceof List))
        			throw new IllegalArgumentException("Expected " + nIndices + "-dimensional list structure: " + structure);
        		Object index = stack.getValue(i);
        		try {
        			int idx = intValue(index);
            		
            		a = ((List)a).get(idx);
        		}
        		catch (Exception e) {
        			throw new IllegalArgumentException("List indices must all be integers: " + index, e);        			
        		}
        	}
        	stack.replaceWithResult(nIndices, a);
        } else {
            throw new RuntimeException("Unknown data structure: " + structure);
        }
	}

	public Object lookupVariable(Environment env, ExprVariable expr) {
		
		long loc = expr.getVariableLocation();
		Object res;
		if (loc >= 0) {
			res = env.getByPosition(loc);
		} else if (loc == Environment.CONSTANT) {
			res = expr.getCachedValue();
		} else if (loc == Environment.NOPOS) {
			loc = env.lookupByName(expr.getName(), tmpSink);
			res = tmp;
			expr.setVariableLocation(loc);
			if (loc == Environment.CONSTANT)
				expr.setCachedValue(res);
		} else {
			throw new UndefinedInterpreterException("Undefined variable location: " + loc);
		}
		return res;
	}

	public Object selectField(Object composite, String fieldName) {
		if (composite instanceof ClassObject) {
			Class c = ((ClassObject)composite).getClassObject();
			try {
				Field f = c.getField(fieldName);
				return f.get(null);
			}
			catch (NoSuchFieldException nsfe1) {
				return new MethodObject(c, fieldName, this);
			} catch (IllegalAccessException iae) {
				throw new InterpreterException("Permission denied to access static field " + fieldName + " in class " + c.getName() + ".", iae);
			}			
		} else {
			Class c = composite.getClass();
			try {
				Field f = c.getField(fieldName);
				return f.get(composite);
			} catch (NoSuchFieldException nsfe1) {
				return new MethodObject(composite, fieldName, this);
			} catch (IllegalAccessException iae) {
				throw new InterpreterException("Permission denied to access field " + fieldName + " in " + composite + ".", iae);
			}			
		}
	}

	public void  assignField(Object composite, String fieldName, Object value) {
		// check if the name of e is a Field in the enclosingObject. if so, return that value. otherwise,
		// assume it's a method.
		Class c = composite.getClass();
		Field f;
		try {
			f = c.getField(fieldName);
			f.set(composite, value);
		} catch (NoSuchFieldException nsfe1) {
			// maybe the enclosing object is a Class?
			if (composite instanceof Class) {
				Class javaClass = (Class) composite;
				try {
					f = javaClass.getField(fieldName);
					f.set(composite, value);
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
	
	
	public Object  convertJavaResult(Object res) {
		if (res instanceof Class) {
			return new ClassObject((Class)res, this);
		}
		
		return res;
	}

	
	public  boolean  isAssignableToJavaType(Object v, Class c) {
		if (v == null)
			return !c.isPrimitive();
		
		assert v != null;
		
		Class vc = v.getClass();
		if (c.isPrimitive()) {
			Class cobj = getObjectType(c);
			if (cobj.isAssignableFrom(vc))
				return true;
			if (vc == BigInteger.class) {
				return cobj == Integer.class || cobj == Long.class;
			} else {
				return false;
			}
		} else {
			return c.isAssignableFrom(vc);
		}
	}
	
	public Object convertToJavaType(Object v, Class c) {
		if (v == null)
			return v;
		
		assert v != null;
		
		Class vc = v.getClass();
		if (c.isPrimitive()) {
			Class cobj = getObjectType(c);
			if (cobj.isAssignableFrom(vc))
				return v;

			if (v instanceof BigInteger) {
				if (cobj == Long.class)
					return new Long(((BigInteger)v).longValue());
				if (cobj == Integer.class)
					return new Integer(((BigInteger)v).intValue());
				throw new UndefinedInterpreterException("Cannot convert from " + vc + " to " + c + ".");
			}
			throw new UndefinedInterpreterException("Cannot convert from " + vc + " to " + c + ".");
		} else {
			return v;
		}
	}
	
    private static boolean isAssignableFrom(Class c1, Class c2) {
        return getObjectType(c1).isAssignableFrom(getObjectType(c2));
    }

    private static Class getObjectType(Class c) {

        if (c == Boolean.TYPE) return Boolean.class;
        if (c == Character.TYPE) return Character.class;
        if (c == Byte.TYPE) return Byte.class;
        if (c == Short.TYPE) return Short.class;
        if (c == Integer.TYPE) return Integer.class;
        if (c == Long.TYPE) return Long.class;
        if (c == Float.TYPE) return Float.class;
        if (c == Double.TYPE) return Double.class;

        return c;
    }
    
    private ObjectSink  tmpSink = new ObjectSink () {
    	public void putObject(Object value) {
    		tmp = value;
    	}
    };
    
    private Object tmp;

}
