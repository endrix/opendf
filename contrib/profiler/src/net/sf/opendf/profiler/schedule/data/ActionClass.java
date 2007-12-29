package net.sf.opendf.profiler.schedule.data;

/**
 * 
 * @author jornj
 */

public class ActionClass {
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
}

