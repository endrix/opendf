package net.sf.caltrop.cal.i2.util;

import net.sf.caltrop.cal.i2.Evaluator;
import net.sf.caltrop.cal.i2.Function;

abstract public class FunctionOf2Eval implements Function {

	public void apply(int n, Evaluator evaluator) {
		assert n == 2;
		
		evaluator.replaceWithResult(n, f(evaluator.getValue(0), evaluator.getValue(1), evaluator));
	}
	
	abstract public Object  f(Object a, Object b, Evaluator evaluator);

}
