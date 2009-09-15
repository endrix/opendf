package net.sf.opendf.cal.i2.types;

import net.sf.opendf.cal.ast.TypeExpr;
import net.sf.opendf.cal.i2.Evaluator;
import net.sf.opendf.cal.i2.java.ClassObject;

public class JavaType extends AbstractType {

	//
	//  Type
	//
	
	@Override
	public boolean contains(Object v) {
		if (v == null)
			return true;
		return javaClass.isAssignableFrom(v.getClass());
	}

	@Override
	public Object convert(Object v) {
		if (contains(v)) {
			return v;
		} else {
			throw new TypeConversionException(this, v);
		}
	}

	@Override
	public boolean convertible(Object v) {
		return contains(v);
	}

	//
	//  Ctor
	//
	
	public JavaType(TheClass typeClass, Class c) {
		super(typeClass);
		this.javaClass = c;
	}
	
	public String toString() {
		return this.getTypeClass().getName() + "(javaClass=\"" + javaClass.getName() + "\")";
	}
	
	private Class javaClass ;
	
	////////////////////////////////////////////////////////////////////////////
	////  TypeClass
	////////////////////////////////////////////////////////////////////////////
	
	public static class TheClass extends AbstractTypeClass  {

		@Override
		public Type createType(TypeExpr te, Evaluator eval) {
			Object c = eval.getEnvironment().getByName(this.getName());
			if (c instanceof ClassObject) {
				Class jc = ((ClassObject)c).getClassObject();
				return new JavaType(this, jc);
			} else {
				throw new RuntimeException("Is not visible Java type: '" + this.getName() + "'.");
			}
		}
		
		public TheClass(String name, TypeSystem typeSystem) {
			super(name, typeSystem);
		}
	}



}
