package net.sf.caltrop.cal.i2.types;

import java.util.Map;

public abstract class AbstractType implements Type {

	public abstract boolean contains(Object v);
	
	public abstract Object convert(Object v);

	public abstract boolean convertible(Object v);

	public TypeClass getTypeClass() {
		return typeClass;
	}

	public AbstractType(TypeClass typeClass) {
		this.typeClass = typeClass;
	}

	private TypeClass			typeClass;
}
