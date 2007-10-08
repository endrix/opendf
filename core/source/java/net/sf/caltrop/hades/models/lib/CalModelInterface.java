/* 
BEGINCOPYRIGHT X
	
	Copyright (c) 2007, Xilinx Inc.
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

package net.sf.caltrop.hades.models.lib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.caltrop.cal.ast.Actor;
import net.sf.caltrop.hades.cal.CalInterpreter;
import net.sf.caltrop.hades.des.DiscreteEventComponent;
import net.sf.caltrop.hades.des.components.ParameterDescriptor;
import net.sf.caltrop.hades.models.ModelInterface;


/**
 *
 * @author jwj@acm.org
 */
public class CalModelInterface implements ModelInterface {


	public String getName(Object modelSource) {
		Actor a = (Actor) modelSource;
		return a.getName();
	}

	public String getPackageName(Object modelSource)
	{
		Actor a = (Actor)modelSource;
		return a.getPackage();
	}

	public ParameterDescriptor[] getParameters(Object modelSource) {
		Actor a = (Actor) modelSource;
		ParameterDescriptor[] pd = new ParameterDescriptor[a.getParameters().length];
		for (int i = 0; i < pd.length; i++) {
			pd[i] = new ParameterDescriptor(a.getParameters()[i].getName(), Object.class);
		}
		return pd;
	}

	public DiscreteEventComponent instantiate(Object modelSource, Map env, Map locMap, ClassLoader loader) {
		CalInterpreter interpreter = new CalInterpreter((Actor) modelSource, env);
		return interpreter ;
	}

	public Map createLocationMap(Object modelSource) {
		Map map = new HashMap();
		List list = new ArrayList();
		Object index = new Integer(0);
		list.add(index);
		map.put(modelSource, list);
		return map;
	}
}
