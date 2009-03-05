package net.sf.opendf.cal.i2.types;

public interface IntegralTypeClass extends TypeClass {
	
	boolean  hasMaxSize();
	
	int      getMaxSize();
	
	boolean  isSigned();

}
