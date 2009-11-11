package eu.actorsproject.xlim.absint;

import java.util.HashSet;

import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueOperator;

/**
 * A DemandDrivenContext is a Context, whose abstract values are evaluated on demand.
 * 
 * This implementation must only be used for non-circular dependences,
 * since it doesn't handle dependence cycles.
 */
public class DemandContext<T> extends Context<T> {

	protected AbstractDomain<T> mDomain;
		
	private HashSet<ValueOperator> mOnEvaluationStack=
		new HashSet<ValueOperator>();
		
	public DemandContext(AbstractDomain<T> domain) {
		mDomain=domain;
	}
			
	public DemandContext<T> createSubContext() {
		return new DemandSubContext<T>(this);
	}
	
	@Override
	public T get(ValueNode node) {
		DemandContext<T> context=evaluatedInContext(node);
		if (context!=null) {
			return context.mValueMap.get(node);
		}
		else {
			// Evaluate it
			ValueOperator def=node.getDefinition();
			if (def==null)
				return null; // Can't evaluate inputs
			
			evaluate(def);
			return mValueMap.get(node);
		}
	}
	
	@Override
	public boolean hasValue(ValueNode node) {
		// is there is a cached value or a definition to evaluate?
		return node.getDefinition()!=null 
		       || hasCachedValue(node);
	}	

	public boolean hasCachedValue(ValueNode node) {
		return evaluatedInContext(node)!=null;
	}
	
	public void evaluate(ValueOperator def) {
		// Check for cyclic dependences
		assert(mOnEvaluationStack.contains(def)==false);
		mOnEvaluationStack.add(def);
		def.evaluate(this, mDomain);
		mOnEvaluationStack.remove(def);
	}
	
	protected DemandContext<T> evaluatedInContext(ValueNode node) {
		if (mValueMap.containsKey(node))
			return this;
		else
			return null;
	}
}

class DemandSubContext<T> extends DemandContext<T> {
	
	private DemandContext<T> mParent;
	
	DemandSubContext(DemandContext<T> parent) {
		super(parent.mDomain);
		mParent=parent;
	}
	
	@Override
	protected DemandContext<T> evaluatedInContext(ValueNode node) {
		if (mValueMap.containsKey(node))
			return this;
		else
			return mParent.evaluatedInContext(node);
	}
}