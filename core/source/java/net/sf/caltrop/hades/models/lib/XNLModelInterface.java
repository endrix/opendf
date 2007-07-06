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

import static net.sf.caltrop.hades.models.lib.Util.buildNetworkFromXDF;
import static net.sf.caltrop.hades.models.lib.Util.getPlatform;
import static net.sf.caltrop.util.xml.Util.root;
import static net.sf.caltrop.util.xml.Util.xpathEvalNode;
import static net.sf.caltrop.util.xml.Util.xpathEvalNodes;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.caltrop.cal.interpreter.environment.Environment;
import net.sf.caltrop.cal.interpreter.util.Platform;
import net.sf.caltrop.hades.cal.EnvironmentWrapper;
import net.sf.caltrop.hades.des.DiscreteEventComponent;
import net.sf.caltrop.hades.des.components.ParameterDescriptor;
import net.sf.caltrop.hades.des.schedule.Scheduler;
import net.sf.caltrop.hades.models.ModelInterface;
import net.sf.caltrop.hades.network.Network;
import net.sf.caltrop.util.xml.ElementPredicate;
import net.sf.caltrop.util.xml.TagNamePredicate;
import net.sf.caltrop.util.xml.Util;
import net.sf.caltrop.util.source.LoadingErrorException;
import net.sf.caltrop.util.source.LoadingErrorRuntimeException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class XNLModelInterface implements ModelInterface {

	public String getName(Object modelSource) {
		return root(modelSource).getAttribute(attrName);
	}

	public String getPackageName(Object modelSource) {
		try {
			Node qid = xpathEvalNode("/Network/Package/QID", modelSource);
			List ids = Util.listElements((Element)qid, predID);
			String p = "";
			for (int i = 0; i < ids.size(); i++) {
				if (i != 0) {
					p += ".";
				}
				Element e = (Element)ids.get(i);
				p += e.getAttribute(attrId);
			}
			return p;
		}
		catch (Exception exc) {
			throw new RuntimeException("Could not get package name.", exc);
		}
	}

	public ParameterDescriptor[] getParameters(Object modelSource) {
		NodeList nlPars = xpathEvalNodes("/Network/Decl[@kind='Param']", modelSource);
		ParameterDescriptor [] pds = new ParameterDescriptor[nlPars.getLength()];
		for (int i = 0; i < nlPars.getLength(); i++) {
			String name = ((Element)nlPars.item(i)).getAttribute(attrName);
			pds[i] = new ParameterDescriptor(name, Object.class);
		}
		return pds;
	}

	public DiscreteEventComponent instantiate(Object modelSource,
			Map env, Map locMap, ClassLoader loader) {
		
		
		//
		//		pass 1: input/output ports
		//
		Set inputs = new HashSet();
		Set outputs = new HashSet();

		NodeList nlInputs = xpathEvalNodes("/Network/Port[@kind='Input']", modelSource);
		for (int i = 0; i < nlInputs.getLength(); i++) {
			Element e = (Element)nlInputs.item(i);
			inputs.add(e.getAttribute(attrName));
		}
		
		NodeList nlOutputs = xpathEvalNodes("/Network/Port[@kind='Output']", modelSource);
		for (int i = 0; i < nlOutputs.getLength(); i++) {
			Element e = (Element)nlOutputs.item(i);
			outputs.add(e.getAttribute(attrName));
		}
		
		Network pn = new Network(new XNLNetworkCreator(modelSource), inputs, outputs, env, loader);
		
		return pn;		
	}

	public Map createLocationMap(Object modelSource) {
		// TODO Auto-generated method stub
		return null;
	}


	
	private final static String attrId = "id";
	private final static String attrKind = "kind";
	private final static String attrName = "name";
	
	private final static String valInput = "Input";
	
	private final static String tagID = "ID";
	private final static String tagPackage = "Package";
	private final static String tagQID = "QID";
	
	private final static ElementPredicate predID = new TagNamePredicate(tagID);
	private final static ElementPredicate predPackage = new TagNamePredicate(tagPackage);
	private final static ElementPredicate predQID = new TagNamePredicate(tagQID);
	
	static public class XNLNetworkCreator implements Network.Creator {

		public void createNetwork(Network n, double t, Scheduler s, Map env, ClassLoader loader) {

			ClassLoader myLoader = (loader == null) ? this.getClass().getClassLoader() : loader;
			Platform myPlatform = getPlatform(myLoader);
			
			Environment thisEnv = myPlatform.createGlobalEnvironment(new EnvironmentWrapper(env, myPlatform.context()));
			Document xdfModel = net.sf.caltrop.nl.Network.translate((Document)modelSource, 
					                                                          thisEnv, 
					                                                          myPlatform.context());

			try
            {
                buildNetworkFromXDF(n, xdfModel, myPlatform, env, myLoader);
            }
            catch (LoadingErrorException lee)
            {
                throw new LoadingErrorRuntimeException("XDF Network instantiation failed", lee);
            }
            catch (Exception e)
            {
                throw new RuntimeException("XDF Network instantiation failed", e);
            }

		}
		
		public XNLNetworkCreator(Object modelSource) {
			this.modelSource = modelSource;
		}
		
		private Object modelSource;		
	}
	
// 	private static Class loadClassFromElement(Element e, ClassLoader loader) {
// 		String unqualifiedClassName = e.getAttribute(attrName);
// 		NodeList nlId = xpathEvalNodes("QID/ID", e);
// 		String packageName = "";
// 		for (int i = 0; i < nlId.getLength(); i++) {
// 			Element eID = (Element)nlId.item(i);
// 			packageName += eID.getAttribute(attrId) + ".";
// 		}
		
// 		String className = packageName + unqualifiedClassName;
		
// 		try { 
// 			Class c;
// 			try {
// 				c = loader.loadClass(className);
// 			} catch (Exception exc) {
// 				c = Class.forName(className);
// 			}
			
// 			return c;
// 		}
// 		catch (ClassNotFoundException exc) {
// 			exc.printStackTrace();
// 			throw new RuntimeException("Could not locate model class '" + packageName + unqualifiedClassName + "'.", exc);
// 		}
// 	}


}
