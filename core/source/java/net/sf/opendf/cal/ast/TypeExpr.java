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

package net.sf.opendf.cal.ast;

import java.util.Map;

import net.sf.opendf.cal.i2.Configuration;
import net.sf.opendf.cal.i2.Environment;
import net.sf.opendf.cal.i2.Evaluator;
import net.sf.opendf.cal.i2.OperandStack;
import net.sf.opendf.cal.i2.types.Type;
import net.sf.opendf.cal.i2.types.TypeSystem;

/**
 * @author Christopher Chang <cbc@eecs.berkeley.edu>
 * @author Jorn W. Janneck <jorn.janneck@xilinx.com>
 */

public class TypeExpr extends ASTNode {
    public String getName() {
        return name;
    }

    public TypeExpr[] getParameters() {
        return parameters;
    }
    
    public Map<String, TypeExpr>  getTypeParameters() {
    	return typeParameters;
    }
    
    public Map<String, Expression>  getValueParameters() {
    	return valueParameters;
    }


    public TypeExpr(String _name, TypeExpr[] _parameters) {
        this.name = _name;
        this.parameters = _parameters;
    }
    
    public TypeExpr(String name) {
    	this.name = name;
    }
    
    public TypeExpr(String name, Map<String, TypeExpr> typeParameters, Map<String, Expression> valueParameters) {
    	this.name = name;
    	this.typeParameters = typeParameters;
    	this.valueParameters = valueParameters;
    }
    
    public Type  evaluate(TypeSystem ts, Evaluator eval) {
    	if (ts != lastTypeSystem || lastEnvironment != eval.getEnvironment()) {
    		lastTypeSystem = ts;
    		lastEnvironment = eval.getEnvironment();
    		lastType = ts.doEvaluate(this, eval);
    	}
		return lastType;
    }


    private String name;
    private TypeExpr [] parameters;
    private Map<String, TypeExpr>  typeParameters;
    private Map<String, Expression>  valueParameters;
    
    private TypeSystem  lastTypeSystem;
    private Environment lastEnvironment;
    private Type        lastType;
}
