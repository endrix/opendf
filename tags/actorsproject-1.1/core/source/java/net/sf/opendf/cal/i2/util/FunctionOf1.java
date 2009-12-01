package net.sf.opendf.cal.i2.util;

import net.sf.opendf.cal.i2.Evaluator;
import net.sf.opendf.cal.i2.Function;

abstract public class FunctionOf1 implements Function {

	public void apply(int n, Evaluator evaluator) {
		assert n == 1;
		
		evaluator.replaceWithResult(n, f(evaluator.getValue(0)));
	}
	
	abstract public Object  f(Object a);

}