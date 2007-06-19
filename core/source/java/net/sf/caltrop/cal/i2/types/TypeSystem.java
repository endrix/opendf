package net.sf.caltrop.cal.i2.types;

import net.sf.caltrop.cal.i2.Environment;
import net.sf.caltrop.cal.interpreter.ast.ExprLiteral;
import net.sf.caltrop.cal.interpreter.ast.TypeExpr;

public interface TypeSystem {

	/**
	 * Determine a type for the specified object. The type should be as 
	 * specific as possible.
	 * 
	 * @param v
	 * @return
	 */

	Type  typeOf(Object v);

	/**
	 * Evaluate the type expression in the specified environment, and create the 
	 * corresponding type object.
	 * 
	 * @param te
	 * @param env
	 * @return
	 */

	Type  evaluate(TypeExpr te, Environment env);
	
	/**
	 * Determine the type of the literal.
	 *  
	 * @param e
	 * @return
	 */

	Type  literalType(ExprLiteral e);
	
	/**
	 * Determine a supertype for the two arguments. This supertype should be 
	 * as specific as possible. If no such supertype exists, return <b>null</b>.
	 * 
	 * @param t1 A type.
	 * @param t2 Another type.
	 * @return A supertype of the two arguments, <b>null</b> if none exists.
	 */
	
	Type  lub(Type t1, Type t2);
}
