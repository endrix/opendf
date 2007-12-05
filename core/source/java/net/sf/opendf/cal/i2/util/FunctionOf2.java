package net.sf.opendf.cal.i2.util;

import net.sf.opendf.cal.i2.Evaluator;
import net.sf.opendf.cal.i2.Function;

abstract public class FunctionOf2 implements Function {

	public void apply(int n, Evaluator evaluator) {
		assert n == 2;
		
		evaluator.replaceWithResult(n, f(evaluator.getValue(0), evaluator.getValue(1)));
	}
	
	abstract public Object  f(Object a, Object b);

}
