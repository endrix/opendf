package net.sf.opendf.execution.harness;

public class Token {

	
	public double getTime() {
		return time;
	}

	public Object getValue() {
		return value;
	}

	public long getStep() {
		return step;
	}

	public Token(double time, Object value, long step) {
		this.time = time;
		this.value = value;
		this.step = step;
	}
	
	public Token(double time, Object value) {
		this (time, value, -1);
	}
	
	private double 	time;
	private Object 	value;
	private long 	step;

}
