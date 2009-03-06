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

import eu.actorsproject.xlim.XlimBlockElement;
import eu.actorsproject.xlim.XlimBlockModule;
import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.XlimIfModule;
import eu.actorsproject.xlim.XlimInstruction;
import eu.actorsproject.xlim.XlimLoopModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimPhiNode;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTestModule;

public abstract class XlimTraversal<ResultT,ArgT> {

	public ResultT traverse(XlimDesign design, ArgT arg) {
		for (XlimTaskModule task: design.getTasks())
			traverse(task, arg);
		return null;
	}
	
	public ResultT traverse(XlimTaskModule task, ArgT arg) {
		return traverseContainerModule(task,arg);
	}
	
	protected ResultT traverseContainerModule(XlimContainerModule m, ArgT arg) {
		for (XlimBlockElement element: m.getChildren())
			element.accept(mVisitor, arg);
		return null;
	}
	
	protected ResultT traverseBlockModule(XlimBlockModule m, ArgT arg) {
		return traverseContainerModule(m,arg);
	}
	
	protected ResultT traverseTestModule(XlimTestModule m, ArgT arg) {
		return traverseContainerModule(m,arg);
	}
	
	protected ResultT traverseIfModule(XlimIfModule m, ArgT arg) {
		traverseTestModule(m.getTestModule(),arg);
		traverseContainerModule(m.getThenModule(),arg);
		traverseContainerModule(m.getElseModule(),arg);
		traversePhiNodes(m.getPhiNodes(),arg);
		return null;
	}
	
	protected ResultT traverseLoopModule(XlimLoopModule m, ArgT arg) {
		traversePhiNodes(m.getPhiNodes(),arg);
		traverseTestModule(m.getTestModule(),arg);
		traverseContainerModule(m.getBodyModule(),arg);
		return null;
	}
	
	protected ResultT traversePhiNodes(Iterable<? extends XlimPhiNode> phiNodes, ArgT arg) {
		for (XlimPhiNode phi: phiNodes) {
			handlePhiNode(phi,arg);
		}
		return null;
	}
	
	protected abstract ResultT handleOperation(XlimOperation op, ArgT arg);
	
	protected abstract ResultT handlePhiNode(XlimPhiNode phi, ArgT arg);
	
	protected XlimBlockElement.Visitor<ResultT,ArgT> mVisitor = new XlimBlockElement.Visitor<ResultT,ArgT>() {
		public ResultT visitBlockModule(XlimBlockModule m, ArgT arg) {
			return traverseBlockModule(m,arg);
		}
		public ResultT visitIfModule(XlimIfModule m, ArgT arg) {
			return traverseIfModule(m,arg);
		}
		public ResultT visitLoopModule(XlimLoopModule m, ArgT arg) {
			return traverseLoopModule(m,arg);
		}
		public ResultT visitOperation(XlimOperation op, ArgT arg) {
			return handleOperation(op,arg);
		}
	};
}
