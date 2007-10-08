package net.sf.caltrop.cal.i2.types;

import java.util.Map;

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

	private TheIntegerType(TypeClass typeClass, Map<String, Type> typeParameters, Map<String, Object> valueParameters) {
		super(typeClass, typeParameters, valueParameters);
	}

	//
	//  data
	//



	//
	//  the class
	//

	public static class TheClass extends AbstractTypeClass {

		public Type createType(Map<String, Type> typeParameters,
				Map<String, Object> valueParameters) {
			return new TheIntegerType(this, typeParameters, valueParameters);
		}

		public TheClass (TypeSystem ts) {
			super("integer", ts);
		}
	}
}
