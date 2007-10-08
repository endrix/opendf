package net.sf.caltrop.cal.i2.types;

import java.util.HashMap;
import java.util.Map;

import net.sf.caltrop.cal.ast.ExprLiteral;
import net.sf.caltrop.cal.ast.TypeExpr;
import net.sf.caltrop.cal.i2.Environment;
import net.sf.caltrop.cal.i2.util.Platform;

public class DefaultTypeSystem implements TypeSystem {

	public Type doEvaluate(TypeExpr te, Environment env) {
		// TODO Auto-generated method stub
		return null;
	}

	public Type evaluate(TypeExpr te, Environment env) {
		// TODO Auto-generated method stub
		return null;
	}

	public Type evaluate(TypeExpr te) {
		// TODO Auto-generated method stub
		return null;
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

	public DefaultTypeSystem (Platform platform) {
		this.platform = platform;
		initializeTypeClasses();
	}
	
	private Platform platform;
	
	private Map<String, TypeClass> typeClasses = new HashMap<String, TypeClass>();

	
	private void initializeTypeClasses() {
		addTypeClass(new TheIntType.TheClass(this));
//		addTypeClass(new TheIntegerType);
	}
	
	private void addTypeClass(TypeClass tc) {
		typeClasses.put(tc.getName(), tc);
	}
}
