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
		else
			throw new RuntimeException("Cannot convert to '" + v + "' to integral type. (" + v.getClass().getName() + ")");
		if (!this.isSigned() && b.signum() < 0) {
			// then just negate, continue with masking.
			b = b.negate();
		}
		
		// now if this type is size-limited, eliminate excess bits, adjust for sign
		if (this.hasSize()) {
			b = b.and(this.getMask());
			if (b.compareTo(this.maxValue()) > 0)
				b = b.negate();
			// FIXME: This isn't quite right.
		}
		return b;	
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
		if (!hasSize())
			return null;
		if (maxValue == null) {
			int n = (isSigned()) ? size() - 1 : size();
			
			BigInteger b = BigInteger.ONE.shiftLeft(n);
			maxValue = b.subtract(BigInteger.ONE);
			minValue = (isSigned()) ? b.negate() : BigInteger.ZERO;			
		}
		return maxValue;
	}
	
	public BigInteger minValue() {
		if (!hasSize())
			return null;
		if (minValue == null) {
			int n = (isSigned()) ? size() - 1 : size();
			
			BigInteger b = BigInteger.ONE.shiftLeft(n);
			maxValue = b.subtract(BigInteger.ONE);
			minValue = (isSigned()) ? b.negate() : BigInteger.ZERO;
		}
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
	
	protected AbstractIntegralType(TypeClass typeClass) {
		super (typeClass);
		maxValue = minValue = null;
	}
	
	protected BigInteger  getMask() {
		if (!hasSize())
			return null;
		if (mask == null) {
			BigInteger b = BigInteger.ONE.shiftLeft(size());
			mask = b.subtract(BigInteger.ONE);
		}
		return mask;
	}
	
	private BigInteger  maxValue;
	private BigInteger  minValue;
	private BigInteger  mask;
}
