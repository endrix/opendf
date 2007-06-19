package net.sf.caltrop.cal.i2.types;

import java.util.Map;

public abstract class AbstractType implements Type {

	public abstract boolean contains(Object v);
	
	public abstract Object convert(Object v);

	public abstract boolean convertible(Object v);

	public abstract boolean isConvertibleTo(Type t);

	public abstract boolean isSubtypeOf(Type t);

	public String getName() {
		return name;
	}

	public Map<String, Type> getTypeParameters() {
		return typeParameters;
	}
	
	public Map<String, Object> getValueParameters() {
		return valueParameters;
	}
	
	public AbstractType(String name, Map<String, Type> typeParameters, Map<String, Object> valueParameters) {
		this.name = name;
		this.typeParameters = typeParameters;
		this.valueParameters = valueParameters;
	}

	
	private String  			name;
	private Map<String, Type>	typeParameters;
	private Map<String, Object>	valueParameters;
}
