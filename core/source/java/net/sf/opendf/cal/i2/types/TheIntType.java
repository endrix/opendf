package net.sf.caltrop.cal.i2.types;

import java.math.BigInteger;
import java.util.Map;

import net.sf.caltrop.cal.ast.Expression;
import net.sf.caltrop.cal.ast.TypeExpr;
import net.sf.caltrop.cal.i2.Evaluator;

public class TheIntType extends AbstractIntegralType implements IntegralType {

	//
	//  IntegralType
	//
	
	@Override
	public boolean hasSize() {
		return true;
	}
	
	@Override
	public int size() {
		return size;
	}
	
	@Override
	public int maxSize() {
		return 32;
	}
	
	//
	//  RealType
	//
	
	public boolean  isSigned() {
		return true;
	}
	
	// 
	// Ctor
	//
	

	private TheIntType(TypeClass typeClass, int sz) {
		super(typeClass);
		this.size = sz;
	}
	
	//
	//  data
	//
	
	private int size;

	
	
	//
	//  the class
	//
	
	public static class TheClass extends AbstractTypeClass {

		public Type createType(TypeExpr te, Evaluator eval) {
			Map<String, Expression> vp = te.getValueParameters();
			int sz = getIntParameter(te, "size", 32, eval);
			return new TheIntType(this, sz);
		}

		public TheClass (TypeSystem ts) {
			super("int", ts);
		}
	}
	
	final static String vpSize = "size"; 
}
