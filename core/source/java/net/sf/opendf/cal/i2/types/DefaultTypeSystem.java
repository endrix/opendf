package net.sf.opendf.cal.i2.types;

import java.util.HashMap;
import java.util.Map;

import net.sf.opendf.cal.ast.ExprLiteral;
import net.sf.opendf.cal.ast.TypeExpr;
import net.sf.opendf.cal.i2.Environment;
import net.sf.opendf.cal.i2.Evaluator;
import net.sf.opendf.cal.i2.util.Platform;

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
		String name = te.getName();
		if (name == null || "".equals(name.trim())) {
			// FIXME: also issue warning
			return null;			
		}
		TypeClass tc = typeClasses.get(name);
		if (tc == null) {
			tc = new JavaType.TheClass(name, this);
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
		addTypeClass(new TheBooleanType.TheClass("bool", this));
		
		addTypeClass(new BoundedIntegralType.TheClass("short", this, true, 16));
		addTypeClass(new BoundedIntegralType.TheClass("ushort", this, false, 16));
		addTypeClass(new BoundedIntegralType.TheClass("int", this, true, 32));
		addTypeClass(new BoundedIntegralType.TheClass("uint", this, false, 32));
		addTypeClass(new BoundedIntegralType.TheClass("long", this, true, 64));
		addTypeClass(new BoundedIntegralType.TheClass("ulong", this, false, 64));

		addTypeClass(new BoundedIntegralType.TheClass("byte", this, false, 8));
		addTypeClass(new TheIntegerType.TheClass("integer", this));
		
		addTypeClass(new ListType.TheClass("list", this));
	}
	
	private void addTypeClass(TypeClass tc) {
		typeClasses.put(tc.getName(), tc);
	}
}
