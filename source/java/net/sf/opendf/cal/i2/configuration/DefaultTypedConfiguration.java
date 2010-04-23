package net.sf.opendf.cal.i2.configuration;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.opendf.cal.ast.ExprIf;
import net.sf.opendf.cal.ast.ExprLiteral;
import net.sf.opendf.cal.ast.ExprVariable;
import net.sf.opendf.cal.ast.StmtAssignment;
import net.sf.opendf.cal.ast.TypeExpr;
import net.sf.opendf.cal.i2.Configuration;
import net.sf.opendf.cal.i2.Environment;
import net.sf.opendf.cal.i2.Evaluator;
import net.sf.opendf.cal.i2.Executor;
import net.sf.opendf.cal.i2.ObjectSink;
import net.sf.opendf.cal.i2.OperandStack;
import net.sf.opendf.cal.i2.UndefinedInterpreterException;
import net.sf.opendf.cal.i2.UndefinedVariableException;
import net.sf.opendf.cal.i2.java.ClassObject;
import net.sf.opendf.cal.i2.java.MethodObject;
import net.sf.opendf.cal.i2.types.*;
import net.sf.opendf.cal.i2.environment.AbstractEnvironment; 
import net.sf.opendf.cal.interpreter.InterpreterException;

public class DefaultTypedConfiguration extends DefaultUntypedConfiguration implements Configuration{

	
	@Override
	public TypeSystem getTypeSystem() {
		return typeSystem;
	}
	
	public DefaultTypedConfiguration() {
		this(new DefaultTypeSystem());
	}
	
	public DefaultTypedConfiguration(TypeSystem ts) {
		this.typeSystem = ts;
	}

	public void assign(Object value, Environment env, StmtAssignment stmt) {		
		try {
			long i = env.lookupByName(stmt.getVar(), tmpSink);
			Type t1 =  (Type) ((AbstractEnvironment) env).getTypeByPosition(i);
			if (t1 != null) {					
				value = t1.convert(value);
			}
		} catch (UndefinedVariableException e) {
			// Do nothing. This only means that the variable to be assigned is not previous declared, 
			// which is ok in a mixed typed/untyped environment
		}
		super.assign(value, env, stmt);
	}

	public void assign(Object value, Environment env, StmtAssignment stmt,
			int nIndices, OperandStack stack) {		
		long i = env.lookupByName(stmt.getVar(), tmpSink);
		try {
		    Type t1 =  (Type) ((AbstractEnvironment) env).getTypeByPosition(i);
		    if (t1 != null) {
			  Type t2 = findElementType(t1);
			  if (t2 != null) {
				value= t2.convert(value);
		    }	
		}
	    } catch (UndefinedVariableException e) {
		   // Do nothing. This only means that the variable to be assigned is not previous declared, 
		  // which is ok in a mixed typed/untyped environment
	    }
		super.assign(value, env, stmt, nIndices, stack);
	}
	
	private Type findElementType(Type t) { //FIXME this should work not *only* for Lists
	   if (t instanceof ListType) {
		   return findElementType(((ListType) t).getElementType());
	   } else {
		   return t;
	   }
	}
	
	
	private TypeSystem typeSystem;
	
}
