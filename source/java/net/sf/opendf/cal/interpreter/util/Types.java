package net.sf.opendf.cal.interpreter.util;

import java.util.Map;

import net.sf.opendf.cal.ast.Expression;
import net.sf.opendf.cal.ast.TypeExpr;
import net.sf.opendf.cal.interpreter.Context;
import net.sf.opendf.cal.interpreter.ExprEvaluator;
import net.sf.opendf.cal.interpreter.environment.Environment;

public class Types {
	
	public static TypeCheckResult  checkType(TypeExpr tp, Object val, Context context, Environment env) {
		
		ExprEvaluator evaluator = new ExprEvaluator(context, env);
		
		if (typeInt.equals(tp.getName())) {
			if (!context.isInteger(val))
				return TypeCheckResult.NO;
			
			Map vp = tp.getValueParameters();
			Expression objSzExpr = (Expression)((vp == null) ? null : vp.get(attrSize));
			Object objSz = evaluator.evaluate(objSzExpr);
			if (objSz == null || !context.isInteger(objSz)) {
				return TypeCheckResult.YES;
			}
			int sz = context.intValue(objSz);
			if (sz < context.getMinimalIntegerLength(val)) {
				return TypeCheckResult.NO;
			} else {
				return TypeCheckResult.YES;
			}
		}
		
		return TypeCheckResult.YES;
	}
	
	public static enum TypeCheckResult {YES, NO, UNDECIDED};
	
	public final static String  typeBoolean = "boolean";
	public final static String  typeInt = "int";
	public final static String  typeList = "list";
		
	public final static String  attrSize = "size";
}
