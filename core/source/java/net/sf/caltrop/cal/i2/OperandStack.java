package net.sf.caltrop.cal.i2;

public interface OperandStack {
	
	void  push(Object v);
	
	Object pop();
	
	void  pop(int n);
	
	void  replaceWithResult(int n, Object v);

	Object getValue(int n);
}

