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

import eu.actorsproject.xlim.XlimDesign;
import java.util.Iterator;

public class XmlPrinter extends OutputGenerator {

	public XmlPrinter(PrintStream sink) {
		super(sink);
	}
	
	public void printDocument(XmlElement document) {
		println(xmlDeclaration());
		printElement(document);
	}
	
	public void printElement(XmlElement element) {
		Iterator<? extends XmlElement> pChild=element.getChildren().iterator();
		boolean empty=!pChild.hasNext();
		
		println(startTag(element,empty));
		increaseIndentation();
		while (pChild.hasNext())
			printElement(pChild.next());
		decreaseIndentation();
		if (!empty) {
			println("</"+element.getTagName()+">");
		}
	}
	
	public void printComment(String s) {
		println("<!-- "+s+" -->");
	}
	
	protected String xmlDeclaration() {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";
	}
	
	protected String startTag(XmlElement element, boolean isEmpty) {
		String attributes=element.getAttributeDefinitions();
		String space=attributes.length()!=0? " " : "";
		String start="<" + element.getTagName(); 
		String end=isEmpty? "/>" : ">";
		return start+space+attributes+end;
	}
}
