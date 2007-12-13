package net.sf.opendf.cal.i2.types;

import java.util.Map;

import net.sf.opendf.cal.ast.Expression;
import net.sf.opendf.cal.ast.TypeExpr;
import net.sf.opendf.cal.i2.Evaluator;

public abstract class AbstractTypeClass implements TypeClass {

	public abstract Type createType(TypeExpr te, Evaluator eval);

	
	
	public String getName() {
		return name;
	}

	public TypeSystem getTypeSystem() {
		return typeSystem;
	}
	
	public AbstractTypeClass(String name, TypeSystem typeSystem) {
		this.name = name;
		this.typeSystem = typeSystem;
	}
	
	protected int  getIntParameter(TypeExpr te, String name, int defaultValue, Evaluator eval) {
		Map<String, Expression> vp = te.getValueParameters();
		if (vp == null) {
			return defaultValue;
		}
		Expression e = vp.get(name);
		if (e == null) {
			return defaultValue;
		}
		Object v = eval.valueOf(e);
		return eval.getConfiguration().intValue(v);
	}
	
	protected Type getTypeParameter(TypeExpr te, String name, Type defaultValue, Evaluator eval) {
		Map<String, TypeExpr> tp = te.getTypeParameters();
		if (tp == null) {
			return defaultValue;
		}
		TypeExpr e = tp.get(name);
		if (e == null) {
			return defaultValue;
		}
		
		return typeSystem.evaluate(e, eval);
	}

	private String 		name;
	private TypeSystem	typeSystem;
}
