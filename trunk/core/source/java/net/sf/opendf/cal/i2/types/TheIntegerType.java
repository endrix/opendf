package net.sf.opendf.cal.i2.types;

import java.math.BigInteger;
import java.util.Map;

import net.sf.opendf.cal.ast.TypeExpr;
import net.sf.opendf.cal.i2.Evaluator;

public class TheIntegerType extends AbstractIntegralType implements IntegralType {

	//
	//  Type
	//
	
	public boolean  contains(Object v) {
		return v instanceof BigInteger;
	}
	
	public Object  convert(Object v) {
		if (v instanceof BigInteger)
			return v;
		if ((v instanceof Long)
				|| (v instanceof Integer)
				|| (v instanceof Short)
				|| (v instanceof Byte)) {
			return BigInteger.valueOf(((Number)v).longValue());
		}
		throw new TypeConversionException(this, v);
	}
	
	public boolean  convertible(Object v) {
		if (contains(v))
			return true;
		if ((v instanceof Long)
			|| (v instanceof Integer)
			|| (v instanceof Short)
			|| (v instanceof Byte))
			return true;
		
		return false;
	}
	
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
			return singletonIntegerType;
		}

		public TheClass (String name, TypeSystem ts) {
			super(name, ts);
		}
		
		private Type singletonIntegerType = new TheIntegerType(this);
	}
}
