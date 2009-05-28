package net.sf.opendf.profiler.data;


/**
 * 
 * @author jornj
 */
public class Action implements Comparable<Action> {
	
	public ActionClass   actionClass;
	public int           actorID;
	
	public Action (int actorID, ActionClass actionClass) {
		this.actorID = actorID;
		this.actionClass = actionClass;
	}
	
	public Action (int actorID, String actorClassName, int actionID) {
		this(actorID, new ActionClass(actorClassName, actionID));
	}
	
	public Action (Step s) {
		this(s.getActorId(), s.getActorClassName(), s.getActionId());
	}
	
	public int hashCode() {
		return actorID * actionClass.hashCode();
	}
	
	public boolean equals(Object a) {
		if (! (a instanceof Action))
			return false;
		Action aa = (Action) a;
		return actorID == aa.actorID && actionClass.equals(aa.actionClass);
	}
	
	public String  toString() {
		return "[" + actionClass + " @ " + actorID + "]";
	}

	public int compareTo(Action o) {		
		int n = actorID - o.actorID;
		if (n == 0)
			return actionClass.compareTo(o.actionClass);
		else
			return n;
	}


}
