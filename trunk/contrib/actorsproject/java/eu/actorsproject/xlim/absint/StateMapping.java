/* 
 * Copyright (c) Ericsson AB, 2009
 * Author: Carl von Platen (carl.von.platen@ericsson.com)
 * All rights reserved.
 *
 * License terms:
 *
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met:
 *     * Redistributions of source code must retain the above 
 *       copyright notice, this list of conditions and the 
 *       following disclaimer.
 *     * Redistributions in binary form must reproduce the 
 *       above copyright notice, this list of conditions and 
 *       the following disclaimer in the documentation and/or 
 *       other materials provided with the distribution.
 *     * Neither the name of the copyright holder nor the names 
 *       of its contributors may be used to endorse or promote 
 *       products derived from this software without specific 
 *       prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package eu.actorsproject.xlim.absint;


import java.util.Map;

import eu.actorsproject.xlim.dependence.Location;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.StateLocation;

/**
 * Represents a mapping from state variables/actor ports to value nodes
 * and provides conversion between Context and StateSummary.
 * 
 * The mapping is kept as a collection of value nodes with associated 
 * state carriers.
 */
public class StateMapping {

	private Iterable<ValueNode> mValueNodes;
	
	public StateMapping(Iterable<ValueNode> valueNodes) {
		assert(haveStateCarriers(valueNodes));
		mValueNodes=valueNodes;
	}
	
	/**
	 * @return value nodes (from which state carriers can be retrieved)
	 */
	public Iterable<ValueNode> getValueNodes() {
		return mValueNodes;
	}
	 
	/**
	 * Converts a StateSummary (mapping from state carriers to abstract values)
	 * to a Context (mapping from value nodes to abstract values).
	 * 
	 * @param stateSummary
	 * @return corresponding Context (for this StateMapping)
	 */
	public<T extends AbstractValue<T>> Context<T> createContext(StateSummary<T> summary) {
		Context<T> context=new Context<T>();
		updateContext(context,summary);
		return context;
	}
		
	/**
	 * Updates a context using a state summary
	 * 
	 * @param context  a mapping from value nodes to abstract values
	 * @param summary  a mapping from state carriers to abstract values
	 * @return true iff the context was modified
	 * 
	 * If the summary is incomplete (doesn't include all the state carriers
	 * of this mapping), the null "top" element is used for values that are 
	 * not already present in 'context'. In this way, the updated context is
	 * complete (which is required for abstract interpretation), but we don't
	 * remove information. 
	 */
	public<T extends AbstractValue<T>> boolean updateContext(Context<T> context,
			                                                 StateSummary<T> summary) {
		boolean changed=false;
		
		// TODO: is this method used, apart from createContext?
		for (ValueNode valueNode: getValueNodes()) {
			StateLocation location=valueNode.getLocation().asStateLocation();
			assert(location!=null);
			
			T aValue=summary.get(location.asStateLocation());
			// Should we do the update for a null value?
			// Yes, if the null value means "top" (summary.hasValue)
			// Yes, if the context doesn't already contain a mapping for that value
			// (in the latter case we use "top" to represent missing values, but
			//  we don't want to overwrite values for which we have information)
			if (aValue!=null || summary.hasValue(location) || context.hasValue(valueNode)==false)
				if (context.put(valueNode, aValue))
					changed=true;
		}
		return changed;
	}
	
	/**
	 * Converts a Context to a StateSummary
	 * 
	 * @param context       a mapping from value nodes to abstract values
	 * @param includePorts  if true: include actor ports in summary
	 *                      if false: include state variables only
	 * @return corresponding StateSummary
	 */
	public<T extends AbstractValue<T>> StateSummary<T> createStateSummary(Context<T> context,
                                                                          boolean includePorts) {
		StateSummary<T> summary=new StateSummary<T>();
		updateSummary(summary,context,includePorts,null);
		return summary;
	}
		
	/**
	 * Updates a state summary using a context
	 * 
	 * @param summary      a mapping from state carriers to abstract values
	 * @param context      a mapping from value nodes to abstract values
	 * @param updatePorts  if true: include actor ports in summary
	 *                     if false: include state variables only
	 * @param wideningOps  optional widening operators (null if none)
	 * @return true iff state summary was modified
	 */
	public<T extends AbstractValue<T>> boolean updateSummary(StateSummary<T> summary,
			                                                 Context<T> context,
			                                                 boolean updatePorts,
			                                                 Map<ValueNode, ? extends WideningOperator<T> > wideningOps) {
		boolean changed=false;
		
		for (ValueNode valueNode: getValueNodes()) {
			StateLocation location=valueNode.getLocation().asStateLocation();
			assert(location!=null);
			
			// Possibly filter out ports from the summary
			if (updatePorts || location.asStateVar()!=null) {
				assert(context.hasValue(valueNode));
				T aValue=context.get(valueNode);
				WideningOperator<T> w=(wideningOps!=null)? wideningOps.get(valueNode) : null;
				
				if (summary.add(location, aValue, w))
					changed=true;
			}
		}
		
		return changed;
	}
	
	/**
	 * @param valueNodes
	 * @return true iff all valueNodes are side effects on StateLocations
	 *         (actor ports and state variables)
	 */
	private static boolean haveStateCarriers(Iterable<ValueNode> valueNodes) {
		for (ValueNode valueNode: valueNodes) {
			Location location=valueNode.getLocation();
			if (location==null || location.isStateLocation()==false)
				return false;
		}
		return true;
	}
}
