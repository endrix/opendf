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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;


/**
 * Builds an XlimDocument from a stream of XmlEvents
 * ...and it keeps references to locations in the source file
 * for the purpose of error reporting in multiple passes
 */
class XmlEventDocument implements XlimDocument {

	private String mFileName;
	private List<XmlEventElement> mAllElements;
	
	protected XmlEventDocument(String fileName) {
		mFileName=fileName;
		mAllElements=new ArrayList<XmlEventElement>();
	}
	
	public static XlimDocument read(File file) throws IOException, XMLStreamException {
		FileReader reader=new FileReader(file);
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory.IS_COALESCING, true);
		factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, true);
		// factory.setIgnoringComments(true);
		
		XMLEventReader eventReader = factory.createXMLEventReader(reader);

		XmlEventDocument document=new XmlEventDocument(file.getPath());
		document.readAllTags(eventReader);
		
		return document;
	}

	private void readAllTags(XMLEventReader eventReader) throws XMLStreamException {
		// Skip possible elements priot to the document root
		while (eventReader.hasNext()) {
			XMLEvent event=eventReader.nextEvent();
			
			if (event.getEventType()==XMLStreamConstants.START_ELEMENT) {
				readAllTags(eventReader, event.asStartElement());
				return;
			}
		}
		throw new XMLStreamException("Premature end of file");
	}
	
	private void readAllTags(XMLEventReader eventReader, StartElement start) throws XMLStreamException {
		XmlEventElement newElement=new XmlEventElement(start, mAllElements.size());
		mAllElements.add(newElement);
		
		XMLEvent next=eventReader.nextTag();
		while (next.isStartElement()) {
			readAllTags(eventReader,next.asStartElement());
			next=eventReader.nextTag();
		}

		// match start-end tags
		String startTagName=start.getName().getLocalPart();
        String endTagName=next.asEndElement().getName().getLocalPart();
        if (startTagName.equals(endTagName)==false)
        	throw new XMLStreamException("Expecting end tag </"+startTagName+">",
        			                     next.getLocation());
        
        // Mark end of element
		newElement.setEnd(mAllElements.size());
	}
	
	@Override
	public String getFileName() {
		return mFileName;
	}

	@Override
	public XlimElement getRootElement() {
		if (mAllElements.isEmpty())
			return null;
		else
			return mAllElements.get(0);
	}
	
	protected class XmlEventElement implements XlimElement, XlimAttributeList, XlimLocation {

		private StartElement mStartElement;
		private int mLineNumber;
		private int mIndexInDocument;
		private int mEndInDocument;
		
		protected XmlEventElement(StartElement startElement,
				                  int indexInDocument) {
			mStartElement=startElement;
			mLineNumber=startElement.getLocation().getLineNumber();
			mIndexInDocument=indexInDocument;
			mEndInDocument=indexInDocument+1;
		}
		
		protected int getNextSibling() {
			return mEndInDocument;
		}
		
		protected void setEnd(int endInDocument) {
			mEndInDocument=endInDocument;
		}

		@Override
		public String getTagName() {
			return mStartElement.getName().getLocalPart();
		}

		@Override
		public XlimAttributeList getAttributes() {
			return this; // this is (also) an XlimAttributeList
		}

		@Override
		public String getAttributeValue(String attributeName) {
			QName qName=new QName(attributeName);
			Attribute attribute=mStartElement.getAttributeByName(qName);
			return (attribute!=null)? attribute.getValue() : null;
		}

		/*
		 * Implements XlimAttributeList.iterator
		 */
		@Override 
		public Iterator<XlimAttribute> iterator() {
			return new AttributeIterator(mStartElement.getAttributes());
		}

		@Override
		public Iterable<XlimElement> getElements() {
			return new ElementList();
		}

		@Override
		public XlimLocation getLocation() {
			return this;  // this is (also) an XlimLocation
		}
		
		@Override
		public String getFileName() {
			return mFileName;
		}

		@Override
		public int getLineNumber() {
			return mLineNumber;
		}

		protected class ElementList implements Iterable<XlimElement> {
			public Iterator<XlimElement> iterator() {
				return new ElementIterator();
			}
		}
		
		protected class ElementIterator implements Iterator<XlimElement> {

			private int mIndex;
			
			public ElementIterator() {
				mIndex=mIndexInDocument+1;
			}
			
			@Override
			public boolean hasNext() {
				return mIndex<mEndInDocument;
			}

			@Override
			public XlimElement next() {
				XmlEventElement element=mAllElements.get(mIndex);
				mIndex=element.getNextSibling();
				return element;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		}
		
		protected class AttributeIterator implements Iterator<XlimAttribute> {

			Iterator pAttribute;

			AttributeIterator(Iterator i) {
				pAttribute=i;
			}

			public boolean hasNext() {
				return pAttribute.hasNext();
			}

			public XlimAttribute next() {
				Attribute attribute=(Attribute) pAttribute.next();
				return new AttributeAdapter(attribute);
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		}
	}
	
	static class AttributeAdapter implements XlimAttribute {

		Attribute mAttribute;

		public AttributeAdapter(Attribute attribute) {
			mAttribute = attribute;
		}

		public String getAttributeName() {
			return mAttribute.getName().getLocalPart();
		}

		public String getAttributeValue() {
			return mAttribute.getValue();
		}
	}
}
