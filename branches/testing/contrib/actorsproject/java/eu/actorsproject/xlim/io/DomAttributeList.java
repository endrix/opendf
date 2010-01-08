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
 
package eu.actorsproject.xlim.io;

import java.util.Iterator;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

class DomAttributeList implements XlimAttributeList {

	private Element mElement;
	
	DomAttributeList(Element element) {
		mElement=element;
	}
	
	public String getAttributeValue(String attributeName) {
		return mElement.getAttribute(attributeName);
	}

	public Iterator<XlimAttribute> iterator() {
		return new IteratorAdapter(mElement.getAttributes());
	}	

	static class IteratorAdapter implements Iterator<XlimAttribute> {

		private NamedNodeMap mAttributes;
		private int i;
		
		IteratorAdapter(NamedNodeMap attributes) {
			mAttributes=attributes;
			i=0;
		}
		
		public boolean hasNext() {
			return i<mAttributes.getLength();
		}

		public XlimAttribute next() {
			Node node=mAttributes.item(i++);
			return new AttributeAdapter(node);
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}
	
	static class AttributeAdapter implements XlimAttribute {

		private Node mNode;
				
		public AttributeAdapter(Node node) {
			mNode = node;
		}

		public String getAttributeName() {
			return mNode.getLocalName();
		}

		public String getAttributeValue() {
			return mNode.getNodeValue();
		}	
	}
}

