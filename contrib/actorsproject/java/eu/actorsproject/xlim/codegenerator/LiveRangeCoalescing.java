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

import java.util.ArrayList;
import java.util.List;

import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.util.OperationHandler;
import eu.actorsproject.xlim.util.OperationPlugIn;

/**
 * Implements "coalescing" of live ranges by which copy instructions are avoided
 */

public class LiveRangeCoalescing  {

	protected LocalSymbolTable mLocalSymbols;
	protected OperationPlugIn<CoalesceHandler> mCoalesceHandlers;
	protected List<XlimOperation> mCopyOps;
	protected CoalesceHandler mNoopHandler;
	
	public LiveRangeCoalescing(LocalSymbolTable localSymbols) {
		CoalesceHandler defaultHandler=new CoalesceHandler();
		mNoopHandler=new NoopHandler();
		
		mLocalSymbols = localSymbols;
		mCoalesceHandlers=new OperationPlugIn<CoalesceHandler>(defaultHandler);
		mCoalesceHandlers.registerHandler("noop", mNoopHandler);
		mCoalesceHandlers.registerHandler("cast", mNoopHandler);
		mCopyOps=new ArrayList<XlimOperation>();
	}

	/**
	 * @param op   Candidate to add (an operation that defines/starts a live range)
	 * 
	 * Adds a candidate, for which coalescing will be attempted.
	 * Should be called for each operation that constitues a "statement". Has no effect
	 * for operations that will be generated as an "expression".
	 */
	public void addCandidate(XlimOperation op) {
		CoalesceHandler handler=mCoalesceHandlers.getOperationHandler(op);
		handler.addCandidate(op);
	}
	
	/**
	 * Processes the collection of added candidates after their live ranges have been
	 * determined
	 */
	public void processCandidates() {
		mNoopHandler.processCandidates();
	}
	
	/**
	 * Coalesces the live ranges of t1 and t2 so that t1 is the new representer (denotes
	 * both original live ranges). To end up in the correct scope, t1 should dominate
	 * t2.
	 * 
	 * @param t1  Temporary variable, the result of the coalesing
	 * @param t2  Temporary variable, which is merged into t1
	 */
	protected void mergeLiveRanges(TemporaryVariable t1, TemporaryVariable t2) {
		System.out.println("// "+t1.getOutputPort().getUniqueId()
				           + " and "+t2.getOutputPort().getUniqueId()+" coalesced");
		t2.mergeWith(t1);
	}
	
    /**
     * Base class (and default implementaion) of CoaleseHandler,
     * which identifies candidates for coalesing.
     * 
     * The handler is called for each operation that defines or
     * mutates a live range. 
     */
    protected class CoalesceHandler implements OperationHandler {
    
    	@Override
    	public boolean supports(XlimOperation op) {
    		return true;
    	}
    	
    	public void addCandidate(XlimOperation op) {
    		// default is not to try coalsesing!
    	}
    	
    	public void processCandidates() {
    		// Nothing to do!
    	}
    }
    
    /**
     * Handles copy operations: noop and cast (to/from same type)
     */
    protected class NoopHandler extends CoalesceHandler {
    	
    	/* 
    	 * Supports op if source and destination types are the same and
    	 * the source is an OutputPort (not a stateVariable) with an
    	 * associated temporary    	 
    	 */
    	@Override
    	public boolean supports(XlimOperation op) {
    		XlimSource source=op.getInputPort(0).getSource();
    		XlimOutputPort sourcePort=source.asOutputPort();
    		XlimType sourceType=source.getType();
    		XlimOutputPort destPort=op.getOutputPort(0);
    		XlimType destType=destPort.getType();
    		
    		return (sourceType==destType 
    				&& sourcePort!=null
    				&& mLocalSymbols.getTemporaryVariable(sourcePort)!=null
    				&& mLocalSymbols.getTemporaryVariable(destPort)!=null);
    	}
    	
    	@Override
    	public void addCandidate(XlimOperation op) {
    		mCopyOps.add(op);
    	}
    	
    	@Override
    	public void processCandidates() {
    		for (XlimOperation op: mCopyOps)
    			tryCoalesce(op);
    	}
    	
    	protected void tryCoalesce(XlimOperation op) {
    		XlimSource source=op.getInputPort(0).getSource();
    		XlimOutputPort sourcePort=source.asOutputPort();
    		TemporaryVariable sourceTemp=mLocalSymbols.getTemporaryVariable(sourcePort);
    		TemporaryVariable destTemp=mLocalSymbols.getTemporaryVariable(op.getOutputPort(0));
    		assert(sourceTemp!=null && destTemp!=null);
    		
    		if (sourceTemp.mutationOverlaps(destTemp)==false
    			&& destTemp.mutationOverlaps(sourceTemp)==false) {
    			System.out.println("// coalescing: "+op);
    			mergeLiveRanges(sourceTemp,destTemp);
    			// We can now remove the operation  (so that we don't have to test 
    			// for this special case in the code generator)
    			op.getOutputPort(0).substitute(sourcePort);
    			op.getParentModule().remove(op);
    		}
    		else {
    			System.out.println("// couldn't coalesce: "+op+" due to overlapping mutation");
    		}
    	}
    }
}
