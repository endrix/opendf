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

import static net.sf.caltrop.cal.interpreter.util.ImportUtil.handleImportList;
import static net.sf.caltrop.util.Util.xpathEvalElement;
import static net.sf.caltrop.util.Util.xpathEvalNodes;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import net.sf.caltrop.cal.interpreter.Context;
import net.sf.caltrop.cal.interpreter.ExprEvaluator;
import net.sf.caltrop.cal.interpreter.SimpleThunk;
import net.sf.caltrop.cal.interpreter.ast.Import;
import net.sf.caltrop.cal.interpreter.environment.Environment;
import net.sf.caltrop.cal.interpreter.util.ASTFactory;
import net.sf.caltrop.cal.interpreter.util.DefaultPlatform;
import net.sf.caltrop.cal.interpreter.util.ImportHandler;
import net.sf.caltrop.cal.interpreter.util.ImportMapper;
import net.sf.caltrop.cal.interpreter.util.Platform;
import net.sf.caltrop.cal.interpreter.util.SourceReader;
import net.sf.caltrop.hades.cal.EnvironmentWrapper;
import net.sf.caltrop.hades.des.DiscreteEventComponent;
import net.sf.caltrop.hades.des.MessageListener;
import net.sf.caltrop.hades.des.util.Attributable;
import net.sf.caltrop.hades.network.Network;
import net.sf.caltrop.util.Logging;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class Util {

	public static Platform  getPlatform(ClassLoader loader) {

		//
		//		build environment
		//
		
		String platformName = System.getProperty("CalPlatform");
		Platform myPlatform;
		if (platformName == null) {
			myPlatform = DefaultPlatform.thePlatform;
		} else {
			try {
				myPlatform = (Platform)XDFModelInterface.class.getClassLoader().loadClass(platformName).newInstance();
			}
			catch (Exception exc) {
				throw new RuntimeException("Cannot load platform class: " + platformName, exc);
			}
		}
		
		return myPlatform;
	}
	

	public static void  buildNetworkFromXDF(Network n, Object source, Platform platform, Map env, ClassLoader loader) {
		
		Logging.dbg().info("BEGIN NETWORK XDF");
		try {
			String xml = net.sf.caltrop.util.Util.createXML(xpathEvalElement("/XDF", source));
			Logging.dbg().info(xml);
		}
		catch (Exception e) {
			Logging.dbg().info("  ERROR creating network XDF: " + e.getMessage());
		}
		Logging.dbg().info("END NETWORK XDF");

		Import [] imports = ASTFactory.buildImports(xpathEvalNodes("/XDF/Import", source));
		ImportHandler [] importHandlers = platform.getImportHandlers(loader);
		ImportMapper [] importMappers = platform.getImportMappers();
		Environment localEnv = handleImportList(platform.createGlobalEnvironment(), importHandlers, imports, importMappers);

		localEnv = createEnvironment(xpathEvalNodes("/XDF/Decl[@kind='Variable']", source), env, platform);
		Context context = platform.context();

		//
		//		add processes
		//

		ExprEvaluator evaluator = new ExprEvaluator(context, localEnv);

		Map instanceMap = new HashMap();

		NodeList nlInstances = xpathEvalNodes("/XDF/Instance", source);
		
		for (int i = 0; i < nlInstances.getLength(); i++) {
			Element inst = (Element)nlInstances.item(i);
			Element classElement = xpathEvalElement("Class", inst);
			Class c = loadClassFromElement(classElement, loader);
			NodeList nlPars = xpathEvalNodes("Parameter", inst);

			Map pars = new HashMap();
			for (int j = 0; j < nlPars.getLength(); j++) {
				Element ePar = (Element)nlPars.item(j);
				Object name = ePar.getAttribute(attrName);
				Element eVal = xpathEvalElement("Expr", ePar);
				net.sf.caltrop.cal.interpreter.ast.Expression expr = ASTFactory.buildExpression(eVal);
				// NOTE: Ideally, we could also use lazy evaluation to pass parameters down the 
				//       hierarchy. However, lazy evaluation only works "automatically" when using 
				//       environments, and since we use an ordinary map to pass the value down, we
				//       need to evaluate it eagerly. Perhaps at some later point we could build
				//       an implementation of Map that does lazy evaluation.
				Object val = evaluator.evaluate(expr);
				pars.put(name, val);
			}

			try {
				Constructor constructor = c.getConstructor(new Class [] {Object.class});
				DiscreteEventComponent dec = (DiscreteEventComponent)constructor.newInstance(new Object[] {pars});
				n.addProcess(dec);
				instanceMap.put(inst.getAttribute(attrId), dec);
			}
			catch (Exception exc) {
				throw new RuntimeException("Cannot instantiate model class '" + c.getName() + "'.", exc);
			}
		}


		//
		//			add connections
		//

		NodeList nlConnections = xpathEvalNodes("XDF/Connection", source);

		for (int i = 0; i < nlConnections.getLength(); i++) {
			Element eConnection = (Element)nlConnections.item(i);
			n.addConnection((DiscreteEventComponent)instanceMap.get(eConnection.getAttribute(attrSrc)),
					eConnection.getAttribute(attrSrcPort),
					(DiscreteEventComponent)instanceMap.get(eConnection.getAttribute(attrDst)),
					eConnection.getAttribute(attrDstPort));

			DiscreteEventComponent dst = (DiscreteEventComponent)instanceMap.get(eConnection.getAttribute(attrDst));
			if (dst != null) {
				MessageListener port = dst.getInputConnectors().getConnector(eConnection.getAttribute(attrDstPort));
				if (port != null && port instanceof Attributable) {
					NodeList nlAttrs = xpathEvalNodes("Attribute[@name != 'connectionMonitor']", eConnection);
					for (int j = 0; j < nlAttrs.getLength(); j++) {
						Element eAttr = (Element)nlAttrs.item(j);
						String value = eAttr.getAttribute(attrValue);
						Element eVal = null;
						if (value != null && !"".equals(value)) {
							eVal = SourceReader.readExprDOM(value);
						} else {
							eVal = xpathEvalElement("Expr", eAttr);
						}
						Object val = null;
						if (eVal != null) {
							net.sf.caltrop.cal.interpreter.ast.Expression expr = ASTFactory.buildExpression(eVal);
							val = evaluator.evaluate(expr);
						}
						((Attributable)port).set(eAttr.getAttribute(attrName), val);
					}
				} else {
					NodeList nlAttrs = xpathEvalNodes("Attribute[@name != 'connectionMonitor']", eConnection);
					if (nlAttrs != null && nlAttrs.getLength() > 0) {
						Logging.dbg().warning("WARNING:: Found attributes for non-attributable port (" + eConnection.getAttribute(attrDstPort) + ")");
					}
				}
			}
			Element monitorAttr= (Element)xpathEvalElement("Attribute[@name = 'connectionMonitor']", eConnection);
			if (monitorAttr != null) {
				// FIXME: add monitor code here
			}
		}
	}
	
	public static Class loadClassFromElement(Element e, ClassLoader loader) {
		try {
			Logging.dbg().warning("LoadClassFromElement 1:: " + net.sf.caltrop.util.Util.createXML(e));
        } catch (Exception exc) {
            System.out.println(exc);
            exc.printStackTrace();
            throw new RuntimeException();
        }
		String unqualifiedClassName = e.getAttribute(attrName);
		Logging.dbg().warning("LoadClassFromElement 2:: " + unqualifiedClassName);
		NodeList nlId = xpathEvalNodes("QID/ID", e);
		String packageName = "";
		for (int i = 0; i < nlId.getLength(); i++) {
			Element eID = (Element)nlId.item(i);
			packageName += eID.getAttribute(attrId) + ".";
		}
		
		String className = packageName + unqualifiedClassName;

		try { 
			Class c;
			try {
				c = loader.loadClass(className);
				Logging.dbg().warning("Loaded '" + className + "' with '" + loader.getClass().getName() + "'");
			} catch (Exception exc) {
				Logging.dbg().warning("Default loader for '" + className + "' because an exception was thrown: " + exc.getMessage());
				c = Class.forName(className);
			}
			
			return c;
		}
		catch (ClassNotFoundException exc) {
			exc.printStackTrace();
			throw new RuntimeException("Could not locate model class '" + packageName + unqualifiedClassName + "'.", exc);
		}
	}
	
	private  static net.sf.caltrop.cal.interpreter.environment.Environment  createEnvironment(NodeList decls, Map env, Platform platform) {
		net.sf.caltrop.cal.interpreter.environment.Environment wrappedEnv = new EnvironmentWrapper(env, platform.createGlobalEnvironment(), platform.context());
		net.sf.caltrop.cal.interpreter.environment.Environment localEnv = platform.context().newEnvironmentFrame(wrappedEnv);
		
		// FIXME: handle imports
		
		for (int i = 0; i < decls.getLength(); i++) {
			Element eDecl = (Element)decls.item(i);
			Object name = eDecl.getAttribute(attrName);
			Element eVal = xpathEvalElement("Expr", eDecl);
			net.sf.caltrop.cal.interpreter.ast.Expression expr = ASTFactory.buildExpression(eVal);
			// NOTE: In order to allow out-of-order declaration of variables, we need to lazily evaluate
			//       them. Therefore, thunks are used in building the local environment.
			localEnv.bind(name, new SimpleThunk(expr, platform.context(), localEnv));
		}

		return localEnv;
	}
	


	
	private final static String attrDst = "dst";
	private final static String attrDstPort = "dst-port";
	private final static String attrId = "id";
	private final static String attrKind = "kind";
	private final static String attrName = "name";
	private final static String attrSrc = "src";
	private final static String attrSrcPort = "src-port";
	private final static String attrValue = "value";
	
	private final static String valInput = "Input";
	
	private final static String tagID = "ID";
	private final static String tagPackage = "Package";
	private final static String tagQID = "QID";
	
}
