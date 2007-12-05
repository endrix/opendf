package net.sf.opendf.cal.i2.types;

import java.util.Map;

import net.sf.opendf.cal.ast.TypeExpr;
import net.sf.opendf.cal.i2.Evaluator;

public class TheIntegerType extends AbstractIntegralType implements IntegralType {

	//
	//  IntegralType
	//

	@Override
	public boolean hasSize() {
		return false;
	}

	@Override
	public int size() {
		throw new RuntimeException("Cannot retrieve size of unbounded integer type.");
	}

	@Override
	public int maxSize() {
		throw new RuntimeException("Cannot retrieve max size of unbounded integer type.");
	}
	
	//
	//  RealType
	//
	
	public boolean  isSigned() {
		return true;
	}

	// 
	// Ctor
	//

	private TheIntegerType(TypeClass typeClass) {
		super(typeClass);
	}

	//
	//  data
	//



	//
	//  the class
	//

	public static class TheClass extends AbstractTypeClass {

		public Type createType(TypeExpr te, Evaluator eval) {
			return new TheIntegerType(this);
		}

		public TheClass (TypeSystem ts) {
			super("integer", ts);
		}
	}
}
