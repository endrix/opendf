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

import java.util.Map;

import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.XlimOperation;

/**
 * A DependenceSlice, which is limited to an XlimModule
 */
public class ModuleDependenceSlice extends DependenceSlice {

	private XlimModule mBoundingModule;
	
	private static LiteralValuePredicate sLiteralValuePredicate=
		new LiteralValuePredicate();

	/**
	 * @param name            name of the slice (for printout)
	 * @param calleeSlices    mapping from call node to dependence slice:
	 *                        an entry for each call node, which might be
	 *                        encountered in the slice, is required.
	 * @param boundingModule  module, to which the slice is limited:
	 *                        values computed outside the module are 
	 *                        considered inputs to the slice
	 */
	public ModuleDependenceSlice(String name,
			                     Map<CallNode, DependenceSlice> calleeSlices,
			                     XlimModule boundingModule) {
		super(name, calleeSlices);
		mBoundingModule=boundingModule;
	}

	@Override
	protected boolean includeInSlice(ValueOperator def) {
		if (def==null)
			return false; // initial value (not in slice)
		else {
			// Definitions outside the bounding modules are not in slice,
			// but we make a special exception for constants (always in slice)...
			XlimModule defInModule=def.getParentModule();
			return (mBoundingModule.leastCommonAncestor(defInModule)==mBoundingModule
					|| isLiteral(def));
		}
	}
	
	private boolean isLiteral(ValueOperator def) {
		return def.accept(sLiteralValuePredicate,null);
	}
	
	/**
	 * Helper class, which detects literal-value operators
	 */
	static class LiteralValuePredicate implements ValueOperator.Visitor<Boolean, Object> {
		public Boolean visitOperation(XlimOperation xlimOp, Object dummyArg) {
			return xlimOp.getKind().equals("$literal_Integer");
		}

		public Boolean visitPhi(PhiOperator phi, Object dummyArg) {
			return false;
		}

		public Boolean visitTest(TestOperator test, Object dummyArg) {
			return false;
		}		
	}
}
