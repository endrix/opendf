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

import java.util.HashMap;
import java.util.Map;

import eu.actorsproject.xlim.XlimContainerModule;
import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.XlimInputPort;
import eu.actorsproject.xlim.XlimInstruction;
import eu.actorsproject.xlim.XlimLoopModule;
import eu.actorsproject.xlim.XlimModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimPhiContainerModule;
import eu.actorsproject.xlim.XlimPhiNode;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.XlimType;

public class NativeTypeTransformation {

	private boolean mTrace=false;
	private NativeTypePlugIn mTypePlugIn;
	private OperationPlugIn<OperationTypeHandler> mTypeHandlers;
	
	public NativeTypeTransformation(NativeTypePlugIn plugIn) {
		mTypePlugIn=plugIn;
		mTypeHandlers=new OperationPlugIn<OperationTypeHandler>(null);
		registerTypeHandlers();
	}
	
	public void transform(XlimDesign design) {
		Analysis typeAnalysis=new Analysis();
		Transform transform=new Transform();
		
		HashMap<Object,Transformation> transformations=
			new HashMap<Object,Transformation>();
		
		// Find transformations
		typeAnalysis.traverse(design, transformations);
		// Apply transformations
		transform.traverse(design, transformations);
	}
	
	protected void register(String op, OperationTypeHandler typeHandler) {
		mTypeHandlers.registerHandler(op,typeHandler);
	}
	
	protected XlimType getType(XlimInputPort input) {
		return input.getSource().getSourceType();
	}
	
	private int declaredWidth(XlimInputPort input) {
		return getType(input).getSize();
	}
	
	private int declaredWidth(XlimOutputPort output) {
		return output.getType().getSize();
	}
		
	protected XlimType nativeType(XlimType type) {
		return mTypePlugIn.nativeType(type);
	}
	
	protected int actualWidth(XlimOutputPort output, XlimOperation op) {
		OperationTypeHandler handler=mTypeHandlers.getOperationHandler(op);
		return handler.actualWidth(output);
	}
	
	protected boolean mayNeedZeroExtend(XlimInputPort input, XlimOperation op) {
		OperationTypeHandler handler=mTypeHandlers.getOperationHandler(op);
		return handler.zeroExtend(input, op);
	}
	
	protected boolean mayNeedSignExtend(XlimInputPort input, XlimOperation op) {
		OperationTypeHandler handler=mTypeHandlers.getOperationHandler(op);
		return handler.signExtend(input, op);
	}
	
	protected XlimType signExtendFrom(XlimInputPort input, XlimOperation op) {
		OperationTypeHandler handler=mTypeHandlers.getOperationHandler(op);
		return handler.signExtendFrom(input, op);
	}
	
	/**
	 * There are two classes of transformations: those performed to input ports
	 * and those performed to output ports.
	 */
	private class Transformation {
		
		public void apply(XlimInputPort port, 
				          XlimInstruction instr,
				          XlimContainerModule patchPoint) {
			throw new UnsupportedOperationException();
		}
		
		public void apply(XlimOutputPort port, XlimOperation op) {
			throw new UnsupportedOperationException();
		}
	}
	
	enum TransformKind { ZeroExtend, SignExtend };
	
	private class InputPortTransformation extends Transformation {
		
		private TransformKind mKind;
		private int mFromWidth;
		private XlimType mExtendToType;
	
		public InputPortTransformation(TransformKind kind, int fromWidth, XlimType toType) {
			mKind=kind;
			mFromWidth=fromWidth;
			mExtendToType=toType;
		}

		protected void startPatch(XlimInputPort port, 
				                  XlimInstruction instr,
				                  XlimContainerModule module) {
			module.startPatchBefore(instr.isOperation());
		}
		
		@Override
		public void apply(XlimInputPort port, 
				          XlimInstruction instr,
				          XlimContainerModule module) {
			XlimSource oldSource=port.getSource();
			XlimOutputPort newSource=null;

			startPatch(port, instr, module);

			if (mKind==TransformKind.SignExtend) {
				XlimOperation sex=module.addOperation("signExtend", 
						                               oldSource, 
						                               mExtendToType);
				sex.setIntegerValueAttribute(mFromWidth);
				newSource=sex.getOutputPort(0);
				
				if (mTrace)
					System.out.println("// NativeTypeTransform: added "+sex.toString());
			}
			else if (mKind==TransformKind.ZeroExtend) {
				XlimOperation mask=module.addLiteral((1L << mFromWidth)-1);
				XlimOutputPort maskOutput=mask.getOutputPort(0);
				maskOutput.setType(nativeType(maskOutput.getType()));
				
				XlimOperation and=module.addOperation("bitand",oldSource,maskOutput,mExtendToType);
				newSource=and.getOutputPort(0);
				
				if (mTrace) {
					System.out.println("// NativeTypeTransform: added "+mask.toString());
					System.out.println("// NativeTypeTransform: added "+and.toString());
				}
			}
			
			assert(newSource!=null);
			port.setSource(newSource);
			
			// close patch
			module.completePatchAndFixup();
		}
	}
	
	private class PhiTransformation extends InputPortTransformation {
	
		public PhiTransformation(TransformKind kind, 
				                 int fromWidth,
				                 XlimType toType) {
			super(kind, fromWidth, toType);
		}
		
		@Override
		protected void startPatch(XlimInputPort port, 
                XlimInstruction instr,
                XlimContainerModule module) {
			// The source of a phi-node has to be patched in the predecessor module.
			// There are four cases: in the then/else modules of an if-module and
			// the pre-header/body of a loop-module.
			XlimModule parent=instr.getParentModule();
			XlimModule grandParent=parent.getParentModule();
			if (module==grandParent) {
				// This is the case of the loop pre-header, patch before the loop
				assert(parent instanceof XlimLoopModule);
				module.startPatchBefore((XlimLoopModule) parent);
			}
			else {
				// The other three cases are simple, just patch at end of predecessor
				module.startPatchAtEnd();
			}
		}
	}
	
	private class OutputPortTransformation extends Transformation {
		
		private int mFromWidth;
		private XlimType mExtendToType;
	
		public OutputPortTransformation(int fromWidth, XlimType toType) {
			mFromWidth=fromWidth;
			mExtendToType=toType;
		}
				
		@Override
		public void apply(XlimOutputPort oldOut, XlimOperation op) {
			// open a patch
			XlimContainerModule module=op.getParentModule();
			module.startPatchAfter(op);

			// Generate sign-extension
			XlimOperation sex=module.addOperation("signExtend", 
						                             oldOut, 
						                             mExtendToType);
			sex.setIntegerValueAttribute(mFromWidth);
			XlimOutputPort newOut=sex.getOutputPort(0);
			
			// substitute 'newOut' for 'oldOut' in all uses of 'oldOut'
			oldOut.substitute(newOut);
			
			// yes, we even substituted here (undo)
			sex.getInputPort(0).setSource(oldOut);
			
			// set new native type
			XlimType nativeT=nativeType(oldOut.getType());
			oldOut.setType(nativeT);
			
			// close patch
			module.completePatchAndFixup();
			
			if (mTrace)
				System.out.println("// NativeTypeTransform: added "+sex.toString());
		}
	}

	/**
	 * Analysis finds the transformations necessary to widen the Xlim types
	 * to the types with native support. Note that it is necessary to work in
	 * two passes: first find the transformations, then apply them (doing it
	 * in one go would interfere with analysis).
	 */
	protected class Analysis extends XlimTraversal<Object,Map<Object,Transformation>> {
			
		@Override
		protected Object handleOperation(XlimOperation op, Map<Object,Transformation> map) {
			for (XlimInputPort input: op.getInputPorts()) {
				if (mayNeedZeroExtend(input,op)) {
					// Zero extend input?
					// This is needed in the context of bitwise operators (shorter operand
					// zero-extended to the width of the native type). The longer operand
					// always has a consistent sign-extension. Everything is also OK if
					// operands have the same size (no extension needed).
					XlimType declaredT=getType(input);
					XlimType nativeT=nativeType(declaredT);
					int fromW=declaredT.getSize();
					int actualW=actualWidth(op.getOutputPort(0), op);
					if (fromW<actualW && declaredT!=nativeT) {
						assert(fromW<nativeT.getSize());
						if (mTrace)
							traceZeroExtend(input,op,fromW,nativeT," (actual:"+actualW+")");
						map.put(input, new InputPortTransformation(TransformKind.ZeroExtend,
								                                   fromW,
								                                   nativeT));
					}
				}
				else if (mayNeedSignExtend(input,op)) {
					// Sign-extend input?
					// This is needed when writing to ports and state variables
					// that are *shorter* than the input. We then extend from the
					// width of the port/state variable to the width of the native type
					XlimType fromT=signExtendFrom(input,op);
					XlimType nativeT=nativeType(fromT);
					int fromW=fromT.getSize();
					int actualW=declaredWidth(input);
					if (fromW<actualW && fromT!=nativeT) {
						assert(fromW<nativeT.getSize());
						if (mTrace)
							traceSignExtend(input,op,fromW,nativeT," (was:"+actualW+")");
						map.put(input, new InputPortTransformation(TransformKind.SignExtend,
                                                                   fromW,
                                                                   nativeT));
					}
				}
			}
			
			for (XlimOutputPort output: op.getOutputPorts()) {
				// Sign extend output?
				// This is needed if the declared type is *shorter* than the worst-case
				// ("actual") width. We then extend up to the width of the native type. 
				XlimType declaredT=output.getType();
				XlimType nativeT=nativeType(declaredT);
				int fromW=declaredT.getSize();
				int actualW=actualWidth(output, op);
			
				if (fromW<actualW && declaredT!=nativeT) {
					assert(fromW<nativeT.getSize());
					if (mTrace)
						traceSignExtend(output,fromW,nativeT," (actual:"+actualW+")");
					map.put(output, new OutputPortTransformation(fromW,nativeT));
				}
				if (mTrace && nativeT.getSize()>32)
					System.out.println("// NativeTypeTransform: "+op.toString()
							           +" uses long long ("+fromW+")");
			}
			return null;
		}

		@Override
		protected Object handlePhiNode(XlimPhiNode phi, Map<Object,Transformation> map) {
			XlimType outT=phi.getOutputPort(0).getType();
			int outW=outT.getSize();
			XlimType nativeT=nativeType(outT);
			
			for (XlimInputPort input: phi.getInputPorts()) {
				// Sign-extend inputs of phi-node?
				// This needed if the inputs are *longer* than the declared size of the output
				// We then decide for a new sign-bit and extend to the width of the native type
				int inW=declaredWidth(input);
				if (inW>outW && outT!=nativeT) {
					assert(outW<nativeT.getSize());
					if (mTrace)
						traceSignExtend(input,phi,outW,nativeT," (was:"+inW+")");
					map.put(input, new PhiTransformation(TransformKind.SignExtend,outW,nativeT));
				}
			}
			if (mTrace && nativeT.getSize()>32)
				System.out.println("// NativeTypeTransform: "+phi.toString()
						           +" uses long long ("+outW+")");
			return null;
		}

		private void traceSignExtend(XlimOutputPort output, 
				                     int fromWidth,
				                     XlimType toType, 
				                     String comment) {
			assert(fromWidth<toType.getSize());
			XlimInstruction instr=output.getParent();
			System.out.println("// NativeTypeTransform: "+instr.toString()
					+" sign-extend "+output.getUniqueId()
					+" from "+fromWidth+" to " + toType.getSize()
					+comment);
		}

		private void traceSignExtend(XlimInputPort input, 
				                     XlimInstruction parent,
				                     int fromWidth, 
				                     XlimType toType, 
				                     String comment) {
			assert(fromWidth<toType.getSize());
			XlimSource source=input.getSource();
			System.out.println("// NativeTypeTransform: "+parent.toString()
					+" sign-extend "+source.getUniqueId()
					+" from "+fromWidth+" to " + toType.getSize()
					+comment);
		}

		private void traceZeroExtend(XlimInputPort input, 
				                     XlimOperation parent,
				                     int fromWidth, 
				                     XlimType toType, 
				                     String comment) {
			assert(fromWidth<toType.getSize());
			XlimSource source=input.getSource();
			System.out.println("// NativeTypeTransform: "+parent.toString()
					+" zero-extend "+source.getUniqueId()
					+" from "+fromWidth+" to " + toType.getSize()
					+comment);
		}
	}
	
	/**
	 * This is the second pass, which applies the transformations found by analysis.
	 */
	protected class Transform extends XlimTraversal<Object,Map<Object,Transformation>> {
					
		@Override
		protected Object handleOperation(XlimOperation op, Map<Object,Transformation> map) {
			for (XlimInputPort input: op.getInputPorts()) {
				Transformation t=map.get(input);
				if (t!=null) {
					// A transformation of the input was found by Analysis
					t.apply(input,op,op.getParentModule());
				}
			}	
			for (XlimOutputPort output: op.getOutputPorts()) {
				Transformation t=map.get(output);
				if (t!=null) {
					// A transformation of the output port was found by Analysis
					t.apply(output,op);
				}
				else {
					// Default action is to just widen to the native type
					XlimType nativeT=nativeType(output.getType());
					output.setType(nativeT);
				}
			}
			return null;
		}
		
		@Override
		protected Object handlePhiNode(XlimPhiNode phi, Map<Object,Transformation> map) { 
			int path=0;
			for (XlimInputPort input: phi.getInputPorts()) {
				Transformation t=map.get(input);
				if (t!=null) {
					// The source of a phi-node is patched in the predecessor module
					// (then/else of an if-module, pre-header/body of a loop)
					XlimPhiContainerModule parent=phi.getParentModule();
					XlimContainerModule predecessor=parent.predecessorModule(path);
					t.apply(input,phi,predecessor);
				}
				path++;
			}
			
			// Widen the output
			XlimOutputPort output=phi.getOutputPort(0);
			XlimType nativeT=nativeType(output.getType());
			output.setType(nativeT);
			return null;
		}		
	}
	
	private void registerTypeHandlers(){
		// AddTypeHandler: actual result width is widest operand + 1
		OperationTypeHandler addHandler=new AddTypeHandler();
		mTypeHandlers.registerHandler("$add", addHandler);
		mTypeHandlers.registerHandler("$sub", addHandler);
		mTypeHandlers.registerHandler("$negate", addHandler);
		
		// MulTypeHandler: actual result width is sum of operand widths
		mTypeHandlers.registerHandler("$mul", new MulTypeHandler());

		// DivTypeHandler: declared width of first operand
		OperationTypeHandler divHandler=new DivTypeHandler();
		register("$div", divHandler);
		register("rshift", divHandler);
		register("urshift", divHandler);
		
		// FixedWidthTypeHandler(33): result of lshift has width 33
		register("lshift", new FixedWidthTypeHandler(33));
		
		// BitwiseTypeHandler: actual result is widest operand (zero-extend)
		OperationTypeHandler bitwiseHandler=new BitwiseTypeHandler();
		register("bitand", bitwiseHandler);
		register("bitor", bitwiseHandler);
		register("bitxor", bitwiseHandler);
		register("bitnot", bitwiseHandler);
		
		// BooleanTypeHandler: actual result has width 1
		OperationTypeHandler booleanHandler=new FixedWidthTypeHandler(1);
		register("$and", booleanHandler);
		register("$or", booleanHandler);
		register("$not", booleanHandler);
		register("$eq", booleanHandler);
		register("$ge", booleanHandler);
		register("$gt", booleanHandler);
		register("$le", booleanHandler);
		register("$lt", booleanHandler);
		register("$ne", booleanHandler);

		// DeclaredTypeHandler: use the declared type of the output port
		OperationTypeHandler declaredTypeHandler=new DeclaredTypeHandler();
		register("pinRead", declaredTypeHandler);
		register("pinPeek", declaredTypeHandler);
		register("pinStatus", declaredTypeHandler);
		register("pinAvail", declaredTypeHandler);
		register("var_ref", declaredTypeHandler);
		
		// NoopHandler: use the type of the sole input port
		OperationTypeHandler noopHandler=new NoopTypeHandler();
		register("cast", noopHandler);
		register("noop", noopHandler);
		
		// Sign-extend handler: the type depends on the position of the sign-bit
		register("signExtend", new SignExtendTypeHandler());
		
		// PinWriteHandler: no output port, but possible sign-extension of input
		register("pinWrite", new PinWriteHandler());
		
		// AssignHandler: no output port, but possible sign-extension of input
		register("assign", new AssignHandler());
		
		// NoOutputTypeHandler: operations w/o output port (and no sign-extension of inputs)
		OperationTypeHandler noOutputHandler=new NoOutputTypeHandler();
		register("taskCall", noOutputHandler);
		register("pinWait", noOutputHandler);
		
		// MaxWidthTypeHandler: actual result is widest operand 
		register("$selector", new MaxWidthTypeHandler());

		// LiteralTypeHandler: width of the literal
		register("$literal_Integer", new LiteralTypeHandler());
	}
	
	protected abstract class OperationTypeHandler implements OperationHandler {
		@Override
		public boolean supports(XlimOperation op) {
			return true;
		}
		
		public abstract int actualWidth(XlimOutputPort output);
				
		public boolean zeroExtend(XlimInputPort input, XlimOperation parent) {
			return false;
		}
		
		public boolean signExtend(XlimInputPort input, XlimOperation parent) {
			return false;
		}
		
		public XlimType signExtendFrom(XlimInputPort input, XlimOperation parent) {
			throw new UnsupportedOperationException("Operation does not sign-extend its input");
		}
	} 
	
	protected class AddTypeHandler extends OperationTypeHandler {

		// actual result width is widest operand + 1

		@Override
		public int actualWidth(XlimOutputPort output) {
			int maxWidth=0;
			XlimOperation op=output.getParent().isOperation();
			for (XlimInputPort input: op.getInputPorts())
				maxWidth=Math.max(maxWidth, declaredWidth(input));
			return 1+maxWidth;
		}
	}
	
	protected class MulTypeHandler extends OperationTypeHandler {

		// actual result width is sum of operand widths

		@Override
		public int actualWidth(XlimOutputPort output) {
			int width=0;
			XlimOperation op=output.getParent().isOperation();
			for (XlimInputPort input: op.getInputPorts())
				width+=declaredWidth(input);
			return width;
		}
	}

	protected class MaxWidthTypeHandler extends OperationTypeHandler {

		// actual result is widest operand (zero-extend)

		@Override
		public int actualWidth(XlimOutputPort output) {
			int width=0;
			XlimOperation op=output.getParent().isOperation();
			for (XlimInputPort input: op.getInputPorts())
				width=Math.max(width, declaredWidth(input));
			return width;
		}
	}
	
	protected class BitwiseTypeHandler extends MaxWidthTypeHandler {
		
		// BitwiseTypeHandler: actual result is widest operand (zero-extend inputs)

		@Override
		public boolean zeroExtend(XlimInputPort input, XlimOperation parent) {
			return true;
		}
	}

	
	protected class FixedWidthTypeHandler extends OperationTypeHandler {

		int mWidth;
		
		FixedWidthTypeHandler(int width) {
			mWidth=width;
		}
		
		@Override
		public int actualWidth(XlimOutputPort output) {
			return mWidth;
		}
	}
	
	protected class DeclaredTypeHandler extends OperationTypeHandler {

		// use the declared type of the output port

		@Override
		public int actualWidth(XlimOutputPort output) {
			return declaredWidth(output);
		}
	}

	protected class NoopTypeHandler extends OperationTypeHandler {

		// use the type of the first input port

		@Override
		public int actualWidth(XlimOutputPort output) {
			XlimOperation op=output.getParent().isOperation();
			return declaredWidth(op.getInputPort(0));
		}
	}

	protected class SignExtendTypeHandler extends OperationTypeHandler {

		// width is the position of the sign-bit (size attribute)

		@Override
		public int actualWidth(XlimOutputPort output) {
			XlimOperation op=output.getParent().isOperation();
			return (int)(long) op.getIntegerValueAttribute();
		}
	}
	
	protected class DivTypeHandler extends OperationTypeHandler {

		// declared width of first operand

		@Override
		public int actualWidth(XlimOutputPort output) {
			XlimOperation op=output.getParent().isOperation();
			return declaredWidth(op.getInputPort(0));
		}
	}
	
	protected class NoOutputTypeHandler extends OperationTypeHandler {

		// for operations w/o output port (and no sign-extension of inputs)

		@Override
		public int actualWidth(XlimOutputPort output) {
			throw new UnsupportedOperationException("Operation has no output port");
		}
	}

	protected class PinWriteHandler extends NoOutputTypeHandler {

		// no output port, but possible sign-extension of input

		@Override
		public boolean signExtend(XlimInputPort input, XlimOperation parent) {
			XlimTopLevelPort port=parent.getPortAttribute();
			XlimType type=port.getType();
			return type.isInteger();
		}
		
		@Override
		public XlimType signExtendFrom(XlimInputPort input, XlimOperation parent) {
			XlimTopLevelPort port=parent.getPortAttribute();
			XlimType type=port.getType();
			assert(type.isInteger());
			return type;
		}
	}
	
	protected class AssignHandler extends NoOutputTypeHandler {

		// no output port, but possible sign-extension of input

		@Override
		public boolean signExtend(XlimInputPort input, XlimOperation parent) {
			int lastPort=parent.getNumInputPorts()-1;
			XlimInputPort dataPort=parent.getInputPort(lastPort);
			
			return input==dataPort && getType(input).isInteger();
		}
		
		@Override
		public XlimType signExtendFrom(XlimInputPort input, XlimOperation parent) {
			XlimStateVar stateVar=parent.getStateVarAttribute();
			XlimType type=stateVar.getInitValue().getCommonElementType();
			assert(type!=null && type.isInteger());
			
			return type;
		}
	}
	
	protected class LiteralTypeHandler extends OperationTypeHandler {

		// width of the literal

		@Override
		public int actualWidth(XlimOutputPort output) {
			XlimType type=output.getType();
			if (type.isBoolean())
				return 1;
			else {
				XlimOperation op=output.getParent().isOperation();
				long value=op.getIntegerValueAttribute();
				int width=1;
				while (value>=256 || value<-256) {
					width+=8;
					value>>=8;
				}
				while (value!=0 && value!=-1) {
					width++;
					value>>=1;
				}
				return width;
			}
		}
	}
}
