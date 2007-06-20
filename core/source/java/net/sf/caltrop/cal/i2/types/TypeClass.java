package net.sf.caltrop.cal.i2.types;

import java.util.Map;

public interface TypeClass {
	
	String  	getName();
	
	Type    	createType(Map<String, Type> typeParameters, Map<String, Object> valueParameters);
	
	TypeSystem	getTypeSystem();
}
