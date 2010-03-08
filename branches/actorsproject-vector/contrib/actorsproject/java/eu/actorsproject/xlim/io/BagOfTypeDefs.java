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

package eu.actorsproject.xlim.io;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;

import eu.actorsproject.xlim.XlimDesign;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimOutputPort;
import eu.actorsproject.xlim.XlimPhiNode;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.type.TypeArgument;
import eu.actorsproject.xlim.util.XlimTraversal;

/**
 * Container of used types, from which the set of required XlimTypeDefs can be generated
 */
public class BagOfTypeDefs implements Iterable<XlimTypeDef> {

	private LinkedHashSet<XlimType> mUsedTypes=new LinkedHashSet<XlimType>();
	private HashSet<XlimType> mNeedsTypeDef=new HashSet<XlimType>(); 
	private FindAllTypeDefs mFindAllTypeDefs=new FindAllTypeDefs();


	@Override
	public Iterator<XlimTypeDef> iterator() {
		return new TypedefIterator();
	}

	public void addUsedTypes(XlimDesign design) {
		for (XlimStateVar s: design.getStateVars())
			addUsedType(s.getType());
		mFindAllTypeDefs.traverse(design, null);
		
		// Allocate typedef names
		int i=0;
		for (XlimType t: mUsedTypes) {
			String name=null;
			if (mNeedsTypeDef.contains(t)) {
				name="typedef"+i;
				++i;
			}
			t.setTypeDefName(name);
		}
	}
	
	private void addUsedType(XlimType type) {
		// We need typeDefs for parametric types (except "int")
		if (type.isInteger()==false && isParametric(type) && mNeedsTypeDef.add(type)) {
			addRecursive(type);
		}
	}
	
	private boolean isParametric(XlimType type) {
		return type.getTypeArguments().iterator().hasNext();
	}
	
	private void addRecursive(XlimType type) {
		if (mUsedTypes.contains(type)==false) {
			// First add type parameters, on which the type depends
			for (TypeArgument arg: type.getTypeArguments())
				if (arg.isTypeParameter())
					addRecursive(arg.getType());
			mUsedTypes.add(type);
		}
	}

	private XlimTypeElement createTypeElement(XlimType type) {
		// Create a <type> element with possible parameters
		XlimTypeElement typeElement=new XlimTypeElement(type.getTypeName(), null);
		
		for (TypeArgument arg: type.getTypeArguments()) {
			if (arg.isValueParameter())
				typeElement.addValuePar(arg.getName(), arg.getValue());
			else {
				typeElement.addTypePar(arg.getName(), createTypeReference(arg.getType()));
			}
		}
		
		return typeElement;
	}

	private XlimTypeElement createTypeReference(XlimType type) {
		String typeDef=type.getTypeDefName();
		
		if (typeDef!=null) {
			// Has a typeDef
			return new XlimTypeElement(typeDef, null);
		}
		else {
			// Create a <type> element with possible parameters
			return createTypeElement(type);
		}
	}
	
	private class FindAllTypeDefs extends XlimTraversal<Object,Object> {

		@Override
		protected Object handleOperation(XlimOperation op, Object arg) {
			handlePorts(op.getOutputPorts());
			return null;
		}

		@Override
		protected Object handlePhiNode(XlimPhiNode phi, Object arg) {
			handlePorts(phi.getOutputPorts());
			return null;
		}
		
		private void handlePorts(Iterable<? extends XlimOutputPort> ports) {
			for (XlimOutputPort p: ports)
				addUsedType(p.getType());
		}
	}

	private class TypedefIterator implements Iterator<XlimTypeDef> {

		Iterator<XlimType> mIterator;
		XlimType mNext;
		
		TypedefIterator() {
			mIterator=mUsedTypes.iterator();
			mNext=skip();
		}
		
		@Override
		public boolean hasNext() {
			return mNext!=null;
		}

		@Override
		public XlimTypeDef next() {
			assert(mNext!=null);
			XlimTypeElement typeElement=createTypeElement(mNext);
			String name=mNext.getTypeDefName();
			
			mNext=skip();
			return new XlimTypeDef(name, typeElement, null);
		}

		XlimType skip() {
			while (mIterator.hasNext()) {
				XlimType next=mIterator.next();
				if (mNeedsTypeDef.contains(next))
					return next;
			}
			return null;
		}
		
		@Override
		public void remove() {
			throw new UnsupportedOperationException();			
		}
	}
	
}
