package net.sf.opendf.cal.i2.types;

import java.math.BigInteger;

/**
 * Instances of IntegralType types are representations of integers.
 * 
 * @author jornj
 *
 */

public interface IntegralType extends RealType {
	
	/**
	 * Determine whether this type has a size limit for representations of 
	 * its values.
	 * 
	 * @return True, if the values represented by this type are limited in size.
	 */

	boolean  hasSize();
	
	/**
	 * Return the size limit for integer representations, in bits. The behavior 
	 * of this method is undefined if (@link #hasSize} returns false for this type.
	 * 
	 * @return The size limit.
	 */
	
	int      size();
	
	BigInteger	maxValue();

	BigInteger  minValue();

}
