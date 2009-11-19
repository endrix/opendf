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

package eu.actorsproject.xlim.dependence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


/**
 * FixupContext keeps track of current values of Locations (state variables,
 * actor ports and local aggregates) when building or patching the data dependence 
 * graph.
 * It also keeps track of exposed uses, which must be resolved by associating
 * them with a proper definition (ValueNode).
 * Further it provides methods to process a patch (finding exposed uses and
 * current values at the end of the patch), resolve exposed uses and propagate
 * new values to code that follows the patch. 
 */
public class FixupContext {

	private HashMap<Location,ArrayList<ValueUsage>> mExposedUses;
	private HashMap<Location,ValueNode> mCurrValues;
	private HashSet<Location> mNewValues;
	
	public FixupContext() {
		mExposedUses=new HashMap<Location,ArrayList<ValueUsage>>();
		mCurrValues=new HashMap<Location,ValueNode>();
		mNewValues=new HashSet<Location>();
	}
		
	private FixupContext(FixupContext c) {
		mCurrValues=new HashMap<Location,ValueNode>(c.mCurrValues);
	}
	
	/**
	 * @return A "sub-context" of this context for the purposes of fixing up code
	 * The sub-context shares the set of exposed uses with its parent, it has a copy of
	 * the current values and an initially empty set of new values.
	 */
	public FixupContext createFixupSubContext() {
		FixupContext result=new FixupContext(this);
		result.mExposedUses=mExposedUses;
		result.mNewValues=new HashSet<Location>();
		return result;
	}
	
	/**
	 * @return A "sub-context" of this context for the purposes of propagating new values
	 * The sub-context lacks the set of exposed uses, it has a copy of the current and new values.
	 */
	public FixupContext createPropagationSubContext() {
		FixupContext result=new FixupContext(this);
		result.mExposedUses=null;
		result.mNewValues=new HashSet<Location>(mNewValues);
		return result;		
	}
	
	/**
	 * @param location 
	 * @return the "current" value of location
	 */
	public ValueNode getValue(Location location) {
		return mCurrValues.get(location);
	}
	
	/**
	 * @return the set of stateful resources with new values (needs to be propagated)
	 * 
	 * Whereas it is OK to iterate over the new values for the purposes of propagating values,
	 * we mustn't iterate and do endPropagation(), since this would modify the set we are iterating over.
	 */
	public Set<Location> getNewValues() {
		return mNewValues;
	}

	/**
	 * @return a stateful resource with exposed use (need to be resolved)
	 * 
	 * Note that we mustn't iterate over the exposed uses when resolving them (map is modified).
	 */
	public Set<Location> getExposedUses() {
		return mExposedUses.keySet();
	}
	
	
	/**
	 * @param newValue
	 * Sets a new "current" value of stateful resource carrier. The value is marked
	 * as new, which means that it will have to be propagated out of the patch.
	 */
	public void setNewValue(ValueNode newValue) {
		Location location=newValue.getLocation();
		if (location!=null) {
			mCurrValues.put(location, newValue);
			mNewValues.add(location);
		}
		// else: this is an output-port (no state carrier)
	}
	
	/**
	 * @param newValues
	 * Sets a new "current" values of a collection of stateful resource carrier. 
	 * The values are marked as new, which means that they will have to be propagated 
	 * out of the patch.
	 */
	public void setNewValues(Iterable<? extends ValueNode> newValues) {
		for (ValueNode v: newValues)
			setNewValue(v);
	}

	private ArrayList<ValueUsage> getExposedUses(Location location) {
		assert(location!=null);
		ArrayList<ValueUsage> l=mExposedUses.get(location);
		if (l==null) {
			// This is the first use, create the array
			l=new ArrayList<ValueUsage>();
			mExposedUses.put(location, l);
		}
		return l;
	}
		
	/**
	 * @param use
	 * Connects use to a definition (ValueNode), if there is one
	 * Otherwise it is put in the collection of upwards exposed usages
	 */
	public void fixup(ValueUsage use) {
		if (use.needsFixup()) {
		    Location location=use.getFixupLocation();
		    assert(location!=null);
			ValueNode currValue=getValue(location);
			if (currValue!=null) {
				// Fix-up value usage
				use.setValue(currValue);
			}
			else {
				// Add an exposed usage
				ArrayList<ValueUsage> l=getExposedUses(location);
				l.add(use);
			}
		}
		// else: this is an output-port use (no location)
	}
	
	/**
	 * @param usages collection of ValueUsages
	 * Connects a collection of usages to their definitions (ValueNode), 
	 * or puts them in the collection of upwards exposed usages if there is none
	 */
	public void fixup(Iterable<? extends ValueUsage> usages) {
		for (ValueUsage use: usages)
			fixup(use);
	}
	
	/**
	 * @param contextWithExposedUses
	 * Fixes up exposed usages of 'contextWithExposedUses' using this context
	 * (or puts them in the collection of exposed usages in this context)
	 */
	public void fixup(FixupContext contextWithExposedUses) {
		for (ArrayList<ValueUsage> usages: contextWithExposedUses.mExposedUses.values()) {
			fixup(usages);
		}
	}
	
	/**
	 * Attempts to resolve exposed usages using a definition (ValueNode)
	 * @param def a definition, which dominates the patched code
	 * @return true if an exposed use was resolved
	 */
	public boolean resolveExposedUses(ValueNode def) {
		Location location=def.getLocation();
		if (location!=null) {
			ArrayList<ValueUsage> expUses=mExposedUses.remove(location);
			if (expUses!=null) {
				resolve(expUses,def);
				return true;
			}
		}
		return false;
	}

	private void resolve(Iterable<? extends ValueUsage> expUses, ValueNode def) {
		for (ValueUsage use: expUses)
			use.setValue(def);
	}
	
	/**
	 * Attempts to resolve exposed usages using a collection of definitions
	 * @param definitions definitions, which dominates the patched code
	 */

	public void resolveExposedUses(Iterable<? extends ValueNode> definitions) {
		for (ValueNode def: definitions)
			resolveExposedUses(def);
	}
	
	/**
	 * Attempts to resolve exposed usages using another use (which refers
	 * to a definition/ValueNode)
	 * @param use  a use, which dominates the patched code
	 */
	public void resolveExposedUsesViaUse(ValueUsage use) {
		if (use.needsFixup()) {
			Location location=use.getFixupLocation();
			assert(location!=null);
			ArrayList<ValueUsage> expUses=mExposedUses.remove(location);
			if (expUses!=null) {
				resolve(expUses,use.getValue());
			}
		}
	}
	
	public void resolveExposedUsesViaUse(Iterable<? extends ValueUsage> usages) {
		for (ValueUsage use: usages)
			resolveExposedUsesViaUse(use);
	}
	
	/**
	 * @return true if there are remaining unresolved exposed uses
	 */
	public boolean remainingExposedUses() {
		return mExposedUses.isEmpty()==false;
	}
	
	
	/**
	 * If necessary, propagates a new value to a use that is dominated 
	 * by the patch
	 * @param use
	 */
	public void propagateNewValue(ValueUsage use) {
		
		if (use.needsFixup()) {
			Location location=use.getFixupLocation();
			assert(location!=null);
			if (mNewValues.contains(location)) {
				ValueNode currValue=getValue(location);
				assert(currValue!=null);
				use.setValue(currValue);
			}
		}
	}
	
	/**
	 * If necessary, propagates new values to uses that are dominated 
	 * by the patch
	 * @param use
	 */
	public void propagateNewValues(Iterable<? extends ValueUsage> usages) {
		for (ValueUsage use: usages)
			propagateNewValue(use);
	}
	
	/**
	 * Ends propagation of a new value if a definition of the same
	 * stateful resource was found
	 * @param def
	 * @return true if the propagation of a new value ended
	 */
	public boolean endPropagation(ValueNode def) {
		Location location=def.getLocation();
		if (location!=null)
			return mNewValues.remove(location);
		else
			return false;
	}
	
	/**
	 * Ends propagation of new values, for which definitions of the same
	 * stateful resource were found
	 * @param definitions
	 */
	public void endPropagation(Iterable<? extends ValueNode> definitions) {
		for (ValueNode def: definitions)
			endPropagation(def);
	}
	
	/**
	 * @return true if there are remaining new values to propagate
	 */
	public boolean remainingNewValues() {
		return mNewValues.isEmpty()==false;
	}
}
