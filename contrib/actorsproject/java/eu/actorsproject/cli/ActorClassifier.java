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

package eu.actorsproject.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import eu.actorsproject.util.XmlPrinter;
import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.schedule.ActionSchedule;
import eu.actorsproject.xlim.schedule.Classifier;
import eu.actorsproject.xlim.util.CopyPropagation;
import eu.actorsproject.xlim.util.DeadCodeAnalysis;
import eu.actorsproject.xlim.util.DeadCodePlugIn;
import eu.actorsproject.xlim.util.DeadCodeRemoval;

/**
 * Classifies an actor according to its action schedule:
 * a) statically schedulable actors (SDF, CSDF etc.)
 * b) dynamic-dataflow (DDF) actors
 * c) actors with non-deterministic/timing-dependent behavior
 * 
 * Classification is conservative: a statically schedulable
 * actor can incorrectly be classified as (b) or (c) and a
 * DDF actor can incorrectly be classified as (c).
 *
 * This is a first shot, much remains to be done. 
 */
public class ActorClassifier extends CheckXlim {

	private CopyPropagation mCopyPropagation;
	private DeadCodeAnalysis mDeadCodeAnalysis;
	private DeadCodeRemoval mDeadCodeRemoval;
	private XmlPrinter mPrinter ; 

	public ActorClassifier (String outdir){
		try {
			mPrinter=new XmlPrinter(new PrintStream(new File(outdir)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public ActorClassifier(){
		mPrinter=new XmlPrinter(System.out);
	}

	@Override
	public void initSession(String args[]) {
		super.initSession(args);
		mCopyPropagation=new CopyPropagation();
		mDeadCodeAnalysis=new DeadCodeAnalysis();
		mDeadCodeRemoval=new DeadCodeRemoval();
	}

	@Override
	protected XlimDesign read() {
		XlimDesign design=super.read();
		if (design!=null) {
			// First do some clean-ups
			design.createCallGraph();
			copyPropagate(design);
			deadCodeElimination(design);

			// Then find the modes and and print classification
			Classifier classifier=new Classifier(design);
			System.out.println("Actor: "+design.getName());
			System.out.println("File:  "+mInputFile.getPath());
			ActionSchedule schedule=classifier.getActionSchedule();
			schedule.printActionSchedule(mPrinter);
			System.out.println();
		}
		return design;
	}

	protected void copyPropagate(XlimDesign design) {
		mCopyPropagation.copyPropagate(design);
	}

	protected void deadCodeElimination(XlimDesign design) {
		DeadCodePlugIn deadCode=mDeadCodeAnalysis.findDeadCode(design);
		mDeadCodeRemoval.deadCodeRemoval(design, deadCode);
	}

	@Override
	protected void printHelp() {
		String myName=getClass().getSimpleName();
		System.out.println("\nUsage: " + myName + " [-o output file] input-files" );
		System.out.println("\nClassifies the behavior of an actor (specified in XLIM)\n");
		System.out.println("\n [Options] \n");
		System.out.println("\n -o : specifies the output file \n");
	}

	public static void main(String[] args) {
		ActorClassifier compilerSession ; 		 
		if(args.length != 0 && args[0].equals("-o")){
			String[] argswo =  new String[args.length-2];
			for (int i=0; i < args.length-2; i++){
				argswo[i] = args[i+2];
			}
			compilerSession=new ActorClassifier(args[1]);
			compilerSession.runFromCommandLine(argswo);
		}
		else {
			compilerSession=new ActorClassifier();
			compilerSession.runFromCommandLine(args);
		}

		if (compilerSession.mHasErrors)
			System.exit(1);

	}
}
