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
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.XlimType;

import eu.actorsproject.xlim.codegenerator.ExpressionTreeGenerator;
import eu.actorsproject.xlim.codegenerator.OperationGenerator;
import eu.actorsproject.xlim.decision.BlockingCondition;
import eu.actorsproject.xlim.decision.YieldAttribute;
import eu.actorsproject.xlim.type.TypeFactory;
import eu.actorsproject.xlim.util.LiteralPattern;
import eu.actorsproject.xlim.util.OperationHandler;
import eu.actorsproject.xlim.util.OperationPlugIn;
import eu.actorsproject.xlim.util.Session;

public class Operation2c implements OperationGenerator {

	protected OperationPlugIn<BasicGenerator> mGenerators;
	
	private static BasicGenerator sGenerators[] = {
		new TypedPortOperationGenerator("pinRead","pinRead",true),
		new TypedPortOperationGenerator("pinWrite","pinWrite",false),
		new PinPeekFrontGenerator("pinPeek", "pinPeekFront", true),
		new TypedPortOperationGenerator("pinPeek","pinPeek",true),
		new PinStatusGenerator("pinStatus","pinStatus",true),
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
		new PinAvailGenerator("pinAvail","pinAvail",true),
		new YieldGenerator("yield"),
		new SignExtendGenerator("signExtend")
	};
	
	public Operation2c() {
		// No default handler -throws exception for unhandled operations
		mGenerators=new OperationPlugIn<BasicGenerator>(null); 
		for (BasicGenerator generator: sGenerators)
			register(generator);
	}
		
	protected void register(BasicGenerator generator) {
		mGenerators.registerHandler(generator.getOperationKind(), generator);
	}
	
	@Override
	public boolean hasGenerateExpression(XlimOperation op) {
		BasicGenerator generator=mGenerators.getOperationHandler(op);
		return generator.hasGenerateExpression();
	}
	
	@Override
	public boolean reEvaluate(XlimOperation op) {
		BasicGenerator generator=mGenerators.getOperationHandler(op);
		return generator.reEvaluate();
	}
	
	@Override
	public void generateExpression(XlimOperation op, ExpressionTreeGenerator treeGenerator) {
		BasicGenerator generator=mGenerators.getOperationHandler(op);
		generator.generateExpression(op,treeGenerator);
	}
	
	@Override
	public void generateStatement(XlimOperation op, ExpressionTreeGenerator treeGenerator) {
		BasicGenerator generator=mGenerators.getOperationHandler(op);
		generator.generateStatement(op,treeGenerator);
	}
}

abstract class BasicGenerator implements OperationHandler {
	private String mOpKind;
	
	public BasicGenerator(String opKind) {
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
	 *  rather than introducing a BasicGeneratortemporary variable (implies hasGenerateExpression). 
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
}

/**
 * Generic support for operations realized as API calls
 */
class ApiCallGenerator extends BasicGenerator {
	
	protected String mFunctionName;
	private boolean mHasGenerateExpression;
	
	public ApiCallGenerator(String opKind, 
			                String functionName, 
			                boolean hasGenerateExpression) {
		super(opKind);
		mFunctionName=functionName;
		mHasGenerateExpression=hasGenerateExpression;
	}
	
	protected String getFunctionName(XlimOperation op, ExpressionTreeGenerator gen) {
		return mFunctionName;
	}
	
	@Override
	public boolean hasGenerateExpression() {
		return mHasGenerateExpression;
	}
	
	@Override
	public void generateExpression(XlimOperation op, ExpressionTreeGenerator gen) {
		gen.print(getFunctionName(op, gen)+"(");
		generateArguments(op, gen);
		gen.print(")");
	}
	
	@Override
	public void generateStatement(XlimOperation op, ExpressionTreeGenerator gen) {
		if (op.getNumOutputPorts()>1)
			throw new IllegalArgumentException("Unhandled multiple outputs in "+op.toString());
		if (op.getNumOutputPorts()==1) {
			gen.print(op.getOutputPort(0));
			gen.print("=");
		}
		generateExpression(op,gen);
	}
	
	protected void generateArguments(XlimOperation op, ExpressionTreeGenerator gen) {
		for (int i=0; i<op.getNumInputPorts(); ++i) {
			if (i!=0)
				gen.print(", ");
			gen.translateSubTree(op.getInputPort(i));
		}
	}
}

/**
 * Operations on ports (realized as API calls)
 */
class PortOperationGenerator extends ApiCallGenerator {
	
	public PortOperationGenerator(String opKind, 
			                      String functionName,
			                      boolean hasGenerateExpression) {
		super(opKind, functionName, hasGenerateExpression);
	}
	
	protected String getTypeSuffix(XlimOperation op, ExpressionTreeGenerator gen) {
		XlimType t=op.getPortAttribute().getType();
		return gen.getTargetTypeName(t);
	}

	protected String getDirectionSuffix(XlimOperation op) {
		XlimTopLevelPort port=op.getPortAttribute();
		return (port.getDirection()==XlimTopLevelPort.Direction.in)? "In" : "Out";
	}

	@Override
	protected void generateArguments(XlimOperation op, ExpressionTreeGenerator gen) {
		// Add the port attribute as first argument
		gen.print(op.getPortAttribute());
		if (op.getNumInputPorts()!=0)
			gen.print(", ");
		super.generateArguments(op, gen);
	}
}

/**
 * Port operations that add a type suffix to the function name
 */
class TypedPortOperationGenerator extends PortOperationGenerator {
	
	public TypedPortOperationGenerator(String opKind, 
                                       String functionName,
                                       boolean hasGenerateExpression) {
		super(opKind, functionName, hasGenerateExpression);
	}
	
	@Override
	protected String getFunctionName(XlimOperation op, ExpressionTreeGenerator gen) {
		return mFunctionName + "_" + getTypeSuffix(op,gen);
	}	
}

class PinStatusGenerator extends PortOperationGenerator {

	public PinStatusGenerator(String opKind, String functionName, boolean hasGenerateExpression) {
		super(opKind, functionName, hasGenerateExpression);
	}

	@Override
	protected String getFunctionName(XlimOperation op, ExpressionTreeGenerator gen) {
		return mFunctionName+getDirectionSuffix(op);
	}
}

class PinAvailGenerator extends TypedPortOperationGenerator {

	public PinAvailGenerator(String opKind, String functionName, boolean hasGenerateExpression) {
		super(opKind, functionName, hasGenerateExpression);
	}

	@Override
	protected String getFunctionName(XlimOperation op, ExpressionTreeGenerator gen) {
		return mFunctionName+getDirectionSuffix(op)+"_"+getTypeSuffix(op,gen);
	}
}

class PinPeekFrontGenerator extends TypedPortOperationGenerator {

	private LiteralPattern zero=new LiteralPattern(0);
	
	public PinPeekFrontGenerator(String opKind, String functionName, boolean hasGenerateExpression) {
		super(opKind, functionName, hasGenerateExpression);
	}

	/**
	 * @param op XLIM operation
	 * @return true for the special case pinPeek(port; 0) =peeking at the front of the fifo 
	 */
	public boolean supports(XlimOperation op) {
		return zero.matches(op.getInputPort(0).getSource());
	}
	
	@Override
	protected void generateArguments(XlimOperation op, ExpressionTreeGenerator gen) {
		// The port attribute is the only argument (zero input is implicit)
		gen.print(op.getPortAttribute());
	}
}

class VarRefGenerator extends BasicGenerator {

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

class AssignGenerator extends BasicGenerator {
	
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

class TaskCallGenerator extends BasicGenerator {
	
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

class LiteralIntegerGenerator extends BasicGenerator {
	
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
		XlimType t=op.getOutputPort(0).getType();
		String suffix=(t.isInteger() && t.getSize()>32)? "L" : "";
		gen.print(op.getValueAttribute()+suffix);
	}		
}


abstract class ExpressionGenerator extends BasicGenerator {
	
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
		XlimType opType=operationType(op);
		XlimType declaredType=op.getOutputPort(0).getType();
		if (opType!=declaredType) {
			gen.print("(" + getCast(declaredType, gen));
			generateExpression(op, opType, gen);
			gen.print(")");
		}
		else
			generateExpression(op, opType, gen);
	}

	protected abstract XlimType operationType(XlimOperation op);

	protected abstract void generateExpression(XlimOperation op, 
			                                   XlimType opType, 
			                                   ExpressionTreeGenerator gen);
	
	protected void translateSubTree(XlimInputPort input, XlimType opType, ExpressionTreeGenerator gen) {
		XlimType inputType=input.getSource().getSourceType();
		if (inputType!=opType) {
			gen.print("(" + getCast(opType,gen));
			gen.translateSubTree(input);
			gen.print(")");
		}
		else
			gen.translateSubTree(input);
	}	
	
	protected String getCast(XlimType toType, ExpressionTreeGenerator gen) {
		String typeName=gen.getTargetTypeName(toType);
		return "(" + typeName + ") ";
	}
	
	protected String getUnsignedCast(int size) {
		return "(uint"+size+"_t) ";
	}
}

class OperatorGenerator extends ExpressionGenerator {
	
	protected String mOperator;
	
	public OperatorGenerator(String opKind, String operator) {
		super(opKind);
		mOperator=operator;
	}
		
	@Override
	public void generateExpression(XlimOperation op, XlimType opType, ExpressionTreeGenerator gen) {
		if (op.getNumInputPorts()==1) {
			gen.print("COPY(");
			translateSubTree(op.getInputPort(0), opType, gen);
			gen.print(" /* "+mOperator+" */)");
		}
		else {
			translateSubTree(op.getInputPort(0),opType,gen);
			for (int i=1; i<op.getNumInputPorts(); ++i) {
				gen.print(mOperator);
				translateSubTree(op.getInputPort(i),opType,gen);
			}
		}
	}

	@Override
	protected XlimType operationType(XlimOperation op) {
		return op.getOutputPort(0).getType();
	}
}

class BitwiseGenerator extends OperatorGenerator {
		
	public BitwiseGenerator(String opKind, String operator) {
		super(opKind, operator);
	}

	protected void translateSubTree(XlimInputPort input, XlimType opType, ExpressionTreeGenerator gen) {
		XlimType inputType=input.getSource().getSourceType();
		if (inputType!=opType) {
			int inputSize=inputType.getSize();
			String cast=(inputSize<opType.getSize())? 
					getUnsignedCast(inputSize) : getCast(opType, gen); 
			gen.print("(" + cast);
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
		generateExpression(op, operationType(op), gen);
	}

	// operationSize refers to the inputs
	@Override
    protected XlimType operationType(XlimOperation op) {
		TypeFactory fact=Session.getTypeFactory();
		XlimType t1=op.getInputPort(0).getSource().getSourceType();
		XlimType t2=op.getInputPort(1).getSource().getSourceType();
		return fact.leastUpperBound(t1, t2);
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
	public void generateExpression(XlimOperation op, XlimType opType, ExpressionTreeGenerator gen) {
		translateSubTree(op.getInputPort(0), opType, gen);
		gen.print(mOperator);
		gen.translateSubTree(op.getInputPort(1));
	}
	
	// operationSize determined by the first input
	@Override
    protected XlimType operationType(XlimOperation op) {
		return op.getInputPort(0).getSource().getSourceType();
	}
}

class UrshiftGenerator extends DivAndShiftGenerator {
	
	public UrshiftGenerator(String opKind) {
		super(opKind,">>");
	}
	
	@Override
	public void generateExpression(XlimOperation op, XlimType opType, ExpressionTreeGenerator gen) {
		int inputSize=op.getInputPort(0).getSource().getSourceType().getSize();
		
		gen.print("(" + getCast(opType, gen) + " ");
		gen.print("("+getUnsignedCast(inputSize));
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
	public void generateExpression(XlimOperation op, XlimType opType, ExpressionTreeGenerator gen) {
		gen.print(mOperator);
		translateSubTree(op.getInputPort(0), opType, gen);
	}
}

class NoopGenerator extends PrefixOperatorGenerator {
	
	public NoopGenerator(String opKind) {
		super(opKind, "");
	}
	
	@Override
	public void generateExpression(XlimOperation op, XlimType opType, ExpressionTreeGenerator gen) {
		translateSubTree(op.getInputPort(0), opType, gen);
	}
}

class SignExtendGenerator extends PrefixOperatorGenerator {
	public SignExtendGenerator(String opKind) {
		super(opKind, "signExtend");
	}
		
	@Override
	public void generateExpression(XlimOperation op, XlimType opType, ExpressionTreeGenerator gen) {
		int fromSize=(int)(long)op.getIntegerValueAttribute();
		int shifts=opType.getSize()-fromSize;
		assert(shifts>=0);
		translateSubTree(op.getInputPort(0), opType, gen);
		gen.print("<<"+shifts+">>"+shifts);
	}
}

class SelectorGenerator extends ExpressionGenerator {
	
	private XlimType mBoolType;
	
	public SelectorGenerator(String opKind) {
		super(opKind);
		mBoolType=Session.getTypeFactory().create("bool");
	}
	
	@Override
	public boolean supports(XlimOperation op) {
		return (op.getNumInputPorts()==3 && op.getNumOutputPorts()==1);
	}
		
	@Override
	public void generateExpression(XlimOperation op, XlimType opType, ExpressionTreeGenerator gen) {
		translateSubTree(op.getInputPort(0),mBoolType, gen);
		gen.print("? ");
		translateSubTree(op.getInputPort(1), opType, gen);
		gen.print(" : ");
		translateSubTree(op.getInputPort(2), opType, gen);
	}
	
	@Override
	protected XlimType operationType(XlimOperation op) {
		return op.getOutputPort(0).getType();
	}
}

class YieldGenerator extends BasicGenerator {
	
	public YieldGenerator(String opKind) {
		super(opKind);
	}

	@Override
	public void generateStatement(XlimOperation op, ExpressionTreeGenerator gen) {
		YieldAttribute attribute=(YieldAttribute) op.getGenericAttribute();
		String exitCode=gen.getGenericAttribute(attribute);
		// For the time being, support both old-style (pinWait) and new style (exit code) 
		// of communicatig the blocking condition.
		for (BlockingCondition bc: attribute) {
			XlimTopLevelPort port=bc.getPort();
			String pinWait=(port.getDirection()==XlimTopLevelPort.Direction.in)?
					"pinWaitIn(" : "pinWaitOut(";
			gen.print(pinWait);
			gen.print(port);
			gen.print(", "+bc.getTokenCount()+");");
			gen.println();
		}
		gen.print("return "+exitCode);
	}
}

