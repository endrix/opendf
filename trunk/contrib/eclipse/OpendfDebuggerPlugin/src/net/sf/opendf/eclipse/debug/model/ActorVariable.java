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
import static net.sf.orcc.debug.DDPConstants.ATTR_EXPRESSION;
import static net.sf.orcc.debug.DDPConstants.ATTR_FRAME_NAME;
import static net.sf.orcc.debug.DDPConstants.ATTR_VAR_NAME;
import static net.sf.orcc.debug.DDPConstants.REQUEST;
import static net.sf.orcc.debug.DDPConstants.REQ_GET_VALUE;
import static net.sf.orcc.debug.DDPConstants.REQ_SET_VARIABLE;
import net.sf.orcc.debug.DDPClient;
import net.sf.orcc.debug.DDPConstants;
import net.sf.orcc.debug.type.AbstractType;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A variable in an Actor stack frame
 * 
 * @author Rob Esser
 * @version 23 March 2009
 */
public class ActorVariable extends OpendfDebugElement implements IVariable {

	/**
	 * Stack frame this variable belongs to.
	 */
	private ActorStackFrame frame;

	/**
	 * Variable name.
	 */
	protected String name;

	protected AbstractType type;

	/**
	 * Constructs a variable contained in the given stack frame with the given
	 * name.
	 * 
	 * @param frame
	 *            owning stack frame
	 */
	protected ActorVariable(ActorStackFrame frame) {
		super(frame.getDebugTarget());
		this.frame = frame;
	}

	/**
	 * Constructs a variable contained in the given stack frame with the given
	 * name.
	 * 
	 * @param frame
	 *            owning stack frame
	 * @param name
	 *            variable name
	 */
	public ActorVariable(ActorStackFrame frame, JSONObject variable)
			throws JSONException {
		super(frame.getDebugTarget());
		this.frame = frame;
		name = variable.getString(DDPConstants.ATTR_VAR_NAME);
		type = DDPClient.getType(variable.get(DDPConstants.ATTR_VAR_TYPE));
	}

	@Override
	public String getName() throws DebugException {
		return name;
	}

	@Override
	public String getReferenceTypeName() throws DebugException {
		return type.toString();
	}

	@Override
	public IValue getValue() throws DebugException {
		try {
			JSONObject request = new JSONObject();
			request.put(REQUEST, REQ_GET_VALUE);
			request.put(ATTR_ACTOR_NAME, frame.getComponentName());
			request.put(ATTR_FRAME_NAME, frame.getName());
			request.put(ATTR_VAR_NAME, getName());

			JSONObject reply = sendRequest(request);
			return new ActorValue(frame, reply);
		} catch (JSONException e) {
			IStatus status = new Status(IStatus.ERROR, ID_PLUGIN, "json error",
					e);
			throw new DebugException(status);
		}
	}

	@Override
	public boolean hasValueChanged() throws DebugException {
		return false;
	}

	@Override
	public void setValue(IValue value) throws DebugException {
	}

	@Override
	public void setValue(String expression) throws DebugException {
		try {
			JSONObject request = new JSONObject();
			request.put(REQUEST, REQ_SET_VARIABLE);
			request.put(ATTR_ACTOR_NAME, frame.getComponentName());
			request.put(ATTR_FRAME_NAME, frame.getName());
			request.put(ATTR_VAR_NAME, getName());
			request.put(ATTR_EXPRESSION, expression);

			sendRequest(request);
		} catch (JSONException e) {
			IStatus status = new Status(IStatus.ERROR, ID_PLUGIN, "json error",
					e);
			throw new DebugException(status);
		}

		fireChangeEvent(DebugEvent.CONTENT);
	}

	@Override
	public boolean supportsValueModification() {
		return true;
	}

	@Override
	public boolean verifyValue(IValue value) throws DebugException {
		return false;
	}

	@Override
	public boolean verifyValue(String expression) throws DebugException {
		return true;
	}

}
