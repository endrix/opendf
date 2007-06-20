package net.sf.caltrop.cal.i2.types;


/**
 * An instance of BoundedIntegralTypeClass types is a representation of an integral number. The maximal number of 
 * bits available for representing an instance of this type is bounded. Consequently, operations
 * closed over these types may produce arithmetically incorrect results when the number of bits required to 
 * represent the arithmetically correct result exceeds that number.
 * 
 * BoundedIntegralTypeClass types always have a size, i.e. their {@link IntegralType#hasSize} method always returns 
 * <b>true</b>. Furthermore, it is always the case that <tt>t.size() <= tc.maxSize()</tt>.
 * 
 * Note that the maximal number of bits may include a sign bit if the instances of this type are signed.
 * 
 * @author jornj
 *
 */

public interface BoundedIntegralTypeClass extends IntegralTypeClass {
	
}
