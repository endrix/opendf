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

package eu.actorsproject.xlim.codegenerator;

import java.util.HashSet;
import java.util.Set;

import eu.actorsproject.xlim.XlimBlockElement;
import eu.actorsproject.xlim.XlimBlockModule;
import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimIfModule;
import eu.actorsproject.xlim.XlimInputPort;
import eu.actorsproject.xlim.XlimInstruction;
import eu.actorsproject.xlim.XlimLoopModule;
import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimPhiContainerModule;
import eu.actorsproject.xlim.XlimPhiNode;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTestModule;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueOperator;
import eu.actorsproject.xlim.util.XlimTraversal;

/**
 * Implements the storage allocation pass, which allocates temporaries for output ports
 */

public class LocalStorageAssignment implements XlimBlockElement.Visitor<Object,Object> {

	protected static boolean trace=false;
	
	protected LocalSymbolTable mLocalSymbols;
	protected int mNextEvent;
	protected LoopSourceUsage mLoopSourceUsage;
    protected LiveRangeCoalescing mCoalescing;
    
	public LocalStorageAssignment(LocalSymbolTable localSymbols) {
		mLocalSymbols = localSymbols;
		mCoalescing = new LiveRangeCoalescing(localSymbols);
	}

	public void coalesceLiveRanges(XlimTaskModule task) {
		mNextEvent = 0;
		findLiveRanges(task);
		
		if (trace) {
			// Debug print-out
			new LiveRangePrintOut().traverse(task, null);
		}
		
		mCoalescing.processCandidates();
	}
	
	protected void findLiveRanges(XlimContainerModule m) {
		for (XlimBlockElement child: m.getChildren())
			child.accept(this, null);
	}
	
	@Override
	public Object visitBlockModule(XlimBlockModule m, Object dummy) {
		findLiveRanges(m);
		return null;
	}
	
	protected void visitTestModule(XlimTestModule m) {
		findLiveRanges(m);
		registerUse(m.getDecision());
	}
	
	@Override
	public Object visitIfModule(XlimIfModule m, Object dummy)  {
		// First the test-module
		visitTestModule(m.getTestModule());
		
		// then-module and its path to the phi-nodes
		findLiveRanges(m.getThenModule());
		phiNodeUsage(m,0);
		
		// else-module and its path to the phi-nodes
		findLiveRanges(m.getElseModule());
		phiNodeUsage(m,1);
		
		// define the phi-nodes at the end
		phiNodeDef(m);
		
		return null;
	}
	
	@Override
	public Object visitLoopModule(XlimLoopModule m, Object dummy)  {
		// Create a new container for the source usage of the innermost loop
		LoopSourceUsage oldLoopUsage=mLoopSourceUsage;
		mLoopSourceUsage=new LoopSourceUsage(m);

		// Use initial values of phi-nodes and define phi-nodes
		phiNodeUsage(m, 0);
		phiNodeDef(m);
		
		// The test-module
		visitTestModule(m.getTestModule());
		
		// The body-module and its path to the phi-nodes
		findLiveRanges(m.getBodyModule());
		phiNodeUsage(m, 1);	
		
		// Extend the live range of sources that go into the loop (and around-and-around)
		// to (at least) the end of the loop
		for (XlimSource source: mLoopSourceUsage.getInputs()) {
			registerUse(source);
			// source is possibly also an input to the next outer loop...
			if (oldLoopUsage!=null)
				oldLoopUsage.add(source);
		}
		
		// Restore old LoopSourceUsage
		mLoopSourceUsage=oldLoopUsage;
		return null;
	}

	private void phiNodeUsage(XlimPhiContainerModule m, int path) {
    	for (XlimInstruction phi: m.getPhiNodes()) {
    		registerUse(phi.getInputPort(path).getSource());
    	}
    }
	
    private void phiNodeDef(XlimPhiContainerModule m) {
    	for (XlimInstruction phi: m.getPhiNodes()) {
    		registerDef(phi.getOutputPort(0));
    	}
    	
    	// phi-nodes are assumed to be in parallel: thus one "event"
		mNextEvent++;
    }
    
	@Override
	public Object visitOperation(XlimOperation op, Object dummy) {
		boolean newLiveRange=false;
		
		// First register uses of the input ports
		for (XlimInputPort port: op.getInputPorts())
			registerUse(port.getSource());
		
		// Register uses of "locations" (if any)
		if (op.dependsOnLocation()) {
			for (ValueNode input: op.getValueOperator().getInputValues())
				if (input.hasLocation()) {
					XlimSource source=input.getLocation().getSource();
					if (source!=null)
						registerUse(source);
					// else: locations w/o sources are actor ports
				}
		}
		
		// Register definition of output ports
		for (XlimOutputPort port: op.getOutputPorts())
			if (registerDef(port))
				newLiveRange=true;
			
		// Register any "mutations" of locations
		if (op.modifiesLocation()) {
			ValueOperator valueOp=op.getValueOperator();
			for (ValueNode output: valueOp.getOutputValues()) {
				if (output.hasLocation()) {
					XlimSource source=output.getLocation().getSource();
					if (source!=null) {
						// Rule out output ports that are *defined* by this operation
						// -rather than being "mutated"
						XlimOutputPort port=source.asOutputPort();
						if (port==null || port.getParent()!=op)
							registerMutation(source);
					}
					// else: locations w/o sources are actor ports
				}
				// else: outputs w/o location are (scalar) output ports (already taken care of)
			}
			newLiveRange=true;
		}
			
		// Advance the event counter at each statement/definition of live range
		if (newLiveRange) {
			mCoalescing.addCandidate(op);
			mNextEvent++;
		}
		
		return null;
	}
	
	/**
	 * @param output  an output port
	 * @return true iff the output port has an associated temporary variable,
	 *         in which case a new live range was created with mNextEvent as
	 *         starting point (false if no live range was created).
	 */
	protected boolean registerDef(XlimOutputPort output) {
		TemporaryVariable temp=mLocalSymbols.getTemporaryVariable(output);
		if (temp!=null) {
			temp.createLiveRange(mNextEvent);
			return true;
		}
		else
			return false;
	}
	
	protected void registerUse(XlimSource source) {
		XlimOutputPort port=source.asOutputPort();
		
		if (port!=null) {
			TemporaryVariable temp=mLocalSymbols.getTemporaryVariable(port);
			if (temp!=null) {
				temp.extendLiveRange(mNextEvent);
				if (mLoopSourceUsage!=null) {
					// We're in a loop
					mLoopSourceUsage.add(source);
				}
			}
		}
	}

	protected void registerMutation(XlimSource source) {
		XlimOutputPort port=source.asOutputPort();
		
		if (port!=null) {
			TemporaryVariable temp=mLocalSymbols.getTemporaryVariable(port);
			if (temp!=null) {
				temp.registerMutation(mNextEvent);
				if (mLoopSourceUsage!=null) {
					// We're in a loop
					mLoopSourceUsage.add(source);
				}
			}
		}
	}
	
	/**
	 * Keeps track of the sources that are (loop-invariant) inputs to a loop:
	 * their live-ranges should be extended to (at least) the end of the loop, 
	 * since they are used in the next iteration also.
	 * 
	 * In the event that these sources are dead after the loop, the end-point
	 * of their live ranges will be at the end of the loop.
	 */
	private class LoopSourceUsage {
		
		XlimLoopModule mLoop;
		Set<XlimSource> mInputs;
		
		LoopSourceUsage(XlimLoopModule loop) {
			mLoop=loop;
			mInputs=new HashSet<XlimSource>();
		}
		
		void add(XlimSource source) {
			XlimOutputPort port=source.asOutputPort();
			
			if (port==null) {
				// State variable is not local to loop
				mInputs.add(source);
			}
			else {
				XlimModule defedInModule=port.getParent().getParentModule();
				
				if (mLoop.leastCommonAncestor(defedInModule)!=mLoop) {
					// An input to the loop
					mInputs.add(source);
				}
			}
		}
		
		Iterable<XlimSource> getInputs() {
			return mInputs;
		}
	}
	
	/**
	 * Debug print-out of all XLIM operations with live ranges and mutation ranges.
	 */
	private class LiveRangePrintOut extends XlimTraversal<Object,Object> {

		@Override
		protected Object handleOperation(XlimOperation op, Object arg) {
			handleInstruction(op);
			return null;
		}

		@Override
		protected Object handlePhiNode(XlimPhiNode phi, Object arg) {
			handleInstruction(phi);
			return null;
		}
		
		private void handleInstruction(XlimInstruction instr) {
			System.out.println("// "+instr);
			for (XlimOutputPort output: instr.getOutputPorts()) {
				TemporaryVariable temp=mLocalSymbols.getTemporaryVariable(output);
				if (temp!=null) {
					System.out.println("// "+output.getUniqueId()+" ["
							         + temp.getLiveRangeStart()+","+temp.getLiveRangeEnd()+")");
					if (temp.isMutated()) {
						System.out.println("// mutation between ["
								           +temp.getFirstMutation()+","
								           +temp.getLastMutation()+")");
					}
				}
			}
		}
	}
}

