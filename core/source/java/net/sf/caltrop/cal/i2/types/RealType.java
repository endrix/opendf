package net.sf.caltrop.cal.i2.types;

public abstract class RealType extends NumericType implements Type {
	
	public abstract boolean isSigned();

	public abstract double  doubleValue(Object v);
}
