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

package eu.actorsproject.xlim.xlim2c;

import eu.actorsproject.xlim.XlimInputPort;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimStateVar;


import eu.actorsproject.xlim.util.OperationHandler;
import eu.actorsproject.xlim.util.OperationPlugIn;

public class OperationGenerators implements TaskGeneratorPlugIn {

	protected OperationPlugIn<OperationGenerator> mGenerators;
	
	private static OperationGenerator sGenerators[] = {
		new PortOperationGenerator("pinRead","pinRead"),
		new PortOperationGenerator("pinWrite","pinWrite"),
		new PortOperationGenerator("pinPeek","pinPeek"),
		new PortOperationGenerator("pinStatus","pinStatus"),
		new VarRefGenerator("var_ref"),
		new AssignGenerator("assign"),
		new TaskCallGenerator("taskCall"),
		new LiteralIntegerGenerator("$literal_Integer"),
		new OperatorGenerator("$add", "+"),
		new OperatorGenerator("$and", " && "),
		new BitwiseGenerator("bitand", " & "),
		new DivAndShiftGenerator("$div", "/"),
		new OperatorGenerator("$mul","*"),
		new OperatorGenerator("$or"," || "),
		new BitwiseGenerator("bitor"," | "),
		new OperatorGenerator("$sub","-"),
		new BitwiseGenerator("bitxor"," ^ "),
		new OperatorGenerator("lshift","<<"),
		new DivAndShiftGenerator("rshift",">>"),
		new RelOpGenerator("$eq","=="),
		new RelOpGenerator("$ge",">="),
		new RelOpGenerator("$gt",">"),
		new RelOpGenerator("$le","<="),
		new RelOpGenerator("$lt","<"),
		new RelOpGenerator("$ne","!="),
		new PrefixOperatorGenerator("$negate","-"),
		new PrefixOperatorGenerator("$not","!"),
		new PrefixOperatorGenerator("bitnot","~"),
		new UrshiftGenerator("urshift"),
		new NoopGenerator("noop"),
		new NoopGenerator("cast"),
		new SelectorGenerator("$selector"),
		new PortOperationGenerator("pinAvail","pinAvail"),
		new PinWaitGenerator("pinWait","pinWait"),
		new SignExtendGenerator("signExtend")
	};
	
	public OperationGenerators() {
		// No default handler -throws exception for unhandled operations
		mGenerators=new OperationPlugIn<OperationGenerator>(null); 
		for (OperationGenerator generator: sGenerators)
			mGenerators.registerHandler(generator.getOperationKind(), generator);
	}
	
	public boolean hasGenerateExpression(XlimOperation op) {
		OperationGenerator generator=mGenerators.getOperationHandler(op);
		return generator.hasGenerateExpression();
	}
	
	public boolean reEvaluate(XlimOperation op) {
		OperationGenerator generator=mGenerators.getOperationHandler(op);
		return generator.reEvaluate();
	}
	
	public void generateExpression(XlimOperation op, ExpressionTreeGenerator treeGenerator) {
		OperationGenerator generator=mGenerators.getOperationHandler(op);
		generator.generateExpression(op,treeGenerator);
	}
	
	public void generateStatement(XlimOperation op, ExpressionTreeGenerator treeGenerator) {
		OperationGenerator generator=mGenerators.getOperationHandler(op);
		generator.generateStatement(op,treeGenerator);
	}
}

abstract class OperationGenerator implements OperationHandler {
	private String mOpKind;
	
	public OperationGenerator(String opKind) {
		mOpKind=opKind;
	}
	
	/**
	 * @return XLIM operation kind that is supported by the handler 
	 */
	public String getOperationKind() {
		return mOpKind;
	}

	/**
	 * @param op XLIM operation
	 * @return true iff handler supports this operation (which is guaranteed to be of appropriate kind)
	 * This predicate can be used to further narrow down the set of supported operations.
	 * It is for instance possible to only support a particular type, arity etc.
	 */
	public boolean supports(XlimOperation op) {
		return true;
	}

	
	/**
	 * @return true if this handler supports generateExpression() in addition to generateStatement()
	 * 
	 * The effect of returning "false" is that the generateStatement() method will be used (rather than
	 * generateExpression()) and a temporary variable will be allocated for each of the output ports.  
	 */
	
	public boolean hasGenerateExpression() {
		return false;
	}
	
	/**
	 * @return true if it's possible and preferable to re-evaluate the root of the expression tree
	 * 
	 *  The effect of returning "true" is that generateExpression() will be used multiple times 
	 *  rather than introducing a temporary variable (implies hasGenerateExpression). 
	 */
	public boolean reEvaluate() {
		return false;
	}
	
	/**
	 * @param op       the operation at the root of the tree (guaranteed to be supported by the handler)
	 * @param gen      operation generator, which provides the context of the expression tree
	 * Generates C code for an expression tree 
	 */
	public void generateExpression(XlimOperation op, ExpressionTreeGenerator gen) {
		throw new UnsupportedOperationException("generateExpression");
	}
	
	/**
	 * @param op       the operation at the root of the tree (guaranteed to be supported by the handler)
	 * @param gen      operation generator, which provides the context of the expression tree
	 * 
	 * Generates a C statement, which corresponds to the expression tree
	 * default implementation is to generate an assignment to a single output port
	 * thus needs to be overridden for operations with zero or more than one output port
	 */
	public void generateStatement(XlimOperation op, ExpressionTreeGenerator gen) {
		if (op.getNumOutputPorts()!=1)
			throw new IllegalArgumentException("Unhandled multiple outputs in "+op.toString());
		gen.print(op.getOutputPort(0));
		gen.print("=");
		generateExpression(op,gen);
	}
	
	/*
	 * Methods that get the type right
	 */
	protected static String signedCast(int size) {
		if (size==1)
			return "(bool_t)";
		else
			return "(int"+size+"_t)";
	}
			
	protected static String unsignedCast(int size) {
		if (size==1)
			return "(bool_t)";
		else
			return "(uint"+size+"_t)";
	}
	
	protected static int getSize(XlimInputPort input) {
		return input.getSource().getSourceType().getSize();
	}
}

class PortOperationGenerator extends OperationGenerator {
	
	protected String mApiCall;
	
	public PortOperationGenerator(String opKind, String apiCall) {
		super(opKind);
		mApiCall=apiCall;
	}
				
	public boolean hasGenerateExpression() {
		return false;
	}
	
	@Override
	public void generateStatement(XlimOperation op, ExpressionTreeGenerator gen) {
		if (op.getNumOutputPorts()>1)
			throw new IllegalArgumentException("Unhandled multiple outputs in "+op.toString());
		if (op.getNumOutputPorts()==1) {
			gen.print(op.getOutputPort(0));
			gen.print("=");
		}
		gen.print(mApiCall+"(&");
		gen.print(op.getPortAttribute());
		for (int i=0; i<op.getNumInputPorts(); ++i) {
			gen.print(",");
			gen.translateSubTree(op.getInputPort(i));
		}
		gen.print(")");
	}
}

class PinWaitGenerator extends OperationGenerator {

	private String mApiCall;
	
	public PinWaitGenerator(String opKind, String apiCall) {
		super(opKind);
		mApiCall=apiCall;
	}
	
	public boolean hasGenerateExpression() {
		return true;
	}
	
	@Override
	public void generateStatement(XlimOperation op, ExpressionTreeGenerator gen) {
		assert(op.getNumOutputPorts()==0 && op.getNumOutputPorts()==0);
		gen.print(mApiCall+"(&");
		gen.print(op.getPortAttribute());
		gen.print(",");
		Long numTokens=op.getIntegerValueAttribute();
		if (numTokens!=1)
			gen.print(numTokens.toString()+"*");
		// TODO: Probably a good idea to represent ports with smallest possible integral type
		gen.print("sizeof(int))");
		if (op.hasBlockingStyle()) {
			gen.print("; return");
		}
			
	}
}

class VarRefGenerator extends OperationGenerator {

	public VarRefGenerator(String opKind) {
		super(opKind);
	}
	
	@Override
	public boolean supports(XlimOperation op) {
		return (op.getNumInputPorts()==1 && op.getNumOutputPorts()==1);
	}
	
	@Override
	public boolean hasGenerateExpression() {
		return true;
	}
	
	@Override
	public void generateExpression(XlimOperation op, ExpressionTreeGenerator gen) {
		XlimStateVar stateVar=op.getStateVarAttribute();
		int length=stateVar.getInitValue().totalNumberOfElements();
		gen.print(stateVar);
		gen.print("[RANGECHK(");
		gen.translateSubTree(op.getInputPort(0));
		gen.print("," + length + ")]");
	}		
}

class AssignGenerator extends OperationGenerator {
	
	public AssignGenerator(String opKind) {
		super(opKind);
	}
	
	@Override
	public boolean supports(XlimOperation op) {
		return ((op.getNumInputPorts()==1 || op.getNumInputPorts()==2) && op.getNumOutputPorts()==0);
	}
	
	@Override
	public void generateStatement(XlimOperation op, ExpressionTreeGenerator gen) {
		XlimStateVar stateVar=op.getStateVarAttribute();
		int dataPort;
		
		gen.print(stateVar);
		if (op.getNumInputPorts()>1) {
			int length=stateVar.getInitValue().totalNumberOfElements();

			gen.print("[RANGECHK(");
			gen.translateSubTree(op.getInputPort(0));
			gen.print("," + length + ")]");
			dataPort=1;
		}
		else
			dataPort=0;
		gen.print("=");
		gen.translateSubTree(op.getInputPort(dataPort));
	}
}

class TaskCallGenerator extends OperationGenerator {
	
	public TaskCallGenerator(String opKind) {
		super(opKind);
	}
	
	@Override
	public boolean supports(XlimOperation op) {
		return (op.getNumInputPorts()==0 && op.getNumOutputPorts()==0);
	}
		
	@Override
	public boolean hasGenerateExpression() {
		return false;
	}
			
	@Override
	public void generateStatement(XlimOperation op, ExpressionTreeGenerator gen) {
		gen.print(op.getTaskAttribute());
	}
}

class LiteralIntegerGenerator extends OperationGenerator {
	
	public LiteralIntegerGenerator(String opKind) {
		super(opKind);
	}
	
	@Override
	public boolean supports(XlimOperation op) {
		return (op.getNumInputPorts()==0 && op.getNumOutputPorts()==1);
	}
	
	@Override
	public boolean hasGenerateExpression() {
		return true;
	}
	
	@Override
	public boolean reEvaluate() {
		return true;
	}
	
	@Override
	public void generateExpression(XlimOperation op, ExpressionTreeGenerator gen) {
		Long value=op.getIntegerValueAttribute();
		String suffix=(value>Integer.MAX_VALUE || value<Integer.MIN_VALUE)? "L" : "";
		gen.print(value.toString()+suffix);
	}		
}


abstract class ExpressionGenerator extends OperationGenerator {
	
	public ExpressionGenerator(String opKind) {
		super(opKind);
	}

	@Override
	public boolean supports(XlimOperation op) {
		return (op.getNumInputPorts()>=1 && op.getNumOutputPorts()==1);
	}
	
	@Override
	public boolean hasGenerateExpression() {
		return true;
	}

	@Override
	public void generateExpression(XlimOperation op, ExpressionTreeGenerator gen) {
		int opSize=operationSize(op);
		int declaredSize=op.getOutputPort(0).getType().getSize();
		if (opSize!=declaredSize) {
			gen.print("("+signedCast(declaredSize)+" ");
			generateExpression(op, opSize, gen);
			gen.print(")");
		}
		else
			generateExpression(op, opSize, gen);
	}

	protected abstract int operationSize(XlimOperation op);

	protected abstract void generateExpression(XlimOperation op, int opSize, ExpressionTreeGenerator gen);
	
	protected void translateSubTree(XlimInputPort input, int opSize, ExpressionTreeGenerator gen) {
		int inputSize=getSize(input);
		if (inputSize!=opSize) {
			gen.print("("+signedCast(opSize)+" ");
			gen.translateSubTree(input);
			gen.print(")");
		}
		else
			gen.translateSubTree(input);
	}	
}

class OperatorGenerator extends ExpressionGenerator {
	
	protected String mOperator;
	
	public OperatorGenerator(String opKind, String operator) {
		super(opKind);
		mOperator=operator;
	}
		
	@Override
	public void generateExpression(XlimOperation op, int opSize, ExpressionTreeGenerator gen) {
		if (op.getNumInputPorts()==1) {
			gen.print("COPY(");
			translateSubTree(op.getInputPort(0), opSize, gen);
			gen.print(" /* "+mOperator+" */)");
		}
		else {
			translateSubTree(op.getInputPort(0),opSize,gen);
			for (int i=1; i<op.getNumInputPorts(); ++i) {
				gen.print(mOperator);
				translateSubTree(op.getInputPort(i),opSize,gen);
			}
		}
	}

	@Override
	protected int operationSize(XlimOperation op) {
		return op.getOutputPort(0).getType().getSize();
	}
}

class BitwiseGenerator extends OperatorGenerator {
		
	public BitwiseGenerator(String opKind, String operator) {
		super(opKind, operator);
	}

	protected void translateSubTree(XlimInputPort input, int opSize, ExpressionTreeGenerator gen) {
		int inputSize=getSize(input);
		if (inputSize!=opSize) {
			String cast=(inputSize<opSize)? unsignedCast(inputSize) : signedCast(opSize);
			gen.print("("+cast+" ");
			gen.translateSubTree(input);
			gen.print(")");
		}
		else
			gen.translateSubTree(input);
	}	
}

class RelOpGenerator extends OperatorGenerator {

	public RelOpGenerator(String opKind, String operator) {
		super(opKind,operator);
	}

	@Override
	public boolean supports(XlimOperation op) {
		return (op.getNumInputPorts()==2 && op.getNumOutputPorts()==1);
	}
	
	@Override
	public void generateExpression(XlimOperation op, ExpressionTreeGenerator gen) {
		// Override since we don't want to cast the result (is and should be bool)
		generateExpression(op, operationSize(op), gen);
	}

	// operationSize refers to the inputs
	@Override
    protected int operationSize(XlimOperation op) {
		return Math.max(getSize(op.getInputPort(0)), getSize(op.getInputPort(1)));
    }
}

class DivAndShiftGenerator extends OperatorGenerator {
	
	public DivAndShiftGenerator(String opKind, String operator) {
		super(opKind,operator);
	}
	
	@Override
	public boolean supports(XlimOperation op) {
		return (op.getNumInputPorts()==2 && op.getNumOutputPorts()==1);
	}
	
	@Override
	public void generateExpression(XlimOperation op, int opSize, ExpressionTreeGenerator gen) {
		translateSubTree(op.getInputPort(0), opSize, gen);
		gen.print(mOperator);
		gen.translateSubTree(op.getInputPort(1));
	}
	
	// operationSize determined by the first input
	@Override
    protected int operationSize(XlimOperation op) {
		return getSize(op.getInputPort(0));
	}
}

class UrshiftGenerator extends DivAndShiftGenerator {
	
	public UrshiftGenerator(String opKind) {
		super(opKind,">>");
	}
	
	@Override
	public void generateExpression(XlimOperation op, int opSize, ExpressionTreeGenerator gen) {
		gen.print("("+unsignedCast(opSize));
		gen.translateSubTree(op.getInputPort(0));
		gen.print(">>");
		gen.translateSubTree(op.getInputPort(1));
		gen.print(")");
	}
}

class PrefixOperatorGenerator extends OperatorGenerator {
	
	public PrefixOperatorGenerator(String opKind, String operator) {
		super(opKind, operator);
	}
	
	@Override
	public boolean supports(XlimOperation op) {
		return (op.getNumInputPorts()==1 && op.getNumOutputPorts()==1);
	}
		
	@Override
	public void generateExpression(XlimOperation op, int opSize, ExpressionTreeGenerator gen) {
		gen.print(mOperator);
		translateSubTree(op.getInputPort(0), opSize, gen);
	}
}

class NoopGenerator extends PrefixOperatorGenerator {
	
	public NoopGenerator(String opKind) {
		super(opKind, "");
	}
	
	@Override
	public void generateExpression(XlimOperation op, int opSize, ExpressionTreeGenerator gen) {
		translateSubTree(op.getInputPort(0), opSize, gen);
	}
}

class SignExtendGenerator extends PrefixOperatorGenerator {
	public SignExtendGenerator(String opKind) {
		super(opKind, "signExtend");
	}
		
	@Override
	public void generateExpression(XlimOperation op, int opSize, ExpressionTreeGenerator gen) {
		int fromSize=(int)(long)op.getIntegerValueAttribute();
		int shifts=opSize-fromSize;
		assert(shifts>=0);
		translateSubTree(op.getInputPort(0), opSize, gen);
		gen.print("<<"+shifts+">>"+shifts);
	}
}

class SelectorGenerator extends ExpressionGenerator {
	
	public SelectorGenerator(String opKind) {
		super(opKind);
	}
	
	@Override
	public boolean supports(XlimOperation op) {
		return (op.getNumInputPorts()==3 && op.getNumOutputPorts()==1);
	}
		
	@Override
	public void generateExpression(XlimOperation op, int opSize, ExpressionTreeGenerator gen) {
		translateSubTree(op.getInputPort(0),1 /*bool*/, gen);
		gen.print("? ");
		translateSubTree(op.getInputPort(1), opSize, gen);
		gen.print(" : ");
		translateSubTree(op.getInputPort(2), opSize, gen);
	}
	
	@Override
	protected int operationSize(XlimOperation op) {
		// max of "then"/"else" expressions
		return Math.max(getSize(op.getInputPort(1)), getSize(op.getInputPort(2)));
	}
}