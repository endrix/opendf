package net.sf.caltrop.cal.i2.types;

import java.util.Map;

/**
 * A Type object represents a collection of CAL data objects. It provides operations that relate 
 * it to other types, and to objects.
 * 
 * @author jornj
 *
 */

public interface Type {
	
	/**
	 * Get the type class of this type.
	 * 
	 * @return The type class of this type.
	 */

	TypeClass  getTypeClass();
	
	/**
	 * Determine whether the object is an instance of this type.
	 * 
	 * @param v The object.
	 * @return True if the object is an instance of this type.
	 */
		
	boolean  contains(Object v);
	
	/**
	 * Determine whether the object can be converted to this type. Instances 
	 * of a type are always convertible to it, i.e. {@link #contains(Object) contains} 
	 * implies {@link #convertible(Object) convertible}.
	 * 
	 * @param v The object.
	 * @return True, if the object is convertible to an instance of this type.
	 * @see #contains(Object)
	 * @see #convert(Object)
	 */
	
	boolean  convertible(Object v);
	
	/**
	 * Convert object to an instance of this type. If {@link #contains(Object)} returns true 
	 * for this object, then the object itself should be returned. If {@link #convertible(Object)} 
	 * returns false, the result of this method is undefined, and it may throw an exception.
	 * 
	 * The object returned from this method must be an instance of this type, i.e. {@link #contains(Object)} 
	 * should return true for it.
	 * 
	 * @param v The object.
	 * @return An instance of this type resulting from converting the parameter object.
	 * @see #contains(Object)
	 * @see #convertible(Object)
	 */
	
	Object   convert(Object v); // throws TypeConversionWarning; FIXME --- needs to provide waring when losing precision
}
