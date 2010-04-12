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

import java.util.ArrayList;
import java.util.List;

import eu.actorsproject.util.XmlAttributeFormatter;
import eu.actorsproject.util.XmlElement;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.XlimTypeArgument;
import eu.actorsproject.xlim.XlimTypeKind;

/**
 * An XlimTypeElement, which specifies the construction of a type
 */
public class XlimTypeConstruction extends XlimTypeElement {

	private XlimTypeKind mTypeConstructor;
	private XlimLocation mLocation;
	private List<TypeArgElement> mTypeArgs;
	private XlimType mType;
	
	/**
	 * Create a, possibly unresolved, type construction (parameters may refer to typeDefs)   
	 * @param typeConstructor  Name of (built-in) type or typeDef
	 * @param location         Location in XLIM file
	 */
	public XlimTypeConstruction(XlimTypeKind typeConstructor, XlimLocation location) {
		mTypeConstructor=typeConstructor;
		mLocation=location;
		mTypeArgs=new ArrayList<TypeArgElement>();
	}
	
	/**
	 * Create the root element that corresponds to 'type'
	 * @param a type
	 */
	public XlimTypeConstruction(XlimType type) {
		mTypeConstructor=type.getTypeKind();
		mLocation=null;
		mTypeArgs=new ArrayList<TypeArgElement>();
		mType=type;
	}
	
	/**
	 * Adds a value parameter to this type element
	 * @param name  the name of the (formal) parameter
	 * @param value the actual value of the parameter
	 */
	public void addValuePar(String name, String value) {
		mTypeArgs.add(new ValueParElement(name,value));
	}

	/**
	 * Adds a type parameter to this type element
	 * @param name         the name of the (formal) parameter
	 * @param typeElement  the actual value of the parameter
	 */
	public void addTypePar(String name, XlimTypeElement typeElement) {
		mTypeArgs.add(new TypeParElement(name,typeElement));
	}
	
	@Override
	public XlimType createType(ReaderPlugIn plugIn, ReaderContext context) {
		if (mType!=null)
			return mType;

		ArrayList<XlimTypeArgument> arguments=new ArrayList<XlimTypeArgument>();
		for (TypeArgElement element: mTypeArgs) {
			XlimTypeArgument arg=element.createTypeArgument(plugIn, context);
			if (arg!=null)
				arguments.add(arg);
			else
				return null; // No point in proceeding here (error is reported)
		}
		
		try {
			mType=mTypeConstructor.createType(arguments);
		}
		catch (RuntimeException ex) {
			throw new XlimReaderError(ex.getMessage(), mLocation);
		}
		
		return mType;
	}

	@Override
	public XlimType getType() {
		assert(mType!=null);
		return mType;
	}
	
	@Override
	public String getTagName() {
		return "type";
	}
		
	@Override
	public Iterable<? extends XmlElement> getChildren() {
		return mTypeArgs;
	}

	@Override
	public String getAttributeDefinitions(XmlAttributeFormatter formatter) {
		return "name=\"" + mTypeConstructor.getTypeName() + "\"";
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof XlimTypeConstruction) {
			XlimTypeConstruction other=(XlimTypeConstruction) o;
			
			if (mType!=null && other.mType!=null)
				return mType==other.mType;
			else if (mTypeConstructor==other.mTypeConstructor) {
				// First check that all arguments of this type element
				// have a corresponding type element in the other one
				for (TypeArgElement arg1: mTypeArgs) {
					TypeArgElement arg2=other.find(arg1.getName());
					if (arg2==null || arg1.equals(arg2)==false)
						return false;
				}
				
				// Then check that the other type element has no extra arguments
				for (TypeArgElement arg2: other.mTypeArgs) {
					if (find(arg2.getName())==null)
						return false;
				}
				
				// Ok, they are equal
				return true;
			}
			else
				return false;
		}
		else
			return false;
	}
	
	TypeArgElement find(String name) {
		for (TypeArgElement arg: mTypeArgs)
			if (name.equals(arg.getName()))
				return arg;
		return null; // Not found
	}
	
	@Override 
	public int hashCode() {
		int h=mTypeConstructor.hashCode();
		for (TypeArgElement arg: mTypeArgs)
			h += arg.hashCode(); // Order of argument doesn't matter
		return h;
	}
}
