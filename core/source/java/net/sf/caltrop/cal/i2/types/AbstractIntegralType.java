package net.sf.caltrop.cal.i2.types;

import java.math.BigInteger;
import java.util.Map;

abstract public class AbstractIntegralType extends AbstractType implements IntegralType {

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
		if (!this.hasSize())
			return true;
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
		if (contains(v))
			return v;
		
		return v; // FIXME
		
	}

	@Override
	public boolean convertible(Object v) {
		return v instanceof BigInteger;
	}
	
	//
	//  IntegralType
	//

	abstract public boolean hasSize();
	
	public BigInteger maxValue() {
		return maxValue;
	}
	
	public BigInteger minValue() {
		return minValue;
	}

	abstract public int size();
	
	abstract public int maxSize();

	//
	//  RealType
	//

	public double doubleValue(Object v) {
		return ((BigInteger)v).doubleValue();
	}

	abstract public boolean isSigned();
	
	protected AbstractIntegralType(TypeClass typeClass, Map<String, Type> typeParameters, Map<String, Object> valueParameters) {
		super (typeClass, typeParameters, valueParameters);
		if (hasSize()) {
			int n = (isSigned()) ? size() - 1 : size();
			
			BigInteger b = BigInteger.ONE.shiftLeft(n);
			maxValue = b.subtract(BigInteger.ONE);
			minValue = (isSigned()) ? b.negate() : BigInteger.ZERO;
		} else {
			maxValue = minValue = null;
		}
	}
	
	private BigInteger  maxValue;
	private BigInteger  minValue;

}
