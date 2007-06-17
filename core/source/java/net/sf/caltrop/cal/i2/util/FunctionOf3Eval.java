package net.sf.caltrop.cal.i2.util;

import net.sf.caltrop.cal.i2.Evaluator;
import net.sf.caltrop.cal.i2.Function;

abstract public class FunctionOf3Eval implements Function {

	public void apply(int n, Evaluator evaluator) {
		assert n == 3;
		
		evaluator.replaceWithResult(n, f(evaluator.getValue(0), evaluator.getValue(1), evaluator.getValue(2), evaluator));
	}
	
	abstract public Object  f(Object a, Object b, Object c, Evaluator evaluator);

}
