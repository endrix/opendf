package net.sf.opendf.xslt.cal;

import net.sf.opendf.cal.interpreter.Function;

public abstract class AbstractQuaternarySBFunction implements Function {

	public Object apply(Object[] args) {
		TypedObject a = (TypedObject)args[0];
		TypedObject b = (TypedObject)args[1];
		TypedObject c = (TypedObject)args[2];
		TypedObject d = (TypedObject)args[3];
		return doFunction(a, b, c, d);
	}

	public int arity() {
		return 4;
	}
	
	protected TypedObject    doFunction(TypedObject a, TypedObject b, TypedObject c, TypedObject d) {
		if (a.getValue() == TypedContext.UNDEFINED || b.getValue() == TypedContext.UNDEFINED || c.getValue() == TypedContext.UNDEFINED || d.getValue() == TypedContext.UNDEFINED) {
			return new TypedObject(doTypeFunction(a.getType(), b.getType()), TypedContext.UNDEFINED);
		} else {
			return doValueFunction(a, b, c, d);
		}
	}
	
	abstract protected TypedObject  doValueFunction(TypedObject a, TypedObject b, TypedObject c, TypedObject d);

	protected Type         doTypeFunction(Type t1, Type t2) {
		return Type.lub(t1, t2); // default behavior	
	}
	
}
