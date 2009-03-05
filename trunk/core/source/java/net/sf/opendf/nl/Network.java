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

package net.sf.opendf.nl;

import static net.sf.opendf.nl.util.Lib.renderObject;
import static net.sf.opendf.util.xml.Util.xpathEvalElement;
import static net.sf.opendf.util.xml.Util.xpathEvalElements;
import static net.sf.opendf.util.xml.Util.xpathEvalNode;
import static net.sf.opendf.util.xml.Util.xpathEvalNodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.opendf.cal.ast.Expression;
import net.sf.opendf.cal.i2.Configuration;
import net.sf.opendf.cal.i2.Environment;
import net.sf.opendf.cal.i2.Evaluator;
import net.sf.opendf.cal.i2.environment.DynamicEnvironmentFrame;
import net.sf.opendf.cal.i2.environment.Thunk;
import net.sf.opendf.cal.i2.platform.DefaultTypedPlatform;
import net.sf.opendf.cal.i2.util.Platform;
import net.sf.opendf.cal.interpreter.util.ASTFactory;
import net.sf.opendf.cal.util.SourceReader;
import net.sf.opendf.nl.util.DOMFactory;
import net.sf.opendf.nl.util.IDGenerator;
import net.sf.opendf.nl.util.Lib;
import net.sf.opendf.util.logging.Logging;
import net.sf.opendf.util.xml.Util;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



public class Network {
	
	public void  setName(ClassName name) {
		network.getDocumentElement().setAttribute(attrName, name.name);

		if (name.packageName != null && name.packageName.length > 0) {
			Element p = network.createElement(tagPackage);
			Element qid = network.createElement(tagQID);
			p.appendChild(qid);
			for (String pn : name.packageName) {
				Element id = network.createElement(tagID);
				id.setAttribute(attrName, pn);
				qid.appendChild(id);
			}
			network.getDocumentElement().appendChild(p);
		}
	}

	public void   addPort(String name, boolean input, Node type) {
		Element p = network.createElement(tagPort);
		network.getDocumentElement().appendChild(p);
		p.setAttribute(attrName, name);
		p.setAttribute(attrKind, input ? valInput : valOutput);
		if (type != null) {
			Node t = network.importNode(type, true);
			p.appendChild(t);
		}
	}
	
	public Object  addInstance(String name, Node packageQID, Map<String, Node> params, List<Element> attributes) {
		
		String s = Integer.toString(instanceID++);

		Element inst = network.createElement(tagInstance);
		network.getDocumentElement().appendChild(inst);
		inst.setAttribute(attrID, s);

		Element cls = network.createElement(tagClass);
		inst.appendChild(cls);
		cls.setAttribute(attrName, name);
		Node qid = network.importNode(packageQID, true);
		cls.appendChild(qid);
		
		for (String var : params.keySet()) {
			Element par = network.createElement(tagParameter);
			inst.appendChild(par);
			par.setAttribute(attrName, var);
			
			Node expr = network.importNode(params.get(var), true);
			par.appendChild(expr);
		}
		
		for (Element e : attributes) {
			Element e1 = (Element)importNode(e);
			inst.appendChild(e1);
		}

		return s;
	}
	
	public void  addConnection(String src, String srcConn, String dst, String dstConn, List<Element> attributes) {
		
		Element c = network.createElement(tagConnection);
		network.getDocumentElement().appendChild(c);
		c.setAttribute(attrSrc, (src == null) ? "" : src);
		c.setAttribute(attrSrcPort, srcConn);
		c.setAttribute(attrDst, (dst == null) ? "" : dst);
		c.setAttribute(attrDstPort, dstConn);
		for (Element e : attributes) {
			Element e1 = (Element)importNode(e);
			c.appendChild(e1);
		}
	}
	
	public void  addNetworkElement(Element e) {
		network.getDocumentElement().appendChild(e);
	}
		
	public Document  getXDF() {
		return network;
	}
	
	public Element  createElement(String tag) {
		return network.createElement(tag);
	}
	
	public Node 	importNode(Node n) {
		return network.importNode(n, true);
	}
	
		
	public Network () {
		instanceID = 0;
		try {
	        DOMImplementation domImpl = net.sf.opendf.util.xml.Util.getDefaultImplementation().getDocumentBuilder().getDOMImplementation();
	        network = domImpl.createDocument("", tagXDF, null);
		}
		catch (Exception exc) {
			throw new RuntimeException("Could not create network document.");
		}
	}
	
	private Document  network;
	private int       instanceID;
	
	private static final String  attrAlias = "alias";
	private static final String  attrDst = "dst";
	private static final String  attrDstPort = "dst-port";
	private static final String  attrID = "id";
	private static final String  attrKind = "kind";
	private static final String  attrName = "name";
	private static final String  attrSrc = "src";
	private static final String  attrSrcPort = "src-port";
	
	private static final String  tagClass = "Class";
	private static final String  tagConnection = "Connection";
	private static final String  tagInstance = "Instance";
	private static final String  tagID = "ID";
	private static final String  tagQID = "QID";
	private static final String  tagPackage = "Package";
	private static final String  tagParameter = "Parameter";
	private static final String  tagPort = "Port";
	private static final String  tagXDF = "XDF";
	
	private static final String  valInput = "Input";
	private static final String  valOutput = "Output";
	private static final String  valSingle = "single";
	
	
	public static Document  translate(Node nldoc, Environment env, Configuration context) {
		Network n = new Network();
		
		Element name = xpathEvalElement("/Network/QID", nldoc);
		if (name != null) {
			n.setName(qid2ClassName(name));
		}
			
		
		NodeList ports = xpathEvalNodes("/Network/Port", nldoc);
		for (int i = 0; i < ports.getLength(); i++) {
			Element p = (Element)ports.item(i);
			n.addPort(p.getAttribute(attrName), valInput.equals(p.getAttribute(attrKind)), xpathEvalNode("Type", p));
		}
		
		//
		// parameters
		//
		
		for (Element pd : xpathEvalElements("/Network/Decl[@kind='Parameter']", nldoc)) {
			String var = pd.getAttribute(attrName);
			Object val = env.getByName(var);
			addDeclarationToNetwork(var, renderObject(val, context, Network.domFactory), n);
		}
		
		//
		// network attributes
		//
		
		for (Element ae : xpathEvalElements("/Network/Attribute", nldoc)) {
			n.addNetworkElement((Element)n.importNode(ae));
		}
		
		//
		// variable declarations
		//
		
		NodeList vars = xpathEvalNodes("/Network/Decl[@kind='Variable']", nldoc);
		DynamicEnvironmentFrame localEnv = new DynamicEnvironmentFrame(env);
		Evaluator eval = new Evaluator(localEnv, context);
		for (int i = 0; i < vars.getLength(); i++) {
			Element v = (Element)vars.item(i);
			n.addNetworkElement((Element)n.importNode(v));
			Expression expr = ASTFactory.buildExpression(xpathEvalElement("Expr", v));

			// NOTE: In order to allow out-of-order declaration of variables, we need to lazily evaluate
			//       them. Therefore, thunks are used in building the local environment.
			String nm = v.getAttribute("name");
			localEnv.bind(nm, new Thunk(expr, new Evaluator(localEnv, context), localEnv), null);
		}
				
		//
		// entity declarations
		//
		
        Map<String, ClassName> entityEnv = new HashMap<String, ClassName>();
        List<Element> entityImports = xpathEvalElements("/Network/Import[@namespace='Entity']", nldoc);
        for (Element e : entityImports) {
        	if (!valSingle.equals(e.getAttribute(attrKind))) {
        		throw new RuntimeException("No support for package imports of entities.");
        	}
        	ClassName cn = qid2ClassName(xpathEvalElement("QID", e));
        	String alias = e.getAttribute(attrAlias);
        	if (alias == null || "".equals(alias))
        		alias = cn.name;
        	entityEnv.put(alias, cn);
        }
		
		Map<String, Object> entityNames = new HashMap<String, Object>();
 		NodeList entityDecls = xpathEvalNodes("/Network/EntityDecl", nldoc);
 		EntityExprEvaluator.Callback eeeCallback = n.new MyEEECallback();
 		EntityExprEvaluator entityEval = new EntityExprEvaluator(localEnv, context, eeeCallback, entityEnv, idgen);
 		
 		for (int i = 0; i < entityDecls.getLength(); i++) {
 			Element e = (Element)entityDecls.item(i);
 			Object res = entityEval.evaluate(xpathEvalElement("EntityExpr", e));
 			 			
 			String entityVarName = e.getAttribute("name");
 			entityNames.put(entityVarName, res);
 		}
 		
 		NodeList structureStmts = xpathEvalNodes("/Network/StructureStmt", nldoc);
 		StructureStmtEvaluator.Callback sseCallback = n.new MySSECallback();
 		StructureStmtEvaluator sse = new StructureStmtEvaluator(localEnv, context, sseCallback, entityNames, idgen);
 		for (int i = 0; i < structureStmts.getLength(); i++) {
 			Element e = (Element) structureStmts.item(i);
 			sse.execute(e);
 		}
		
		
		return n.getXDF();
	}
	
	private static void  addDeclarationToNetwork(String var, Element expr, Network n) {
		Element eDecl = n.createElement("Decl");
		eDecl.setAttribute("kind", "Variable");
		eDecl.setAttribute("name", var);
		eDecl.appendChild(n.importNode(expr));
		
		n.addNetworkElement(eDecl);
	}
	
	private static ClassName qid2ClassName(Element qid) {
		List<Element> ids = xpathEvalElements("ID", qid);
		
		assert ids.size() > 0;
		
		String [] packageName = new String [ids.size() - 1];
		for (int i = 0; i < ids.size() - 1; i++) {
			packageName[i] = ids.get(i).getAttribute(attrName);
		}
		ClassName cn = new ClassName(ids.get(ids.size() - 1).getAttribute(attrName), packageName);
		return cn;
	}
		
	private static void  addInstanceToNetwork(String idString, String className, String [] packageName, Map<String, Element> parameters, Network n, List<Element> attributes) {
		
		
		Element eInst = n.createElement("Instance");
		eInst.setAttribute("id", idString);
		Element eClass = n.createElement("Class");
		eInst.appendChild(eClass);
		eClass.setAttribute("name", className);
		if (packageName != null && packageName.length > 0) {
			Element eQID = n.createElement("QID");
			eClass.appendChild(eQID);
			for (int j = 0; j < packageName.length; j++) {
				Element id = n.createElement("ID");
				eQID.appendChild(id);
				id.setAttribute("id", packageName[j]);
			}
		}
		for (String s : parameters.keySet()) {
			Element par = n.createElement("Parameter");
			eInst.appendChild(par);
			par.setAttribute("name", s);
			par.appendChild(n.importNode(parameters.get(s)));
		}
		
		for (Element e : attributes) {
			eInst.appendChild(n.importNode(e));
		}
		
		n.addNetworkElement(eInst);
	}
	
	
	public class MyEEECallback implements EntityExprEvaluator.Callback {

		public void  addInstance(String id, String className, String[] packageName, Map<String, Element> pars, List<Element> attributes) {
			addInstanceToNetwork(id, className, packageName, pars, Network.this, attributes);
		}
		
		public void  addDeclaration(String var, Element expr) {
			addDeclarationToNetwork(var, expr, Network.this);
		}

		public DOMFactory getDOM() {
			return Network.domFactory;
		}
		
	};
	
	public class MySSECallback implements StructureStmtEvaluator.Callback {

		public void connect(String src, String srcport, String dst, String dstport, List<Element> attributes) {
			Network.this.addConnection(src, srcport, dst, dstport, attributes);
		}
		
		public void  addDeclaration(String var, Element expr) {
			addDeclarationToNetwork(var, expr, Network.this);
		}		

		public DOMFactory getDOM() {
			return Network.domFactory;
		}
		
	}
	

	public static void resetIDGen () {
	    idgen.reset();
	}
	
	private static IDGenerator  idgen = new IDGenerator() {
		public String newID() {
			return "$id_n$" + id_n++;
		}
		public void reset () { id_n = 0; }
	};
	
	private static DOMFactory domFactory = new DOMFactory() {
		
		private Document doc;
		{
			try {
				DOMImplementation domImpl = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
				doc = domImpl.createDocument("", "DUMMY", null);
			}
			catch (Exception exc) {
				throw new RuntimeException(exc);
			}
		}

		public Node clone(Node n) {
			return doc.importNode(n, true);
		}

		public Element createElement(String tag) {
			return doc.createElement(tag);
		}
		
	};	
	private static long  id_n = 0;
	

	/*
	 * 
	 * 
	 * 
	 * 
	 * 
	 */
	
	
	public static void main (String [] args) throws Exception {
		if (args.length == 0) {
			usage();
		}
		
		boolean verbose = true;
		String platformName = null;
		Map<String, String> params = new HashMap();
		String fileName = null;
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-P")) {
				if (platformName != null) usage();
				i += 1;
				if (i >= args.length) usage();
				platformName = args[i];
			} else if (args[i].equals("-D")) {
				i += 1;
				if (i >= args.length) usage();
				String s = args[i].trim();
				int n = s.indexOf('=');
				if (n < 0) usage();

				String v = s.substring(0, n).trim();
				if ("".equals(v)) usage();
				String expr = s.substring(n + 1);

				params.put(v, expr);

			} else if (!args[i].startsWith("-")) {
				if (fileName != null) {
					System.out.println("F: " + fileName);
					usage();
				}
				fileName = args[i];
			} else {
				usage();
			}
		}
		try {
			if (verbose) Logging.user().info("Compiling '" + fileName + "'...");
			compileSource(fileName, params);
			if (verbose) Logging.user().info("done.");
		} catch (Exception e) {
			e.printStackTrace();
			Logging.user().severe("ERROR: " + e.getMessage() + "(" + e.getClass().getName() + ").");
		}
	}
	
	public static File compileSource (String fileName, Map<String, String> params) throws Exception
	{
		String basename = fileName;
		if (fileName.endsWith(suffixNL)) {
			basename = fileName.substring(0, fileName.length() - suffixNL.length());
		}			
		FileInputStream inputStream = new FileInputStream(fileName);
		Document doc = Lib.readNL(fileName);

		Platform platform = DefaultTypedPlatform.thePlatform;
		Configuration context =  platform.configuration(); // FIXME: make this parametric

		DynamicEnvironmentFrame env = new DynamicEnvironmentFrame(platform.createGlobalEnvironment());
		Evaluator evaluator = new Evaluator(env, platform.configuration());
		for (String v : params.keySet()) {
			Object value = evaluator.valueOf(SourceReader.readExpr(new StringReader((String)params.get(v))));
			env.bind(v, value, null);
		}

		String result = Util.createXML(translate(doc, env, context));
		File outputFile = new File(basename + suffixXDF);
		OutputStream os = new FileOutputStream(outputFile);
		PrintWriter pw = new PrintWriter(os);
		pw.print(result);
		pw.close();
		
		return outputFile;
	}
	
	
	static private void usage() {
		System.out.println("NL2XDF <source> ...");
		throw new RuntimeException("Incorrect parameter format.");
	}
	
	
	private final static String suffixNL = ".nl";
	private final static String suffixXDF = ".xdf";
}
