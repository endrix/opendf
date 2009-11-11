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

import java.util.List;

/**
 * Represents one phase in an actor's local action schedule
 */
public class SchedulingPhase {

	private List<DecisionTree> mLeaves;
	private boolean mIsDeterministic;
	private boolean mMayTerminate;
	private boolean mAlwaysTerminates;
	private PortSignature mMode;
	
	public SchedulingPhase(List<DecisionTree> leaves, boolean isDeterministic) {
		assert(leaves.isEmpty()==false);
		mLeaves=leaves;
		mIsDeterministic=isDeterministic;
		mMayTerminate=false;
		mAlwaysTerminates=true;
		
		// Check if we have a common "mode" (all port rates the same)
		// and initialize the next-phase map
		if (isDeterministic)
			mMode=leaves.get(0).getMode();
		for (DecisionTree leaf: leaves) {
			PortSignature m=leaf.getMode();
			if (mMode!=null && (m==null || mMode.equals(m)==false)) {
				mMode=null;
			}
			
			if (leaf instanceof NullNode) {
				mMayTerminate=true;
				mMode=null;
			}
			else
				mAlwaysTerminates=false;
		}
	}
		
	public boolean mayTerminate() {
		return mMayTerminate;
	}
	
	public boolean alwaysTerminates() {
		return mAlwaysTerminates;
	}
	
	public boolean isDeterministic() {
		return mIsDeterministic;
	}
	
	public PortSignature hasStaticPortSignature() {
		return mMode;
	}
	
	public List<DecisionTree> getLeaves() {
		return mLeaves;
	}	
}
