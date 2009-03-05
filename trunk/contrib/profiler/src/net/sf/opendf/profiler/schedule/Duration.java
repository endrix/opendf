package net.sf.opendf.profiler.schedule;

/**
 * 
 * @author jornj
 */
public class Duration {
	
	public Duration add (Duration d) {
		return new Duration (this.value() + d.value());
	}
	
	public long value() {
		return duration;
	}
	
	public double doubleValue() {
		return (double)value();
	}
	
	public Duration (long d) {
		duration = d;
	}

	public int hashCode() {
		return (int)value();
	}
	
	public boolean equals(Object a) {
		if (a instanceof Time) {
			return value() == ((Time)a).value();
		} else {
			return false;
		}
	}
	
	public String toString() {
		return Long.toString(value());
	}

	
	private long duration;
	
	static final Duration ZERO = new Duration(0);
	static final Duration ONE = new Duration(1);
	static final Duration TICK = new Duration(1);

}
