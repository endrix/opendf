package net.sf.caltrop.cal.i2.types;

import java.util.Map;

/**
 * TypeClass objects are the backbone of the TypeSystem. They are the factories 
 * of type objects and their connection to the type system.
 * 
 * @author jornj
 *
 */

public interface TypeClass {
	
	String  	getName();
	
	Type    	createType(Map<String, Type> typeParameters, Map<String, Object> valueParameters);
	
	TypeSystem	getTypeSystem();
}
