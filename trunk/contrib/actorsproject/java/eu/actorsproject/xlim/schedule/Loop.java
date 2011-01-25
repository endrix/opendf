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

import java.util.List;

import eu.actorsproject.util.Pair;
import eu.actorsproject.util.XmlAttributeFormatter;

/**
 * Represents a natural loop in a control-flow graph
 */
public abstract class Loop extends Region {

	private BasicBlock mHeader;
	private Long mTripCount;
	
	public Loop(BasicBlock header) {
		mHeader=header;
	}
	
	/**
	 * @return the header of the loop, which dominates all BasicBlocks of the loop
	 */
	public BasicBlock getHeader() {
		return mHeader;
	}
	
	/**
	 * @param loop  a Loop
	 * @return true if the given loop is contained within this loop (a loop contains itself by convention)
	 */
	public boolean contains(Loop loop) {
		while (loop!=null && loop!=this) {
			loop=loop.getParent();
		}
		return loop==this;
	}
	
	
	/**
	 * @param b  a BasicBlock
	 * @return true if the given BasicBlock is contained within this loop
	 */
	public abstract boolean contains(BasicBlock b);
	
	
	/**
	 * @return the immediately surrounding loop
	 */
	public abstract Loop getParent();
	
	
	@Override
	public Loop asLoop() {
		return this;
	}
	
	/**
	 * @return the Loops and BasicBlocks immediately surrounded by this loop
	 *         (=Regions), the Regions are sorted in rPostOrder
	 */
	@Override
	public abstract List<Region> getChildren();
	
	/**
	 * @return the Loops that are immediately surrounded by this loop
	 */
	public abstract List<? extends Loop> getInnerLoops();
	
	/**
	 * @return the exits of this loop. Each pair represents a flow edge that exits the loop
	 *         such that the first component is a BasicBlock that is contained within the loop
	 *         and the second component is a BasicBlock that is not contained within the loop.
	 */
	public abstract List<Pair<BasicBlock,BasicBlock>> getExits();
	
	/**
	 * @return the blocks, which have a flow edge back to the header of the loop
	 */
	public abstract List<BasicBlock> getBackEdges();
		
	
	public Long getTripCount() {
		return mTripCount;
	}
	
	public void setTripCount(long tripCount) {
		mTripCount=tripCount;
	}
	
	public boolean hasConstantTripCount() {
		return (getTripCount()!=null);
	}
		
	/*
	 * XmlElement
	 */
	
	@Override
	public String getTagName() {
		return "loop";
	}
	
	@Override
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		return "entry=\""+getHeader().getIdentifier()+"\"";
	}
}
