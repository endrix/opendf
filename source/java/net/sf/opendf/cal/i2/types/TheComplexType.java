package net.sf.opendf.cal.i2.types;

import java.util.Map;

import net.sf.opendf.cal.ast.Expression;
import net.sf.opendf.cal.ast.TypeExpr;
import net.sf.opendf.cal.i2.Evaluator;
import net.sf.opendf.math.Complex;

public class TheComplexType extends AbstractType implements NumericType {

	@Override
	public boolean contains(Object v) {
		return v instanceof Complex;
	}

	@Override
	public Object convert(Object v) {
		if (v instanceof Complex) {
			return v;			
		}
		if (v instanceof Number) {
			return new Complex(((Number)v).doubleValue(), 0);
		}
		throw new TypeConversionException(this, v);
	}

	@Override
	public boolean convertible(Object v) {
		if (contains(v))
			return true;
		if (v instanceof Number)
			return true;
		return false;
	}

	private TheComplexType(TheClass tc) {
		super(tc);
	}
	
	////////////////////////////////////////////////////////////////////////////
	////  TypeClass
	////////////////////////////////////////////////////////////////////////////
	
	public static class TheClass extends AbstractTypeClass {

		@Override
		public Type createType(TypeExpr te, Evaluator eval) {
			return singletonType;
		}
		
		public TheClass(String name, TypeSystem typeSystem) {
			super(name, typeSystem);
			singletonType = new TheComplexType(this);
		}
		
		private Type singletonType;
	}	
}
