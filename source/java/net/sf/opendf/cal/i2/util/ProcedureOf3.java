package net.sf.opendf.cal.i2.util;

import net.sf.opendf.cal.i2.Executor;
import net.sf.opendf.cal.i2.Procedure;

abstract public class ProcedureOf3 implements Procedure {

	public void call(int n, Executor executor) {
		assert n == 4;
		
		p(executor.getValue(0), executor.getValue(1), executor.getValue(2));
		executor.pop(n);
	}
	
	abstract public void  p(Object a, Object b, Object c);

}
