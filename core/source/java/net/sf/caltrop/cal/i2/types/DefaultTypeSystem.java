package net.sf.caltrop.cal.i2.types;

import java.util.HashMap;
import java.util.Map;

import net.sf.caltrop.cal.ast.ExprLiteral;
import net.sf.caltrop.cal.ast.TypeExpr;
import net.sf.caltrop.cal.i2.Environment;
import net.sf.caltrop.cal.i2.Evaluator;
import net.sf.caltrop.cal.i2.util.Platform;

public class DefaultTypeSystem implements TypeSystem {

	public Type evaluate(TypeExpr te, Evaluator eval) {
		if (te == null)
			return null;
		return te.evaluate(this, eval);
	}
	
	public Type doEvaluate(TypeExpr te, Evaluator eval) {
		if (te == null) {
			return null;
		}
		TypeClass tc = typeClasses.get(te.getName());
		if (tc == null) {
			// FIXME: throw exception --- undefined type class.
			return null;
		}
		return tc.createType(te, eval);
	}

	public Type literalType(ExprLiteral e) {
		// TODO Auto-generated method stub
		return null;
	}

	public Type lub(Type t1, Type t2) {
		// TODO Auto-generated method stub
		return null;
	}

	public Type typeOf(Object v) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public boolean assignableTo(Type t1, Type t2) {
		// TODO Auto-generated method stub
		return false;
	}

	public DefaultTypeSystem () {
		initializeTypeClasses();
	}
		
	private Map<String, TypeClass> typeClasses = new HashMap<String, TypeClass>();

	
	private void initializeTypeClasses() {
		addTypeClass(new TheIntType.TheClass(this));
//		addTypeClass(new TheIntegerType);
	}
	
	private void addTypeClass(TypeClass tc) {
		typeClasses.put(tc.getName(), tc);
	}
}
