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

import static net.sf.opendf.eclipse.debug.OpendfDebugConstants.ID_PLUGIN;
import static net.sf.orcc.debug.DDPConstants.ATTR_ACTOR_NAME;
import static net.sf.orcc.debug.DDPConstants.ATTR_FRAME_NAME;
import static net.sf.orcc.debug.DDPConstants.ATTR_INDEXES;
import static net.sf.orcc.debug.DDPConstants.ATTR_VAR_NAME;
import static net.sf.orcc.debug.DDPConstants.REQUEST;
import static net.sf.orcc.debug.DDPConstants.REQ_GET_VALUE;
import net.sf.orcc.debug.type.AbstractType;
import net.sf.orcc.debug.type.ListType;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IIndexedValue;
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
public class ActorArrayValue extends ActorValue implements IIndexedValue {

	public ActorArrayValue(ActorVariable variable, JSONObject value) {
		super(variable, value);
	}

	@Override
	public int getInitialOffset() {
		return 0;
	}

	@Override
	public int getSize() throws DebugException {
		AbstractType type = getVariable().getType();
		if (type instanceof ListType) {
			return ((ListType) type).getSize();
		}
		throw newDebugException("variable does not have list type",
				DebugException.INTERNAL_ERROR, null);
	}

	@Override
	public IVariable getVariable(int offset) throws DebugException {
		return null;
	}

	@Override
	public IVariable[] getVariables(int offset, int length)
			throws DebugException {
		ActorStackFrame frame = getVariable().getFrame();
		try {
			JSONObject request = new JSONObject();
			request.put(REQUEST, REQ_GET_VALUE);
			request.put(ATTR_ACTOR_NAME, frame.getComponentName());
			request.put(ATTR_FRAME_NAME, frame.getName());
			request.put(ATTR_VAR_NAME, getVariable().getName());

			JSONArray array = new JSONArray();
			array.put(offset);
			array.put(length);
			request.put(ATTR_INDEXES, array);

			JSONObject reply = sendRequest(request);
			return ActorValue.createValue(getVariable(), reply).getVariables();
		} catch (JSONException e) {
			IStatus status = new Status(IStatus.ERROR, ID_PLUGIN, "json error",
					e);
			throw new DebugException(status);
		}
	}

	@Override
	public boolean hasVariables() throws DebugException {
		return (getSize() > 0);
	}

}
