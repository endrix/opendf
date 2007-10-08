package net.sf.caltrop.cal.i2.types;

import java.math.BigInteger;
import java.util.Map;

public class TheIntType extends AbstractIntegralType implements IntegralType {

	//
	//  IntegralType
	//
	
	@Override
	public boolean hasSize() {
		return true;
	}
	
	@Override
	public int size() {
		return size;
	}
	
	@Override
	public int maxSize() {
		return 32;
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
	

	private TheIntType(TypeClass typeClass, Map<String, Type> typeParameters, Map<String, Object> valueParameters) {
		super(typeClass, typeParameters, valueParameters);
	}
	
	//
	//  data
	//
	
	private int size;

	
	
	//
	//  the class
	//
	
	public static class TheClass extends AbstractTypeClass {

		public Type createType(Map<String, Type> typeParameters,
				Map<String, Object> valueParameters) {
			return new TheIntType(this, typeParameters, valueParameters);
		}

		public TheClass (TypeSystem ts) {
			super("int", ts);
		}
	}
}
