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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import eu.actorsproject.xlim.XlimFactory;
import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.implementation.DefaultXlimFactory;
import eu.actorsproject.xlim.implementation.InstructionSet;
import eu.actorsproject.xlim.implementation.OperationFactory;
import eu.actorsproject.xlim.io.XlimAttributeList;
import eu.actorsproject.xlim.io.ReaderContext;
import eu.actorsproject.xlim.io.ReaderPlugIn;
import eu.actorsproject.xlim.type.TypeFactory;
import eu.actorsproject.xlim.type.TypeSystem;

/**
 * Session collects all static properties (singleton objects) that
 * are common to a "session" (execution of a compiler/tool): 
 * - The options of the compiler (tool)
 * - The type system
 * - The XLIM instruction set
 * - The console that is used for diagnostic messages
 * - Debug and tracing facilities
 * 
 * Usage by "clients": by means of the "getters" (getTypeFactory etc.)
 * 
 * Usage by Session subclasses (Java applications):
 * (1) Register SessionPlugIns to add features (e.g. in constructor)
 *     Subclasses before superclasses
 * (2) Call the initSession method (e.g in main())
 * 
 *  Override the "create" methods if appropriate
 */
public class Session {

	private static Session sSingletonSession;
	
	private ArrayList<XlimFeature> mPlugIns=
		new ArrayList<XlimFeature>();
	protected TypeSystem mTypeSystem;
	protected InstructionSet mInstructionSet;
	protected XlimFactory mXlimFactory;
	protected ReaderPlugIn mReaderPlugIn;
	protected SessionOptions mSessionOptions;
	
	protected Session() {
		mSessionOptions=new SessionOptions();
	}
	
	public static BagOfTranslationOptions getSessionOptions() {
		return sSingletonSession.mSessionOptions;
	}
	
	public static TypeFactory getTypeFactory() {
		return sSingletonSession.mTypeSystem;
	}
	
	public static OperationFactory getOperationFactory() {
		return sSingletonSession.mInstructionSet;
	}
	
	public static XlimFactory getXlimFactory() {
		return sSingletonSession.mXlimFactory;
	}
	
	public static ReaderPlugIn getReaderPlugIn() {
		return sSingletonSession.mReaderPlugIn;
	}
	
	private static synchronized void setInstance(Session s) {
		if (sSingletonSession!=null) {
			// throw new IllegalStateException("Session already created");
			System.out.println("*** Beware of dragons, assumed singleton Session initialized twice! ***");
		}
		sSingletonSession=s;
	}
	
	protected void register(XlimFeature plugIn) {
		mPlugIns.add(plugIn);
	}
	
	protected void initSession(String args[]) {
		setInstance(this);
		createTypeSystem();
		createInstructionSet();
		createXlimFactory();
		createReaderPlugIn();
		initTypeSystem();
		mTypeSystem.completeInitialization();
		initInstructionSet();
	}
	
	
	protected void createTypeSystem() {
		mTypeSystem=new TypeSystem();
	}
	
	protected void createInstructionSet() {
		mInstructionSet=new InstructionSet();
	}
	
	protected void createXlimFactory() {
		mXlimFactory=new DefaultXlimFactory();
	}
	protected void createReaderPlugIn() {
		mReaderPlugIn=new DefaultReaderPlugIn();
	}
		
	private void initTypeSystem() {
		for (XlimFeature p: mPlugIns)
			p.initialize(mTypeSystem);
	}
	
	private void initInstructionSet() {
		for (XlimFeature p: mPlugIns)
			p.initialize(mInstructionSet);
	}
	
	class DefaultReaderPlugIn implements ReaderPlugIn {

		public XlimFactory getFactory() {
			return mXlimFactory;
		}

		@Override
		public XlimType getType(String typeName, XlimAttributeList attributes) {
			return mTypeSystem.create(typeName, attributes);
		}

		@Override
		public void setAttributes(XlimOperation op, XlimAttributeList attributes, ReaderContext context) {
			mInstructionSet.setAttributes(op,attributes,context);
		}
	}
	
	protected class SessionOptions extends BagOfTranslationOptions {

		private Map<String,TranslationOption> mOptions=new HashMap<String,TranslationOption>();
		
		@Override
		public void registerOption(TranslationOption option) {
			mOptions.put(option.getName(),option);
		}
		
		@Override
		public TranslationOption getOption(String optionName) {
			return mOptions.get(optionName);
		}

		@Override
		protected Object getOverriddenValue(String optionName) {
			TranslationOption option=getOption(optionName);
			if (option!=null)
				return option.getDefaultValue();
			else
				return null;
		}
	}

}
