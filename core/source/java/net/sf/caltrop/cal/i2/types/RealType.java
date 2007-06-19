package net.sf.caltrop.cal.i2.types;

/**
 * Instances of RealType types are representations of real numbers.
 * 
 * @author jornj
 *
 */

public abstract class RealType extends NumericType implements Type {
	
	public abstract boolean isSigned();

	public abstract double  doubleValue(Object v);
}
