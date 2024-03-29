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

import java.util.HashMap;
import java.util.Map;

/**
 * Container for TranslationOptions and their values 
 */
public abstract class BagOfTranslationOptions {

	protected Map<String,Object> mValues=new HashMap<String,Object>();

	public Object getValue(String optionName) {
		Object value=mValues.get(optionName);
		if (value==null)
			value=getOverriddenValue(optionName);
		return value;
	}
	
	public boolean getBooleanValue(String optionName) {
		Object value=getValue(optionName);
		if (value!=null && value instanceof Boolean)
			return (Boolean) value;
		else
			throw new IllegalArgumentException("No such boolean-valued option: "+optionName);
	}
	
	public void setValue(String optionName, String value) {
		TranslationOption option=getOption(optionName);
		if (option!=null) {
			Object o=option.checkValue(value);
			if (o!=null)
				mValues.put(optionName, o);
			else
				throw new IllegalArgumentException("Illegal value for option \""+optionName+"\": \""+value+"\"");
		}
		else
			throw new IllegalArgumentException("No such option: "+optionName);
			
	}
	
	public boolean hasOption(String optionName) {
	    return getOption(optionName)!=null;	
	}
	
	public void registerOption(TranslationOption option) {
		// Override for SessionOptions
		throw new UnsupportedOperationException();
	}
	
	public abstract TranslationOption getOption(String optionName);
	
	protected abstract Object getOverriddenValue(String optionName);
}
