package net.sf.caltrop.xslt.cal;

import net.sf.caltrop.cal.interpreter.Function;

public abstract class AbstractBinarySBFunction implements Function {

	public Object apply(Object[] args) {
		TypedObject a = (TypedObject)args[0];
		TypedObject b = (TypedObject)args[1];
		return doFunction(a, b);
	}

	public int arity() {
		return 2;
	}
	
	protected TypedObject    doFunction(TypedObject a, TypedObject b) {
		if (a.getValue() == TypedContext.UNDEFINED || b.getValue() == TypedContext.UNDEFINED) {
			return new TypedObject(doTypeFunction(a.getType(), b.getType()), TypedContext.UNDEFINED);
		} else {
			return doValueFunction(a, b);
		}
	}
	
	abstract protected TypedObject  doValueFunction(TypedObject a, TypedObject b);

	protected Type         doTypeFunction(Type t1, Type t2) {
		return Type.lub(t1, t2); // default behavior	
	}
	
}
