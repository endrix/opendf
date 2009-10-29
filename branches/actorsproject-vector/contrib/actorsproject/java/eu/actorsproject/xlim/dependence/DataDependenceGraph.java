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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import eu.actorsproject.xlim.XlimModule;

public class DataDependenceGraph {

	private Map<StateLocation,ValueNode> mInputValues;
	private Map<StateLocation,ValueUsage> mOutputValues;
	private CallNode mCallNode;
	
	public DataDependenceGraph(CallNode callNode) {
		mCallNode=callNode;
	}

	/**
	 * @return the port/state variables that are accessed (used and/or modified) 
	 *         in this graph
	 */
	public Set<StateLocation> getAccessedState() {
		return mInputValues.keySet();
	}
	
	/**
	 * @return the ports/state variables that are modified in this graph
	 *         (this is a subset of the accessed ports/state variables) 
	 */
	public Set<StateLocation> getModifiedState() {
		return mOutputValues.keySet();
	}
	
	/**
	 * @param carrier, a port or a state variable
	 * @return the node in this graph that represents the initial value
	 *         of carrier (null if not accessed in this graph)
	 */
	public ValueNode getInputValue(StateLocation carrier) {
		return mInputValues.get(carrier);
	}

	/**
	 * @return the collection of all input values
	 */
	public Collection<ValueNode> getInputValues() {
		return mInputValues.values();
	}
	
	/**
	 * @param carrier, a port or a state variable
	 * @return the node in this graph that represents the final value
	 *         of carrier (null if not accessed in this graph, the initial
	 *         value if not modified in this graph)
	 */
	public ValueNode getOutputValue(StateLocation carrier) {
		ValueUsage output=mOutputValues.get(carrier);
		if (output!=null)
			return output.getValue();
		else
			return mInputValues.get(carrier);
	}
	
	/**
	 * @return Collection of all modified output values
	 *         (input values that remain unchanged are not included)
	 */
	public Collection<ValueNode> getModifiedOutputValues() {
		ArrayList<ValueNode> outputs=new ArrayList<ValueNode>();
		for (ValueUsage usage: mOutputValues.values())
			outputs.add(usage.getValue());
		return outputs;
	}
	
	public void resolveExposedUses(FixupContext context) {
		if (mInputValues==null) {
			// First time: allocate initial values and resolve exposed uses
			mInputValues=new HashMap<StateLocation,ValueNode>();
			// We have to copy the exposed uses (we are modifying the underlying map)
			ArrayList<Location> exposedUses=
				new ArrayList<Location>(context.getExposedUses());
			for (Location location: exposedUses) {
				assert(location.isStateLocation());
				StateLocation stateLoc=location.asStateLocation();
				ValueNode initialValue=new InitialValueNode(stateLoc);
				mInputValues.put(stateLoc, initialValue);
				context.resolveExposedUses(initialValue);
			}
		}
		else {
			// Following times: use existing initial values to resolve exposed uses
			ArrayList<Location> exposedUses=
				new ArrayList<Location>(context.getExposedUses());
			for (Location location: exposedUses) {
				assert(location.isStateLocation());
				ValueNode initialValue=mInputValues.get(location.asStateLocation());
				if (initialValue!=null)
					context.resolveExposedUses(initialValue);
				else {
					// Currently we do not support patches that access new stateful resources.
					// Doing so doesn't seem terribly useful. Support is complicated in that
					// it affects the callers of the task (each call site needs to be updated 
					// and the resulting exposed uses need to be resolved in the caller tasks).
					throw new UnsupportedOperationException("Patch adds access to new location: "
							+location.getDebugName());
				}
			}
		}
	}
	
	public void propagateNewValues(FixupContext context) {
		if (mOutputValues==null) {
			// First time: allocate final value uses and fix them up
			mOutputValues=new HashMap<StateLocation,ValueUsage>();
			for (Location location: context.getNewValues()) {
				if (location.isStateLocation()) {
					StateLocation stateLoc=location.asStateLocation();
					ValueUsage finalUse=new FinalStateUsage(stateLoc);
					mOutputValues.put(stateLoc, finalUse);
					context.propagateNewValue(finalUse);
				}
				// else: location is a local aggregate, 
				// which doesn't propagare further (it goes out of scope)
			}
		}
		else {
			// Following times: use existing final value uses, but set new values
			for (Location location: context.getNewValues()) {
				if (location.isStateLocation()) {
					ValueUsage finalUse=mOutputValues.get(location);
					if (finalUse!=null)
						context.propagateNewValue(finalUse);
					else {
						// Currently we do not support patches that access new stateful resources.
						// Doing so does not seem terribly useful. Support is complicated in that
						// it affects the callers of the task (each call site needs to be updated 
						// and the resulting new values need to be propagated in the caller tasks).
						throw new UnsupportedOperationException("Patch adds access to new state carrier: "
								+location.getDebugName());
					}
				}
			    // else: location is a local aggregate, 
				// which doesn't propagare further (it goes out of scope)
			}
		}
	}
	
	/**
	 * Removes an input/output value (used by CallNode, which also updates callers)
	 * @param carrier
	 */
	void remove(StateLocation carrier) {
		// First check that we are not doing anything stupid
		ValueNode input=mInputValues.get(carrier);
		ValueUsage output=mOutputValues.get(carrier);
		Iterator<ValueUsage> p=input.getUses().iterator();
		assert(p.hasNext()==false || p.next()==output && p.hasNext()==false);
		
		mInputValues.remove(carrier);
		mOutputValues.remove(carrier);
	}
	
	
	private class InitialValueNode extends SideEffect {

		private StateLocation mLocation;
		
		public InitialValueNode(StateLocation location) {
			mLocation=location;
		}
				
		
		@Override
		public Location actsOnLocation() {
			return mLocation;
		}
		
		@Override
		public ValueOperator getDefinition() {
			return null;
		}	

		
		@Override
		public ValueNode getDominatingDefinition() {
			// By convention initial values have no previous definition
			// though they actually have one for each caller
			return null;  
		}

		@Override
		public <Result,Arg> Result accept(Visitor<Result,Arg> visitor, Arg arg) {
			return visitor.visitInitial(this,mCallNode,arg);
		}
	}
	
	// TODO: setting a FinalStateUsage to the same value as the input value means no modification is made
	// Perhaps let the CallNode check, remove output values and update callers?
	private class FinalStateUsage extends ValueUsage {

		private StateLocation mStateCarrier;
		
		public FinalStateUsage(StateLocation carrier) {
			super(null);
			mStateCarrier=carrier;
		}
		
		@Override
		public boolean needsFixup() {
			return true;
		}
		
		@Override
		public Location getFixupLocation() {
			return mStateCarrier;
		}
		
		@Override
		public ValueOperator usedByOperator() {
			return null; // Final usage has no ValueOperator
		}
		
		@Override
		public XlimModule usedInModule() {
			/* 
			 * The "final" usage of a state variable/port, which represents the output of
			 * a CallNode/TaskModule has no associated operator. Instead the value is
			 * considered used in the XlimTaskModule, itself.
			 */
			return mCallNode.getTask();
		}
		
		@Override
		public <Result,Arg> Result accept(Visitor<Result,Arg> visitor, Arg arg) {
			return visitor.visitFinal(this,mCallNode,arg);
		}
	}
}
