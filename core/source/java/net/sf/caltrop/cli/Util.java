package net.sf.caltrop.cli;

import static net.sf.caltrop.cli.Util.createActorParameters;
import static net.sf.caltrop.cli.Util.initializeLocators;
import static net.sf.caltrop.util.xml.Util.applyTransformAsResource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.caltrop.util.Loading;
import net.sf.caltrop.util.io.ClassLoaderStreamLocator;
import net.sf.caltrop.util.io.DirectoryStreamLocator;
import net.sf.caltrop.util.io.StreamLocator;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class Util {

	static Node createActorParameters(Map<String, String> params) throws ParserConfigurationException {
        DOMImplementation domImpl = net.sf.caltrop.util.xml.Util.getDefaultImplementation().getDocumentBuilder().getDOMImplementation();
	    Document doc = domImpl.createDocument("", "Parameters", null);
	    
	    for (String parName : params.keySet()) {
	    	Element p = doc.createElement("Parameter");
	    	p.setAttribute("name", parName);
	    	p.setAttribute("value", params.get(parName));
	    	doc.getDocumentElement().appendChild(p);
	    }
		
		return doc.getDocumentElement();
	}

	static String []  extractPath(String p) {
		List<String> paths = new ArrayList<String>();
		boolean done = false;
		String r = p;
		while (!done) {
			String s = null;
			int n = r.indexOf(File.pathSeparatorChar);
			if (n < 0) {
				s = r;
				done = true;
			} else {
				s = r.substring(0, n);
				r = (r.length() > n) ? r.substring(n + 1) : "";
			}
			s = s.trim();
			if (!"".equals(s)) {
				paths.add(s);
			}
		}
		return paths.toArray(new String [paths.size()]);
	}

	static void  initializeLocators(String [] modelPath, ClassLoader classLoader) {
	
		StreamLocator [] sl = new StreamLocator[modelPath.length + 1];
		for (int i = 0; i < modelPath.length; i++) {
			sl[i] = new DirectoryStreamLocator(modelPath[i]);
		}
		sl[modelPath.length] =	new ClassLoaderStreamLocator(classLoader);
	
		Loading.setLocators(sl);
	}
	
	static Node elaborate(String networkClass, String [] modelPath, ClassLoader classLoader, Map<String, String> params) throws Exception {
		initializeLocators(modelPath, classLoader);

		Node doc = Loading.loadActorSource(networkClass);
		if (doc == null) {
            throw new ClassNotFoundException("Could not load network class '" + networkClass + "'.");
		}
		
        Node res = applyTransformAsResource(doc, inlineParametersTransformName, 
			    new String [] {"actorParameters"}, new Object [] {createActorParameters(params)});

        res = applyTransformAsResource(res, elaborationTransformName);

        return res;
	}

    private static final String inlineParametersTransformName = "net/sf/caltrop/transforms/InlineParameters.xslt";
    private static final String elaborationTransformName = "net/sf/caltrop/transforms/Elaborate.xslt";

}
