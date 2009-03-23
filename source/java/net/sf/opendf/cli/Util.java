package net.sf.opendf.cli;

import static net.sf.opendf.cli.Util.createActorParameters;
import static net.sf.opendf.cli.Util.initializeLocators;
import static net.sf.opendf.util.xml.Util.applyTransformAsResource;
import static net.sf.opendf.util.xml.Util.applyTransformsAsResources;

import java.io.File;
import java.util.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;

import net.sf.opendf.config.ConfigFile;
import net.sf.opendf.config.ConfigGroup;
import net.sf.opendf.util.Loading;
import net.sf.opendf.util.io.ClassLoaderStreamLocator;
import net.sf.opendf.util.io.DirectoryStreamLocator;
import net.sf.opendf.util.io.MultiLocatorStreamLocator;
import net.sf.opendf.util.io.StreamLocator;
import net.sf.opendf.util.logging.Logging;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class Util {

	public static Node createActorParameters(Map<String, String> params) throws ParserConfigurationException {
        DOMImplementation domImpl = net.sf.opendf.util.xml.Util.getDefaultImplementation().getDocumentBuilder().getDOMImplementation();
	    Document doc = domImpl.createDocument("", "Parameters", null);
	    
	    for (String parName : params.keySet()) {
	    	Element p = doc.createElement("Parameter");
	    	p.setAttribute("name", parName);
	    	p.setAttribute("value", params.get(parName));
	    	doc.getDocumentElement().appendChild(p);
	    }
		
		return doc.getDocumentElement();
	}

	public static String []  extractPath(String p) {
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

    public static boolean checkCreateCache (ConfigGroup config)
    {
        boolean doCache = true; 
        final ConfigFile cachePathConfig = (ConfigFile)config.get(ConfigGroup.CACHE_DIR);
        if ("".equals(cachePathConfig.getValue()))
        {
            doCache = false;
        }
        else if (!cachePathConfig.getValueFile().exists())
        {
            Logging.user().warning("Creating non existant cache directory " + cachePathConfig.getValueFile().getAbsolutePath());
            if (!cachePathConfig.getValueFile().mkdirs())
            {
                Logging.user().warning("Could not create cache dir, continuing compilation without caching");
                doCache = false;
            }
        }
        return doCache;
    }

	public static StreamLocator[] initializeLocators(String [] modelPath, ClassLoader classLoader) {
	
		StreamLocator [] sl = new StreamLocator[modelPath.length + 1];
		for (int i = 0; i < modelPath.length; i++) {
			sl[i] = new DirectoryStreamLocator(modelPath[i]);
		}
		sl[modelPath.length] =	new ClassLoaderStreamLocator(classLoader);
	
		Loading.setLocators(sl);
		return sl;
	}

    /**
     * Phase 1 of elaboration consists of the actual elaboration, flattening,
     * annotating instance IDs, attribute handling and adding directives.
     */
    public static Node elaborate(String networkClass, String [] modelPath, ClassLoader classLoader, Map<String, String> params) throws Exception
    {
        StreamLocator locator = new MultiLocatorStreamLocator(initializeLocators(modelPath, classLoader));
		
		Node doc = Loading.loadActorSource(networkClass);
		if (doc == null) {
            throw new ClassNotFoundException("Could not load network class '" + networkClass + "'.");
		}
		
        Node res = applyTransformAsResource(doc, inlineParametersTransformName, 
			    new String [] {"actorParameters"}, new Object [] {createActorParameters(params)}, 
			    locator);

        res = applyTransformAsResource(res, elaborationTransformName, locator);
        
        res = applyTransformsAsResources(res, postElaborationTransformNames, locator);

        return res;
    }
    
    /**
     * Phase 2 of elaboration consists of additional processing including constant prop. 
     */
    public static Node elaboratePostProcess (Node node, String [] modelPath, ClassLoader classLoader, boolean inline) throws Exception
    {
        StreamLocator locator = new MultiLocatorStreamLocator(initializeLocators(modelPath, classLoader));
        
        List<String> postElabXForms = new ArrayList();
        if (inline) postElabXForms.addAll(Arrays.asList(inlineTransforms));
        postElabXForms.addAll(Arrays.asList(postInlineTransforms));
        String[] postElabXFormsArray = postElabXForms.toArray(new String[1]);
        Node res = applyTransformsAsResources(node, postElabXFormsArray, locator);

        return res;
	}

    private static final String inlineParametersTransformName = "net/sf/opendf/transforms/InlineParameters.xslt";
    private static final String elaborationTransformName = "net/sf/opendf/transforms/Elaborate.xslt";
	private static final String [] postElaborationTransformNames = {
		"net/sf/opendf/transforms/xdfFlatten.xslt",
		"net/sf/opendf/transforms/addInstanceUID.xslt",

        // For now the conversion of xmlElement to attributes must be
        // done before the attribute folding.  The TypedContext does
        // not yet handle Maps in evaluation making it impossible to
        // extract the data from the folding function.
		"net/sf/opendf/transforms/xdfBuildXMLAttribute.xslt",
		"net/sf/opendf/transforms/xdfFoldAttributes.xslt",
        
        "net/sf/opendf/cal/transforms/xlim/AddDirectives.xslt"
    };
    private static final String [] inlineTransforms = { "net/sf/opendf/cal/transforms/Inline.xslt" };
    private static final String [] postInlineTransforms = {
        "net/sf/opendf/cal/transforms/AddID.xslt",
        "net/sf/opendf/cal/transforms/xlim/SetActorParameters.xslt",
        
        // Apply transforms to set up conditions for expr evaluation.
        // These are duplicated in the Actor context (where it is done
        // during initial loading) but they need to be run for
        // expressions in the XDF domain (including those introduced
        // by attribute folding).
        "net/sf/opendf/cal/transforms/ContextInfoAnnotator.xslt",  // Adds an attribute to Unary/Binary Ops with the function name for each operator (bitand for &, $eq for =, etc)
        "net/sf/opendf/cal/transforms/CanonicalizeOperators.xslt", // Converts Unary/Binary Ops to the attributed named function application.
        "net/sf/opendf/cal/transforms/AnnotateFreeVars.xslt",      // Generate freeVar notes indicating the name of each free variable
        "net/sf/opendf/cal/transforms/DependencyAnnotator.xslt",   // Adds a dependency note indicating the name of vars (same scope) it depends on (and if they are lazy).  Requied for the VariableSorter
        "net/sf/opendf/cal/transforms/VariableSorter.xslt",        // Re-orders variable declarations based on dependencies
        
        // EvaluateConstantExpressions depends on the annotations
        // provided by AnnotateDecls.
        "net/sf/opendf/cal/transforms/VariableAnnotator.xslt",     // Adds var-ref notes w/ id of the Decl for that var
        "net/sf/opendf/cal/transforms/xlim/AnnotateDecls.xslt",    // Adds declAnn notes with id of the enclosing scope
        "net/sf/opendf/cal/transforms/EvaluateNetworkExpressions.xslt", // Evaluates non-typed constants in attributes and parameters
        "net/sf/opendf/cal/transforms/EvaluateConstantExpressions.xslt",

        // Do not report errors at this point.  There may be some
        // based on a lack of type information.  This will be resolved
        // by the code generators by adding default type information
        //"net/sf/opendf/cal/checks/problemSummary.xslt",
        // Since we are ignoring them (for now) strip them out
        "net/sf/opendf/cal/transforms/stripExprEvalReports.xslt",
        
        "net/sf/opendf/cal/transforms/EliminateDeadCode.xslt",
	};

}
