package net.sf.opendf.xslt.cal;

import net.sf.opendf.cal.interpreter.Function;

public abstract class AbstractUnarySBFunction implements Function {

	public Object apply(Object[] args) {
		TypedObject a = (TypedObject)args[0];
		return doFunction(a);
	}

	public int arity() {
		return 1;
	}
	
	protected  TypedObject    doFunction(TypedObject a) {
		if (a.getValue() == TypedContext.UNDEFINED) {
			return new TypedObject(doTypeFunction(a.getType()), TypedContext.UNDEFINED);
		} else {
			return doValueFunction(a);
		}
	}
	
	abstract protected TypedObject  doValueFunction(TypedObject a);

	protected Type         doTypeFunction(Type t) {
		return t;	// default behavior
	}
}
