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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.actorsproject.util.Pair;
import eu.actorsproject.util.XmlPrinter;
import eu.actorsproject.xlim.XlimInstruction;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.absint.AbstractValue;
import eu.actorsproject.xlim.absint.Context;
import eu.actorsproject.xlim.absint.DemandContext;
import eu.actorsproject.xlim.absint.Evaluator;
import eu.actorsproject.xlim.absint.IntervalWidening;
import eu.actorsproject.xlim.absint.LinearExpression;
import eu.actorsproject.xlim.absint.LinearExpressionDomain;
import eu.actorsproject.xlim.absint.StateMapping;
import eu.actorsproject.xlim.decision2.ActionNode;
import eu.actorsproject.xlim.decision2.DecisionNode;
import eu.actorsproject.xlim.decision2.DecisionTree;
import eu.actorsproject.xlim.decision2.NullNode;
import eu.actorsproject.xlim.dependence.DependenceComponent;
import eu.actorsproject.xlim.dependence.DependenceSlice;
import eu.actorsproject.xlim.dependence.Location;
import eu.actorsproject.xlim.dependence.StateLocation;
import eu.actorsproject.xlim.dependence.ValueNode;
import eu.actorsproject.xlim.dependence.ValueOperator;

public class LoopAnalysis {

	private boolean mTrace=false;           // Trace loop analysis
	private boolean mTraceIvAnalysis=false; // Trace the detection of induction variables
	
	private ControlFlowGraph mCfg;
	private Map<ActionNode,DependenceComponent> mActionNodeComponents;
	private Set<ValueNode> mInputValues;

	private LinearExpressionDomain mLinExpDomain=new LinearExpressionDomain(new Evaluator());
	private LinearExpression mLinExpOne=new LinearExpression(1);

	private BlockVisitor mBlockVisitor=new BlockVisitor();
	private DominatorTree mDom;
	private LoopTree mLoops;

	private Map<Loop,LoopInfo> mLoopInfo=new HashMap<Loop,LoopInfo>();
	private DecisionExtractor mDecisionExtractor=new DecisionExtractor();
	
	public LoopAnalysis(ControlFlowGraph cfg,
			            Map<ActionNode,DependenceComponent> actionNodeComponents,
			            Set<ValueNode> inputValues) {
		mCfg=cfg;
		mActionNodeComponents=actionNodeComponents;
		mInputValues=inputValues;
	}

	public DominatorTree getDominatorTree() {
		return mDom;
	}
	
	public LoopTree getLoopTree() {
		return mLoops;
	}

	public void analyze() {
		mDom=new DominatorTree(mCfg);
		mLoops=LoopTree.create(mCfg, mDom);
		
		if (mTraceIvAnalysis)
		    mLinExpDomain.setTrace(true);
		DemandContext<LinearExpression> initialValues = createInitialContext();
		Map<BasicBlock,DemandContext<LinearExpression>> outputContexts=
			new HashMap<BasicBlock,DemandContext<LinearExpression>>();
		
		findInductionVariables(mLoops.getRootRegions(), initialValues, outputContexts);
		
		Map<ValueNode,Long> constantValues=new HashMap<ValueNode,Long>();
		for (ValueNode input: mInputValues) {
			LinearExpression linexp=initialValues.get(input);
			if (linexp!=null && linexp.isConstant())
				constantValues.put(input, linexp.getConstant());
		}
		
		for (Region r: mLoops.getRootRegions()) {
			if (r.isLoop()) {
				LoopInfo loopInfo=mLoopInfo.get(r.asLoop());
				loopInfo.computeTripCount(constantValues);
			}
		}
	}
	
	private void findInductionVariables(Loop loop,
			                            DemandContext<LinearExpression> inputContext,
                                        Map<BasicBlock,DemandContext<LinearExpression>> outputContexts) {
		DemandContext<LinearExpression> loopContext=createLoopContext();
		Map<BasicBlock,DemandContext<LinearExpression>> bodyOutput=
			new HashMap<BasicBlock,DemandContext<LinearExpression>>();
		
		if (mTraceIvAnalysis) {
		    System.out.println("\nAnalyzing loop"+loop.getHeader().getIdentifier());
		}
		
		// Find linear expressions in the initial values of the loop,
		// which are common to all back edges
		findInductionVariables(loop.getChildren(), loopContext, bodyOutput);
		
		// Create LoopInfo that contains induction variables and loop exits 
		LoopInfo loopInfo=new LoopInfo(loop, inputContext);		
		mLoopInfo.put(loop, loopInfo);

		DemandContext<LinearExpression> meetOverBackEdges = createInputContext(loop.getHeader(), bodyOutput);
		assert(meetOverBackEdges!=null);
		
		for (ValueNode input: mInputValues) {
			StateLocation location=input.getLocation().asStateLocation();
			assert(location!=null);
				
			if (location.asStateVar()!=null) {
				LinearExpression linexp=meetOverBackEdges.get(input);
				if (linexp!=null) {
					if (mTraceIvAnalysis) {
					    System.out.println(location.asStateVar().getSourceName()+": "+linexp);
					}
					loopInfo.addInductionVariable(input, linexp);
				}
			}
		}
		
		for (Pair<BasicBlock,BasicBlock> pair: loop.getExits()) {
			BasicBlock exitFrom=pair.getFirst();
			BasicBlock exitTo=pair.getSecond();
			DemandContext<LinearExpression> output=bodyOutput.get(exitFrom);
			assert(output!=null);
			
			// Create a loop exit
			LoopInfo.LoopExit loopExit=loopInfo.addLoopExit(exitFrom, exitTo, output);
			
			// Find an iteration condition: linexp relOp linexp (may fail, in which case no iteration condition is set)
			exitFrom.getDecisionTree().accept(mDecisionExtractor, loopExit);
		}
		
		// Create output contexts for each loop exit
		loopInfo.createOutputContexts(outputContexts);
	}
	
	private void findInductionVariables(Iterable<Region> subregions,
			                            DemandContext<LinearExpression> regionInput,
			                            Map<BasicBlock,DemandContext<LinearExpression>> outputContexts) {		
		boolean atEntry=true;
		
		for (Region r: subregions) {
			DemandContext<LinearExpression> inputContext = (atEntry)?
					regionInput : createInputContext(r.getHeader(), outputContexts);
			assert(inputContext!=null);
			
			if (r.isLoop()) {
				findInductionVariables(r.asLoop(), inputContext, outputContexts);
			}
			else {
				BasicBlock b=r.asBasicBlock();

				if (mTraceIvAnalysis) {
				    System.out.println("\nAnalyzing basic block "+b.getIdentifier());
				}
				DemandContext<LinearExpression> output=b.getDecisionTree().accept(mBlockVisitor, inputContext);
				outputContexts.put(b, output);
			}
			
			atEntry=false;
		}
	}

	private DemandContext<LinearExpression> createContext(boolean initialValues) {
		DemandContext<LinearExpression> context = new DemandContext<LinearExpression>(mLinExpDomain);
		
		for (ValueNode node: mInputValues) {
			Location location=node.getLocation();
			LinearExpression linexp=null;
			
			if (location==null || location.hasSource()) {
				// Not an actor port
				
				if (location!=null) {
					XlimStateVar stateVar=location.getSource().asStateVar();
					if (stateVar!=null && mLinExpDomain.supportsType(node.getType())) {
						linexp = (initialValues)?
						    mLinExpDomain.initialState(stateVar.asStateLocation())
						    : new LinearExpression(node);
					}
				}	
			}
			
			context.put(node, linexp);
		}
		return context;
	}
	
	private DemandContext<LinearExpression> createInitialContext() {
		return createContext(true);
	}
	
	private DemandContext<LinearExpression> createLoopContext() {
		return createContext(false);
	}	
	
	private DemandContext<LinearExpression> convertToInputContext(DemandContext<LinearExpression> outputContext, 
			                                                      Map<StateLocation, ValueNode> outputMapping) {
		
		DemandContext<LinearExpression> context=new DemandContext<LinearExpression>(mLinExpDomain);
			
		for (ValueNode input: mInputValues) {
			LinearExpression linexp=null;
			StateLocation location=input.getLocation().asStateLocation();
			assert(location!=null);
				
			if (location.asStateVar()!=null) {
				ValueNode output=outputMapping.get(location);
				if (output==null) {
					// actionNode transparent w.r.t. carrier (use input)
					output=input; 
				}
				assert(output!=null);
					
				linexp=outputContext.get(output);
			}
			context.put(input, linexp);
		}
		
		return context;
	}
	
	private DemandContext<LinearExpression> confluenceOperator(DemandContext<LinearExpression> c1, DemandContext<LinearExpression> c2) {
		DemandContext<LinearExpression> context=new DemandContext<LinearExpression>(mLinExpDomain);
		
		for (ValueNode input: mInputValues) {
			LinearExpression linexp1=c1.get(input);
			LinearExpression linexp2=c2.get(input);
			
			if (linexp1==null)
				linexp1=linexp2;
			else if (linexp2!=null) {
				AbstractValue<LinearExpression> temp=linexp1.intersect(linexp2);
				linexp1=(temp!=null)? temp.getAbstractValue() : null;
			}
			
			context.put(input, linexp1);
		}
		
		return context;
	}
	
	private DemandContext<LinearExpression> createInputContext(BasicBlock b, Map<BasicBlock,DemandContext<LinearExpression>> outputContexts) {
		DemandContext<LinearExpression> inputContext=null;
	
		for (BasicBlock pred: b.getPredecessors()) {
			DemandContext<LinearExpression> output=outputContexts.get(pred);
			
			if (inputContext!=null) {
				if (output!=null)
					inputContext=confluenceOperator(inputContext,output);
				// else: not in same region (defer processing until a common region encloses both blocks)
			}
			else
				inputContext=output;
		}
		
		return inputContext;
	}
	
	private class BlockVisitor implements DecisionTree.Visitor<DemandContext<LinearExpression>, DemandContext<LinearExpression>> {
		@Override
		public DemandContext<LinearExpression> visitActionNode(ActionNode actionNode,
                                                            DemandContext<LinearExpression> context) {
			DependenceComponent comp=mActionNodeComponents.get(actionNode);
			comp.evaluate(context,mLinExpDomain);
			return convertToInputContext(context, actionNode.getOutputMapping());
		}

		@Override
		public DemandContext<LinearExpression> visitDecisionNode(DecisionNode decision,
                                                              DemandContext<LinearExpression> context) {
			ValueNode node=decision.getCondition().getValue();
			context.get(node);
			return context;
		}

		@Override
		public DemandContext<LinearExpression> visitNullNode(NullNode node,
				                                          DemandContext<LinearExpression> context) {
			return context;
		}
	}
	
	private class DecisionExtractor implements DecisionTree.Visitor<Object, LoopInfo.LoopExit> {

		@Override
		public Object visitActionNode(ActionNode action, LoopInfo.LoopExit loopExit) {
			return null;
		}

		@Override
		public Object visitDecisionNode(DecisionNode decision, LoopInfo.LoopExit loopExit) {
			// traverse chains of noops
			XlimOutputPort cond=decision.getCondition().getXlimSource().asOutputPort();
			RelOp relOp=null;
			
			while (cond!=null) {
				String opKind=cond.getParent().getKind();
				if (opKind.equals("noop"))
					cond=cond.getParent().getInputPort(0).getSource().asOutputPort();
				else {
					// Find a relational operator (may fail)
					relOp=findRelOp(opKind);
					break;
				}
			}
			
			if (relOp!=null) {
				XlimInstruction op=cond.getParent();
				ValueNode left=op.getInputPort(0).getValue();
				ValueNode right=op.getInputPort(1).getValue();
				
				// Complement condition if this is a termination condition rather than an iteration condition
				BasicBlock exitTo=loopExit.getExitTo();
				if (decision.getChild(true)==exitTo.getDecisionTree()) {
					relOp=relOp.complement();
				}
				loopExit.setIterationCondition(left, relOp, right);
			}
			return null;
		}

		@Override
		public Object visitNullNode(NullNode node, LoopInfo.LoopExit loopExit) {
			return null;
		}
		
		private RelOp findRelOp(String opKind) {
			if (opKind.equals("$eq"))
				return RelOp.Equal;
			else if (opKind.equals("$ne"))
				return RelOp.NotEqual;
			else if (opKind.equals("$lt"))
				return RelOp.LessThan;
			else if (opKind.equals("$le"))
				return RelOp.LessThanEqual;
			else if (opKind.equals("$gt"))
				return RelOp.GreaterThan;
			else if (opKind.equals("$ge"))
				return RelOp.GreaterThanEqual;
			else
				return null;
		}
	}
	
	private enum RelOp {
		Equal,
		NotEqual,
		LessThan,
		LessThanEqual,
		GreaterThan,
		GreaterThanEqual;
		
		RelOp swapOperands() {
			switch (this) {
			case Equal:
			case NotEqual:
				return this;
			case LessThan:
				return GreaterThan;
			case LessThanEqual:
				return GreaterThanEqual;
			case GreaterThan:
				return LessThan;
			case GreaterThanEqual:
				return LessThanEqual;
			default:
				return null;
			}
		}
		
		RelOp complement() {
			switch (this) {
			case Equal:
				return NotEqual;
			case NotEqual:
				return Equal;
			case LessThan:
				return GreaterThanEqual;
			case LessThanEqual:
				return GreaterThan;
			case GreaterThan:
				return LessThanEqual;
			case GreaterThanEqual:
				return LessThan;
			default:
				return null;
			}
		}
		
		boolean evaluate(long x, long y) {
			switch (this) {
			case Equal:
				return (x==y);
			case NotEqual:
				return (x!=y);
			case LessThan:
				return (x<y);
			case LessThanEqual:
				return (x<=y);
			case GreaterThan:
				return (x>y);
			case GreaterThanEqual:
				return (x>=y);
			default:
				throw new IllegalStateException();
			}
		}
	}
	
	class LoopInfo {
	
		Loop mLoop;
		DemandContext<LinearExpression> mInputContext;
		Map<ValueNode,LinearExpression> mInductionVariables=new HashMap<ValueNode,LinearExpression>();
		List<LoopExit> mLoopExits=new ArrayList<LoopExit>();
		
		LoopInfo(Loop loop, DemandContext<LinearExpression> inputContext) {
			mLoop=loop;
			mInputContext=inputContext;
		}
		
		void addInductionVariable(ValueNode variable, LinearExpression linexp) {
			mInductionVariables.put(variable, linexp);
		}
		
		LinearExpression getInductionVariable(ValueNode variable) {
			return mInductionVariables.get(variable);
		}
				
		LoopExit addLoopExit(BasicBlock from, BasicBlock to, DemandContext<LinearExpression> outputContext) {
			LoopExit loopExit=new LoopExit(from, to, outputContext);
			mLoopExits.add(loopExit);
			return loopExit;
		}
		
		void createOutputContexts(Map<BasicBlock,DemandContext<LinearExpression>> outputContexts) {
			for (LoopExit loopExit: mLoopExits) {
				DemandContext<LinearExpression> output=loopExit.createOutputContext();				
				outputContexts.put(loopExit.getExitFrom(), output);
			}
		}
		
		void computeTripCount(Map<ValueNode, Long> constantInOuter) {
			Map<ValueNode, Long> constantValueAtHeader=new HashMap<ValueNode,Long>();

			if (mLoopExits.size()==1) {
				LoopExit loopExit=mLoopExits.get(0);
				
				if (dominatesBackEdges(loopExit.getExitFrom())) {
					LinearExpression iterationCondition=loopExit.mIterationCondition;
				
					if (iterationCondition!=null && !iterationCondition.isConstant()) {
						ValueNode variable=iterationCondition.getVariable();
						Long initValue=computeInitialValue(variable, constantInOuter);

						if (initValue!=null) {
							Long tripCount=loopExit.computeTripCount(initValue);

							if (tripCount!=null) {
								mLoop.setTripCount(tripCount);

								if (mTrace) {
								    System.out.println("Loop "+mLoop.getHeader().getIdentifier()+" has trip count "+tripCount);
								}
							}
						}
					}
				}
			}
			
			// Determine the values, which are constant at the loop header (same in every iteration)
			for (ValueNode input: mInputValues) {
				Long initValue=computeInitialValue(input, constantInOuter);
				
				if (initValue!=null) {
					LinearExpression inductionFormula=getInductionVariable(input);
					if (inductionFormula==null) {
						initValue=null;     // Unknown value in second iteration
					}
					else if (inductionFormula.isConstant()) {
						if (inductionFormula.getConstant()!=initValue) {
							initValue=null; // Not the same in second iteration
						}
					}
					else if (inductionFormula.getVariable()!=input
							 || !inductionFormula.isIdentity()) {
						initValue=null; // Not (necessarily) the same in second iteration
					}
				}
				
				if (initValue!=null) {
					constantValueAtHeader.put(input, initValue);
					
					if (mTrace) {
					    System.out.println(input.getLocation().getDebugName()+"="+initValue+" at Loop "+mLoop.getHeader().getIdentifier());
					}
				}
			}
			
			// Propagate the constant values to inner loops
			for (Loop inner: mLoop.getInnerLoops()) {
				LoopInfo innerLoopInfo=mLoopInfo.get(inner);
				innerLoopInfo.computeTripCount(constantValueAtHeader);
			}
		}
		
		Long computeInitialValue(ValueNode variable, Map<ValueNode, Long> constantInOuter) {
			LinearExpression linexp=mInputContext.get(variable);
			
			if (linexp!=null) {
				if (linexp.isConstant())
					return linexp.getConstant();
				else {
					Long value=constantInOuter.get(linexp.getVariable());
					if (value!=null 
					    && value>=linexp.getMinimumVariableValue() 
					    && value<=linexp.getMaximumVariableValue()) {
						return value*linexp.getScale() + linexp.getOffset();
					}
				}
			}
			return null;
		}
		
		boolean dominatesBackEdges(BasicBlock exitFrom) {
			for (BasicBlock backEdgeFrom: mLoop.getBackEdges()) {
				if (mDom.dominates(exitFrom, backEdgeFrom)==false)
					return false;
			}
			return true;
		}
		
		class LoopExit {
			
			BasicBlock mExitFrom, mExitTo;
			DemandContext<LinearExpression> mOutputContext;
			LinearExpression mIterationCondition;
			RelOp mRelOp;
			
			LoopExit(BasicBlock exitFrom, BasicBlock exitTo, DemandContext<LinearExpression> outputContext) {
				mExitFrom=exitFrom;
				mExitTo=exitTo;
				mOutputContext=outputContext;
			}
					
			BasicBlock getExitFrom() {
				return mExitFrom;
			}
			
			BasicBlock getExitTo() {
				return mExitTo;
			}
			
			void setIterationCondition(ValueNode leftValue, RelOp relop, ValueNode rightValue) {
				LinearExpression left=mOutputContext.get(leftValue);
				LinearExpression right=mOutputContext.get(rightValue);
				
				if (left==null || right==null)
					return;
				
				// put on the form: linear-expression relop constant
				if (left.isConstant()) {
					LinearExpression temp=left;
					left=right;
					right=temp;
					relop = relop.swapOperands();
				}
				
				LinearExpression inductionFormula=getInductionVariable(left.getVariable());
				
				if (right.isConstant() && inductionFormula!=null) {
					// embed constant into the linear expression: new-linexp relop 0
					left=left.subtract(right).getAbstractValue();
					
					// Normalize relational operator: use only Equal, NotEqual and LessThanEqual
					switch (relop) {
					case GreaterThan:
						left=left.negate().getAbstractValue();        // left > 0 --> -left < 0
						// fall through
					case LessThan:
						relop=RelOp.LessThanEqual;
						left=left.add(mLinExpOne).getAbstractValue(); // left < 0 --> left+1 <= 0
						break;
					case GreaterThanEqual:
						relop=RelOp.LessThanEqual;
						left=left.negate().getAbstractValue();        // left >= 0 --> -left <= 0
						break;
					}
					
					mRelOp=relop;
					mIterationCondition=left;
				
					if (mTrace) {
					    System.out.println("Loop iteration condition: "+mIterationCondition+" "+mRelOp+" 0");
					}
				}
			}
			
			/**
			 * @param variable      a variable
			 * @param valueAtExit   a linear expression
			 * @return true if the linear expression is the identity function of the variable
			 *         regardless of the number of loop iterations.
			 */
			boolean isLoopInvariant(ValueNode variable, LinearExpression valueAtExit) {
				
				if (valueAtExit!=null
				    && valueAtExit.getVariable()==variable
				    && valueAtExit.isIdentity()) {
					LinearExpression inductionFormula=mInductionVariables.get(variable);
				
					if (inductionFormula!=null 
						&& inductionFormula.getVariable()==variable
						&& inductionFormula.isIdentity()) {
						
						return true;
					}
				}
				return false;
			}

			DemandContext<LinearExpression> createOutputContext() {
				DemandContext<LinearExpression> outputContext=new DemandContext<LinearExpression>(mLinExpDomain);
				
				for (ValueNode input: mInputValues) {
					StateLocation location=input.getLocation().asStateLocation();
					LinearExpression linexpOuter=null;
					assert(location!=null);
						
					if (location.asStateVar()!=null) {
						LinearExpression linexpInner=mOutputContext.get(input);
						
						if (linexpInner!=null) {
							if (linexpInner.isConstant())
								linexpOuter=linexpInner; // Copy constant values to outer loop
							else if (isLoopInvariant(input, linexpInner)) {
								// Use initial value from outer loop as output
								linexpOuter = mInputContext.get(input);
							}
						}						
					}
					outputContext.put(input, linexpOuter);
				}
				return outputContext;
			}
			
			Long computeTripCount(long initValue) {
				assert(mIterationCondition!=null && mIterationCondition.isConstant()==false);
				long scale=mIterationCondition.getScale();
				long offset=mIterationCondition.getOffset();
				
				// Check that first iteration is taken/may be taken
				if (mIterationCondition.getMinimumVariableValue()>initValue
					|| mIterationCondition.getMaximumVariableValue()<initValue) {
					// Iteration condition is not valid for the initial value (=we don't know anything)
					return null;
				}
				else if (mRelOp.evaluate(scale*initValue + offset, 0)==false) {
					// The second iteration will never be taken
					return 0L;
				}
				
				ValueNode variable=mIterationCondition.getVariable();
				LinearExpression inductionFormula=mInductionVariables.get(variable);
				
				if (inductionFormula==null
					|| inductionFormula.getVariable()!=variable
					|| inductionFormula.getScale()!=1
					|| inductionFormula.getOffset()==0
					|| inductionFormula.getMinimumVariableValue()>initValue
					|| inductionFormula.getMaximumVariableValue()<initValue) {
					// Induction formula doesn't satisfy our assumptions
					// TODO: it's possible to relax some of the assumptions
					return null;
				}
				
				long delta=inductionFormula.getOffset();
				long denom=-scale*initValue-offset;
				long scaleDelta=scale*delta;
				long lastT = divFloor(denom, scaleDelta);
				
				switch (mRelOp) {
				case LessThanEqual:
					// scale*x + offset = scale*(x0 + delta*t) + offset <= 0
					if (scaleDelta<=0)
						return null; // Unbounded or indeterminate (given valid range of variable)
					break;
					
				case NotEqual:
					// scale*x + offset = scale*(x0 + delta*t) + offset != 0
					if (lastT<=0 || scaleDelta*lastT != denom)
						return null; // Unbounded or indeterminate (given valid range of variable)
					// else:  scale*delta divides denom=-scale*initValue-offset
					lastT=lastT-1;  // last t, which satisfies iteration condition
					break;
					
				case Equal:
					// scale*x0 + offset = 0, scale*(x0 + delta) + offset != 0, since scale,delta!=0
					lastT=0; // t==0 satisfies iteration condition
					
				default:
					throw new IllegalStateException("LessThan, Equal or NotEqual expected");
				}
				
				// Finally verify that lastT is within valid range of induction formula
				// and lastT+1 is within valid range of iteration condition
				double lastX=initValue + delta*lastT;
				if (inductionFormula.getMinimumVariableValue()>lastX
					|| inductionFormula.getMaximumVariableValue()<lastX
					|| mIterationCondition.getMinimumVariableValue()>lastX+delta
					|| mIterationCondition.getMaximumVariableValue()<lastX+delta) {
					return null; // Indeterminate trip-count (variable outside valid range)
				}
				else {
					return lastT+1;
				}
			}
			
			long divFloor(long x, long y) {
				if ((x<0) != (y<0)) {
					// negative quotients are rounded upwards
					return (x - Math.abs(y) + 1)/y;
				}
				else
					return x/y; // positive quotients are rounded downwards already
			}
		}
	}		
}
