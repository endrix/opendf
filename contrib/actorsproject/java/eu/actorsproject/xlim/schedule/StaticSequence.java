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

import java.util.Iterator;
import java.util.List;

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.util.XmlElement;

/**
 * Represents a sequence of sub-schedules, which is repeated a fixed 
 * number of times (including one) or an infinte number of times
 */
public class StaticSequence implements StaticSubSchedule {

	private List<StaticSubSchedule> mChildren;
	private int mRepeats;
	private int mNumberOfPhases;
	
	public StaticSequence(List<StaticSubSchedule> children, int repeats) {
		assert(children.isEmpty()==false);
		assert(repeats>0);
		mChildren=children;
		mRepeats=repeats;
		
		for (StaticSubSchedule child: children) {
			mNumberOfPhases+=child.getNumberOfPhases();
		}			
		mNumberOfPhases*=repeats;
	}
		
	/*
	 * Implementation of StaticSubSchedule
	 */
	
	@Override
	public int getNumberOfPhases() {
		return mNumberOfPhases;
	}

	@Override
	public Iterable<StaticPhase> getPhases() {
		return new Iterable<StaticPhase>() {
			public Iterator<StaticPhase> iterator() {
				return new PhaseIterator();
			}		
		};
	}

	@Override
	public int getRepeatCount() {
		return mRepeats;
	}
	
	@Override
	public int getNumberOfSubSchedules() {
		return mChildren.size();
	}

	@Override
	public Iterable<StaticSubSchedule> getSubSchedules() {
		return mChildren;
	}


	/*
	 * Implementation of XmlElement
	 */
	
	@Override
	public String getTagName() {
		return "staticSequence";
	}
	
	@Override
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		return "repeats=\""+getRepeatCount()+"\"";
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return mChildren;
	}
	
	private class PhaseIterator implements Iterator<StaticPhase> {

		int mIteration;
		int mSubSchedule;
		Iterator<StaticPhase> mPhaseIterator=mChildren.get(0).getPhases().iterator();

		public boolean hasNext() {
			return mIteration<mRepeats; 
		}

		public StaticPhase next() {
			StaticPhase phase=mPhaseIterator.next();
			
			if (mPhaseIterator.hasNext()==false) {
				++mSubSchedule;
				if (mSubSchedule==mChildren.size()) {
					++mIteration;
					if (mIteration<mRepeats)
						mSubSchedule=0;
				}
				
				if (mSubSchedule<mChildren.size()) {
					mPhaseIterator=mChildren.get(mSubSchedule).getPhases().iterator();
				}
			}
			return phase;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
