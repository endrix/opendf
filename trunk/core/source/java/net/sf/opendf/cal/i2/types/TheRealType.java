package net.sf.opendf.cal.i2.types;

import net.sf.opendf.cal.ast.TypeExpr;
import net.sf.opendf.cal.i2.Evaluator;

public class TheRealType extends AbstractType implements RealType {

	//
	//  Type
	//
	
	@Override
	public boolean contains(Object v) {
		return v instanceof Number;
	}

	//
	//  RealType
	//
	
	public boolean isSigned() {
		return true;
	}

	public double  doubleValue(Object v) {
		return ((Number)v).doubleValue();
	}
	
	//
	//  Ctor
	//
	
	private TheRealType(TypeClass tc) {
		super(tc);
	}
	
	//////////////////////
	// TypeClass
	//////////////////////

	public static class TheClass extends AbstractTypeClass {

		@Override
		public Type createType(TypeExpr te, Evaluator eval) {
			return singletonType;
		}
		
		public TheClass(String name, TypeSystem typeSystem) {
			super(name, typeSystem);
			singletonType = new TheRealType(this);
		}
		
		private Type singletonType;
	}	

}
