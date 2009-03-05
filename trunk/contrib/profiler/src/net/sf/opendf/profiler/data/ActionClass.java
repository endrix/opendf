package net.sf.opendf.profiler.data;

/**
 * 
 * @author jornj
 */

public class ActionClass implements Comparable<ActionClass> {
	public String		actorClassName;
	public int          action;
	
	public ActionClass(String actorClassName, int action) {
		this.actorClassName = actorClassName;
		this.action = action;
	}
	
	public int hashCode() {
		return actorClassName.hashCode() * action;
	}
	
	public boolean equals(Object a) {
		if (! (a instanceof ActionClass))
			return false;
		ActionClass aa = (ActionClass) a;
		return action == aa.action && actorClassName.equals(aa.actorClassName);
	}
	
	public String  toString() {
		return "(" + actorClassName + ":" + action + ")";
	}


	public int compareTo(ActionClass o) {
		int n = actorClassName.compareTo(o.actorClassName);
		if (n == 0)
			return action - o.action;
		else
			return n;
	}

}

