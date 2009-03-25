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
import java.util.Collections;
import java.util.List;

import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimIfModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimTopLevelPort;

/**
 * A NullNode represents an empty leaf in the decision tree.
 * This is the place where we must block to avoid busy wait.
 */
public class NullNode extends DecisionTree {

	private XlimContainerModule mProgramPoint;
	private List<BlockOnPort> mPorts;
	
	public NullNode(XlimContainerModule programPoint) {
		mProgramPoint=programPoint;
		mPorts=Collections.emptyList();
	}

	@Override
	protected XlimContainerModule getModule() {
		return mProgramPoint;
	}
	
	@Override
	protected PortMap 
	hoistAvailabilityTests(PortMap dominatingTests) {
		return null; // null indicates that no ActionNode is found in this subtree
	}

	@Override
	protected XlimIfModule sinkIntoIfModule() {
		// Insert the IfModule at current program point
		mProgramPoint.startPatchAtEnd();
		XlimIfModule result=mProgramPoint.addIfModule();
		mProgramPoint.completePatchAndFixup();
		
		// update program point
		mProgramPoint=result.getThenModule();
		return result;
	}
	
	/**
     * Decorates the NullNodes of a decision tree with the set of ports that may
     * have been tested for availability tokens (input ports) or space (output ports)
     * and the outcome of that test was failure.
     * @param assertedTests tests that succeeded on *all* paths from the root
	 * @param failedTests   tests that may have failed on *some* path from the
     *                      root of the decision tree to this node.
     */
	@Override
	protected DecisionTree topDownPass(PortMap assertedTests, PortMap failedTests) {
		mPorts=new ArrayList<BlockOnPort>();
		
		for (AvailabilityTest test: failedTests) {
			AvailabilityTest asserted=assertedTests.get(test.getPort());
			if (asserted==null || asserted.getTokenCount()<test.getTokenCount())
				mPorts.add(new BlockOnPort(test));
		}
		
		return this;
	}
    	
 	/**
     * Alters the underlying XLIM-representation so that it uses blocking wait
     */
 	@Override
    public void generateBlockingWait() {
 		if (mPorts.isEmpty()==false) {
 			BlockOnPort last=mPorts.get(mPorts.size()-1);
 			mProgramPoint.startPatchAtEnd();
 			for (BlockOnPort block: mPorts)
 				block.generatePinWait(mProgramPoint, block==last);
			mProgramPoint.completePatchAndFixup();
			
			/* // identify wait-on-multiple pinWaits
			 * if (mPorts.size()>1) {
			 *	 System.out.print("Multiple pinWaits:");
			 *	 for (BlockOnPort bp: mPorts) {
			 *	 	System.out.print(" "+bp.mPort.getSourceName());
			 *	 }
			 *	 System.out.println();
			 * }
			 */
 		}
 		// TODO: else we have a "dead" actor that will never be able to fire again
 		// (same scheduling decision will be taken again and again indefinitely...)
 	}
 	
	
	/* XmlElement interface */

	@Override
	public String getTagName() {
		return "nullNode";
	}

	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return mPorts;
	}
	
	private class BlockOnPort implements XmlElement {
		XlimTopLevelPort mPort;
		int mSize;
		
		BlockOnPort(AvailabilityTest failedTest) {
			mPort=failedTest.getPort();
			mSize=failedTest.getTokenCount();
		}
		
		public void generatePinWait(XlimContainerModule module, boolean last) {
			List<XlimSource> inputs=Collections.emptyList();
			List<XlimOutputPort> outputs=Collections.emptyList();
			
			XlimOperation pinWait=mProgramPoint.addOperation("pinWait",inputs,outputs); 
			pinWait.setPortAttribute(mPort);
			pinWait.setIntegerValueAttribute(mSize);
			if (last)
				pinWait.setBlockingStyle();
		}
				
		@Override
		public String getTagName() {
			return "blockOnPort";
		}

		@Override
		public Iterable<? extends XmlElement> getChildren() {
			return Collections.emptyList();
		}
		
		@Override
		public String getAttributeDefinitions() {
			return "portName=\""+mPort.getSourceName()+"\" size=\""+mSize+"\"";
		}
	}
}
