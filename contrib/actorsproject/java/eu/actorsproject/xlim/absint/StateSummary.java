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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.dependence.StateLocation;

/**
 * Summarizes the abstract state at a certain program point,
 * such as at a ModeNode (the root of a per-mode action scheduler)
 */
public class StateSummary<T extends AbstractValue<T>> implements XmlElement {

	private String mName;
	private HashMap<StateLocation,T> mValueMap=new HashMap<StateLocation,T>();
	
	/**
	 * Creates an empty, unnamed StateSummary
	 */
	public StateSummary() {
	}
		
	/**
	 * Creates an empty, named StateSummary
	 * @param name
	 */
	public StateSummary(String name) {
		mName=name;
	}
	
	/**
	 * @param carrier a state variable or an actor port
	 * @return the abstract value that is associated with 'carrier'
	 */
	public T get(StateLocation carrier) {
		return mValueMap.get(carrier);
	}
	
	/**
	 * @param carrier a state variable or an actor port
	 * @return true iff 'carrier' is associate with a value (possibly null)
	 */
	public boolean hasValue(StateLocation carrier) {
		return mValueMap.containsKey(carrier);
	}

	/**
	 * Adds a new value for 'carrier'. If there already is a value
	 * associated with 'carrier' the two values are "joined"
	 * 
	 * @param carrier  a state variable or an actor port
	 * @param newValue new value (null represents "top")
	 * @return true iff state summary was updated
	 */
	public boolean add(StateLocation carrier, T newValue) {
		T oldValue=mValueMap.get(carrier);
		if (oldValue!=null) {
			// carrier has a (non-null) value already:
			// "join" with new value
			if (newValue!=null)
				newValue=newValue.union(oldValue).getAbstractValue();
			if (newValue!=null && newValue.equals(oldValue))
				return false; // no change
			// else: a new value (possibly null/"top")
		}
		else if (mValueMap.containsKey(carrier))
			return false; // mValueMap already contains "top" element (null)
		// else: there was no old value
		
		// update map
		mValueMap.put(carrier, newValue);
		return true;
	}
	
	/*
	 * Implementation of XmlElement
	 */
	
	@Override
	public String getTagName() {
		return "stateSummary";
	}
	
	@Override
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		return (mName!=null)? "name=\"" + mName +"\"" : "";
	}

	@Override
	public Iterable<XmlElement> getChildren() {
		return new Iterable<XmlElement>() {
			public Iterator<XmlElement> iterator() {
				return new EntryIterator(mValueMap.entrySet().iterator());
			}
		};
	}
	
	private class EntryIterator implements Iterator<XmlElement> {

		Iterator<Map.Entry<StateLocation, T>> mIterator;
		
		EntryIterator(Iterator<Map.Entry<StateLocation, T>> p) {
			mIterator=p;
		}
		
		public boolean hasNext() {
			return mIterator.hasNext();
		}

		public XmlElement next() {
			return new XmlAdaptor(mIterator.next());
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	private class XmlAdaptor implements XmlElement {
		Map.Entry<StateLocation, T> mEntry;
		
		XmlAdaptor(Map.Entry<StateLocation, T> entry) {
			mEntry=entry;
		}
		
		@Override
		public String getTagName() {
			return "entry";
		}
		
		@Override
		public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
			StateLocation carrier=mEntry.getKey();
			String name=carrier.getDebugName();

			String attributes=(name!=null)?
				attributes=" sourceName=\""+name+"\"" : "";
			XlimStateVar stateVar=carrier.asStateVar();
			if (stateVar!=null) {
				attributes=formatter.addAttributeDefinition(attributes, "source", stateVar, stateVar.getUniqueId());
			}
			return attributes;
		}

		@Override
		public Iterable<? extends XmlElement> getChildren() {
			XmlElement aValue=mEntry.getValue();
			if (aValue==null)
				aValue=new TopElement();
			return Collections.singleton(aValue);
		}
	}
	
	private class TopElement implements XmlElement {
		@Override
		public String getTagName() {
			return "top";
		}
		
		@Override
		public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
			return "";
		}

		@Override
		public Iterable<? extends XmlElement> getChildren() {
			return Collections.emptySet();
		}
	}
}
