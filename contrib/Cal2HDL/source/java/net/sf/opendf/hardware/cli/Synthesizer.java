package net.sf.opendf.hardware.cli;


import static net.sf.opendf.cli.Util.checkCreateCache;
import net.sf.opendf.util.xml.Util;

import net.sf.opendf.util.io.ClassLoaderStreamLocator;
import net.sf.opendf.util.io.DirectoryStreamLocator;
import net.sf.opendf.util.io.MultiLocatorStreamLocator;
import net.sf.opendf.util.io.StreamLocator;
import net.sf.opendf.util.logging.Logging;
import net.sf.opendf.util.exception.*;
import net.sf.opendf.cli.*;
import net.sf.opendf.config.*;
import net.sf.opendf.config.AbstractConfig.ConfigError;
import net.sf.opendf.hardware.cli.util.HDLCacheCheck;
import net.sf.opendf.xslt.util.*;

import java.util.*;
import java.util.logging.Level;
import java.io.*;

import org.w3c.dom.Node;

/**
 * A top level, user interface class for sequencing the steps
 * necessary to convert NL to top level VHDL plus Verilog instances
 * for each actor.  The processing is broken down into a series of
 * phases which convert NL to XDF, instantiate the actors, and convert
 * each actor instance to HDL.  
 *
 * @author: imiller (imiller@xilinx.com)
 * <p>Created: Tue Dec 18 09:45:42 2007
 */
public class Synthesizer
{
//    private PrefMap prefs = null;
    private ConfigGroup configs;
    private Node designNode = null;
    private Map<Node, String> instances = new LinkedHashMap<Node, String>();
    private List<String> extraForgeArgs = null;
    private StreamLocator locator;
    private boolean doCache = true;

    // The instance used to verify the cacheability of each instance in the 
    // network.  Also provides correlation between instanceID and cache file names
    private HDLCacheCheck cacheChecker;
    
    // A listener that is registered to pick up semantic check reports
    // from CAL or NL source reading.  This class allows suppression
    // of any report based on values passed in via --suppress-message
    private final NodeErrorListener reportListener;
    
    public Synthesizer (ConfigGroup configs)
    {
        if (!checkConfiguration(configs))
            throw new IllegalArgumentException("Illegal HDL Synthesis Configuration");
    
        // check to see if caching is enabled by the user.
        this.doCache = checkCreateCache(configs); 

        this.cacheChecker = new HDLCacheCheck(this.configs, this.doCache);
        
        // Set up the 'extra' arguments to be passed to the backend HDL compiler.
        this.extraForgeArgs = new ArrayList<String>();
        this.extraForgeArgs.add("-d");
        this.extraForgeArgs.add(((ConfigFile)this.configs.get(ConfigGroup.ACTOR_OUTPUT_DIR)).getValueFile().getAbsolutePath());
        
        // Set up the search path/locators to be used
        String[] modelPath = (String[])((ConfigList)this.configs.get(ConfigGroup.MODEL_PATH)).getValue().toArray(new String[0]);
        StreamLocator[] locators = new StreamLocator[modelPath.length + 2];
        int i = 0;
        for (; i < modelPath.length; i++)
            locators[i] = new DirectoryStreamLocator(modelPath[i]);
        locators[i++] = new ClassLoaderStreamLocator(getClass().getClassLoader());
        // Because the transformation list contains an entry from the OpenDF Core plugin
        // a class loader for that plugin is included as well.
        locators[i++] = new ClassLoaderStreamLocator(Simulator.class.getClassLoader());
        this.locator = new MultiLocatorStreamLocator(locators);
        
        List<String> ids = (List<String>)((ConfigList)configs.get(ConfigGroup.MESSAGE_SUPPRESS_IDS)).getValue();
        this.reportListener = new NodeErrorListener(ids);
    }
    
    public static void main (String args[])
    {
        ConfigGroup synthConfigs = new SynthesisConfigGroup();
        List<String> unparsed = ConfigCLIParseFactory.parseCLI(args, synthConfigs);
        Logging.dbg().fine("Unparsed Synthesizer CLI: " + unparsed);

        if (((ConfigBoolean)synthConfigs.get(ConfigGroup.VERSION)).getValue().booleanValue())
        {
            synthConfigs.usage(Logging.user(), Level.INFO);
            return;
        }
        
        ConfigString topName = (ConfigString)synthConfigs.get(ConfigGroup.TOP_MODEL_NAME);
        if (!topName.isUserSpecified())
        {
            // Take the first unparsed arg with no leading '-'.
            for (String arg : new ArrayList<String>(unparsed))
            {
                if (!arg.startsWith("-"))
                {
                    unparsed.remove(arg);
                    topName.setValue(arg, true);
                }
            }
        }

        boolean valid = unparsed.isEmpty();
        for (AbstractConfig cfg : synthConfigs.getConfigs().values())
        {
            if (!cfg.validate())
            {
                for (ConfigError err : cfg.getErrors())
                {
                    Logging.user().severe(err.getMessage());
                    valid = false;
                }
            }
        }
      
        if (!valid)
        {
            Logging.user().info("Unknown args: " + unparsed);
            synthConfigs.usage(Logging.user(), Level.INFO);
            return;
        }
        
        synthConfigs = synthConfigs.canonicalize();
        if (Logging.dbg().isLoggable(Level.INFO))
        {
            Logging.dbg().info("Canonicalized configuration: ");
            synthConfigs.debug(System.out);
        }
        
        try
        {
            Synthesizer synth = new Synthesizer(synthConfigs);
            synth.synthesize();
        }
        catch (Exception e)
        {
            Logging.user().info("Exception received " + e);
            e.printStackTrace();
            (new ReportingExceptionHandler()).process(e);
        }
    }
    
    public int remainingInstances ()
    {
        return this.instances.keySet().size();
    }
    
    public void synthesize () throws Exception
    {
        Logging.user().info("Elaborating");
        synthElaborate();
        
        //
        // TBD: Insert code here for placing a single actor into a
        // network for synthesis.
            
        generateNetworkHDL();
        
        Logging.user().info("Generating Instances");
        boolean instancesRemain = true;
        while (instancesRemain)
        {
            instancesRemain = generateNextInstanceHDL();
        }
        Logging.user().info("HDL Compilation complete");
    }

    /**
     * Elaborates the top level design and determines which instances are cacheable
     * for hardware code generation.  The cacheable instances (if caching enabled) are 
     * stripped from the network, leaving only Actor stubs.  
     * 
     * @throws Exception
     */
    public void synthElaborate () throws Exception
    {
        ClassLoader cloader = Synthesizer.class.getClassLoader();
        Node eNode = Elaborator.elaborateModel(this.configs, null, cloader);

        File outFile = ((ConfigFile)this.configs.get(ConfigGroup.HDL_OUTPUT_FILE)).getValueFile();
        String baseName = outFile.getName();
        assert baseName.length() != 0 : "Cannot have empty result file name";
        baseName = baseName.substring(0, baseName.lastIndexOf('.'));
        
        // create the class to do the checking via callbacks from the XSLT domain
        XSLTProcessCallbacks.registerListener(XSLTProcessCallbacks.HW_CODEGEN_CACHE_CHECK, this.cacheChecker);
        
        // Check whether the instances should be cached, generating
        // cache keys as needed.
        final Node cacheNode = Util.applyTransformsAsResources(eNode,
                new String[]{
                "net/sf/opendf/transforms/xdfsynth/xdfStripCached.xslt"            
                }, this.locator);
        
        XSLTProcessCallbacks.removeListener(XSLTProcessCallbacks.HW_CODEGEN_CACHE_CHECK, this.cacheChecker);
        
        this.designNode = Elaborator.elabPostProcess(cacheNode, this.configs, null, cloader);
    }

    public void generateNetworkHDL () throws Exception
    {
        if (this.designNode == null)
            synthElaborate();
        
        assert this.designNode != null : "Elaborated design cannot be null";
        
        // Create a listener for receiving the collection of instances to be HDL generated.
        final NodeListenerIF instanceListener = new NodeListenerIF () {
            public void report (Node node, String msg)
            {
                Logging.dbg().fine("Registered " + msg + " for HDL generation");
                Synthesizer.this.instances.put(node, msg);
            }
            public Node respond (Node node, String msg) { throw new UnsupportedOperationException("Unexpected call to respond in instance listener"); }
        };
    
        boolean preserve = false;
        if (this.configs.get(ConfigGroup.XSLT_PRESERVE_INTERMEDIATE) != null)
        {
            preserve = ((ConfigBoolean)this.configs.get(ConfigGroup.XSLT_PRESERVE_INTERMEDIATE)).getValue().booleanValue();
        }

        File outFile = ((ConfigFile)this.configs.get(ConfigGroup.HDL_OUTPUT_FILE)).getValueFile();
        String baseName = outFile.getName();
        assert baseName.length() != 0 : "Cannot have empty result file name";
        baseName = baseName.substring(0, baseName.lastIndexOf('.'));
        File dir = getParentFile(outFile);
        
        Logging.user().info("Generating top level HDL");
        // No need to apply the parameters here as they were included
        // in the elaboration phase
        XSLTProcessCallbacks.registerListener(XSLTProcessCallbacks.ACTOR_INSTANTIATION, instanceListener);
        XSLTProcessCallbacks.registerListener(XSLTProcessCallbacks.SEMANTIC_CHECKS, this.reportListener);
        Node res = Util.applyTransformsAsResources(this.designNode, phase2xdfXForms, this.locator);
        XSLTProcessCallbacks.removeListener(XSLTProcessCallbacks.ACTOR_INSTANTIATION, instanceListener);
        XSLTProcessCallbacks.removeListener(XSLTProcessCallbacks.SEMANTIC_CHECKS, this.reportListener);
        if ((this.reportListener.getMessages("error").size() > 0) ||
                (this.reportListener.getMessages("severe").size() > 0))
        {
            if (preserve)
                writeFile(new File(dir, baseName+"_fail.xdf"), Util.createXML(res));
            throw new Exception("Errors reported in network.  HDL generation failed ");
        }

        if (preserve)
        {
            writeFile(new File(dir, baseName+".xdf"), Util.createXML(this.designNode));
            writeFile(new File(dir, baseName+".sxdf"), Util.createXML(res));
        }
        
        // sxdf -> vhdl
        String vhdl = Util.createTXT(Util.createTransformer(Synthesizer.class.getClassLoader().getResourceAsStream("net/sf/opendf/transforms/xdfsynth/xdfGenerateVHDL.xslt")), res);
        writeFile(outFile, vhdl);
        
        // sxdf -> prj
        String prj = Util.createTXT(Util.createTransformer(Synthesizer.class.getClassLoader().getResourceAsStream("net/sf/opendf/transforms/xdfsynth/xdfSynplicityProjectGen.xslt")),res);
        writeFile(new File(getParentFile(outFile), baseName +".prj"), prj);

        if (((ConfigBoolean)this.configs.get(ConfigGroup.GEN_HDL_SIM_MODEL)).getValue())
        {
            Node actorPortsRouted = Util.applyTransformAsResource(res, "net/sf/opendf/transforms/xdfsynth/xdfRouteQueuesToOutput.xslt", this.locator);

            String simNetworkVHDL = Util.createTXT(Util.createTransformer(Synthesizer.class.getClassLoader().getResourceAsStream("net/sf/opendf/transforms/xdfsynth/xdfGenerateVHDL.xslt")),actorPortsRouted);
            writeFile(new File(dir, baseName +"_sim.vhd"), simNetworkVHDL);
                
            String fixture = Util.createTXT(Util.createTransformer(Synthesizer.class.getClassLoader().getResourceAsStream("net/sf/opendf/transforms/xdfsynth/xdfNetworkTestbench.xslt")),actorPortsRouted);
            writeFile(new File(dir, baseName +"_fixture.v"), fixture);
        }
    }

    public boolean generateNextInstanceHDL () throws Exception
    {
        if (this.instances.isEmpty())
        {
            return false;
        }
        // Remove the node first in case any step causes an error.  This allows 
        // compilation to continue on error without getting stuck in an infinite loopl
        final Node node = this.instances.keySet().iterator().next();
        final String instanceID = this.instances.remove(node);
        
        // Although a bit redundant to define the rundir for each actor, this allows 
        // deferring creation of the directory (if it does not exist) until actual 
        // generation of the instances
        File actorOutputDir = ((ConfigFile)this.configs.get(ConfigGroup.ACTOR_OUTPUT_DIR)).getValueFile();
        if (!actorOutputDir.exists())
        {
            boolean success = actorOutputDir.mkdirs();
            if (!success) Logging.user().severe("Could not create instance output directory " + actorOutputDir);
        }
        
        //XLIMCodeGeneration xlimGen = new XLIMCodeGeneration(new String[] {});
        // The xlimGen handle is used only for the calmlToXlim method which does not use
        // the rundir and filename from the configs, but rather the explicit settings.
        ConfigGroup xlimConfigs = new TransformationConfigGroup();
        xlimConfigs.updateConfig(new ConfigGroupCopyWrapper(this.configs), xlimConfigs.getConfigs().keySet());
        XLIMCodeGeneration xlimGen = new XLIMCodeGeneration(xlimConfigs);
        boolean saveIntermediate = Logging.dbg().isLoggable(Level.INFO);
        boolean quiet = !Logging.user().isLoggable(Level.FINE);

        
        // If the node contains a note with hwcached=true
        //   Copy the HDL from the cache dir to the actor dir
        // If the node is not cached
        //   Generate the HDL
        //   Copy the HDL to the cache dir
        //   Move the cache temp files to active cache files
        
        boolean isCached = this.cacheChecker.checkCacheStatus(node);
        File actorTmp = this.cacheChecker.getActorCacheFile(instanceID, true);
        File actorCache = this.cacheChecker.getActorCacheFile(instanceID, false);
        File contextTmp = this.cacheChecker.getContextCacheFile(instanceID, true);
        File contextCache = this.cacheChecker.getContextCacheFile(instanceID, false);
        File hdlCache = this.cacheChecker.getHDLCacheFile(instanceID);

        Logging.dbg().fine("Checking " +instanceID+ " isCached is "+isCached + " do cache is "+this.doCache);
        if (isCached && !this.doCache)
        {
            throw new RuntimeException("Unexpected internal state.  Node was cached, but caching disabled.");
        }

        if (isCached)
        {
            Logging.user().info("Using cached version for instance '"+instanceID+"' in "+actorOutputDir.getAbsolutePath());
            // Delete the temp files
            actorTmp.delete();
            contextTmp.delete();
            File hdlFile = this.cacheChecker.getHDLCacheFile(instanceID);
            if (!hdlFile.exists()) // This should be impossible based on decision in HDLCacheCheck
                throw new RuntimeException("Missing cache file "+hdlFile);
            File hdlDestFile = new File(actorOutputDir, hdlFile.getName());
            boolean copy = copyFile(hdlFile, hdlDestFile);
            if (!copy)
                throw new RuntimeException("HDL cache file copy failed.  From: "+hdlFile+" to: "+hdlDestFile);
        }
        else
        {
            try
            {
                XSLTProcessCallbacks.registerListener(XSLTProcessCallbacks.SEMANTIC_CHECKS, this.reportListener);
                this.reportListener.clearMessages();
                
                // Generate XLIM.  Watch for semantic check errors
                final Node xlim = xlimGen.calmlToXlim(node, actorOutputDir, instanceID, saveIntermediate);
                if ((this.reportListener.getMessages("error").size() > 0) || (this.reportListener.getMessages("severe").size() > 0))
                {
                    XSLTProcessCallbacks.removeListener(XSLTProcessCallbacks.SEMANTIC_CHECKS, this.reportListener);
                    throw new RuntimeException("Errors reported in "+instanceID+".  HDL generation failed ");
                }
                this.reportListener.clearMessages();

                final File xlimFile = new File(actorOutputDir, instanceID+".xlim");
                
                Logging.user().info("Generating instance '"+instanceID+"' into "+actorOutputDir.getAbsolutePath());
                writeFile(xlimFile, Util.createXML(xlim));

                boolean pass = HDLCodeGenerator.runForge(xlimFile, instanceID, this.extraForgeArgs, quiet);

                if (!pass)
                    throw new RuntimeException("Could not generate HDL for " + instanceID);

                // Only save to the cache AFTER the HDL is generated,
                // in case of problem during HDL generation.
                final File cacheFile = ((ConfigFile)this.configs.get(ConfigGroup.CACHE_DIR)).getValueFile();
                if (this.doCache && cacheFile.exists() && cacheFile.isDirectory())
                {
                    // Copy the tmp cache keys to the active ones
                    Logging.user().info("\tSaving " + instanceID + " to cache (" + hdlCache +")");

                    actorCache.delete();
                    boolean copy = actorTmp.renameTo(actorCache);
                    contextCache.delete();
                    copy &= contextTmp.renameTo(contextCache);
                    copy &= copyFile(new File(actorOutputDir, instanceID+".v"), hdlCache);
                    // Be sure (in case the rename failed) the tmp files are removed
                    actorTmp.delete();
                    contextTmp.delete();
                    if (!copy)
                    {
                        // If the copy failed, delete all cache files.
                        actorCache.delete();
                        contextCache.delete();
                        hdlCache.delete();
                        Logging.user().warning("Cache save failed");
                    }
                }
            } catch (Exception e) {
                // Delete the temp files
                actorTmp.delete();
                contextTmp.delete();
                throw new LocatableException(e, "Compiling " + instanceID);
            }
        }
        
        
        return !this.instances.isEmpty();
    }

    private boolean checkConfiguration (ConfigGroup configs)
    {
        ConfigGroup synthConfigs = configs.canonicalize();
        
        if (((String)synthConfigs.get(ConfigGroup.TOP_MODEL_NAME).getValue()).length() == 0)
        {
            Logging.user().severe("Top level entity name not specified for HDL Synthesis");
            return false;
        }
        
        File oFile = ((ConfigFile)synthConfigs.get(ConfigGroup.HDL_OUTPUT_FILE)).getValueFile();
        if (!oFile.isAbsolute())
        {
            Logging.user().severe("Output file path must be fully specified: " + oFile);
            return false;
        }

        ConfigFile cacheFile = (ConfigFile)synthConfigs.get(ConfigGroup.CACHE_DIR);
        if (!"".equals(cacheFile.getValue()) && !cacheFile.getValueFile().isAbsolute())
        {
            Logging.user().severe("Cache path must be fully specified");
            return false;
        }
        
        this.configs = synthConfigs;
        
        return true;
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
    
    private static boolean copyFile (File from, File to)
    {
        try {
            FileOutputStream fos = new FileOutputStream(to);
            FileInputStream fis = new FileInputStream(from);
            int val = fis.read();
            while (val >= 0)
            {
                fos.write(val);
                val = fis.read();
            }
        } catch (IOException ioe)
        {
            Logging.user().warning("Could not copy cache file '"+from+"' to output '"+to+"'");
            return false;
        }
        return true;
    }

    private static File getParentFile (File file)
    {
        File parent = null;
        try
        {
            parent = file.getCanonicalFile().getParentFile();
        } catch (IOException ioe) {
            Logging.dbg().warning("Could not get canonical parent file from " + file);
            parent = file.getAbsoluteFile().getParentFile();
        }
        return parent;
    }
    
    private static void usage (String reason, List<String> args)
    {
        // Ensure the user can see the usage information
        Logging.user().setLevel(Level.ALL);
        Logging.user().info(reason);
        for (String arg : args)
            Logging.user().info(arg);
        usage();
    }
    private static void usage ()
    {
        // TBD
        System.out.println("-D <param>=<value>: Specify parameter values");
        System.out.println("-o <name>: output filename");
        System.out.println("-ao <name>: output directory name");
        System.out.println("-mp <path>: model path");
        System.out.println("-cache <path>: cache directory");
        System.out.println("--genSimModel: Generate an HDL simulation model");
        throw new IllegalArgumentException("Illegal command line argument form for Synthesizer");
    }
    

    private static final String[] phase2xdfXForms = {
        // Partname copy depends on Actor being inline
        "net/sf/opendf/transforms/xdfsynth/xdfDefaultPartname.xslt",
        "net/sf/opendf/transforms/xdfsynth/xdfCopyPartname.xslt",

        "net/sf/opendf/transforms/xdfsynth/xdfEvaluatePortTypes.xslt",
        
        // copies the input/output network port type from connected instance        
        "net/sf/opendf/transforms/xdfsynth/xdfPropagateTypes.xslt",
        "net/sf/opendf/transforms/xdfsynth/xdfCheckTypes.xslt",
        // add data width note to Ports        
        "net/sf/opendf/transforms/xdfsynth/xdfAnnotateDataWidth.xslt",
        // add fanout count note to Ports
        "net/sf/opendf/transforms/xdfsynth/xdfAnnotateFanout.xslt",
        "net/sf/opendf/transforms/xdfsynth/xdfAddInstanceNames.xslt",
        "net/sf/opendf/transforms/xdfsynth/xdfInsertDefaultBufferSize.xslt",

        // canonicalization must run before the AnnotateBufferSize.
        "net/sf/opendf/transforms/xdfsynth/xdfCanonicalizeAttributeStyle.xslt",
        "net/sf/opendf/transforms/xdfsynth/xdfAnnotateBufferSize.xslt",
        "net/sf/opendf/transforms/xdfsynth/xdfConvertTaps.xslt",
        "net/sf/opendf/transforms/xdfsynth/xdfConvertQueueDbg.xslt",

        "net/sf/opendf/transforms/xdfsynth/checks/xdfSynthChecks.xslt",
        "net/sf/opendf/cal/checks/callbackProblemSummary.xslt",

        // Generate instances in Actors directory (*.calml), strip
        // Actors from instances in XDF leaving ports for
        // instantiation 
        "net/sf/opendf/transforms/xdfsynth/xdfInstantiateActor.xslt"
    };

}


