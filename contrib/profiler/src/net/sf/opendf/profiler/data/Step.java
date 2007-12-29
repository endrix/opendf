package net.sf.opendf.profiler.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 
 * @author jornj
 */

public class Step {
	
	public int  getID() {
		return id;
	}
	
	public String  getActorClassName() {
		return actorClassName;
	}
	
	public int  getActorId() {
		return actorId;
	}
	
	public int  getActionId() {
		return action;
	}
	
	public Action getAction() {
		return new Action(this);
	}
	
	public ActionClass  getActionClass() {
		return new ActionClass(actorClassName, action);
	}
	
	public String  getKind() { 
		return kind;
	}
	
	public String  getTag() {
		return tag;
	}
	
	public void  addPreStep(int stepID) {
		if (preset == null)
			preset = new TreeSet<Integer>();
		preset.add(stepID);
	}
	
	public void  addPreStep(Step s) {
		addPreStep(s.getID());
	}
	
	public void  removePreStep(int stepID) {
		if (preset == null || !preset.contains(stepID))
			throw new RuntimeException("Attempting to remove non-existing prestep.");
		preset.remove(stepID);
	}
	
	public void  removePreStep(Step s) {
		removePreStep(s.getID());
	}
	
	public void  addPostStep(Integer stepID) {
		if (postset == null)
			postset = new TreeSet<Integer>();
		postset.add(stepID);
	}
	
	public void  addPostStep(Step s) {
		addPostStep(s.getID());
	}
	
	public void  removePostStep(Integer stepID) {
		if (postset == null || !postset.contains(stepID))
			throw new RuntimeException("Attempting to remove non-existing poststep.");
		postset.remove(stepID);
	}
	
	public void  removePostStep(Step s) {
		removePostStep(s.getID());
	}
	
	public boolean dependsOn(Integer stepID) {
		return preset().contains(stepID);
	}
	
	public boolean dependsOn(Step s) {
		return dependsOn(s.getID());
	}
	
	public boolean precedes(Integer stepID) {
		return postset().contains(stepID);
	}
	
	public boolean precedes(Step s) {
		return precedes(s.getID());
	}
	
	public Set<Integer> preset() {
		return (preset == null) ? Collections.EMPTY_SET : preset;
	}
	
	public Set<Integer> postset() {
		return (postset == null) ? Collections.EMPTY_SET : postset;
	}
	
	public void setAttribute(Object k, Object v) {
		if (attributes == null) {
			attributes = new HashMap();
		}
		attributes.put(k, v);
	}
	
	public Object getAttribute(Object k) {
		if (attributes == null) {
			return null;
		} else {
			return attributes.get(k);
		}
	}
	
	public boolean hasAttribute(Object k) {
		if (attributes == null)
			return false;
		return attributes.containsKey(k);
	}
	
	public Map  attributes() {
		if (attributes == null) {
			return Collections.EMPTY_MAP;
		} else {
			return attributes;
		}
	}
	
	
	
	
	public  Step(int id, String actorClassName, int actorId, int action, String kind, String tag) {
		this (id, actorClassName, actorId, action, null, kind, tag);
	}
	
	public  Step(int id, String actorClassName, int actorId, int action, String kind, String tag, boolean reduceDependencies) {
		this (id, actorClassName, actorId, action, null, kind, tag);
	}
	
	public  Step(int id, String actorClassName, int actorId, int action, String specialKind, String kind, String tag) {
		this.id = id;
		this.actorClassName = actorClassName;
		this.actorId = actorId;
		this.action = action;
		this.special = (specialKind != null);
		this.specialKind = specialKind;
		this.kind = kind;
		this.tag = tag;
	}
	
	private int id;
	private String actorClassName;
	private int  actorId;
	private int  action;
	private boolean  special;
	private String   specialKind;
	private String kind;
	private String tag;

	private Set<Integer>   preset = null;
	private Set<Integer>   postset = null;
	
	private Map   attributes = null;
	
	
}

