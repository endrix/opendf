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

package eu.actorsproject.xlim.xlim2c;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import eu.actorsproject.util.IntrusiveList;
import eu.actorsproject.util.OutputGenerator;

public class Scope {

	private IntrusiveList<TemporaryVariable> mTemporaries;
	
	public Scope() {
		mTemporaries=new IntrusiveList<TemporaryVariable>();
	}
	
	public void add(TemporaryVariable temp) {
		mTemporaries.addLast(temp);
	}
	
	public void remove(TemporaryVariable temp) {
		temp.out();
	}
	
	public void generateDeclaration(OutputGenerator output, 
			                        TopLevelSymbolTable topLevelSymbols) {
		// Sort allocated temporaries after type
		HashMap<String,ArrayList<TemporaryVariable>> typeMap =
			new HashMap<String,ArrayList<TemporaryVariable>>();
			
		for (TemporaryVariable temp: mTemporaries) {
			String cType=topLevelSymbols.getCName(temp.getType());
			ArrayList<TemporaryVariable> list=typeMap.get(cType);
			if (list==null) {
				list=new ArrayList<TemporaryVariable>();
				typeMap.put(cType, list);
			}
			list.add(temp);
		}
			
		// Print them
		for (Map.Entry<String,ArrayList<TemporaryVariable>> entry: typeMap.entrySet()) {
			boolean first=true;
			output.print(entry.getKey());
			for (TemporaryVariable temp: entry.getValue()) {
				output.print((first? " ":",")+temp.getCName());
				first=false;
			}
			output.println(";");
		}
	}
}
