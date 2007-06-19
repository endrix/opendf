package net.sf.caltrop.cal.i2.types;

import net.sf.caltrop.cal.i2.Environment;
import net.sf.caltrop.cal.interpreter.ast.ExprLiteral;
import net.sf.caltrop.cal.interpreter.ast.TypeExpr;

public interface TypeSystem {

	Type  typeOf(Object v);
	
	Type  evaluate(TypeExpr te, Environment env);
	
	Type  literalType(ExprLiteral e);
}
