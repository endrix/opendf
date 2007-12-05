package net.sf.opendf.cal.i2.types;

/**
 * Instances of RealType types are representations of real numbers.
 * 
 * @author jornj
 *
 */

public interface RealType extends NumericType {
	
	public boolean isSigned();

	public double  doubleValue(Object v);
}
