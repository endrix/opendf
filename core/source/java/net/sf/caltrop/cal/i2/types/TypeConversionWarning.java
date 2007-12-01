
package net.sf.caltrop.cal.i2.types;

public class TypeConversionWarning extends RuntimeException {

	public TypeConversionWarning(Object original, Type type, Object converted) {
		this (original, type, converted, 
		      "Information may be lost converting " + original + " to type "
			   + type + " (resulting in " + converted + ").");
	}
	
	public TypeConversionWarning(Object original, Type type, Object converted, String message) {
		super (message);
		this.original = original;
		this.type = type;
		this.converted = converted;
	}
		
	public Object getOriginal() {
		return original;
	}

	public Type getType() {
		return type;
	}

	public Object getConverted() {
		return converted;
	}
	
	private Object original;
	private Type type;
	private Object converted;

}
