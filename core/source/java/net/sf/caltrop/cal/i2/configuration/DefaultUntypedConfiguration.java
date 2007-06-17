package net.sf.caltrop.cal.i2.configuration;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.caltrop.cal.i2.Configuration;
import net.sf.caltrop.cal.i2.Environment;
import net.sf.caltrop.cal.i2.Evaluator;
import net.sf.caltrop.cal.i2.OperandStack;
import net.sf.caltrop.cal.i2.UndefinedInterpreterException;
import net.sf.caltrop.cal.interpreter.ast.ExprIf;
import net.sf.caltrop.cal.interpreter.ast.ExprLiteral;
import net.sf.caltrop.cal.interpreter.ast.ExprVariable;
import net.sf.caltrop.cal.interpreter.ast.StmtAssignment;

public class DefaultUntypedConfiguration implements Configuration {

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
		env.setByName(stmt.getVar(), value);
	}

	public void assign(Object value, Environment env, StmtAssignment stmt,
			int nIndices, OperandStack stack) {
		throw new RuntimeException("Indexed assignment NYI.");
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
		throw new RuntimeException("Class objects NYI.");
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
		throw new RuntimeException("Indexing NYI.");
	}

	public Object lookupVariable(Environment env, ExprVariable expr) {
		return env.getByName(expr.getName());
	}

	public Object selectField(Object a, String fieldName) {
		// TODO Auto-generated method stub
		return null;
	}

}
