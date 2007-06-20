package net.sf.caltrop.cal.i2.types;

import java.util.Map;

public abstract class AbstractType implements Type {

	public abstract boolean contains(Object v);
	
	public abstract Object convert(Object v);

	public abstract boolean convertible(Object v);

	public abstract boolean isConvertibleTo(Type t);

	public abstract boolean isSubtypeOf(Type t);

	public TypeClass getTypeClass() {
		return typeClass;
	}

	public Map<String, Type> getTypeParameters() {
		return typeParameters;
	}
	
	public Map<String, Object> getValueParameters() {
		return valueParameters;
	}
	
	public AbstractType(TypeClass typeClass, Map<String, Type> typeParameters, Map<String, Object> valueParameters) {
		this.typeClass = typeClass;
		this.typeParameters = typeParameters;
		this.valueParameters = valueParameters;
	}


	private TypeClass			typeClass;
	private Map<String, Type>	typeParameters;
	private Map<String, Object>	valueParameters;
}
