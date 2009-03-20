package net.sf.opendf.cal.i2.types;

import java.math.BigInteger;
import java.util.Map;

abstract public class AbstractIntegralType extends AbstractType implements IntegralType {

	//
	//  Type
	//
	
	abstract public boolean  contains(Object v);

	abstract public Object  convert(Object v);
	
	abstract public boolean  convertible(Object v);
	
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
	

	//
	//  RealType
	//

	public double doubleValue(Object v) {
		return ((BigInteger)v).doubleValue();
	}

	abstract public boolean isSigned();
	
	protected AbstractIntegralType(TypeClass typeClass) {
		super (typeClass);
		maxValue = minValue = cardinality = null;
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
	
	protected BigInteger  getCardinality() {
		if (cardinality == null) {
			cardinality = BigInteger.ONE.shiftLeft(size());
		}
		return cardinality;
	}
	
	private BigInteger  maxValue;
	private BigInteger  minValue;
	private BigInteger  cardinality;
	private BigInteger  mask;
}
