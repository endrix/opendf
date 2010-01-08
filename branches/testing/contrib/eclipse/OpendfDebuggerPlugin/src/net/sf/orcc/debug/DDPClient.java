/*
 * Copyright (c) 2009, IETR/INSA Rennes
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the IETR/INSA of Rennes nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package net.sf.orcc.debug;

import net.sf.orcc.debug.type.AbstractType;
import net.sf.orcc.debug.type.BoolType;
import net.sf.orcc.debug.type.IntType;
import net.sf.orcc.debug.type.ListType;
import net.sf.orcc.debug.type.StringType;
import net.sf.orcc.debug.type.UintType;
import net.sf.orcc.debug.type.VoidType;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Implementation of a Dataflow Debug Protocol client.
 * 
 * @author Matthieu Wipliez
 * 
 */
public class DDPClient {

	/**
	 * Returns a Location from the given JSON array.
	 * 
	 * @param array
	 *            a JSON array
	 * @return a Location from the given JSON array
	 * @throws JSONException
	 */
	public static Location getLocation(JSONArray array) throws JSONException {
		int lineNumber = array.getInt(0);
		int charStart = array.getInt(1);
		int charEnd = array.getInt(2);
		return new Location(lineNumber, charStart, charEnd);
	}

	/**
	 * Returns an AbstractType from the given object.
	 * 
	 * @param obj
	 *            an object.
	 * @return an abstract type
	 * @throws JSONException
	 */
	public static AbstractType getType(Object obj) throws JSONException {
		AbstractType type = null;

		if (obj instanceof String) {
			String name = (String) obj;
			if (name.equals(BoolType.NAME)) {
				type = new BoolType();
			} else if (name.equals(StringType.NAME)) {
				type = new StringType();
			} else if (name.equals(VoidType.NAME)) {
				type = new VoidType();
			} else {
				throw new JSONException("Unknown type: " + name);
			}
		} else if (obj instanceof JSONArray) {
			JSONArray array = (JSONArray) obj;
			String name = array.getString(0);
			if (name.equals(IntType.NAME)) {
				int size = array.getInt(1);
				type = new IntType(size);
			} else if (name.equals(UintType.NAME)) {
				int size = array.getInt(1);
				type = new UintType(size);
			} else if (name.equals(ListType.NAME)) {
				int size = array.getInt(1);
				AbstractType subType = getType(array.get(2));
				type = new ListType(size, subType);
			} else {
				throw new JSONException("Unknown type: " + name);
			}
		} else {
			throw new JSONException("Invalid type definition: "
					+ obj.toString());
		}

		return type;
	}

}
