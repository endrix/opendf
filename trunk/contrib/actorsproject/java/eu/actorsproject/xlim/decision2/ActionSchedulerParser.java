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

package eu.actorsproject.xlim.decision2;

import eu.actorsproject.xlim.XlimBlockElement;
import eu.actorsproject.xlim.XlimBlockModule;
import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimIfModule;
import eu.actorsproject.xlim.XlimInputPort;
import eu.actorsproject.xlim.XlimLoopModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTestModule;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.util.InstructionPattern;
import eu.actorsproject.xlim.util.OperationHandler;
import eu.actorsproject.xlim.util.OperationPlugIn;
import eu.actorsproject.xlim.util.WildcardInstructionPattern;
import eu.actorsproject.xlim.util.XlimTreePattern;

/**
 * Parses an action scheduler (a specific Xlim "task" module)
 * an produces its DecisionTree
 */
public class ActionSchedulerParser {

	private Classifier mClassifier;
	private ConditionPlugIn mConditionParser;
	
	public ActionSchedulerParser() {
		mClassifier=new Classifier();
		mConditionParser=new ConditionPlugIn();
	}
	
	/**
	 * @param  actionScheduler
	 * @return the decision tree representation of an action scheduler
	 */
	public DecisionTree parseXlim(XlimTaskModule actionScheduler) {
		XlimLoopModule schedulingLoop=findSchedulingLoop(actionScheduler);
		
		if (schedulingLoop==null)
			error();
		
		return parseDecisionTree(schedulingLoop.getBodyModule());
	}

	public XlimLoopModule findSchedulingLoop(XlimTaskModule actionScheduler) {
		XlimLoopModule schedulingLoop=null;
		
		for (XlimBlockElement child: actionScheduler.getChildren()) {
			switch (mClassifier.classify(child)) {
			case LoopModule:
				if (schedulingLoop==null)
					schedulingLoop=(XlimLoopModule) child;
				else
					error();
				break;
			case SideEffectFreeOperation:
				break;
			default:
				error();
			}
		}
		
		return schedulingLoop;
	}
	
	private void error() {
		throw new IllegalStateException("Unable to parse action scheduler");
	}
	
	private DecisionTree parseDecisionTree(XlimIfModule ifModule) {
		DecisionTree ifTrue=parseDecisionTree(ifModule.getThenModule());
		DecisionTree ifFalse=parseDecisionTree(ifModule.getElseModule());
		if (ifTrue!=null || ifFalse!=null) {
			if (ifTrue==null)
				ifTrue=new NullNode();
			if (ifFalse==null)
				ifFalse=new NullNode();
			
			XlimTestModule testModule=ifModule.getTestModule();
			XlimSource decision=testModule.getDecision();
			ValueNode value=testModule.getValueUsage().getValue();
			Condition cond=mConditionParser.parse(decision, value);
			return new DecisionNode(cond, ifTrue, ifFalse);
		}
		else
			return null;
	}

	private DecisionTree parseDecisionTree(XlimContainerModule module) {
		DecisionTree root=null;
		boolean foundAction=false;
		for (XlimBlockElement child: module.getChildren()) {
			switch (mClassifier.classify(child)) {
			case IfModule:
				DecisionTree t=parseDecisionTree((XlimIfModule) child);
				if (t!=null) {
					if (root!=null)
						error();
					else
						root=t;
				}
				// else: no side-effects in IfModule -do nothing
				break;
			case TaskCall:
			case OtherSideEffect:
				foundAction=true;
				break;
			case SideEffectFreeOperation:
				break;  // do nothing
			default:
				// Can't handle loops/block modules in this context
				// In particular, we no longer handle "mutex" modules
				error();	
			}
		}
		
		if (foundAction)
			return new ActionNode(module);
		else
			return root;
	}

	enum Classification {
		BlockModule,
		IfModule,
		LoopModule,
		SideEffectFreeOperation,
		TaskCall,
		OtherSideEffect
	};
	
	private class Classifier implements XlimBlockElement.Visitor<Classification,Object> {

		public Classification classify(XlimBlockElement element) {
			return element.accept(this, null);
		}
		
		@Override
		public Classification visitBlockModule(XlimBlockModule m, Object dummyArg) {
			return Classification.BlockModule;
		}

		@Override
		public Classification visitIfModule(XlimIfModule m, Object dummyArg) {
			return Classification.IfModule;
		}

		@Override
		public Classification visitLoopModule(XlimLoopModule m, Object dummyArg) {
			return Classification.LoopModule;
		}

		@Override
		public Classification visitOperation(XlimOperation op, Object dummyArg) {
			if (op.modifiesState())
				if (op.getCallSite()!=null)
					return Classification.TaskCall;
				else
					return Classification.OtherSideEffect;
			else
				return Classification.SideEffectFreeOperation;
		}
	}
	
	
	private class ConditionHandler implements OperationHandler {
		
		@Override
		public boolean supports(XlimOperation op) {
			return true;
		}

		Condition parseCondition(XlimOperation op, XlimOutputPort output) {
			return new AtomicCondition(output, output.getValue());			
		}
	}

	private class PinStatusHandler extends ConditionHandler {
		@Override
		Condition parseCondition(XlimOperation op, XlimOutputPort output) {
			return new AvailabilityTest(output, op, 1);			
		}
	}
	
	private class PinAvailHandler extends ConditionHandler {
		// pinAvail(PORT) >= constant  ($ge (>=) operation matched when looking up the handler)
		XlimTreePattern mPattern=new WildcardInstructionPattern(new InstructionPattern("pinAvail"),
				                                                new InstructionPattern("$literal_Integer"));
		
		@Override
		public boolean supports(XlimOperation op) {
			// By matching the pattern we distinguish the special case from 
			// other ones that has $ge (>=) as root
			return op.getNumOutputPorts()==1 && mPattern.matches(op.getOutputPort(0));
		}	
		
		@Override
		Condition parseCondition(XlimOperation op, XlimOutputPort output) {
			// Knowing that the pattern matches, we simply get the pinAvail and $literal_Integer
			XlimOperation pinAvail=mPattern.getOperand(0, output).isOperation();
			XlimOperation literal=mPattern.getOperand(1, output).isOperation();
			long numTokens=literal.getIntegerValueAttribute();
			return new AvailabilityTest(output, pinAvail, (int) numTokens);			
		}
	}
	
	private class ConjunctionHandler extends ConditionHandler {
		@Override
		Condition parseCondition(XlimOperation op,
				                 XlimOutputPort output) {
			Conjunction result=new Conjunction(output);
			// Parse the inputs of the $and operator 
			for (XlimInputPort input: op.getInputPorts()) {
				XlimSource src=input.getSource();
				ValueNode value=input.getValue();
				Condition cond=mConditionParser.parse(src, value);
			    cond.addTo(result);
			}
			return result;			
		}
	}

	private class ConditionPlugIn extends OperationPlugIn<ConditionHandler> {
		
		public ConditionPlugIn() {
			super(new ConditionHandler());
			registerHandler("$and", new ConjunctionHandler());
			registerHandler("pinStatus", new PinStatusHandler());
			// pinAvail(port) >= $literal_Integer 
			// other instances of >= are handled by the default ConditionHandler
			registerHandler("$ge", new PinAvailHandler());
		}
		
		public Condition parse(XlimSource src, ValueNode value) {
			XlimOutputPort port=src.asOutputPort();
			if (port!=null) {
				XlimOperation op=port.getParent().isOperation();
				if (op!=null) {
					ConditionHandler handler=getOperationHandler(op);
					return handler.parseCondition(op, port);
				}
			}
			return new AtomicCondition(src, value);
		}
	}
}