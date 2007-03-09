/* 
BEGINCOPYRIGHT X,UC
	
	Copyright (c) 2007, Xilinx Inc.
	Copyright (c) 2003, The Regents of the University of California
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

package net.sf.caltrop.cal.interpreter.ast;

import java.io.Serializable;
import java.util.List;

import net.sf.caltrop.cal.interpreter.util.Utility;


public class Actor extends ASTNode implements Serializable {

	
	public  Actor() {}
    
	public Actor(Import [] imports, String name, String pkg, Decl [] parameters,
        PortDecl [] inputPorts, PortDecl [] outputPorts,
        Action [] initializers, Action [] actions, Decl [] stateVars)
    {
		this(imports, name, pkg, parameters, inputPorts, outputPorts, initializers, actions, stateVars, null);
	}
		
	public Actor(Import [] imports, String name, String pkg, Decl [] parameters,
        PortDecl [] inputPorts, PortDecl [] outputPorts,
        Action [] initializers, Action [] actions, Decl [] stateVars, ScheduleFSM scheduleFSM)
    {
		this(imports, name, pkg, parameters, inputPorts, outputPorts, initializers, actions, stateVars, scheduleFSM, null, null);
	}
	
	public Actor(Import [] imports, String name, String pkg, Decl [] parameters,
        PortDecl [] inputPorts, PortDecl [] outputPorts,
        Action [] initializers, Action [] actions, Decl [] stateVars,
        ScheduleFSM scheduleFSM, List[] priorities, Expression [] invariants)
    {
		this.imports = imports;
        this.name = name;

        if (pkg == null) throw new IllegalArgumentException("Package name must be non null");
        this.pkg = pkg;
        
        this.parameters = parameters;
        this.inputPorts = inputPorts;
        this.outputPorts = outputPorts;
        this.initializers = initializers;
        this.actions = actions;
        this.stateVars = stateVars;
        this.scheduleFSM = scheduleFSM;
        this.priorities = priorities;
        this.invariants = invariants;
    }

    public Import[] getImports() {
        return imports;
    }

    public Action[] getActions() {
        return actions;
    }

    public Action[] getInitializers() {
        return initializers;
    }

    public PortDecl[] getInputPorts() {
        return inputPorts;
    }

    public Expression[] getInvariants() {
        return invariants;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the package or the empty String if no package
     * @return a non-null String
     */
    public String getPackage() {
        return this.pkg;
    }

    public Decl[] getParameters() {
        return parameters;
    }

    public PortDecl[] getOutputPorts() {
        return outputPorts;
    }

    public Decl[] getStateVars() {
        return stateVars;
    }

    public ScheduleFSM getScheduleFSM() {
    	return scheduleFSM;
    }

    public List [] getPriorities() {
    	return priorities;
    }

    

    private Import []       imports;
    private String          name;
    private String          pkg;
    private Decl []         parameters;
    private PortDecl []     inputPorts;
    private PortDecl []     outputPorts;
    private Action []       actions;
    private Decl []         stateVars;
    private ScheduleFSM     scheduleFSM;
    private List [] 		priorities;
    private Expression []   invariants;


    private Action []       initializers;

    public String toString() {
        Utility.increaseTabDepth(2);
        String tabs = Utility.getHeadingTabs();
        String result = "Actor " + this.name + ":\n" + tabs + "inputPorts:\n" + Utility.arrayToString(this.inputPorts) +
                "\n" + tabs + "outputPorts:\n" + Utility.arrayToString(this.outputPorts) + "\n" + tabs + "actions:\n" +
                Utility.arrayToString(this.actions);
        Utility.decreaseTabDepth(2);
        return result;
    }

}
