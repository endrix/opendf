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
package eu.actorsproject.xlim.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.actorsproject.xlim.XlimBlockElement;
import eu.actorsproject.xlim.XlimBlockModule;
import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimIfModule;
import eu.actorsproject.xlim.XlimLoopModule;
import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimPhiContainerModule;
import eu.actorsproject.xlim.XlimStateCarrier;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.dependence.StatePhiOperator;
import eu.actorsproject.xlim.dependence.ValueNode;

public class KilledStateProperty {

	protected Map<XlimContainerModule,Set<XlimStateCarrier>> mKilled;
	protected Map<XlimModule,Set<XlimStateCarrier>> mKilledInParent;
	protected ElementVisitor mElementVisitor;
	
	protected KilledStateProperty() {
		mKilled=new HashMap<XlimContainerModule,Set<XlimStateCarrier>>();
		mKilledInParent=new HashMap<XlimModule,Set<XlimStateCarrier>>();
		mElementVisitor=new ElementVisitor();
	}
	
	/**
	 * @param task a task module
	 * @return the killed-state property of 'task'
	 */
	public static KilledStateProperty analyze(XlimTaskModule task) {
		KilledStateProperty result=new KilledStateProperty();
		Set<XlimStateCarrier> empty=Collections.emptySet();
		result.mKilledInParent.put(task,empty);
		result.computeKilled(task);
		return result;
	}
	
	/**
	 * @param elem an XlimBlockElement (operation or module)
	 * @return Set of state carriers that are defined in 'elem'
	 */
	public Set<XlimStateCarrier> killed(XlimBlockElement elem) {
		return elem.accept(mElementVisitor, null);
	}
	
	/**
	 * @param m an XlimModule
	 * @return Set of state carriers that are defined on the path from
	 *         the entry of m's parent to the entry of m.
	 */
	public Set<XlimStateCarrier> killedInParent(XlimModule m) {
		return mKilledInParent.get(m);
	}
	
	private Set<XlimStateCarrier> computeKilled(XlimContainerModule m) {
		Set<XlimStateCarrier> killed=new HashSet<XlimStateCarrier>();
		for (XlimBlockElement elem: m.getChildren())
			killed.addAll(elem.accept(mElementVisitor, killed));
		mKilled.put(m, killed);
		return killed;
	}
	
	private class ElementVisitor 
		implements XlimBlockElement.Visitor<Set<XlimStateCarrier>,Set<XlimStateCarrier>> {

		@Override
		public Set<XlimStateCarrier> visitBlockModule(XlimBlockModule m, 
				                                      Set<XlimStateCarrier> killedInParent) {
			if (killedInParent!=null)
				mKilledInParent.put(m, new HashSet<XlimStateCarrier>(killedInParent));
				
			Set<XlimStateCarrier> result=mKilled.get(m);
			if (result==null)
				result=computeKilled(m);
			return result;
		}

		@Override
		public Set<XlimStateCarrier> visitIfModule(XlimIfModule m, 
                                                   Set<XlimStateCarrier> killedInParent) {
			if (killedInParent!=null) {
				mKilledInParent.put(m, new HashSet<XlimStateCarrier>(killedInParent));
				// No state carriers killed between entry of If to test/then/else
				Set<XlimStateCarrier> empty=Collections.emptySet();
				visitSubModule(m.getTestModule(), empty);
				visitSubModule(m.getThenModule(), empty);
				visitSubModule(m.getElseModule(), empty);
			}
			return visitPhiContainer(m);
		}

		@Override
		public Set<XlimStateCarrier> visitLoopModule(XlimLoopModule m, 
                                                   Set<XlimStateCarrier> killedInParent) {
			Set<XlimStateCarrier> result=visitPhiContainer(m);
			if (killedInParent!=null) {
				mKilledInParent.put(m, new HashSet<XlimStateCarrier>(killedInParent));
				// State with phi:s in the loop are killed from entry of loop
				// to the Test/Body modules
				visitSubModule(m.getTestModule(), result);
				visitSubModule(m.getBodyModule(), result);
			}
			return result;
		}

		private Set<XlimStateCarrier> visitPhiContainer(XlimPhiContainerModule m) {
			Set<XlimStateCarrier> result=new HashSet<XlimStateCarrier>();
			for (StatePhiOperator phi: m.getStatePhiOperators()) {
				XlimStateCarrier carrier=phi.getOutput().getStateCarrier();
				result.add(carrier);
			}
			return result;
		}
		
		private void visitSubModule(XlimContainerModule m, 
				                    Set<XlimStateCarrier> killedInParent) {
			mKilledInParent.put(m, killedInParent);
			computeKilled(m);
		}
		
		@Override
		public Set<XlimStateCarrier> visitOperation(XlimOperation op,
				                                    Set<XlimStateCarrier> killedInParent) {
			if (op.mayModifyState()) {
				Set<XlimStateCarrier> result=new HashSet<XlimStateCarrier>();
				for (ValueNode output: op.getValueOperator().getOutputValues()) {
					XlimStateCarrier carrier=output.getStateCarrier();
					if (carrier!=null)
						result.add(carrier);
				}
				return result;
			}
			else
				return Collections.emptySet();
		}
	}
}
