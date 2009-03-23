package net.sf.opendf.cal.i2.types;

public class TypeConversionException extends RuntimeException {
	
	public Type  getType() {
		return type;
	}
	
	public Object  getValue() {
		return value;
	}

	public TypeConversionException(Type t, Object v) {
		this("Type conversion exception. Cannot convert '" + v + "' to type '" + t + "'.", t, v);
	}
	
	public TypeConversionException(String message, Type t, Object v) {
		super(message);
		this.type = t;
		this.value = v;
	}
	
	private Type type;
	private Object value;
	
	
}
