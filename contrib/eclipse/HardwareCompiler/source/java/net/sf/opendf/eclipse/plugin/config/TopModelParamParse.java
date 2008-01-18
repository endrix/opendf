package net.sf.opendf.eclipse.plugin.config;

import static net.sf.opendf.util.xml.Util.xpathEvalElements;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.sf.opendf.util.Loading;
import net.sf.opendf.util.io.StreamLocator;
import net.sf.opendf.cal.util.CalWriter;
import net.sf.opendf.cli.Util;

public class TopModelParamParse
{

    public static List<ModelParameter> parseModel (String topModel, String[] modelPath) throws ModelAnalysisException
    {
        StreamLocator[] sl = Util.initializeLocators(modelPath, TopModelParamParse.class.getClassLoader());
        for (int i=0; i < sl.length; i++)
            System.out.println("Stream locator:were " + sl[i]);
        
        Node document = Loading.loadActorSource(topModel);
        if (document == null)
        {
            throw new ModelAnalysisException("Model '"+topModel+"' could not be loaded");
        }

        java.util.List<Element> parameters;
        java.util.List<Element> inputs;
        java.util.List<Element> outputs;
        
        if( xpathEvalElements("//Note[@kind='Report'][@severity='Error']", document ).size() > 0 )
        {
            throw new ModelAnalysisException("Model contains errors");
        }

        parameters = xpathEvalElements( "(Actor|Network)/Decl[@kind='Parameter']|XDF/Parameter", document );
        inputs     = xpathEvalElements( "(XDF|Actor|Network)/Port[@kind='Input']"              , document );
        outputs    = xpathEvalElements( "(XDF|Actor|Network)/Port[@kind='Output']"             , document );

        ArrayList<ModelParameter> modelParams = new ArrayList();
        for (Element e : parameters)
        {
            NodeList types = e.getElementsByTagName("Type");
            String type = types.getLength() > 0 ? CalWriter.CalmlToString(types.item(0)):"";
            String name = e.getAttribute("name");
            NodeList values = e.getElementsByTagName("Expr");
            String value = values.getLength() > 0 ? CalWriter.CalmlToString(values.item(0)):"";
            
            modelParams.add(new ModelParameter(name, type, value));
        }
        
        return modelParams;
    }
    
    public static class ModelParameter
    {
        private String name;
        private String type;
        private String defaultValue;
        
        public ModelParameter (String nam, String typ, String val)
        {
            this.name = nam;
            this.type = typ;
            this.defaultValue = val;
        }
        public String getName () { return this.name; }
        public String getType () { return this.type; }
        public String getValue () { return this.defaultValue; }
    }
    
    public static class ModelAnalysisException extends Exception
    {
        public ModelAnalysisException (String msg)
        {
            super(msg);
        }
    }
}
