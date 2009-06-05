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

package eu.actorsproject.xlim.decision;

import java.util.ArrayList;
import java.util.Iterator;

import eu.actorsproject.xlim.XlimTopLevelPort;

/**
 * Represents a blocking condition: pinAvail(port)>=N1 or ... or pinAvail(port_k)>=N_k,
 * which is used as the attribute of the "yield" operation.
 * 
 * The condition is necessary (but not always sufficient) for further
 * action firings: it is thus not useful to execute the action scheduler
 * until the condition is satisfied.
 * The special case of an empty blocking condition (k=0) signifies actor
 * termination -it should be interpreted as no amount of available tokens
 * will enable action firing.
 */
public class YieldAttribute implements Iterable<BlockingCondition> {
	private ArrayList<BlockingCondition> mChildren=
		new ArrayList<BlockingCondition>();

	@Override
	public Iterator<BlockingCondition> iterator() {
		return mChildren.iterator();
	}
	
	public void add(BlockingCondition cond) {
		// Insert blocking conditions ordered (to facilitate hashCode/equals)
		int N=mChildren.size();
		int i=0;
		while (i<N && cond.compareTo(mChildren.get(i))>0)
			++i;
		
		if (i<N) {
			// We don't want multiple blocking conditions on the same port
			assert(cond.getPort()!=mChildren.get(i).getPort());
			
			// Insert new blocking condition at 'i'
			while (i<N) {
				BlockingCondition temp=mChildren.get(i);
				mChildren.set(i, cond);
				cond=temp;
				++i;
			}
		}
		
		mChildren.add(cond);
	}
	
	public int size() {
		return mChildren.size();
	}
	
	public boolean willNeverUnblock() {
		return mChildren.isEmpty();
	}
	
	@Override
	public boolean equals(Object o) {
		return (o instanceof YieldAttribute 
				&& mChildren.equals(((YieldAttribute) o).mChildren));
	}
	
	@Override
	public int hashCode() {
		return mChildren.hashCode();
	}
}
