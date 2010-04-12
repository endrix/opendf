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

package eu.actorsproject.util;

import java.io.PrintStream;

import java.util.ArrayList;
import java.util.List;

public class XmlPrinter extends OutputGenerator {

	private static XmlAttributeFormatter sEmptyFormatter=new XmlAttributeFormatter();
	private XmlAttributeFormatter mAttributeFormatter=sEmptyFormatter;
	private List<XmlAttributeFormatter.PlugIn> mAttributeFormatterPlugIns;
	private List<XmlElementPlugIn<? extends XmlElement>> mElementPlugIns=
		new ArrayList<XmlElementPlugIn<? extends XmlElement>>();
	private XmlElementPlugIn<XmlElement> mDefaultElementPlugIn=
		new XmlElementPlugIn<XmlElement>(XmlElement.class);
	
	public XmlPrinter(PrintStream sink) {
		super(sink);
	}
	
	/**
	 * Prints an XML document (including XML declaration)
	 * @param document
	 */
	public void printDocument(XmlElement document) {
		println(xmlDeclaration());
		printElement(document);
	}
	
	/**
	 * Prints an XML element (and its enclosed elements)
	 * @param element
	 */
	public<T extends XmlElement> void printElement(T element) {
		XmlElementPlugIn<? super T> plugIn=getElementPlugIn(element);
		boolean empty=plugIn.isEmpty(element);
		String tagName=plugIn.getTagName(element);
		String attributes=plugIn.getAttributeDefinitions(element, mAttributeFormatter);
		
		println(startTag(tagName,attributes,empty));
		
 		increaseIndentation();
		plugIn.printChildren(element, this);
		decreaseIndentation();
		if (!empty) {
			println("</"+element.getTagName()+">");
		}
	}
	
	private String startTag(String tagName, String attributes, boolean empty) {
		String space=attributes.length()!=0? " " : "";
		String end=empty? "/>" : ">";
		return "<" + tagName + space + attributes + end; 
	}
	
	/**
	 * Prints an XML comment
	 * @param s
	 */
	public void printComment(String s) {
		println("<!-- "+s+" -->");
	}
	
	protected String xmlDeclaration() {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";
	}
	

	/**
	 * Registers a custom attribute formatter for type T
	 * @param plugIn
	 * 
	 * Note: The applicability of plug-ins is simply tested in order of registration,
	 * thus register a specialized plug-in/subclass before a more general one/super class.
	 */
	public<T> void register(XmlAttributeFormatter.PlugIn<T> plugIn) {
		
		if (mAttributeFormatter==sEmptyFormatter) {
			mAttributeFormatter=new CustomAttributeFormatter();
			mAttributeFormatterPlugIns=new ArrayList<XmlAttributeFormatter.PlugIn>();
		}
		mAttributeFormatterPlugIns.add(plugIn);
	}
	
	/**
	 * Registers a custom element formatter for type T
	 * @param plugIn
	 * 
	 * Note: The applicability of plug-ins is simply tested in order of registration,
	 * thus register a specialized plug-in/subclass before a more general one/super class.
	 */
	public<T extends XmlElement> void register(XmlElementPlugIn<T> plugIn) {
		mElementPlugIns.add(plugIn);
	}
	
	private<T extends XmlElement> XmlElementPlugIn<? super T> getElementPlugIn(T element) {
		for (XmlElementPlugIn plugIn: mElementPlugIns) {
			if (plugIn.getElementClass().isInstance(element))  {
				// Since the attribute-class is checked, this should be type safe
				return plugIn;
			}
		}
		return mDefaultElementPlugIn;
	}
	
	private class CustomAttributeFormatter extends XmlAttributeFormatter {
		@Override
		protected<T> PlugIn<? super T> getCustomFormatter(T attributeValue) {
			for (XmlAttributeFormatter.PlugIn plugIn: mAttributeFormatterPlugIns) {
				if (plugIn.getAttributeClass().isInstance(attributeValue)) {
					// Since the attribute-class is checked, this should be type safe 
					return plugIn;
				}
			}
			return null;
		}
	}
}
