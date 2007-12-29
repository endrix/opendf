package net.sf.opendf.profiler.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 
 * @author jornj
 */

public class Trace {
	
	public void addStep(Step s) {
		steps.put(s.getID(), s);
		if (rootId == null)
			rootId = s.getID();
	}
	
	public SortedMap<Integer, Step>  getSteps() {
		return steps;
	}
	
	public Set<Step> removeInitialStep(Step s) {
		if (!s.preset().isEmpty())
			throw new RuntimeException("Attempting to remove non-initial step.");
		
		Set<Step> newInitialSteps = new HashSet<Step>();
		for (Integer id : s.postset()) {
			Step b = getStep(id);
			b.removePreStep(s);
			if (b.preset().isEmpty())
				newInitialSteps.add(b);
		}
		
		steps.remove(s.getID());
		
		return newInitialSteps;
	}
	
	public Set initialSteps() {
		Set<Step> is = new HashSet<Step>();
		for (Integer sid : steps.keySet()) {
			Step s = steps.get(sid);
			if (s.preset().isEmpty())
				is.add(s);
		}
		return is;
	}
	
	public void addDependency(Step a, Step b) {
		
		a.addPostStep(b);
		b.addPreStep(a);
	}
	
	public void addDependency(Integer aID, Integer bID) {

		Step a = getStep(aID);
		Step b = getStep(bID);
		
		addDependency(a, b);
	}
	
	public int   length() {
		return steps.size();
	}
	
	public Step  getStep(Integer id) {
		Step s = (Step)steps.get(id);
		if (s == null)
			throw new RuntimeException("Could not find step #" + id + ".");
		return s;
	}
	
	public Step  getRootStep() {
		return getStep(rootId);
	}
	
	public Iterator<Integer> stepsIterator() {
		return steps.keySet().iterator();
	}
	
	public Trace() {
		this (true);
	}
	
	public Trace(boolean reduceDeps) {
		this.reduceDeps = reduceDeps;
		steps = new TreeMap<Integer, Step>();
		deps = new ArrayList<Dependency>();
	}
		

	private SortedMap<Integer, Step>  steps;
	private List<Dependency> deps;
	private Integer rootId = null;
	private boolean reduceDeps;
}
