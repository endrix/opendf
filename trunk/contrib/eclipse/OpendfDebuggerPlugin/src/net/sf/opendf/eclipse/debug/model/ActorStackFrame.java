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

import net.sf.orcc.debug.DDPClient;
import net.sf.orcc.debug.DDPConstants;
import net.sf.orcc.debug.Location;

import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Actor stack frame representing the execution stack of an actor action or if
 * an actor has not yet selected an action then the enabling status of each
 * action is displayed
 * 
 * @author Rob Esser
 * @version 23 March 2009
 */
public class ActorStackFrame extends OpendfDebugElement implements IStackFrame {

	private ActorThread actorThread;

	/**
	 * the component name of the artifact
	 */
	private String componentName;

	/**
	 * the id of the frame
	 */
	private int frameId;

	/**
	 * the name of the artifact the frame represents
	 */
	private String functionOrActionName;

	/**
	 * the location in the source of the frame
	 */
	private Location location;

	/**
	 * the filename of the artifact
	 */
	private String sourceFileName;

	/**
	 * Constructs a stack frame in the given thread with the given frame data.
	 * 
	 * @param thread
	 * @param data
	 *            frame data
	 * @param id
	 *            stack frame id (0 is the bottom of the stack)
	 */
	public ActorStackFrame(ActorThread thread, JSONObject frame, int id) {
		super(thread.getDebugTarget());
		frameId = id;
		actorThread = thread;
		parseJSON(frame);
	}

	@Override
	public boolean canResume() {
		return getThread().canResume();
	}

	@Override
	public boolean canStepInto() {
		return getThread().canStepInto();
	}

	@Override
	public boolean canStepOver() {
		return getThread().canStepOver();
	}

	@Override
	public boolean canStepReturn() {
		return getThread().canStepReturn();
	}

	@Override
	public boolean canSuspend() {
		return getThread().canSuspend();
	}

	@Override
	public boolean canTerminate() {
		return getThread().canTerminate();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ActorStackFrame) {
			ActorStackFrame sf = (ActorStackFrame) obj;
			return sf.getThread().equals(getThread())
					&& sf.getSourceFileName().equals(getSourceFileName())
					&& sf.frameId == frameId;
		}
		return false;
	}

	@Override
	public int getCharEnd() throws DebugException {
		return -1;
	}

	@Override
	public int getCharStart() throws DebugException {
		return -1;
	}

	/**
	 * Returns this stack frame's actor name.
	 * 
	 * @return
	 */
	public String getComponentName() {
		return componentName;
	}

	/**
	 * Returns this stack frame's unique identifier within its thread
	 */
	protected int getIdentifier() {
		return frameId;
	}

	@Override
	public int getLineNumber() throws DebugException {
		return location.getLineNumber();
	}

	@Override
	public String getName() throws DebugException {
		return functionOrActionName;
	}

	@Override
	public IRegisterGroup[] getRegisterGroups() throws DebugException {
		return null;
	}

	/**
	 * Returns the name of the source file this stack frame is associated with.
	 */
	public String getSourceFileName() {
		return sourceFileName;
	}

	@Override
	public IThread getThread() {
		return actorThread;
	}

	@Override
	public IVariable[] getVariables() throws DebugException {
		return actorThread.getVariables(this);
	}

	@Override
	public int hashCode() {
		return getSourceFileName().hashCode() + frameId;
	}

	@Override
	public boolean hasRegisterGroups() throws DebugException {
		return false;
	}

	@Override
	public boolean hasVariables() throws DebugException {
		return getVariables().length > 0;
	}

	@Override
	public boolean isStepping() {
		return getThread().isStepping();
	}

	@Override
	public boolean isSuspended() {
		return getThread().isSuspended();
	}

	@Override
	public boolean isTerminated() {
		return getThread().isTerminated();
	}

	/**
	 * Parses the JSON object and initializes this frame from it.
	 * 
	 * @param frame
	 *            a JSON object
	 */
	private void parseJSON(JSONObject frame) {
		try {
			String extractedFileName = frame.getString(DDPConstants.ATTR_FILE);
			sourceFileName = (new Path(extractedFileName)).lastSegment();
			componentName = frame.getString(DDPConstants.ATTR_ACTOR_NAME);
			functionOrActionName = frame
					.getString(DDPConstants.ATTR_ACTION_NAME);
			location = DDPClient.getLocation(frame
					.getJSONArray(DDPConstants.ATTR_LOCATION));

			JSONArray variables = frame
					.getJSONArray(DDPConstants.ATTR_VARIABLES);
			IVariable[] vars = new IVariable[variables.length() + 1];
			vars[0] = new ActorStateContainerVariable(this);
			int length = variables.length();
			for (int i = 0; i < length; i++) {
				vars[i + 1] = new ActorVariable(this, variables
						.getJSONObject(i));
			}

			actorThread.setVariables(this, vars);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void resume() throws DebugException {
		getThread().resume();
	}

	@Override
	public void stepInto() throws DebugException {
		getThread().stepInto();
	}

	@Override
	public void stepOver() throws DebugException {
		getThread().stepOver();
	}

	@Override
	public void stepReturn() throws DebugException {
		getThread().stepReturn();
	}

	@Override
	public void suspend() throws DebugException {
		getThread().suspend();
	}

	@Override
	public void terminate() throws DebugException {
		getThread().terminate();
	}

}
