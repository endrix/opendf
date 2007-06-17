package net.sf.caltrop.cal.i2.util;

import net.sf.caltrop.cal.i2.Executor;
import net.sf.caltrop.cal.i2.Procedure;

abstract public class ProcedureOf1Eval implements Procedure {

	public void call(int n, Executor executor) {
		assert n == 1;
		
		p(executor.getValue(0), executor);
		executor.pop();
	}
	
	abstract public void  p(Object a, Executor executor);

}
