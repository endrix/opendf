/* 
 * Copyright (c) Ericsson AB, 2010
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

package eu.actorsproject.xlim.schedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import eu.actorsproject.util.ConcatenatedIterable;
import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.util.XmlElement;

/**
 * Represents a Static action schedule. It consists of at least one of
 * the following two components: 
 * a) an initial schedule, which is execute once,
 * b) (followed by) a schedule that is repeated indefinitely.
 */
public class StaticActionSchedule {

	private StaticSubSchedule mInitialSchedule;
	private StaticSubSchedule mRepeatedSchedule;
	
	/**
	 * @param initialSchedule   the initial schedule (or null if none)
	 * @param repeatedSchedule  the repeated schedule (or null if none)
	 * 
	 * One, but not both, of these components can be left out (null)
	 */
	public StaticActionSchedule(StaticSubSchedule initialSchedule, 
			                    StaticSubSchedule repeatedSchedule) {
		assert(initialSchedule!=null || repeatedSchedule!=null);
		mInitialSchedule = initialSchedule;
		mRepeatedSchedule = repeatedSchedule;
	}
		
	/**
	 * @return true iff the schedule has an initial (finite) sequence
	 *         (possibly followed by a sequence that is repeated indefinitely)
	 */
	public boolean hasInitialSchedule() {
		return mInitialSchedule!=null;
	}

	/**
	 * @return the initial sequence of the schedule (or null, if there is no such sequence)
	 */
	public StaticSubSchedule getInitialSchedule() {
		return mInitialSchedule;
	}

	/**
	 * @return true iff the schedule repeats indefinitely
	 */
	public boolean repeatsForever() {
		return mRepeatedSchedule!=null;
	}

	/**
	 * @return the part of the schedule that is repeated indefinitely
	 */
	public StaticSubSchedule getRepeatedSchedule() {
		return mRepeatedSchedule;
	}

	/**
	 * @return iteration over the phases of the schedule
	 *         (a possible initial schedule, possibly followed 
	 *          by an indefinitely repeated schedule).
	 */
	public Iterable<StaticPhase> getPhases() {
		if (repeatsForever()) {
			RepeatedIterable repeatedIterable=new RepeatedIterable();

			if (hasInitialSchedule()) {
				return new ConcatenatedIterable<StaticPhase>(mInitialSchedule.getPhases(),
						                                     repeatedIterable);
			}
			else
				return repeatedIterable;
		}
		else
			return mInitialSchedule.getPhases();
	}

	/**
	 * @param idRenaming mapping from "old" to "new" XLIM identifiers
	 * @return PhasePrinter of the StaticSchedule
	 * 
	 * The set of XLIM identifiers that may be renamed are references to
	 * a) The ports of the actor (port names)
	 * b) The state variables ("source"/"target" references)
	 * c) Actions/TaskModules ("target" names)
	 * d) Type definitions ("typeName")
	 * The name of OutputPorts of operations are also renamed so that they
	 * associate each use with a unique definition that dominates the use.
	 */
	public PhasePrinter getPhasePrinter(Map<String,String> idRenaming) {
		return new PhasePrinter(getPhases());
	}
	
	
	/**
	 * @return the XML representation of the StaticActionSchedule
	 */
	public XmlElement asXmlElement() {
		if (mRepeatedSchedule!=null) {
			ForeverElement foreverElement=new ForeverElement();
			if (mInitialSchedule!=null)
				return new SequenceElement(foreverElement);
			else
				return foreverElement;
		}
		else
			return mInitialSchedule;
	}
	
	private class RepeatedIterable implements Iterable<StaticPhase> {

		public Iterator<StaticPhase> iterator() {
			return new RepeatedIterator();
		}
	}
	
	private class RepeatedIterator implements Iterator<StaticPhase> {
		
		private Iterator<StaticPhase> mPhase;
		
		RepeatedIterator() {
			mPhase=mRepeatedSchedule.getPhases().iterator();
		}

		@Override
		public boolean hasNext() {
			return true;
		}

		@Override
		public StaticPhase next() {
			// Restart from beginning at end of period
			if (mPhase.hasNext()==false)
				mPhase=mRepeatedSchedule.getPhases().iterator();
			return mPhase.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	private class ForeverElement implements XmlElement {

		private Iterable<? extends XmlElement> mChildren;
		
		ForeverElement() {
			if (mRepeatedSchedule.getNumberOfSubSchedules()==0)
				mChildren=Collections.singleton(mRepeatedSchedule);
			else
				mChildren=mRepeatedSchedule.getSubSchedules();
		}
		
		public String getTagName() {
			return "forever";
		}
		
		public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
			return "";
		}

		public Iterable<? extends XmlElement> getChildren() {
			return mChildren;
		}		
	}
	
	private class SequenceElement implements XmlElement {
		private ArrayList<XmlElement> mChildren;
		
		SequenceElement(ForeverElement foreverElement) {
			mChildren=new ArrayList<XmlElement>();
			if (mInitialSchedule!=null)
				mChildren.add(mInitialSchedule);
			if (foreverElement!=null)
				mChildren.add(foreverElement);
		}
		
		public String getTagName() {
			return "sequence";
		}
		
		public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
			return "repeats=\"1\"";
		}

		public Iterable<XmlElement> getChildren() {
			return mChildren;
		}		
	}
}
