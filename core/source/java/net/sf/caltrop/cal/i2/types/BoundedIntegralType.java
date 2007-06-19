package net.sf.caltrop.cal.i2.types;

import java.math.BigInteger;
import java.util.Map;

/**
 * An instance of BoundedIntegralType types is a representation of an integral number. The maximal number of 
 * bits available for representing an instance of this type is bounded. Consequently, operations
 * closed over these types may produce arithmetically incorrect results when the number of bits required to 
 * represent the arithmetically correct result exceeds that number.
 * 
 * Note that the maximal number of bits may include a sign bit if the instances of this type are signed.
 * 
 * @author jornj
 *
 */

public interface BoundedIntegralType extends IntegralType {

	abstract public int  		maximalBitSize();
	abstract public BigInteger	maxValue();
	abstract public BigInteger  minValue();

}
