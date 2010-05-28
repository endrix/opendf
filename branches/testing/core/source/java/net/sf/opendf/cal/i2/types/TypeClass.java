package net.sf.opendf.cal.i2.types;


import net.sf.opendf.cal.ast.TypeExpr;
import net.sf.opendf.cal.i2.Environment;
import net.sf.opendf.cal.i2.Evaluator;

/**
 * TypeClass objects are the backbone of the TypeSystem. They are the factories 
 * of type objects and their connection to the type system.
 * 
 * @author jornj
 *
 */

public interface TypeClass {
	
	String  	getName();
	
	Type    	createType(TypeExpr te, Evaluator eval);
	
	TypeSystem	getTypeSystem();
}