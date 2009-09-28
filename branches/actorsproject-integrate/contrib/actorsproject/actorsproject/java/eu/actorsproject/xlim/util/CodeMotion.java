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
import java.util.Map;

import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimIfModule;
import eu.actorsproject.xlim.XlimLoopModule;
import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimPhiNode;
import eu.actorsproject.xlim.XlimTaskModule;

/**
 * Moves code to the location given by the code-motion-plugin
 * (see LatestEvaluationAnalysis for an implementation)
 */
public class CodeMotion {

	protected boolean mTrace=false;
	
	public void codeMotion(XlimTaskModule task, CodeMotionPlugIn plugIn) {
		CodeMotionTraversal taskTraversal=new CodeMotionTraversal();
		taskTraversal.traverse(task, plugIn);
	}


	class CodeMotionTraversal extends XlimTraversal<Object, CodeMotionPlugIn> {

		Map<XlimModule, List<XlimOperation>> mInsert=
			new HashMap<XlimModule, List<XlimOperation>>();

		@Override
		protected Object handleOperation(XlimOperation op, CodeMotionPlugIn arg) {
			// Collect all operations that are to be moved to a particular module
			// Data dependences are preserved by insertion in top-down order
			XlimModule toModule=arg.insertionPoint(op);
			// We can't move to the entry of loops/if-modules, so the code
			// will instead be moved to the pre-header/surrounding container
			XlimModule toContainer=(toModule instanceof XlimContainerModule)? 
					toModule : toModule.getParentModule();	
			// Avoid local code motion 
			if (toContainer!=op.getParentModule()) {
				List<XlimOperation> operations=mInsert.get(toModule);
				if (operations==null) {
					operations=new ArrayList<XlimOperation>();
					mInsert.put(toModule, operations);
				}
				operations.add(op);
			}

			return null;
		}

		@Override
		protected Object handlePhiNode(XlimPhiNode phi, CodeMotionPlugIn arg) {
			return null;
		}

		protected void move(List<XlimOperation> operations, XlimContainerModule m) {
			for (XlimOperation op: operations) {
				if (mTrace) {
					System.out.println("// CodeMotion: " + op.toString()
							           + " moved from " + op.getParentModule()
							           + " to "+m);
				}
				
				m.cutAndPaste(op);
			}
			m.completePatchAndFixup();
		}

		@Override
		protected Object traverseContainerModule(XlimContainerModule m, CodeMotionPlugIn arg) {
			// Move all operations that should end up in this module
			List<XlimOperation> operations=mInsert.get(m);
			if (operations!=null) {
				m.startPatchAtBeginning();
				move(operations,m);
			}
			return super.traverseContainerModule(m, arg);
		}

		@Override
		protected Object traverseIfModule(XlimIfModule m, CodeMotionPlugIn arg) {
			// Move all operations that should be inserted before this module 
			List<XlimOperation> operations=mInsert.get(m);
			if (operations!=null) {
				XlimContainerModule container=m.getParentModule();
				container.startPatchBefore(m);
				move(operations,container);
			}
			return super.traverseIfModule(m, arg);
		}

		@Override
		protected Object traverseLoopModule(XlimLoopModule m, CodeMotionPlugIn arg) {
			// Move all operations that should be inserted before this module 
			List<XlimOperation> operations=mInsert.get(m);
			if (operations!=null) {
				XlimContainerModule container=m.getParentModule();
				container.startPatchBefore(m);
				move(operations,container);
			}
			return super.traverseLoopModule(m, arg);
		}
	}
}