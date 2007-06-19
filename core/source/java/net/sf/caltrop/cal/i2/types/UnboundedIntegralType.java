package net.sf.caltrop.cal.i2.types;

import java.util.Map;

public class UnboundedIntegralType extends IntegralType implements Type {

	@Override
	public double doubleValue(Object v) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isSigned() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean contains(Object v) {
		// TODO Auto-generated method stub
		return false;
	}

	public Object convert(Object v) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean convertible(Object v) {
		// TODO Auto-generated method stub
		return false;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<String, Type> getTypeParameters() {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<String, Object> getValueParameters() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isAssignableTo(Type t) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isSubtypeOf(Type t) {
		// TODO Auto-generated method stub
		return false;
	}

}
