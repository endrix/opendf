package net.sf.opendf.cal.i2.types;

import java.util.Map;

import net.sf.opendf.cal.ast.Expression;
import net.sf.opendf.cal.ast.TypeExpr;
import net.sf.opendf.cal.i2.Evaluator;

public class BoundedIntegralTypeClass extends AbstractTypeClass implements IntegralTypeClass {

	@Override
	public Type createType(TypeExpr te, Evaluator eval) {
		Map<String, Expression> vp = te.getValueParameters();
		int sz = getIntParameter(te, vpSize, getMaxSize(), eval);
		return new BoundedIntegralType(this, sz);
	}
	
	public boolean  isSigned() {
		return signed;
	}

	public boolean hasMaxSize() {
		return true;
	}
	
	public int getMaxSize() {
		return maxSize;
	}

	public BoundedIntegralTypeClass(String name, TypeSystem typeSystem, boolean signed, int maxSize) {
		super(name, typeSystem);
		this.maxSize = maxSize;
		this.signed = signed;
	}
	
	private int maxSize;
	private boolean signed;

	final static String vpSize = "size"; 
}
