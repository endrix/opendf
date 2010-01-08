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

package eu.actorsproject.xlim;

import java.util.List;

public interface XlimContainerModule extends XlimModule {
	/** 
	 * @return Block elements of this container module
	 */
	@Override
	Iterable<? extends XlimBlockElement> getChildren();

	/** 
	 * @return Block elements of this container module (in reverse order)
	 */
	Iterable<? extends XlimBlockElement> getChildrenReverse();
	
	/**
	 * @return value of mutex attribute (if present and "true")
	 */	
	boolean isMutex();
	
	void setMutex();

	// TODO: put all these methods in a separate class, retrieved by starting a patch?
	
	XlimBlockModule addBlockModule(String kind);
	XlimIfModule addIfModule();
	XlimLoopModule addLoopModule();
	XlimOperation addOperation(String kind, 
                               List<? extends XlimSource> inputs,
                               List<? extends XlimOutputPort> outputs);
	
	/* convenience methods */
	
	XlimOperation addOperation(String kind);
	XlimOperation addOperation(String kind, XlimSource input);
	XlimOperation addOperation(String kind, XlimSource input1, XlimSource input2);
	XlimOperation addOperation(String kind, List<? extends XlimSource> inputs);
	
	XlimOperation addOperation(String kind, XlimType output);
	XlimOperation addOperation(String kind, XlimSource input, XlimType output);
	XlimOperation addOperation(String kind, XlimSource input1, XlimSource input2, XlimType output);
	XlimOperation addOperation(String kind, List<? extends XlimSource> inputs,  XlimType output);
	XlimOperation addLiteral(long value);
	XlimOperation addLiteral(boolean value);
	
	/**
	 * Starts a patch at the end of the module. Subsequent calls to
	 * addOperation, addBlockModule, etc. will insert the new elements 
	 * at the end of the module.
	 * Complete the patch using completePatchAndFixup().
	 */
	void startPatchAtEnd();
	
	/**
	 * Starts a patch at the beginning of the module. Subsequent calls to
	 * addOperation, addBlockModule, etc. will insert the new elements 
	 * at the beginning of the module.
	 * Complete the patch using completePatchAndFixup().
	 */
	void startPatchAtBeginning();
	
	/**
	 * Starts a patch before the given block element. Subsequent calls to
	 * addOperation, addBlockModule, etc. will insert the new elements prior
	 * to 'element'.
	 * @param element
	 * Complete the patch using completePatchAndFixup().
	 */
	void startPatchBefore(XlimBlockElement element);
	
	/**
	 * Starts a patch after the given block element. Subsequent calls to
	 * addOperation, addBlockModule, etc. will insert the new elements after 'element'.
	 * @param element
	 * Complete the patch using completePatchAndFixup().
	 */
	void startPatchAfter(XlimBlockElement element);
	
	/**
	 * Completes the current patch and fixes up the data dependence graph. 
	 * The fix-up progresses down into added sub-modules and into following code 
	 * that uses possible new values.
	 */
	void completePatchAndFixup();
	
	
	/**
	 * Removes element from container module and data dependence graph
	 * @param element
	 */
	void remove(XlimBlockElement element);
	
	/**
	 * Removes element from its container module, but not from data dependence graph
	 * and inserts it into this container 
	 * @param element
	 */
	void cutAndPaste(XlimBlockElement element);
	
	/**
	 * Removes all elements from "module", but not from data dependence graph
	 * and inserts it into this container 
	 * @param module
	 */
	void cutAndPasteAll(XlimContainerModule module);
}
