package net.sf.caltrop.cal.i2.types;

import java.util.Map;

public abstract class AbstractTypeClass implements TypeClass {

	public abstract Type createType(Map<String, Type> typeParameters,
			Map<String, Object> valueParameters);

	
	
	public String getName() {
		return name;
	}

	public TypeSystem getTypeSystem() {
		return typeSystem;
	}
	
	public AbstractTypeClass(String name, TypeSystem typeSystem) {
		this.name = name;
		this.typeSystem = typeSystem;
	}

	private String 		name;
	private TypeSystem	typeSystem;
}
