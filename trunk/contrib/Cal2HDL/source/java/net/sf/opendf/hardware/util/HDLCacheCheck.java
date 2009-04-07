package net.sf.opendf.hardware.util;

import java.io.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.sf.opendf.config.ConfigFile;
import net.sf.opendf.config.ConfigGroup;
import net.sf.opendf.util.logging.Logging;
import net.sf.opendf.util.xml.Util;
import net.sf.opendf.xslt.util.NodeListenerIF;
import net.sf.saxon.dom.DocumentBuilderFactoryImpl;

import static net.sf.opendf.util.xml.Util.xpathEvalNode;
import static net.sf.opendf.util.xml.Util.xpathEvalNodes;

/**
 * In response to being called via the XSLTProcessCallbacks this class checks the
 * user specified cache (if enabled) to determine if the given Instance element is a
 * match to the cache.  If so, a Note is generated as the response.
 * 
 * If there is not a cache match, temporary cache keys (files) are created.  These will 
 * be made active after successful generation of the hardware.
 * 
 * @author imiller
 *
 */
public class HDLCacheCheck implements NodeListenerIF
{
    private static final String CACHE_NOTE_KIND = "hwcached";
    private static final String ACTOR = ".hw.actor";
    private static final String CONTEXT = ".hw.context";

    private File cacheDir = null;
    private boolean doCache = false;
    
    public HDLCacheCheck (ConfigGroup config, boolean doCache)
    {
        this.doCache = doCache;
        
        final ConfigFile cachePathConfig = (ConfigFile)config.get(ConfigGroup.CACHE_DIR);
        if (cachePathConfig != null)
        {
            this.cacheDir = cachePathConfig.getValueFile();
        }
    }
    
    public void report (Node report, String message)
    {
        throw new UnsupportedOperationException("No reporting support in HDL Cache Checking. "+message);
    }

    public Node respond (Node node, String message)
    {
        // Check each instance against any existing cache entries
        // If there is no match, create a temp cache entry

        if (!this.doCache)
            return createNote(false);
        
        // Snag the UID
        try
        {
            Node saxNode = toSaxon(node);
            Node uid = xpathEvalNode("./Instance/Note[@kind='UID']/@value", saxNode);
            Node actor = xpathEvalNode("./Instance/Actor", saxNode);
            NodeList context = xpathEvalNodes("./Instance/*[self::Parameter or self::Note[@kind='Directive']]", saxNode);
            
            if (uid == null) return createNote(false);
            if (actor == null) return createNote(false);
            if (context == null) return createNote(false);
            
            String uidString = uid.getNodeValue().trim();
            File actorTmp = getActorCacheFile(uidString, true);
            File contextTmp = getContextCacheFile(uidString, true);
            File actorCache = getActorCacheFile(uidString, false);
            File contextCache = getContextCacheFile(uidString, false);
            File hdlCache = getHDLCacheFile(uidString);
            
            // Write the actor to a temp cache file
            if (actorTmp.exists())
                Logging.user().warning("Overwriting existing tmp cache file: "+actorTmp);
            writeFile(actorTmp, actor);
            
            // Write the params/directives to a temp cache file
            if (contextTmp.exists())
                Logging.user().warning("Overwriting existing tmp cache file: "+contextTmp);
            writeFile(contextTmp, context);
            
            // If the non-temp cache files exist, do a diff
            if (!actorCache.exists() || !contextCache.exists() || !hdlCache.exists())
            {
                Logging.dbg().info("Cache file(s) missing.  Not using cache");
                actorCache.delete();
                contextCache.delete();
                hdlCache.delete();
                return createNote(false);
            }
            
            // If the diff succeeds (both actor and context)
            if (!diff(actorCache, actorTmp))
            {
                Logging.dbg().info("Cache invalid, actor did not match");
                actorCache.delete(); // Invalid cache, delete it
                contextCache.delete(); // Invalid cache, delete it
                hdlCache.delete();
                return createNote(false);
            }
            
            if (!diff(contextCache, contextTmp))
            {
                Logging.dbg().info("Cache invalid, context did not match");
                actorCache.delete(); // Invalid cache, delete it
                contextCache.delete(); // Invalid cache, delete it
                hdlCache.delete();
                return createNote(false);
            }
            
        }catch (Exception e) { Logging.user().severe("Exception in response " + e); }
        
        return createNote(true);
    }

    public File getActorCacheFile (String uid, boolean temp)
    {
        String suffix = ACTOR + (temp?".tmp":"");
        return new File(this.cacheDir, uid+suffix);
    }
    public File getContextCacheFile (String uid, boolean temp)
    {
        String suffix = CONTEXT + (temp?".tmp":"");
        return new File(this.cacheDir, uid+suffix); 
    }
    public File getHDLCacheFile (String uid)
    {
        return new File(this.cacheDir, uid+".v"); 
    }
    
    private static void writeFile(File file, NodeList n)
    {
        String s = "";
        for (int i=0; i < n.getLength(); i++)
        {
            s += Util.createXML(n.item(i));
        }
        writeFile(file, s);
    }
    
    private static void writeFile(File file, Node n)
    {
        writeFile(file, Util.createXML(n));
    }
    
    private static void writeFile(File file, String s)
    {
        try
        {
            PrintWriter pw = new PrintWriter(new FileOutputStream(file));
            pw.print(s);
            pw.close();
        }
        catch (IOException ioe)
        {
            throw new RuntimeException(ioe);
        }
    }

    private static boolean diff (File cache, File check)
    {
        if (!cache.exists() || !check.exists())
        {
            Logging.dbg().fine("Cannot diff non existant file: " + cache + " or " + check);
            return false;
        }

        boolean match = true;
        FileInputStream checkStream = null;
        FileInputStream cacheStream = null;
        
        try
        {
            checkStream = new FileInputStream(check);
            cacheStream = new FileInputStream(cache);

            // Do a byte by byte comparison (exact binary match) of
            // the cache and node XML
            int checkValue = checkStream.read();
            int cacheValue = cacheStream.read();
            match = checkValue != -1 && cacheValue != -1; // read returns -1 on EOF
            while (match)
            {
                checkValue = checkStream.read();
                cacheValue = cacheStream.read();
                if (checkValue == -1 && cacheValue == -1)
                    break;
                
                match = checkValue == cacheValue;
            }
            checkStream.close();
            cacheStream.close();
        }
        catch (IOException ioe)
        {
            try{
                if (checkStream != null) checkStream.close();
                if (cacheStream != null) cacheStream.close();
            }catch (IOException aioe) {} // we tried...
            
            Logging.dbg().fine("Instance cache not valid due to exception " + ioe.getMessage());
            return false;
        }

        return match;
    }
    
    /**
     * Returns true if the node contains a note which indicates that the 
     * cache is valid for the node
     * @param node
     * @return
     */
    public boolean checkCacheStatus (Node node)
    {
        try
        {
            Node saxNode = toSaxon(node);
            // The note may be attached to an instance or Actor
            Node cached = xpathEvalNode("//*[self::Instance or self::Actor]/Note[@kind='"+CACHE_NOTE_KIND+"']/@value", saxNode);
            if (cached.getNodeValue().toLowerCase().equals("true"))
            {
                return true;
            }
        } catch (Exception e) {
            Logging.dbg().warning("Error in checking cache status " + e);
        }
        return false;
    }
    
    private Node createNote (boolean isCached)
    {
        Document doc = emptyDocument();
        Element eNote = doc.createElement("Note");
        eNote.setAttribute("kind", CACHE_NOTE_KIND);
        eNote.setAttribute("value", isCached?"true":"false");
        
        try {
            Node result = toSaxon(eNote);
            reallyEmbarrassingHackToMakeThingsWork(result);
            return result;        
        } catch (Exception e)
        { Logging.user().severe("Could not return cache check note"); }
        return null;
    }
    private static Node toSaxon( Node xalan ) throws Exception
    {
        String string = net.sf.opendf.util.xml.Util.createXML( xalan );
//      opendf.main.Util.setSAXON();
        StringReader reader = new StringReader(string);
        return DocumentBuilderFactoryImpl.newInstance().newDocumentBuilder().parse(
                new org.xml.sax.InputSource(reader));          
    }
    
    private static Document emptyDocument()
    {
        try
        {
            //opendf.main.Util.setDefaultDBFI(); 
            DOMImplementation di = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
            //System.out.println("DI: " + di.getClass().getName());
            
            return di.createDocument("", "HDLCachCheckResult", null);
        }
        catch(Exception e)
        {
            throw new RuntimeException("Cannot create empty DOM document.", e);
        }
    }
    
    private static void  reallyEmbarrassingHackToMakeThingsWork(Node n) throws TransformerException
    {
        TransformerFactory xff = TransformerFactory.newInstance();
        Transformer serializer = xff.newTransformer();
        try
        {
            OutputStream os = new ByteArrayOutputStream();
            serializer.transform(new DOMSource(n),
                new StreamResult(os));
            os.close();
        }
        catch (IOException ioe)
        {
            Logging.dbg().severe("IO Exception in really embarrasing hack. " + ioe);
        }
    }
    
}
