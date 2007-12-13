package net.sf.opendf.cal.i2.types;

import java.util.Map;

public abstract class AbstractType implements Type {

	public abstract boolean contains(Object v);
	
	public Object convert(Object v) {
		if (contains(v)) {
			return v;
		} else {
			throw new TypeConversionException(this, v);
		}
	}

	public boolean convertible(Object v) {
		return contains(v);
	}

	public TypeClass getTypeClass() {
		return typeClass;
	}

	public AbstractType(TypeClass typeClass) {
		this.typeClass = typeClass;
	}
	
	public String toString() {
		return this.getTypeClass().getName();
	}

	private TypeClass			typeClass;
}
