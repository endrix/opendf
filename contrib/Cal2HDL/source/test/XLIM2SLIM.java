package test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import net.sf.openforge.frontend.xlim.builder.XLIMBuilder;
import net.sf.openforge.util.io.ClassLoaderStreamLocator;
import net.sf.openforge.util.xml.Util;

public class XLIM2SLIM {

	
	public static void main(String [] args) throws Exception {
		
		Node doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(args[0]));
		
		doc = Util.applyTransforms(doc, xlimTransforms);

		String s = Util.createXML(doc);
		FileOutputStream f = new FileOutputStream(args[1]);
		PrintWriter pw = new PrintWriter(f);
		pw.print(s);
		pw.flush();
		f.close();
	}
	
	public static void _main(String [] args) throws Exception {
		
		Node doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(args[0]));
		
		for (int i = 0; i < xlimTransforms.length; i++) {
			doc = Util.applyTransform(doc, xlimTransforms[i]);
			String s = Util.createXML(doc);
			FileOutputStream f = new FileOutputStream(args[0] + "--" + i + ".xml");
			PrintWriter pw = new PrintWriter(f);
			pw.print(s);
			pw.flush();
			f.close();
		}
	}
	
	
	
    private static String[] xlimTransformPaths = {
        "net/sf/openforge/transforms/xlim/XLIMLoopFix.xslt",
        "net/sf/openforge/transforms/xlim/XLIMFixSelector.xslt",
        "net/sf/openforge/transforms/xlim/XLIMTagify.xslt",
        "net/sf/openforge/transforms/xlim/XLIMMakePortNames.xslt",
        "net/sf/openforge/transforms/xlim/XLIMSizeAndType.xslt",
        "net/sf/openforge/transforms/xlim/XLIMAddVarReads.xslt",
        "net/sf/openforge/transforms/xlim/XLIMInsertCasts.xslt",
        "net/sf/openforge/transforms/xlim/XLIMAddVarReadScope.xslt",
        "net/sf/openforge/transforms/xlim/XLIMBuildControl.xslt",
        "net/sf/openforge/transforms/xlim/XLIMRoutePorts.xslt",
        "net/sf/openforge/transforms/xlim/XLIMFixNames.xslt",
        "net/sf/openforge/transforms/xlim/XLIMMakeDeps.xslt",
        "net/sf/openforge/transforms/xlim/XLIMProcessPHI.xslt",
        "net/sf/openforge/transforms/xlim/XLIMFixNames.xslt",
        "net/sf/openforge/transforms/xlim/XLIMCreateExits.xslt",
        "net/sf/openforge/transforms/xlim/XLIMTagify.xslt",
        "net/sf/openforge/transforms/xlim/XLIMAddControlDeps.xslt"
    };

    
    private static Transformer [] xlimTransforms = 
    	Util.getTransformersAsResources(xlimTransformPaths, 
    									Util.getSaxonImplementation(),
    									new ClassLoaderStreamLocator(XLIMBuilder.class.getClassLoader()));

}
