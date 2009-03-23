/* 
BEGINCOPYRIGHT X,UC

	Copyright (c) 2009, EPFL
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
	- Neither the names of the copyright holders nor the names 
	  of contributors may be used to endorse or promote 
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
package net.sf.opendf.plugin.causation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.sf.opendf.cal.i2.Environment;
//import net.sf.opendf.cal.i2.environment.HashEnvironment;
import net.sf.opendf.cal.i2.environment.DynamicEnvironmentFrame;

/**
 *  The CausationLoggingEnvironment generates state dependencies between steps (action firings) 
 *  based on the modification of variables. 
 * 
 */

public class CausationLoggingEnvironment extends DynamicEnvironmentFrame {
	
	//
	//  override: HashEnvironment
	//
	
    public Object getByName(Object variable) {
    	if (!writtenVars.contains(variable)) {
    		// only register as read variable if it has not been written to in this 
    		// step
    		readVars.add(variable);
    	}
        return super.getByName(variable);
    }

    public long setByName(Object variable, Object value) {
    	writtenVars.add(variable);
		return super.setByName(variable, value);
    }
    
    public void freezeLocal() {
    	throw new RuntimeException("CausationLoggingEnvironment does not support freezing.");
    }


    //
    //  Ctor
    //
    
	public CausationLoggingEnvironment(CausationTraceBuilder ctb, Environment parent/*, Context context*/) {
		super(parent/*, context*/);
		assert ctb != null;
		
		this.ctb = ctb;
	}
	
	
	//
	//  CausationLoggingEnvironment
	//
	
	void  resetVarSets() {
		readVars.clear();
		writtenVars.clear();
	}
	
	void  commitReadVars() {
		Object step = ctb.currentStep();
    	if (step == null) {
    		resetVarSets();
    		return;
    	}

    	for (Iterator i = readVars.iterator(); i.hasNext(); ) {
    		Object variable = i.next();
        	if (lastModifyingStep.containsKey(variable)) {
        		ctb.beginDependency(lastModifyingStep.get(variable));
        		ctb.setDependencyAttribute("kind", "stateVar");
        		ctb.setDependencyAttribute("var", variable.toString());
        		ctb.setDependencyAttribute("dir", "WR");
        		ctb.endDependency();
        	}
        	
        	if (readingSteps.containsKey(variable)) {
        		Set s = (Set)readingSteps.get(variable);
        		s.add(step);
        	} else {
        		Set s = new HashSet();
        		s.add(step);
        		readingSteps.put(variable, s);
        	}
    	}
    	
    	for (Iterator i = writtenVars.iterator(); i.hasNext(); ) {
    		Object variable = i.next();
    		if (readingSteps.containsKey(variable)) {
    			Set s = (Set)readingSteps.get(variable);
    			readingSteps.remove(variable);
    			s.remove(step);
    			for (Iterator j = s.iterator(); j.hasNext(); ) {
    				Object step2 = j.next();
    				ctb.beginDependency(step2);
            		ctb.setDependencyAttribute("kind", "stateVar");
            		ctb.setDependencyAttribute("var", variable.toString());
            		ctb.setDependencyAttribute("dir", "RW");
            		ctb.endDependency();
    			}
    		}
    		lastModifyingStep.put(variable, step);
    	}
    	resetVarSets();
	}
		
	private CausationTraceBuilder   ctb;
	
	/**
	 * This maps each variable to the step during which it was last modified.
	 */
	private Map  lastModifyingStep = new HashMap();
	private Map  readingSteps = new HashMap();
	private Set  readVars = new HashSet();
	private Set  writtenVars = new HashSet();
}

