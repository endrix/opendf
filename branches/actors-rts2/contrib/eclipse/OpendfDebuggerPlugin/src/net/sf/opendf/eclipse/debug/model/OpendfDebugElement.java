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

import net.sf.opendf.eclipse.debug.OpendfDebugConstants;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Common functions for all debug elements.
 * 
 * @author Rob Esser
 * @version 19 March 2009
 */
public abstract class OpendfDebugElement extends DebugElement {

	/**
	 * Constructs a new debug element in the given target.
	 * 
	 * @param target
	 *            debug target
	 */
	public OpendfDebugElement(IDebugTarget target) {
		super(target);
	}

	/**
	 * Returns the breakpoint manager
	 * 
	 * @return the breakpoint manager
	 */
	protected IBreakpointManager getBreakpointManager() {
		return DebugPlugin.getDefault().getBreakpointManager();
	}

	/**
	 * 
	 * @see org.eclipse.debug.core.model.IDebugElement#getModelIdentifier()
	 */
	public String getModelIdentifier() {
		return OpendfDebugConstants.ID_OPENDF_DEBUG_MODEL;
	}

	/**
	 * Returns the debug target as a Opendf target.
	 * 
	 * @return Opendf debug target
	 */
	protected OpendfDebugTarget getOpendfDebugTarget() {
		return (OpendfDebugTarget) getDebugTarget();
	}

	/**
	 * Throws a debug exception with the given message, error code, and
	 * underlying exception.
	 */
	protected DebugException newDebugException(String message, int code,
			Throwable exception) {
		return new DebugException(new Status(IStatus.ERROR,
				OpendfDebugConstants.ID_PLUGIN, code, message, exception));
	}

	/**
	 * Throws a debug exception with the given message, error code, and
	 * underlying exception.
	 */
	protected DebugException newDebugExceptionJSON(int code,
			JSONException exception) {
		return newDebugException("JSON exception", code, exception);
	}

	/**
	 * Sends a request to the execution engine, waits for and returns the reply.
	 * 
	 * @param request
	 *            the request as a JSON object
	 * @return reply the reply as a JSON object
	 * @throws DebugException
	 *             if the request fails
	 */
	public JSONObject sendRequest(JSONObject request) throws DebugException {
		return getOpendfDebugTarget().sendRequest(request);
	}

}
