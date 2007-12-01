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
import net.sf.caltrop.cal.i2.types.DefaultTypeSystem;
import net.sf.caltrop.cal.i2.types.TypeSystem;
import net.sf.caltrop.cal.interpreter.InterpreterException;

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
	
	private TypeSystem typeSystem;
	
}
