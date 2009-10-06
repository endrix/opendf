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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import eu.actorsproject.xlim.XlimOperation;

public class OperationPlugIn<T extends OperationHandler> {

	protected HashMap<String,ArrayList<T>> mHandlers;
	protected T mDefaultHandler;
	
	public OperationPlugIn(T defaultHandler) {
		mHandlers=new HashMap<String,ArrayList<T>>();
		mDefaultHandler=defaultHandler;
	}

	/**
	 * @param opKind  an operation "kind" that is supported by the handler
	 * @param handler an operation-handler 
	 * Registers an operation-handler. For multiple handlers of the same operation "kind",
	 * the order of registration matters: the handler inserted first will be the first one to
	 * have its supports() predicate evaluated (thus, special cases first and a possible "catch all" last).
	 */
	public void registerHandler(String opKind, T handler) {
		ArrayList<T> handlers=mHandlers.get(opKind);
		if (handlers==null) {
			handlers=new ArrayList<T>();
			mHandlers.put(opKind, handlers);
		}
		handlers.add(handler);
	}
	
	/**
	 * @param op Operation
	 * @return   Operation handler, which supports the operation
	 */
	public T getOperationHandler(XlimOperation op) {
		String opKind=op.getKind();
		List<T> handlers=mHandlers.get(opKind);
		if (handlers!=null)
		    for (T h: handlers)
			    if (h.supports(op))
				    return h;
		if (mDefaultHandler!=null)
			return mDefaultHandler;
		else
			throw new RuntimeException("Unhandled operation: "+opKind);
	}
}
