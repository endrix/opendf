
package net.sf.caltrop.xslt.cal;


import net.sf.caltrop.cal.interpreter.ast.TypeExpr;


/**
 * A TypedObject represents a data object inside the Cal interpreter on the 
 * SystemBuilder platform. It carries the relevant runtime type information 
 * alongside the object representing the data value. In fact, operations may
 * fail to operate on the data value, and the data value may even be absent, 
 * and still the type information is operated upon correctly.
 * 
 * @author jornj
 */

public class TypedObject {
	
	public Type getType() {
		return type;
	}
	public Object getValue() {
		return value;
	}
		
	public TypedObject(Type type, Object value) {
		super();
		// TODO Auto-generated constructor stub
		this.type = type;
		this.value = value;
	}
	
	public String toString() {
		return value.toString();
	}

	private Object    value;
	private Type      type;
}
