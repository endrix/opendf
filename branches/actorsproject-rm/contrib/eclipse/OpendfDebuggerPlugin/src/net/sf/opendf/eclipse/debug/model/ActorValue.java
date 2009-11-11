/* 
BEGINCOPYRIGHT X
	
	Copyright (c) 2009, Xilinx Inc.
	All rights reserved.
	
	Redistribution and use in source and binary forms, 
	with or without modification, are permitted provided 
	that the following conditions are met:
	- Redistributions of source code must retain the above 
	  copyright notice, this list of conditions and the 
	  following disclaimer.
	- Redistributions in binary form must reproduce the 
	  above copyright notice, this list of conditions and 
	  the following disclaimer in the documentation and/or 
	  other materials provided with the distribution.
	- Neither the name of the copyright holder nor the names 
	  of its contributors may be used to endorse or promote 
	  products derived from this software without specific 
	  prior written permission.
	
	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
	CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
	INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
	MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
	DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
	CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
	SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
	NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
	LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
	HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
	CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
	OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
	SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
	
ENDCOPYRIGHT
 */
package net.sf.opendf.eclipse.debug.model;

import net.sf.orcc.debug.DDPConstants;
import net.sf.orcc.debug.type.AbstractType;
import net.sf.orcc.debug.type.ListType;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Value of an Actor variable.
 * 
 * @author Rob Esser
 * @author Matthieu Wipliez
 */
public class ActorValue extends OpendfDebugElement implements IValue {

	public static ActorValue createValue(ActorVariable variable,
			JSONObject value) {
		AbstractType type = variable.getType();
		if (type instanceof ListType) {
			return new ActorArrayValue(variable, value);
		} else {
			return new ActorValue(variable, value);
		}
	}

	private String value;

	private ActorVariable variable;

	private IVariable[] variables;

	protected ActorValue(ActorVariable variable, JSONObject value) {
		super(variable.getOpendfDebugTarget());
		this.variable = variable;
		parseJSON(value);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ActorValue
				&& ((ActorValue) obj).value.equals(value);
	}

	@Override
	public String getReferenceTypeName() throws DebugException {
		return null;
	}

	@Override
	public String getValueString() throws DebugException {
		return value;
	}

	protected ActorVariable getVariable() {
		return variable;
	}

	@Override
	public IVariable[] getVariables() throws DebugException {
		return variables;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public boolean hasVariables() throws DebugException {
		return (variables.length > 0);
	}

	@Override
	public boolean isAllocated() throws DebugException {
		return true;
	}

	/**
	 * Parses the JSON object and initializes this value from it.
	 * 
	 * @param value
	 *            a JSON object
	 */
	private void parseJSON(JSONObject value) {
		try {
			this.value = value.getString(DDPConstants.ATTR_VALUE);
			JSONArray array = value.getJSONArray(DDPConstants.ATTR_VARIABLES);
			int length = array.length();
			variables = new IVariable[length];
			for (int i = 0; i < length; i++) {
				variables[i] = new ActorVariable(variable.getFrame(), array
						.getJSONObject(i));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
