package net.sf.opendf.xslt.cal;

import net.sf.opendf.cal.interpreter.Function;

public abstract class AbstractTernarySBFunction implements Function {

	public Object apply(Object[] args) {
		TypedObject a = (TypedObject)args[0];
		TypedObject b = (TypedObject)args[1];
		TypedObject c = (TypedObject)args[2];
		return doFunction(a, b, c);
	}

	public int arity() {
		return 3;
	}
	
	protected TypedObject    doFunction(TypedObject a, TypedObject b, TypedObject c) {
		if (a.getValue() == TypedContext.UNDEFINED || b.getValue() == TypedContext.UNDEFINED || c.getValue() == TypedContext.UNDEFINED) {
			return new TypedObject(doTypeFunction(a.getType(), b.getType(), c.getType()), TypedContext.UNDEFINED);
		} else {
			return doValueFunction(a, b, c);
		}
	}
	
	abstract protected TypedObject  doValueFunction(TypedObject a, TypedObject b, TypedObject c);

	protected Type         doTypeFunction(Type t1, Type t2, Type t3) {
		assert true;
		return null; 
		// return Type.lub(t1, t2); // default behavior	
	}
	
}
