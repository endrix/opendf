package net.sf.opendf.cal.i2.types;

import net.sf.opendf.cal.ast.ExprLiteral;
import net.sf.opendf.cal.ast.TypeExpr;
import net.sf.opendf.cal.i2.Environment;
import net.sf.opendf.cal.i2.Evaluator;

/**
 * A TypeSystem is a structured collection of types and type classes. The TypeSystem itself
 * contains references to TypeClass objects that it can resolve by name. It also controls the
 * relation between types.
 * 
 * This is in contrast to the Type objects themselves, which determine their relation to data
 * objects.
 * 
 * @author jornj
 *
 */

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
	 * Evaluate the type expression in the global (or empty) environment, and create the 
	 * corresponding type object.
	 * 
	 * @param te
	 * @param env
	 * @return
	 */

	Type  evaluate(TypeExpr te, Evaluator eval);
	
	/**
	 * Evaluate the type expression in the specified environment, and create the 
	 * corresponding type object. Do not use caching.
	 * 
	 * @param te
	 * @param env
	 * @return
	 */

	Type  doEvaluate(TypeExpr te, Evaluator eval);
	
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
	
	/**
	 * Determine whether t1 is assignable to t2. This is the case iff for every
	 * object a that is an instance of t1 (i.e. t1.contains(a)), a is convertible to t2,
	 * i.e. t2.convertible(a). 
	 */
	
	boolean assignableTo(Type t1, Type t2);
}
