package net.sf.caltrop.cal.i2.types;

import java.util.Map;

public interface Type {
	
	/**
	 * Get the type name of this type.
	 * 
	 * @return The type name.
	 */

	String  getName();
	
	/**
	 * Get the value parameters of this type.
	 * 
	 * @return A map containing the value parameters.
	 */

	Map<String, Object> getValueParameters();
	
	/**
	 * Get the type parameters of this type.
	 * 
	 * @return A map containing the type parameters.
	 */

	Map<String, Type>	getTypeParameters();
	
	/**
	 * Determine whether the object is an instance of this type.
	 * 
	 * @param v The object.
	 * @return True if the object is an instance of this type.
	 */
		
	boolean  contains(Object v);
	
	/**
	 * Determine whether the object can be converted to this type. Instances 
	 * of a type are always convertable to it, i.e. {@link #contains(Object) contains} 
	 * implies {@link #convertible(Object) convertable}.
	 * 
	 * @param v The object.
	 * @return True, if the object is convertable to an instance of this type.
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
	
	Object   convert(Object v);

	/**
	 * Determine whether this type is a subtype of the argument. If it is, then any instance of this type is also 
	 * an instance of the argument type.
	 * 
	 * @param t A type.
	 * @return True, if this type is a subtype of t.
	 * @see #contains(Object)
	 */
	boolean  isSubtypeOf(Type t);
	
	/**
	 * Determine whether this type is assignable to the argument type. If it is, then any instance of this type 
	 * is convertible to the argument type.
	 * 
	 * @param t A type.
	 * @return True, if this type is assignable to t.
	 */
	boolean  isAssignableTo(Type t);
}
