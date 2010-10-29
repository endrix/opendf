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
import eu.actorsproject.xlim.XlimInitValue;
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
import eu.actorsproject.xlim.XlimTypeKind;

public class NativeTypeTransformation {

	private boolean mTrace=false;
	private NativeTypePlugIn mNativeTypePlugIn;
	private OperationPlugIn<DefaultHandler> mHandlers;
	private XlimTypeKind mIntTypeKind;
	
	public NativeTypeTransformation(NativeTypePlugIn plugIn) {
		mNativeTypePlugIn=plugIn;
		mHandlers=new OperationPlugIn<DefaultHandler>(new DefaultHandler());
		mIntTypeKind=Session.getTypeFactory().getTypeKind("int");
		
		// BitwiseHandler: possible zero-extension of input
		DefaultHandler bitwiseHandler=new BitwiseHandler();
		mHandlers.registerHandler("bitand", bitwiseHandler);
		mHandlers.registerHandler("bitor", bitwiseHandler);
		mHandlers.registerHandler("bitxor", bitwiseHandler);
		mHandlers.registerHandler("urshift", new URShiftHandler());
		
		// PinWriteHandler: no output port, but possible sign/zero-extension of input
		mHandlers.registerHandler("pinWrite", new PinWriteHandler());
		
		// AssignHandler: no output port, but possible sign/zero-extension of input
		mHandlers.registerHandler("assign", new AssignHandler());
		
		// cast: a possible sign/zero extension might still be needed 
		// (although same native type is used for input and output)
		mHandlers.registerHandler("cast", new CastHandler());
		
		// noop: fixes the confusion regarding List-types
		mHandlers.registerHandler("noop", new NoopHandler());
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
		
		// Transform ports and state variables to use native types
		transformPorts(design.getInputPorts());
		transformPorts(design.getOutputPorts());
		transformPorts(design.getInternalPorts());
		transformStateVars(design.getStateVars());
	}
	
	private void transformPorts(Iterable<? extends XlimTopLevelPort> ports) {
		for (XlimTopLevelPort p: ports) {
			XlimType nativeT=mNativeTypePlugIn.nativePortType(p.getType());
			p.setType(nativeT);
		}
	}
	
	private void transformStateVars(Iterable<? extends XlimStateVar> stateVars) {
		for (XlimStateVar s: stateVars) {
			XlimInitValue init=s.getInitValue();
			XlimType elementT=init.getCommonElementType();
			assert(elementT!=null);
			XlimType nativeT=mNativeTypePlugIn.nativeElementType(elementT);
			init.setCommonElementType(nativeT);
		}
	}
	
	private boolean isSignedInt(XlimType type) {
		return type.getTypeKind()==mIntTypeKind;
	}
	
	private XlimType getElementType(XlimType type) {
		while (type.isList()) {
			type=type.getTypeParameter("type");
		}
		return type;
	}
	
	/**
	 * Analysis is the first pass of NativeTypeTransformation
	 * 
	 * Analysis finds the transformations necessary to widen the Xlim types
	 * to the types with native support. Note that it is necessary to work in
	 * two passes: first find the transformations, then apply them (doing it
	 * in one go would interfere with analysis).
	 */
	protected class Analysis extends XlimTraversal<Object,Map<Object,Transformation>> {
			
		@Override
		protected Object handleOperation(XlimOperation op, Map<Object,Transformation> map) {
			mHandlers.getOperationHandler(op).handleOperation(op, map);			
			
			return null;
		}

		@Override
		protected Object handlePhiNode(XlimPhiNode phi, Map<Object,Transformation> map) {
			XlimType outT=phi.getOutputPort(0).getType();
			XlimType nativeT=mNativeTypePlugIn.nativeType(outT);
			
			if (outT!=nativeT) {
				int outW=outT.getSize();
				
				for (XlimInputPort input: phi.getInputPorts()) {
					// Sign-extend inputs of phi-node?
					// This needed if the inputs are *longer* than the declared size of the output
					// We then decide for a new sign-bit and extend to the width of the native type
					XlimType inT=input.getSource().getType();
					int inW=inT.getSize();
					if (inW>outW) {
						if (mTrace) {
							String extensionType=isSignedInt(nativeT)? " sign-extend " : " zero-extend ";
							System.out.println("// NativeTypeTransform: " + phi.toString()
									+ extensionType + input.getSource().getUniqueId()
									+ " from " + outW
									+ " to " + nativeT.getSize()
									+ " (was:" + inW + ")");
						}
					
						Transformation t=new PhiTransformation(outW,nativeT);
						map.put(input, t);
					}
				}
			}
			return null;
		}
	}
	
	/**
	 * Transform is the second pass, which applies the transformations found by analysis.
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
					XlimType nativeT=mNativeTypePlugIn.nativeType(output.getType());
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
			XlimType nativeT=mNativeTypePlugIn.nativeType(output.getType());
			output.setType(nativeT);
			return null;
		}		
	}

	/**
	 * There are two kinds of transformations: those performed to input ports
	 * and those performed to output ports.
	 */
	private class Transformation {
		
		/**
		 * Applies a transformation to input port 'port' of instruction 'instr' 
		 * and emits the code at 'patchPoint' 
		 * @param port
		 * @param instr
		 * @param patchPoint
		 */
		public void apply(XlimInputPort port, 
				          XlimInstruction instr,
				          XlimContainerModule patchPoint) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Applies a transformation to output port 'port' of instruction 'instr' 
		 * and emits the code directly following the given instruction
		 * @param port
		 * @param instr
		 * @param patchPoint
		 */

		public void apply(XlimOutputPort port, XlimOperation op) {
			throw new UnsupportedOperationException();
		}
		
		/**
		 * Sign-extends the integer 'oldSource' from 'fromWidth' to 'toType'
		 * and emits the code at the patch point in 'inModule'.
		 * @param oldSource
		 * @param fromWidth
		 * @param toType
		 * @param inModule
		 * @return output port (result) of sign-extension
		 */
		protected XlimOutputPort signExtend(XlimSource oldSource,
				                            int fromWidth,
				                            XlimType toType,
				                            XlimContainerModule inModule) {
			XlimOperation sex=inModule.addOperation("signExtend", 
					oldSource, 
					toType);
			sex.setIntegerValueAttribute(fromWidth);

			if (mTrace)
				System.out.println("// NativeTypeTransform: added "+sex.toString());

			return sex.getOutputPort(0);
		}

		/**
		 * Zero-extends the integer 'oldSource' from 'fromWidth' to 'toType'
		 * and emits the code at the patch point in 'inModule'.
		 * @param oldSource
		 * @param fromWidth
		 * @param toType
		 * @param inModule
		 * @return output port (result) of zero-extension
		 */
		protected XlimOutputPort zeroExtend(XlimSource oldSource,
				                            int fromWidth,
				                            XlimType toType,
				                            XlimContainerModule inModule) {
			XlimOperation mask=inModule.addLiteral((1L << fromWidth)-1);
			XlimOutputPort maskOutput=mask.getOutputPort(0);
			maskOutput.setType(toType);

			XlimOperation and=inModule.addOperation("bitand",oldSource,maskOutput,toType);

			if (mTrace) {
				System.out.println("// NativeTypeTransform: added "+mask.toString());
				System.out.println("// NativeTypeTransform: added "+and.toString());
			}

			return and.getOutputPort(0);
		}
	}
	
	
	/**
	 * Transformation of InputPorts
	 */
	
	private abstract class InputPortTransformation extends Transformation {
		
		protected void startPatch(XlimInputPort port, 
				                  XlimInstruction instr,
				                  XlimContainerModule module) {
			module.startPatchBefore(instr.isOperation());
		}
		
		protected abstract XlimOutputPort apply(XlimSource oldSource, XlimContainerModule inModule);
		
		@Override
		public void apply(XlimInputPort port, 
				          XlimInstruction instr,
				          XlimContainerModule module) {
			XlimSource oldSource=port.getSource();

			startPatch(port, instr, module);
			
			XlimOutputPort newSource=apply(oldSource,module);
						
			assert(newSource!=null);
			port.setSource(newSource);
			
			// close patch
			module.completePatchAndFixup();
		}
	}
	
	/**
	 * sign-extension of input (used for pinWrite and assign) 
	 */
	private class SignedInputPortTransformation extends InputPortTransformation {

		private int mFromWidth;
		private XlimType mExtendToType;
		
		public SignedInputPortTransformation(int fromWidth, XlimType toType) {
			mFromWidth=fromWidth;
			mExtendToType=toType;
		}
		
		@Override
		protected XlimOutputPort apply(XlimSource oldSource, XlimContainerModule inModule) {
			return signExtend(oldSource,mFromWidth,mExtendToType,inModule);
		}
	}
	
	/**
	 * zero-extension of input (used for bitwise operations: bitand, bitor, bitxor)
	 */
	private class UnsignedInputPortTransformation extends InputPortTransformation {

		private int mFromWidth;
		private XlimType mExtendToType;
		
		public UnsignedInputPortTransformation(int fromWidth, XlimType toType) {
			mFromWidth=fromWidth;
			mExtendToType=toType;
		}
		
		@Override
		protected XlimOutputPort apply(XlimSource oldSource, XlimContainerModule inModule) {
			return zeroExtend(oldSource,mFromWidth,mExtendToType,inModule);
		}
	}
	
	/**
	 * PhiNodes need different handling, since added code is not patched in
	 * their parent module (a Loop or If-module), but in the predecessor that
	 * corresponds to the respective input port.
	 */
	private class PhiTransformation extends InputPortTransformation {
	
		private int mFromWidth;
		private XlimType mExtendToType;
		
		public PhiTransformation(int fromWidth, XlimType toType) {
			mFromWidth=fromWidth;
			mExtendToType=toType;
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
		
		@Override
		protected XlimOutputPort apply(XlimSource oldSource, XlimContainerModule inModule) {
			if (isSignedInt(mExtendToType)) {
				return signExtend(oldSource,mFromWidth,mExtendToType,inModule);
			}
			else {
				return zeroExtend(oldSource,mFromWidth,mExtendToType,inModule);
			}
		}
	}
	
	
	/**
	 * Transformation of output ports
	 */
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

			// Generate sign/zero-extension
			XlimOutputPort newOut=(isSignedInt(mExtendToType))?
					signExtend(oldOut,mFromWidth,mExtendToType,module) :
					zeroExtend(oldOut,mFromWidth,mExtendToType,module);
			XlimInputPort refToOldOut=newOut.getParent().getInputPort(0);
			
			// substitute 'newOut' for 'oldOut' in all uses of 'oldOut'
			oldOut.substitute(newOut);
			
			// yes, we even substituted here (undo)
			refToOldOut.setSource(oldOut);
			
			// set new native type
			XlimType nativeT=mNativeTypePlugIn.nativeType(oldOut.getType());
			oldOut.setType(nativeT);
			
			// close patch
			module.completePatchAndFixup();
		}
	}
	
	
	/**
	 * DefaultHandler, which is applicable to most operations,
	 * sign-extends (integer) outputs from their declared width
	 * to the width of the native type.
	 */
	protected class DefaultHandler implements OperationHandler {
		@Override
		public boolean supports(XlimOperation op) {
			return true;
		}
		
		public void handleOperation(XlimOperation op, 
				                    Map<Object,Transformation> transformations) {
			for (XlimOutputPort output: op.getOutputPorts()) {
				XlimType declaredT=output.getType();
				
				if (declaredT.isList()) {
					XlimType actualT=output.actualOutputType();
					assert(mNativeTypePlugIn.nativeType(declaredT)==mNativeTypePlugIn.nativeType(actualT));
				}
				else {
					// Sign extend output?
					// This is needed if the declared type is *shorter* than the worst-case
					// ("actual") width. We then extend up to the width of the native type. 
					XlimType nativeT=mNativeTypePlugIn.nativeType(declaredT);
					if (declaredT!=nativeT) {
						int fromW=declaredT.getSize();
						int actualW=output.actualOutputType().getSize();

						if (fromW<actualW) {
							if (mTrace) {
								String extensionType=isSignedInt(nativeT)? " sign-extend " : " zero-extend ";
								System.out.println("// NativeTypeTransform: " + op.toString()
										+ extensionType + output.getUniqueId()
										+ " from " + fromW
										+ " to " + nativeT.getSize()
										+ " (actual:" + actualW + ")");
							}
							Transformation t=new OutputPortTransformation(fromW, nativeT);
							transformations.put(output, t);
						}
					}
				}
			}			
		}
	} 

	/**
	 * FIXME: this class is only motivated by the unpredicatble typing of noops:
	 * We are forced to view noops as casts and must relax the assertions of the DefaultHandler
	 */

	private class AddCastInputPortTransformation extends InputPortTransformation {
		XlimType mCastToType;
		
		public AddCastInputPortTransformation(XlimType toType) {
			mCastToType=toType;
		}
		
		@Override
		protected XlimOutputPort apply(XlimSource oldSource, XlimContainerModule inModule) {
			XlimOperation cast=inModule.addOperation("cast", oldSource, mCastToType);
			
			if (mTrace)
				System.out.println("// NativeTypeTransform: added "+cast.toString());
			
			return cast.getOutputPort(0);
		}
	}
	
	protected class NoopHandler extends DefaultHandler {

		@Override
		public void handleOperation(XlimOperation op, 
                                    Map<Object,Transformation> transformations) {
			assert(op.getNumOutputPorts()==1 && op.getNumInputPorts()==1);
			
			XlimType declaredT=op.getOutputPort(0).getType();
			if (declaredT.isList()) {
				XlimType actualT=op.getOutputPort(0).actualOutputType();
				XlimType inT=mNativeTypePlugIn.nativeType(actualT);
				XlimType outT=mNativeTypePlugIn.nativeType(declaredT);
				
				if (inT!=outT) {
					XlimInputPort input=op.getInputPort(0);
					if (mTrace)
						System.out.println("// NativeTypeTransform: " + op.toString()
								+ " cast " + input.getSource().getUniqueId()
								+ " from " + inT
								+ " to " + outT);

					Transformation t=new AddCastInputPortTransformation(outT);
					transformations.put(input, t);
				}
			}
			else
				super.handleOperation(op, transformations);
		}
	}
	
	/**
	 * The DefaultHandler doesn't do the trick for the cast operator
	 * What could happen is that we end up with a noop-type conversion after promotion to
	 * native types. If, however, the original (output) type of the cast is narrower then
	 * a sign- or zero-extension is required.
	 */
	protected class CastHandler extends DefaultHandler {
		
		public void handleOperation(XlimOperation op, 
				                    Map<Object,Transformation> transformations) {
			assert(op.getNumOutputPorts()==1 && op.getNumInputPorts()==1);
			
			XlimOutputPort output=op.getOutputPort(0);
			XlimType declaredT=output.getType();
			XlimType nativeT=mNativeTypePlugIn.nativeType(declaredT);
			
			if (declaredT.isList()) {
				if (declaredT!=nativeT) {
					XlimType declaredElementT=getElementType(declaredT);
					XlimType inT=op.getInputPort(0).getSource().getType();
					XlimType inElementT=getElementType(inT);
					
					int fromW=declaredElementT.getSize();
					int actualW=inElementT.getSize();
					// assert(fromW>=actualW);
					if (fromW<actualW) {
						// FIXME: Here we need to sign-/zero-extend the elements of the list
						// How to achieve that??? 
						// throw new UnsupportedOperationException("Cast "+inT+" to "+declaredT);
					}
				}
			}
			else {
				// Sign extend output?
				// This is needed if the declared type is *shorter* than the worst-case
				// ("actual") width. We then extend up to the width of the native type. 
					
				if (declaredT!=nativeT) {
					int fromW=declaredT.getSize();
					XlimType inT=op.getInputPort(0).getSource().getType();
					int actualW=inT.getSize();
					
					if (fromW<actualW) {
						if (mTrace) {
							String extensionType=isSignedInt(nativeT)? " sign-extend " : " zero-extend ";
							System.out.println("// NativeTypeTransform: " + op.toString()
										       + extensionType + output.getUniqueId()
										       + " from " + fromW
										       + " to " + nativeT.getSize()
										       + " (actual:" + actualW + ")");
						}
						Transformation t=new OutputPortTransformation(fromW, nativeT);
						transformations.put(output, t);
					}
				}
			}			
		}
	}		
	
	/**
	 * In addition to dealing with the output ports (see DefaultHandler),
	 * the BitwiseHandler zero-extends the shorter of its inputs.
	 */
	protected class BitwiseHandler extends DefaultHandler {
		
		public void handleOperation(XlimOperation op, 
                                    Map<Object,Transformation> transformations) {
			
			for (XlimInputPort input: op.getInputPorts()) {
				// Zero extend input?
				// This is needed in the context of bitwise operators (shorter operand
				// zero-extended to the width of the native type). The longer operand
				// always has a consistent sign-extension. Everything is also OK if
				// operands have the same size (no extension needed).
				XlimType declaredT=input.getSource().getType();
				XlimType nativeT=mNativeTypePlugIn.nativeType(declaredT);
				if (declaredT!=nativeT) {
					int fromW=declaredT.getSize();
					int actualW=op.getOutputPort(0).actualOutputType().getSize();
					if (fromW<actualW) {
						if (mTrace)
							System.out.println("// NativeTypeTransform: " + op.toString()
									+ " zero-extend " + input.getSource().getUniqueId()
									+ " from " + fromW
									+ " to " + nativeT.getSize()
									+ " (actual:" + actualW + ")");

						Transformation t=new UnsignedInputPortTransformation(fromW,nativeT);
						transformations.put(input, t);
					}
				}
			}
			
			// Also transform the outputs, if needed
			super.handleOperation(op, transformations);
		}
	}
	
	/**
	 * In addition to dealing with the output ports (see DefaultHandler),
	 * the URShiftHandler zero-extends the its left input.
	 */
	protected class URShiftHandler extends DefaultHandler {
		
		public void handleOperation(XlimOperation op, 
                                    Map<Object,Transformation> transformations) {
			
			// Zero extend input?
			// The left input of URShift ('x' in x>>>count) is zero-extended to the
			// width of the native type.
			XlimInputPort input=op.getInputPort(0);
			XlimType declaredT=input.getSource().getType();
			XlimType nativeT=mNativeTypePlugIn.nativeType(declaredT);
			if (declaredT!=nativeT) {
				int width=declaredT.getSize();
				if (mTrace) {
					int actualW=op.getOutputPort(0).actualOutputType().getSize();
					System.out.println("// NativeTypeTransform: " + op.toString()
							+ " zero-extend " + input.getSource().getUniqueId()
							+ " from " + width
							+ " to " + nativeT.getSize()
							+ " (actual:" + actualW + ")");
				}
				Transformation t=new UnsignedInputPortTransformation(width,nativeT);
				transformations.put(input, t);
			}
			
			// Also transform the outputs, if needed
			super.handleOperation(op, transformations);
		}
	}
	
	/**
	 * The InputSignExtendHandler is used for pinWrite and assign operations,
	 * which require its input to have a valid sign-extension to the width
	 * of the native type that is used for the port or state variable.
	 */
	protected abstract class InputSignExtendHandler extends DefaultHandler {
		
		public void handleOperation(XlimOperation op, 
                                    Map<Object,Transformation> transformations) {

			XlimType inputT=getDataPort(op).getSource().getType();
			XlimType fromT=extendFromType(op);
			
			if (inputT.isList()) {
				// Make sure the elements in the input matches that of the state variable (or port)
				XlimType elementT=inputT;
				while (elementT.isList())
					elementT=elementT.getTypeParameter("type");
				
				assert(fromT==elementT);
			}
			else {
				// Sign/zero-extend input?
				// This is needed when writing to ports and state variables
				// that are *shorter* than the input. We then extend from the
				// width of the port/state variable to the width of the native type
				XlimType nativeT=nativeType(op);
				if (fromT!=nativeT) {				
					// TODO: do we have to convert input to integer sometimes (e.g. real-to-int)?
					assert(inputT.isInteger() && nativeT.isInteger() && fromT.isInteger());

					int fromW=fromT.getSize();
					int actualW=inputT.getSize();			
					if (fromW<actualW) {
						XlimInputPort input=getDataPort(op);
						boolean signed=isSignedInt(fromT);
						
						if (mTrace) {
							String extensionType=signed? " sign-extend " : " zero-extend ";
							System.out.println("// NativeTypeTransform: " + op.toString()
									+ extensionType + input.getSource().getUniqueId()
									+ " from " + fromW
									+ " to " + nativeT.getSize()
									+ " (was:" + actualW + ")");
						}
						
						Transformation t=signed?
								new SignedInputPortTransformation(fromW,nativeT) :
								new UnsignedInputPortTransformation(fromW,nativeT);
						transformations.put(input, t);
					}
				}
			}
			
			// Also transform the outputs, if needed
			super.handleOperation(op, transformations);
		}
		
		protected abstract XlimInputPort getDataPort(XlimOperation op);
		
		protected abstract XlimType extendFromType(XlimOperation op);
		
		protected abstract XlimType nativeType(XlimOperation op);
	}
	
	protected class PinWriteHandler extends InputSignExtendHandler {

		@Override
		protected XlimInputPort getDataPort(XlimOperation op) {
			return op.getInputPort(0);
		}
		
		@Override
		protected XlimType extendFromType(XlimOperation op) {
			return op.getPortAttribute().getType();
		}

		@Override
		protected XlimType nativeType(XlimOperation op) {
			return mNativeTypePlugIn.nativePortType(extendFromType(op));
		}
	}
	
	protected class AssignHandler extends InputSignExtendHandler {

		@Override
		protected XlimInputPort getDataPort(XlimOperation op) {
			int dataPort=op.getNumInputPorts()-1;
			return op.getInputPort(dataPort);
		}

		@Override
		protected XlimType extendFromType(XlimOperation op) {
			XlimType elementT=op.getLocation().getType();
			while (elementT.isList())
				elementT=elementT.getTypeParameter("type");
			return elementT;
		}

		@Override
		protected XlimType nativeType(XlimOperation op) {
			XlimType targetT=op.getLocation().getType();
			if (targetT.isList())
				return mNativeTypePlugIn.nativeElementType(extendFromType(op));
			else
				return mNativeTypePlugIn.nativeType(targetT);
		}		
	}
}
