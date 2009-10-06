package net.sf.opendf.cal.i2.types;

public interface FunctionType extends Type {

	boolean  	acceptsTypes(Type [] argTypes);
	
	Type     	resultType(Type [] argTypes);
}
