package net.sf.opendf.cli;

import java.util.*;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.sf.opendf.util.logging.Logging;
import net.sf.opendf.xslt.util.NodeListenerIF;

public class NodeErrorListener implements NodeListenerIF
{
    private Set<String> suppressIDs;
    public NodeErrorListener (Collection<String> suppressableMessages)
    {
        this.suppressIDs = new LinkedHashSet(suppressableMessages);
    }

    public void report (Node report, String message)
    {
        try
        {
            Node reportNode = net.sf.opendf.util.xml.Util.xpathEvalElement("Note[@kind='Report']", report);
            
            String severity = ((Element)report).getAttribute("severity");
            String id = ((Element)report).getAttribute("id");

            boolean suppress = false;
            for (String suppressable : this.suppressIDs)
            {
                if (id.startsWith(suppressable))
                {
                    suppress = true;
                    break;
                }
            }
            
            if (!suppress)
            {
                String msg = message + "[" + id +"]";
                if (severity.toUpperCase().equals("ERROR")) { Logging.user().severe(msg); }
                else if (severity.toUpperCase().startsWith("WARN")) { Logging.user().warning(msg); }
                else { Logging.user().info(severity + ": " + msg); }
            }
        }
        catch (Exception e)
        {
            Logging.user().severe(message);
        }
    }

}
