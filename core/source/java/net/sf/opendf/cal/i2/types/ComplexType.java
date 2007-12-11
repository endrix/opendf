package net.sf.opendf.cal.i2.types;

import java.util.Map;

import net.sf.opendf.cal.ast.Expression;
import net.sf.opendf.cal.ast.TypeExpr;
import net.sf.opendf.cal.i2.Evaluator;

public class ComplexType extends AbstractType implements NumericType {

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

	public TypeClass getTypeClass() {
		// TODO Auto-generated method stub
		return null;
	}

	private ComplexType(TheClass tc) {
		super(tc);
	}
	
	////////////////////////////////////////////////////////////////////////////
	////  TypeClass
	////////////////////////////////////////////////////////////////////////////
	
	public static class TheClass extends AbstractTypeClass {

		@Override
		public Type createType(TypeExpr te, Evaluator eval) {
			return new ComplexType(this);
		}
		
		public TheClass(String name, TypeSystem typeSystem) {
			super(name, typeSystem);
		}
	}	
}
