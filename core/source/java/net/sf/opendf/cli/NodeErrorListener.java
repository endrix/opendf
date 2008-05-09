package net.sf.opendf.cli;

import java.util.*;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.sf.opendf.util.logging.Logging;
import net.sf.opendf.xslt.util.NodeListenerIF;

public class NodeErrorListener implements NodeListenerIF
{
    private Set<String> suppressIDs;
    private Map<String, Set<String>> reportedMessages = new HashMap();
    
    public NodeErrorListener (Collection<String> suppressableMessages)
    {
        this.suppressIDs = new LinkedHashSet(suppressableMessages);
    }

    /**
     * Clears the cache of reported messages.
     */
    public void clearMessages ()
    {
        this.reportedMessages.clear();
    }
    
    /**
     *  Returns a Set view of all the messages logged with the specified severity (case not sensitive).
     *  
     * @param level case-insensitive key
     * @return Set of messages logged with the severity at level
     */
    public Set<String> getMessages (String level)
    {
        String key = level.toUpperCase();
        if (reportedMessages.containsKey(key))
            return reportedMessages.get(key);
        return Collections.EMPTY_SET;
    }
    
    private void storeMessage (String level, String message)
    {
        String key = level.toUpperCase();
        Set<String> msgs = this.reportedMessages.get(key);
        if (msgs == null)
        {
            msgs = new HashSet();
            this.reportedMessages.put(key, msgs);
        }
        msgs.add(message);
    }
    
    public void report (Node report, String message)
    {
        try
        {
            Logging.dbg().fine("Node error report: " + message);

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
                storeMessage(severity, msg);
            }
        }
        catch (Exception e)
        {
            Logging.user().severe(message);
        }
    }

    public Node respond (Node node, String message)
    {
        throw new UnsupportedOperationException("Callback response not supported.  Callback from: \""+message+"\"");
    }
}
