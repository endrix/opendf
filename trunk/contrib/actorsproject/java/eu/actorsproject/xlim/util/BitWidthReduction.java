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

import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.XlimInputPort;
import eu.actorsproject.xlim.XlimLoopModule;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimPhiNode;
import eu.actorsproject.xlim.XlimSource;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTestModule;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.dependence.Location;
import eu.actorsproject.xlim.type.TypeFactory;

/**
 * Reduces number of bits used in operations by considering how results are used.
 * 
 * Optionally, a NativeTypePlugIn can be specified, by which bitwidths are reduced
 * to a sufficiently wide type with native support (thus not necessarily as narrow
 * as possible).
 */
public class BitWidthReduction  {

	private boolean mTrace=false;
	private NativeTypePlugIn mNativeTypes;
	private AnalysisPhase mAnalysisPhase = new AnalysisPhase();
	private TransformationPhase mTransformationPhase = new TransformationPhase();
	private TypeFactory mTypeFact = Session.getTypeFactory();
	private OperationPlugIn<BitWidthHandler> mBitWidthHandlers;
	
	/**
	 * Constructor with no NativeTypePlugIn, by which as narrow types as possible
	 * are selected
	 */
	public BitWidthReduction() {
	}
	
	/**
	 * @param nativeTypes  used to select next larger type with native support
	 */
	public BitWidthReduction(NativeTypePlugIn nativeTypes) {
		mNativeTypes=nativeTypes;
	}
	
	protected void register(String opKind, BitWidthHandler handler) {
		mBitWidthHandlers.registerHandler(opKind, handler);
	}
	
	protected void registerHandlers() {
		// A "pretty useful" handler, which applies to many operations:
		// it propagates the used bitwidth of its output to its input(s). 
		BitWidthHandler prettyUsefulOne=new PrettyUsefulHandler();

		register("noop", prettyUsefulOne);
		register("cast", prettyUsefulOne);
		register("$add", prettyUsefulOne);
		register("$sub", prettyUsefulOne);
		register("$mul", prettyUsefulOne);
		register("$negate", prettyUsefulOne);
		register("bitand", prettyUsefulOne);
		register("bitor", prettyUsefulOne);
		register("bitxor", prettyUsefulOne);
		register("bitnot", prettyUsefulOne);
		register("$vcons", prettyUsefulOne);
		register("$selector", prettyUsefulOne);
		
		// var_ref: propagate a sufficiently wide index type
		register("var_ref", new VarRefHandler());
		
		// assign: propagate width of location (and index type)
		register("assign", new AssignHandler());
		
		register("signExtend", new SignExtendHandler());
		
		register("lshift", new LShiftHandler());
		
		BitWidthHandler rShiftHandler=new RShiftHandler();
		register("rshift", rShiftHandler);
		register("urshift", rShiftHandler);
	}
	
	public void transform(XlimDesign design) {
		for (XlimTaskModule task: design.getTasks())
			transform(task);
	}
	
	public void transform(XlimTaskModule task) {
		if (mBitWidthHandlers==null) {
			//	Initialize the OperationPlugIn the first time around
			mBitWidthHandlers=new OperationPlugIn<BitWidthHandler>(new BitWidthHandler());
			registerHandlers();
		}
				
		BitWidthContainer bitWidths=new BitWidthContainer();
		mAnalysisPhase.traverse(task,bitWidths);
		mTransformationPhase.traverse(task, bitWidths);
	}
	
	class AnalysisPhase extends BottomUpXlimTraversal<Object,BitWidthContainer> {
		@Override
		protected Object handleOperation(XlimOperation op, BitWidthContainer bw) {
			if (mTrace)
			    System.out.println("// "+op);
			BitWidthHandler handler=mBitWidthHandlers.getOperationHandler(op);
			handler.propagate(op, bw);
			return null;
		}

		@Override
		protected Object traverseLoopModule(XlimLoopModule m, BitWidthContainer bw) {
			// For loops, we play it simple: just a single round
			// We thus start by the phi-nodes and are pessimistic about them
			for (XlimPhiNode phi: m.getPhiNodes()) {
				bw.useFullWidth(phi.getInputPort(0));
				bw.useFullWidth(phi.getInputPort(1));
				bw.useFullWidth(phi.getOutputPort(0));  // to prevent reduction
			}
			
			traverseContainerModule(m.getBodyModule(),bw);
			traverseTestModule(m.getTestModule(),bw);
			return null;
		}	
		
		
		@Override
		protected Object traverseTestModule(XlimTestModule m, BitWidthContainer bw) {
			XlimOutputPort port=m.getDecision().asOutputPort();
			
			// visit decision
			if (port!=null)
				bw.useFullWidth(port);
			
			// visit all operations in the module
			super.traverseTestModule(m, bw);
			return null;
		}

		@Override
		protected Object handlePhiNode(XlimPhiNode phi, BitWidthContainer bw) {
			// This method is just used by the If-module (Loop-module treats phi:s differently)
			// Propagate used bitwidth to inputs
			Integer usedWidth=bw.getWidestUse(phi.getOutputPort(0));
			
			if (usedWidth!=null) {
				bw.use(phi.getInputPort(0), usedWidth);
				bw.use(phi.getInputPort(1), usedWidth);
			}
			else {
				bw.useFullWidth(phi.getInputPort(0));
				bw.useFullWidth(phi.getInputPort(1));
			}
			return null;
		}
	}
	
	class TransformationPhase extends XlimTraversal<Object,BitWidthContainer> {

		@Override
		protected Object handleOperation(XlimOperation op, BitWidthContainer bw) {
			BitWidthHandler handler=mBitWidthHandlers.getOperationHandler(op);
			handler.transform(op, bw);
			return null;
		}

		@Override
		protected Object handlePhiNode(XlimPhiNode phi, BitWidthContainer bw) {
			XlimType newType=bw.getNarrowedType(phi.getOutputPort(0));
			if (newType!=null)
				phi.getOutputPort(0).setType(newType);
			return null;
		}
	}
	
	protected class BitWidthContainer {
		private HashMap<XlimOutputPort,Integer> mPortMap=new HashMap<XlimOutputPort,Integer>();
		
		public Integer getWidestUse(XlimOutputPort port) {
			return mPortMap.get(port);
		}
		
		public XlimType getNarrowedType(XlimOutputPort port) {
			Integer widestUse=mPortMap.get(port);
			
			if (widestUse!=null) {
				XlimType oldType=port.getType();
				int oldWidth=getScalarType(oldType).getSize();
				
				if (widestUse < oldWidth) {
					XlimType newType=narrow(oldType, widestUse);
					
					if (mNativeTypes!=null) {
						newType=mNativeTypes.nativeType(newType);
						if (getScalarType(newType).getSize() < oldWidth)
							return newType;
					}
					else
						return newType;
				}
			}
			
			return null; // No transformation of types
		}
		
		private XlimType narrow(XlimType oldType, int newWidth) {
			if (oldType.isList()) {
				XlimType elementType=narrow(oldType.getTypeParameter("type"), newWidth);
				int listSize=oldType.getIntegerParameter("size");
				return mTypeFact.createList(elementType, listSize);
			}
			else {
				assert(oldType.isInteger());
				return mTypeFact.createInteger(newWidth);
			}
		}
		
		public boolean useFullWidth(XlimInputPort input) {
			XlimSource source=input.getSource();
			XlimOutputPort port=source.asOutputPort();
			
			if (port!=null)
				return useFullWidth(port);
			else
				return false;
		}
		
		public boolean useFullWidth(XlimOutputPort port) {
			XlimType declaredType=getScalarType(port.getType());
			if (declaredType.isInteger())
				return use(port, declaredType.getSize());
			else
				return false;
		}
		
		public boolean use(XlimInputPort input, int usedWidth) {
			XlimSource source=input.getSource();
			XlimOutputPort port=source.asOutputPort();
			
			if (port!=null) {
				XlimType declaredType=getScalarType(port.getType());
				int declaredWidth=declaredType.getSize();
				
				if (declaredType.isInteger()) {
					if (usedWidth > declaredWidth) {
						usedWidth=declaredWidth; // At most declared width
					}
					
					return use(port, usedWidth);
				}
			}
			
			return false;
		}
		
		private boolean use(XlimOutputPort output, int usedWidth) {
			Integer currentWidth=mPortMap.get(output);  				

			// Keep the widest use
			if (currentWidth==null || usedWidth > currentWidth) {
				if (mTrace) {
				    System.out.print(output.getUniqueId()+": "+usedWidth+" bits used");
				    if (currentWidth!=null)
					    System.out.println(" ("+currentWidth+")");
				    else
					    System.out.println(" (first appearence)");
				}
				mPortMap.put(output, usedWidth);
				return true;
			}
			return false;
		}

		private XlimType getScalarType(XlimType type) {
			while (type.isList()) {
				type=type.getTypeParameter("type");
			}
			return type;
		}		
	}
	
	protected class BitWidthHandler implements OperationHandler {
		@Override
		public boolean supports(XlimOperation op) {
			return true;
		}
		
		public void propagate(XlimOperation op, BitWidthContainer bw) {
			// Default is to use full (declared) width
			for (XlimInputPort input: op.getInputPorts()) {
				bw.useFullWidth(input);
			}
		}
		
		public void transform(XlimOperation op, BitWidthContainer bw) {
			// Default is to do no transformation
		}
		
		
		// Useful support routines
		
		protected int bitWidth(XlimType type) {
			while (type.isList())
				type=type.getTypeParameter("type");
			
			return type.getSize();
		}
		
		protected boolean supportedType(XlimType type) {
			while (type.isList())
				type=type.getTypeParameter("type");
			
			return type.isInteger();
		}
		
		protected int indexWidth(XlimType listType) {
			assert(listType.isList());
			
			int maxIndex=listType.getIntegerParameter("size")-1;
			int numBits=0;
			while (maxIndex>=16) {
				maxIndex >>>= 4;
				numBits += 4;
			}
			while (maxIndex!=0) {
				maxIndex >>>= 1;
				numBits++;
			}
			
			// We need an additional sign-bit
			return numBits+1;
		}
	}
	
	
	/**
	 * A BitWidth handler that propagates its output width to its inputs
	 * Applies to: noop, cast, $add, $sub, $mul, $negate, bitand, bitor, bitxor, bitnot, 
	 * $vcons, $selector
	 */
	protected class PrettyUsefulHandler extends BitWidthHandler {
		
		@Override
		public boolean supports(XlimOperation op) {
			return supportedType(op.getOutputPort(0).getType());
		}

		protected int bitWidth(XlimOperation op, BitWidthContainer bw) {
			Integer usedWidth=bw.getWidestUse(op.getOutputPort(0));
			if (usedWidth==null)
				return bitWidth(op.getOutputPort(0).getType());
			else
				return usedWidth;
		}

		protected void propagate(XlimOperation op, int usedWidth, BitWidthContainer bw) {
			for (XlimInputPort input: op.getInputPorts())
				bw.use(input, usedWidth);
		}
		
		@Override
		public void propagate(XlimOperation op, BitWidthContainer bw) {
			// Propagate output type to inputs
			// * If the output is narrower: this width will be propagated
			// * If the output is wider: declared width of input is propagated, use() takes care of this
			// * Booleans and floats inputs: nothing happens, again use() handles this case
			int usedWidth=bitWidth(op,bw);
			propagate(op, usedWidth, bw);
		}
		
		@Override
		public void transform(XlimOperation op, BitWidthContainer bw) {
			XlimType newType=bw.getNarrowedType(op.getOutputPort(0));
			if (newType!=null) {
				if (mTrace)
					System.out.println("// reduced biwidth: "+op.getOutputPort(0).getType()+" to "
							           + newType + " in "+op);
				op.getOutputPort(0).setType(newType);
			}
		}
	}	
	
	/**
	 * signExtend: uses at most "fromWidth" bits 
	 */
	protected class SignExtendHandler extends PrettyUsefulHandler {
		
		@Override
		public boolean supports(XlimOperation op) {
			return true;
		}

		
		@Override
		protected int bitWidth(XlimOperation op, BitWidthContainer bw) {
			int fromWidth=(int)(long)op.getIntegerValueAttribute();
			Integer usedWidth=bw.getWidestUse(op.getOutputPort(0));
			
			if (usedWidth!=null && usedWidth<fromWidth)
				return usedWidth;
			else
				return fromWidth;
		}
	}


	/**
	 * lshift, rshift and urshift:
	 * propagate width only to first input port
	 * constant shift-count is a common and useful special case
	 */
	protected abstract class ShiftHandler extends PrettyUsefulHandler {

		protected InstructionPattern mLiteralPattern=new InstructionPattern("$literal_Integer");
		
		@Override
		public boolean supports(XlimOperation op) {
			return true;
		}

		@Override
		protected int bitWidth(XlimOperation op, BitWidthContainer bw) {
			int usedWidth=super.bitWidth(op, bw);
			XlimSource shiftCount=op.getInputPort(1).getSource();
			
			if (mLiteralPattern.matches(shiftCount)) {
				XlimOutputPort port=shiftCount.asOutputPort();
				XlimOperation literalOp=(XlimOperation) port.getParent();
				return constantShift(usedWidth, literalOp.getIntegerValueAttribute().intValue());
			}
			else
				return usedWidth;
		}

		/**
		 * @param usedWidth   Width of result
		 * @param shiftCount  constant, by which input is shifted
		 * @return used width of input
		 */
		protected abstract int constantShift(int usedWidth, int shiftCount);
		
		@Override
		protected void propagate(XlimOperation op, int usedWidth, BitWidthContainer bw) {
			bw.use(op.getInputPort(0), usedWidth);
			bw.useFullWidth(op.getInputPort(1));  // shift-count
		}
	}
	
	
	protected class LShiftHandler extends ShiftHandler {

		@Override
		protected int constantShift(int usedWidth, int shiftCount) {
			if (usedWidth>shiftCount)
				return usedWidth-shiftCount;
			else
				return 1;
		}
	}

	protected class RShiftHandler extends ShiftHandler {

		@Override
		protected int constantShift(int usedWidth, int shiftCount) {
			return usedWidth+shiftCount;
		}
	}

	protected class VarRefHandler extends BitWidthHandler {

		@Override
		public void propagate(XlimOperation op, BitWidthContainer bw) {
			Location loc=op.getLocation();
			XlimSource locSource=loc.getSource();
			XlimType locType=locSource.getType();
			
			// The width needed on the index port depends on the size of the list
			int usedWidth=indexWidth(locType);
			bw.use(op.getInputPort(0), usedWidth);
		}
	}
	
	protected class AssignHandler extends BitWidthHandler {
		
		@Override
		public void propagate(XlimOperation op, BitWidthContainer bw) {
			Location loc=op.getLocation();
			XlimSource locSource=loc.getSource();
			XlimType locType=locSource.getType();
			int dataPort=0;
			
			if (op.getNumInputPorts()==2) {
				// assignment with both index and data ports
				dataPort=1;
				
				// The width needed on the index port depends on the size of the list
				int usedWidth=indexWidth(locType);
				bw.use(op.getInputPort(0), usedWidth);
			}
			
			if (supportedType(locType)) {
				int usedWidth=bitWidth(locType);
				bw.use(op.getInputPort(dataPort), usedWidth);
			}
		}
	}
}
