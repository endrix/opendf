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

package eu.actorsproject.util;

/**
 * Container for customized attribute formatters (XmlAttributeFormatter.PlugIn)
 */
public class XmlAttributeFormatter {

	public static abstract class PlugIn<T> {
		private Class<T> mClass;
		
		public PlugIn(Class<T> c) {
			mClass=c;
		}
		
		/**
		 * @return the "greatest" super class, which this plug-in supports
		 */
		public final Class<T> getAttributeClass() {
			return mClass;
		}
		
		/**
		 * @param value
		 * @return formatted representation of 'value'
		 */
		public abstract String getAttributeValue(T value);
	}
	
	/**
	 * @param o
	 * @return true if there is a custom formatter for objetcs of value's class
	 */
	public boolean hasCustomFormatter(Object value) {
		return getCustomFormatter(value)!=null;
	}
	
	/**
	 * @param attributeName  
	 * @param attributeValue
	 * @return the string attributeName="attribute value", where the attribute value
	 *         is formatted by a custom formatter (if present) or the toString() method
	 *         otherwise.
	 */
	public<T> String getAttributeDefinition(String attributeName, T attributeValue) {
		return getAttributeDefinition(attributeName,attributeValue,attributeValue);
	}
	
	/**
	 * @param attributeName  
	 * @param attributeValue
	 * @param defaultValue   value to convert toString, if formatter not present
	 * 
	 * @return the string attributeName="attribute value", where the attribute value
	 *         is formatted by a custom formatter (if present).
	 *         Otherwise defaultValue.toString() is used (empty string for null default value).
	 */
	public<T> String getAttributeDefinition(String attributeName, T attributeValue, Object defaultValue) {
		PlugIn<? super T> plugIn=getCustomFormatter(attributeValue);
		String value=null;
		if (plugIn!=null)
			value=plugIn.getAttributeValue(attributeValue);
		if (value==null && defaultValue!=null)
			value=defaultValue.toString();
		if (value!=null)
			return attributeName+"=\""+value+"\"";
		else
			return "";
	}
	
	/**
	 * Adds the attributeName="attribute value" to 'otherDefinitions' with proper spacing.
	 * @param otherDefinitions  Attribute definitions appearing prior to this one
	 * @param attributeName
	 * @param attributeValue
	 * @return concatenated string
	 */
	public<T> String addAttributeDefinition(String otherDefinitions, String attributeName, T attributeValue) {
		return addAttributeDefinition(otherDefinitions,attributeName,attributeValue,attributeValue);
	}
	
	/**
	 * Adds the attributeName="attribute value" to 'otherDefinitions' with proper spacing.
	 * @param otherDefinitions  Attribute definitions appearing prior to this one
	 * @param attributeName
	 * @param attributeValue
	 * @return concatenated string
	 */
	public<T> String addAttributeDefinition(String otherDefinitions, 
			                             String attributeName, T attributeValue,
			                             Object defaultValue) {
		String def=getAttributeDefinition(attributeName, attributeValue, defaultValue);
		String optSpace=(otherDefinitions!=null && !otherDefinitions.isEmpty())? " " : "";
		return otherDefinitions+optSpace+def;
	}
	
	protected<T> PlugIn<? super T> getCustomFormatter(T attributeValue) {
		return null;
	}
}
