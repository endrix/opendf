package net.sf.opendf.profiler.schedule;

/**
 * 
 * @author jornj
 */
public class Time implements Comparable{
	
	public Time add (Duration d) {
		return new Time(time + d.value());
	}
	
	public Duration durationUntil(Time t) {
		if (t.isBefore(this))
			throw new RuntimeException("Illegal argument: " + t);
		return new Duration(t.value() - value());
	}
	
	public boolean isBefore(Time t) {
		return this.value() < t.value();
	}
	
	public boolean equals(Time t) {
		return value() == t.value();
	}
	
	public int compareTo(Object t) {
		if (!(t instanceof Time))
			throw new RuntimeException("Cannot compare Time to this: " + t);
		Time tm = (Time)t;
		if (this.isBefore(tm))
			return -1;
		if (tm.isBefore(this))
			return 1;
		return 0;
	}
	
	
	public long value() {
		return time;
	}

	public Time(long tm) {
		time = tm;
	}
	
	private long  time;
	
	public int hashCode() {
		return (int)time;
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
	
	static final Time ZERO = new Time(0);


}
