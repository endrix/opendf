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

import eu.actorsproject.util.XmlElement;

/**
 * Represents a static sub-schedule of action firings. 
 * The building blocks of the sub-schedule are
 * (a) StaticPhase,     actions with the same consumption/production rate
 *                      and the associated decisions to select among them.
 * (b) StaticSequence,  a sequence of SubSchedules that is repeated a
 *                      fixed number of times (including one)
 *                      
 * The StaticSubSequence can also be viewed as a (flattened) sequence
 * consisting of StaticPhases only. In this view each StaticSequence is
 * essentialy duplicated according to their repeat counts. 
 */
public interface StaticSubSchedule extends XmlElement {

	/**
	 * @return number of sub-schedules in this StaticSubSchedule
	 *         (0 in the case of a StaticPhase -it has no SubSchedules)
	 */
	int getNumberOfSubSchedules();

	/**
	 * @return the repeat count of this StaticSubSchedule
	 */
	int getRepeatCount();
	
	/**
	 * @return iteration over the sub-schedules of this StaticSubSchedule
	 *         (each sub-schedule appears once, regardless of the repeat count)
	 */
	Iterable<StaticSubSchedule> getSubSchedules();

	/**
	 * @return number of StaticPhases in the SubSchedule
	 *         (with duplication according to repeat counts)
	 */
	int getNumberOfPhases();
	
	/**
	 * @return iteration over the phases of the schedule
	 *         (phases may appear several times, according to repeat counts)
	 */
	Iterable<StaticPhase> getPhases(); 
}
