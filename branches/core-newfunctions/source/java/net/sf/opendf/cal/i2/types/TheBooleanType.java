package net.sf.opendf.cal.i2.types;

import net.sf.opendf.cal.ast.TypeExpr;
import net.sf.opendf.cal.i2.Evaluator;

public class TheBooleanType extends AbstractType {

	//
	//  Type
	//
	
	@Override
	public boolean contains(Object v) {
		return v == null || v instanceof Boolean;
	}
	
	//
	//  Ctor
	//
	
	public TheBooleanType(TypeClass typeClass) {
		super(typeClass);
	}

	//
	//  the class
	//

	public static class TheClass extends AbstractTypeClass {

		public Type createType(TypeExpr te, Evaluator eval) {
			return singletonBooleanType;
		}

		public TheClass (String name, TypeSystem ts) {
			super(name, ts);
		}
		
		private Type singletonBooleanType = new TheBooleanType(this);
	}

}
