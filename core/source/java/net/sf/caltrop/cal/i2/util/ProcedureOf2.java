package net.sf.caltrop.cal.i2.util;

import net.sf.caltrop.cal.i2.Executor;
import net.sf.caltrop.cal.i2.Procedure;

abstract public class ProcedureOf2 implements Procedure {

	public void call(int n, Executor executor) {
		assert n == 2;
		
		p(executor.getValue(0), executor.getValue(1));
		executor.pop(n);
	}
	
	abstract public void  p(Object a, Object b);

}
