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

import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.XlimTaskModule;

public class XlimTransformer {

	private CopyPropagation mCopyPropagation=new CopyPropagation();
	private DeadCodeAnalysis mDeadCodeAnalysis=new DeadCodeAnalysis();
	private DeadCodeRemoval mDeadCodeRemoval=new DeadCodeRemoval();
	private BlockingWaitGenerator mBlockingWaitGenerator=new BlockingWaitGenerator();
	private LatestEvaluationAnalysis mLatestEvaluationAnalysis=
		new LatestEvaluationAnalysis();
	private CodeMotion mCodeMotion=new CodeMotion();
	private NativeTypeTransformation mNativeTypeTransformation=
		new NativeTypeTransformation(new NativeTypesDefault());
	
	protected boolean mDoCopyPropagation=true;
	protected boolean mDoDeadCodeRemoval=true;
	protected boolean mGenerateBlockingWaits=true;
	protected boolean mDoCodeMotion=true;
	protected boolean mTransformToNativeTypes=true;
	
	public void transform(XlimDesign design) {
		if (mDoCopyPropagation)
			copyPropagate(design);
		if (mDoDeadCodeRemoval)
			deadCodeElimination(design);
		if (mGenerateBlockingWaits)
			mBlockingWaitGenerator.generateBlockingWaits(design);
		if (mDoDeadCodeRemoval)
			deadCodeElimination(design);
		if (mDoCodeMotion)
			codeMotion(design);
		if (mTransformToNativeTypes)
			mNativeTypeTransformation.transform(design);
	}

	public void copyPropagate(XlimDesign design) {
		mCopyPropagation.copyPropagate(design);
	}
	
	public void deadCodeElimination(XlimDesign design) {
		DeadCodePlugIn deadCode=mDeadCodeAnalysis.findDeadCode(design);
		mDeadCodeRemoval.deadCodeRemoval(design, deadCode);
	}
	
	public void codeMotion(XlimDesign design) {
		for (XlimTaskModule task: design.getTasks()) {
			CodeMotionPlugIn plugIn=mLatestEvaluationAnalysis.analyze(task);
			mCodeMotion.codeMotion(task, plugIn);
		}
	}
}
