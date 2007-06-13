package net.sf.caltrop.cal.i2.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.caltrop.cal.i2.Configuration;
import net.sf.caltrop.cal.i2.Environment;
import net.sf.caltrop.cal.i2.Evaluator;
import net.sf.caltrop.cal.i2.ObjectSink;
import net.sf.caltrop.cal.i2.OperandStack;
import net.sf.caltrop.cal.interpreter.ast.ExprIf;
import net.sf.caltrop.cal.interpreter.ast.ExprLiteral;
import net.sf.caltrop.cal.interpreter.ast.ExprVariable;
import net.sf.caltrop.cal.interpreter.ast.StmtAssignment;

public class UntypedConfiguration implements Configuration, ObjectSink {

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
		// TODO
	}

	public void assign(Object value, Environment env, StmtAssignment stmt,
			int nIndices, OperandStack stack) {
		// TODO
	}

	public void assignField(Object value, Environment env, StmtAssignment stmt,
			String field) {
		// TODO
	}

	public boolean booleanValue(Object v) {
		return ((Boolean)v).booleanValue();
	}

	public Object cond(Evaluator evaluator, ExprIf expr) {
		if (booleanValue(evaluator.valueOf(expr.getCondition()))) {
			return evaluator.valueOf(expr.getThenExpr());
		} else {
			return evaluator.valueOf(expr.getElseExpr());			
		}
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

	public Object createLiteralValue(ExprLiteral literal) {
		// TODO Auto-generated method stub
		return null;
	}

	public void indexInto(Object structure, int nIndices, OperandStack stack) {
		// TODO Auto-generated method stub
	}

	public Object lookupVariable(Environment env, ExprVariable expr) {
		env.lookupByName(expr.getName(), this); // FIXME: cache position
		return tmp;
	}

	public Object selectField(Object a, String fieldName) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object createClassObject(Class c) {
		// TODO Auto-generated method stub
		return null;
	}
	//
	//  ObjectSink
	//
	
	public void putObject(Object value) {
		tmp = value;
	}
	
	private Object tmp;
	
}
