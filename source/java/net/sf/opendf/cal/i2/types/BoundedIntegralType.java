package net.sf.opendf.cal.i2.types;

import java.math.BigInteger;
import java.util.Map;

import net.sf.opendf.cal.ast.Expression;
import net.sf.opendf.cal.ast.TypeExpr;
import net.sf.opendf.cal.i2.Evaluator;

public class BoundedIntegralType extends AbstractIntegralType implements IntegralType {

	//
	//  Type
	//
	
	@Override
	public boolean contains(Object v) {
		if (!(v instanceof BigInteger))
			return false;
		BigInteger b = (BigInteger)v;
		if (!this.isSigned() && b.signum() < 0)
			return false;
		int c1 = b.compareTo(this.minValue());
		if (c1 < 0) 
			return false;
		int c2 = b.compareTo(this.maxValue());
		if (c2 > 0) 
			return false;
		return true;
	}
	

	@Override
	public Object convert(Object v) {
		if (v == null)
			return null;
		if (contains(v))
			return v;
		
		// this number is either negative for an unsigned type, or it is too big.
		// let's see if this type is unsigned, and the number is negative:
		
		BigInteger b = null;
		if (v instanceof BigInteger)
			b = (BigInteger)v;
		else if (v instanceof Long)
			b = BigInteger.valueOf(((Long)v).longValue());
		else if (v instanceof Integer) 
			b = BigInteger.valueOf(((Integer)v).intValue());
		else if (v instanceof Short) 
			b = BigInteger.valueOf(((Short)v).intValue());
		else if (v instanceof Byte) 
			b = BigInteger.valueOf(((Byte)v).intValue());
		else if (v instanceof Boolean)
			b = BigInteger.valueOf((((Boolean) v).booleanValue() ? 1 : 0)); //FIXME Remove when type assertions are implemented 		
		else			
			throw new TypeConversionException(this, v);

		b = b.and(this.getMask());
		if (b.compareTo(this.maxValue()) > 0)
			b = b.subtract(this.getCardinality());
		return b;	
	}

	@Override
	public boolean convertible(Object v) {
		return v instanceof BigInteger;
	}


	
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
		
	//
	//  RealType
	//
	
	public boolean  isSigned() {
		return typeClass.isSigned();
	}
	
	// 
	// Ctor
	//
	
	private BoundedIntegralType(TheClass typeClass, int sz) {
		super(typeClass);
		this.size = sz;
		this.typeClass = typeClass;
	}
	
	public String toString() {
		return this.getTypeClass().getName() + "(size=" + size + ")";
	}
	
	//
	//  data
	//
	
	private TheClass typeClass;
	private int size;
	
	
	////////////////////////////////////////////////////////////////////////////
	////  TypeClass
	////////////////////////////////////////////////////////////////////////////
	
	public static class TheClass extends AbstractTypeClass implements IntegralTypeClass {

		@Override
		public Type createType(TypeExpr te, Evaluator eval) {
			int sz = getIntParameter(te, vpSize, getMaxSize(), eval);
			return new BoundedIntegralType(this, sz);
		}
		
		public boolean  isSigned() {
			return signed;
		}

		public boolean hasMaxSize() {
			return true;
		}
		
		public int getMaxSize() {
			return maxSize;
		}

		public TheClass(String name, TypeSystem typeSystem, boolean signed, int maxSize) {
			super(name, typeSystem);
			this.maxSize = maxSize;
			this.signed = signed;
		}
		
		private int maxSize;
		private boolean signed;

		final static String vpSize = "size"; 
	}


}
