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

package eu.actorsproject.xlim.util;

import java.util.Map;

import eu.actorsproject.util.XmlAttributeRenaming;
import eu.actorsproject.util.XmlPrinter;
import eu.actorsproject.xlim.XlimStateVar;
import eu.actorsproject.xlim.XlimTaskModule;
import eu.actorsproject.xlim.XlimTopLevelPort;
import eu.actorsproject.xlim.XlimType;
import eu.actorsproject.xlim.io.ReaderContext;
import eu.actorsproject.xlim.io.ReaderPlugIn;
import eu.actorsproject.xlim.io.XlimTypeDef;

/**
 * Utility for renaming XLIM identifiers in XML-printouts.
 * 
 * The set of XLIM identifiers that may be renamed are references to
 * a) The ports of the actor (port names)
 * b) The state variables ("source"/"target" references)
 * c) Actions/TaskModules ("target" names)
 * d) Type definitions ("typeName")
 */

public class XlimAttributeRenaming {

	private XmlAttributeRenaming<XlimTopLevelPort> mPortPlugIn=
		new XmlAttributeRenaming<XlimTopLevelPort>(XlimTopLevelPort.class);
	private XmlAttributeRenaming<XlimStateVar> mStateVarPlugIn=
		new XmlAttributeRenaming<XlimStateVar>(XlimStateVar.class);
	private XmlAttributeRenaming<XlimTaskModule> mActionPlugIn=
		new XmlAttributeRenaming<XlimTaskModule>(XlimTaskModule.class);
	private XmlAttributeRenaming<XlimType> mTypePlugIn=
		new XmlAttributeRenaming<XlimType>(XlimType.class);
	
	/**
	 * Rename XLIM identifiers according to the given mapping
	 * 
	 * @param originalIds  Original identifiers (a ReaderContext)
	 * @param mapping      mapping from "original" to "new" identifiers
	 */
	public void rename(ReaderContext originalIds, Map<String,String> mapping) {
		for (Map.Entry<String,String> entry: mapping.entrySet()) {
			String oldId=entry.getKey();
			String newId=entry.getValue();
			
			XlimTopLevelPort port=originalIds.getTopLevelPort(oldId);
			if (port!=null) {
				mPortPlugIn.rename(port, newId);
			}
			else {
				XlimStateVar stateVar=originalIds.getStateVar(oldId);
				if (stateVar!=null) {
					mStateVarPlugIn.rename(stateVar, newId);
				}
				else {
					XlimTaskModule action=originalIds.getTask(oldId);
					if (action!=null) {
						mActionPlugIn.rename(action, newId);
					}
					else {
						XlimTypeDef typeDef=originalIds.getTypeDef(oldId);
						if (typeDef!=null) {
							mTypePlugIn.rename(typeDef.getType(), newId);
						}
						else {
							// Perhaps we should let this pass, but keep it for now to check the mapping
							throw new IllegalArgumentException("Identifier "+oldId+" not found");
						}
					}
				}
			}
		}
	}

	/**
	 * Use the original names (as read from file) for StateVariables and typeDefs
	 * (By default shorter/more readable names are used in print-outs)
	 * 
	 * @param originalIds  Original identifiers (a ReaderContext)
	 */
	public void useOriginalNames(ReaderContext originalIds) {
		// "rename" StateVars
		Map<String,XlimStateVar> originalStateVars=originalIds.getOriginalStateVars();
		for (Map.Entry<String,XlimStateVar> entry: originalStateVars.entrySet()) {
			mStateVarPlugIn.rename(entry.getValue(), entry.getKey());
		}
		
		// "rename" TypeDefs
		Map<String,XlimTypeDef> originalTypeDefs=originalIds.getOriginalTypeDefs();
		ReaderPlugIn readerPlugIn=Session.getReaderPlugIn();
		for (Map.Entry<String,XlimTypeDef> entry: originalTypeDefs.entrySet()) {
			XlimType type=entry.getValue().getType(readerPlugIn,originalIds);
			mTypePlugIn.rename(type, entry.getKey());
		}
	}
	
	/**
	 * Registers plug-ins, which perform the renaming, with an XmlPrinter
	 * @param printer
	 */
	public void registerPlugIns(XmlPrinter printer) {
		printer.register(mPortPlugIn);
		printer.register(mStateVarPlugIn);
		printer.register(mActionPlugIn);
		printer.register(mTypePlugIn);
	}
}
