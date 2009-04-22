package net.sf.opendf.hardware.cli;

import net.sf.opendf.cli.*;
import net.sf.opendf.config.*;
import net.sf.opendf.util.logging.Logging;
import net.sf.opendf.util.exception.*;
import net.sf.openforge.app.Forge;

import java.util.*;
import java.util.logging.Level;
import java.io.*;

/**
 * HDLCodeGenerator is a front end to the SystemBuilder tool designed
 * to sequence the necessary set of steps for conversion of XLIM to
 * Verilog HDL by sequencing XSLT transformations and the Forge
 * backend HDL compiler.
 *
 * <p>Created: Mon Dec 17 10:36:20 2006
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id:$
 */
public class HDLCodeGenerator extends XSLTTransformRunner
{
    private static final String XLIMEXT = ".xlim";

    
    private static String[] forgeFlags = {
        //"-vv", // be verbose 
        "-pipeline", // Allow auto-insertion of registers based on
                     // max-gate-depth spec in the XLIM (simply turns feature on)
        "-noblockio", // Do not automatically infer fifo interfaces
                      // from top level function signature (legacy C
                      // interface feature)
        "-no_block_sched", // Do not perform block-based scheduling
                           // (auto s/w pipelining of top level tasks)
        "-simple_arbitration", // Use simple arbitration of shared
                               // memories (assumes logic handles contention)
        "-noedk", // Do not generate EDK pcore compliant directory
                  // output structure.
        "-loopbal", // Balance loop latency.  Ensures that all paths
                    // take at least 1 cycle if any path does so that
                    // loop iteration flop can be removed.
        "-multdecomplimit","2", // Any multiplier which can be
                                // decomposed into 2 or fewer
                                // add/subtract + shift stages is.
        "-comb_lut_mem_read", // Reads of LUT based memories are
                              // performed combinationally.
        "-nolog", // No log file generation
        "-dplut", // Allow generation of dual ported LUT memories
                  // (default is to only use dual port BRAMs)
        "-noinclude", // Suppress generation of _sim and _synth files
    };
    /** Additional command line arguments to be passed to the backend
     * (Forge) compiler.
     */
    private final List<String> additionalArgs = new ArrayList();

    /** The non-null input file. */
    private File inputFile;

    /**
     * Creates a new HDLCodeGenerator object and parses the specified
     * argument array to set up the appropriate environment.
     */
    private HDLCodeGenerator (ConfigGroup configs) throws FileNotFoundException
    {
        super(configs);
        this.inputFile= ((ConfigFile)configs.get(ConfigGroup.TOP_MODEL_FILE)).getValueFile();
        List<String> ids = (List<String>)((ConfigList)configs.get(ConfigGroup.MESSAGE_SUPPRESS_IDS)).getValue();

        if (!inputFile.exists())
        {
            throw new FileNotFoundException("Could not find input file at " + inputFile.getAbsolutePath());
        }
        
        File destinationDir = null;
        if (configs.get(HDLConfigGroup.OUTPUT_DIR).isUserSpecified())
        {
            destinationDir = ((ConfigFile)configs.get(HDLConfigGroup.OUTPUT_DIR)).getValueFile();
        }
        
        this.additionalArgs.addAll(((ConfigList)configs.get(HDLConfigGroup.FORGE_ARGS)).getValue());
        

        if (destinationDir == null)
        {
            setRunDir(inputFile.getAbsoluteFile().getParentFile());
        }
        else
        {
            if (!destinationDir.exists())
            {
                if (!destinationDir.mkdirs())
                {
                    Logging.user().warning("Could not make destination directory for HDL generation: " + destinationDir);
                }
            }
            setRunDir(destinationDir);
        }
    }
    
    // JWJ: These are vestiges of a design that should probably be overhauled.
    // TODO: rethink XSLTTransformRunner design
    
   	protected void runTransforms() throws SubProcessException {
        final String noRootName = inputFile.getName();
        final String prefix = noRootName.endsWith(XLIMEXT) ? 
        					  noRootName.substring(0,noRootName.length()-XLIMEXT.length())
        					  : noRootName;

        boolean pass = runForge(this.inputFile, prefix, this.additionalArgs, this.isQuiet());
        
        if (!pass) {
        	throw new SubProcessException("Cannot generate HDL for '" + inputFile.getName() + "'.");
        }
	}

	public static boolean runForge (File xlimFile, String prefix, List forgeArgs, boolean quiet)
    {
        final String[] forgeRunFlags;
        int forgeArgIndex = 0;
        int flagsLength = forgeFlags.length + forgeArgs.size() + 1 + 2;

        if (!quiet)
        {
            flagsLength += 1;
            forgeRunFlags = new String[flagsLength];
            forgeRunFlags[0] = "-vv";
            forgeArgIndex++;
        }
        else
        {
            forgeRunFlags = new String[flagsLength];
        }
        
            
        System.arraycopy(forgeFlags, 0, forgeRunFlags, forgeArgIndex, forgeFlags.length);
        forgeArgIndex += forgeFlags.length;
        
        System.arraycopy(forgeArgs.toArray(), 0, forgeRunFlags, forgeArgIndex, forgeArgs.size());
        forgeArgIndex += forgeArgs.size();
        
        forgeRunFlags[forgeArgIndex++] = "-o";
        forgeRunFlags[forgeArgIndex++] = prefix;
        
        // run Forge on the xlim
        String fullPath = "";
        try
        {
            fullPath = xlimFile.getCanonicalPath();
        }
        catch (IOException ioe)
        {
            fullPath = xlimFile.getAbsolutePath();
        }

        forgeRunFlags[forgeArgIndex++] = fullPath;

        if (Logging.dbg().isLoggable(Level.FINER) || true)
        {
            String dbg = "";
            for (int i=0; i < forgeRunFlags.length; i++) dbg += forgeRunFlags[i] + " ";
            Logging.dbg().finer("Running " + dbg);
        }
        
        return Forge.runForge(forgeRunFlags);
    }
    
    
    /**
     * The HDLCodeGenerator frontend takes in an xlim filename and
     * runs the complete code generation path to XLIM on the specified
     * file.  Optional arguments are detailed in the usage. 
     */
    public static void main (String args[])
    {
        try
        {
            ConfigGroup configuration = new HDLConfigGroup();
            configuration = parseConfig(args, configuration);
            HDLCodeGenerator codeGen = new HDLCodeGenerator(configuration);
            
            codeGen.runTransforms();
        }
        catch (Exception e)
        {
            (new ReportingExceptionHandler()).process(e);
            System.exit(-1);
        }
    }

    protected static void usage (PrintStream ps)
    {
        ps.println("Usage: java HDLCodeGenerator [options] file{"+XLIMEXT+"}");
        ps.println("Options:");
        
        baselineUsage(ps);

        ps.println("  -d <dir>       Use the specified output directory.");
        ps.println("  -X<flag>       Pass specified flag to the backend HDL code generation process");
    }
    
    private static class HDLConfigGroup extends TransformationConfigGroup
    {
        private static final String FORGE_ARGS = "hdl.forge.args";
        private static final String OUTPUT_DIR = "hdl.forge.output.dir";
        
        public HDLConfigGroup ()
        {
            super();
            
            registerConfig(FORGE_ARGS, new ConfigList(FORGE_ARGS, "HDL Code Gen Args",
                    "-X", // cla
                    "Specification of additional arguments to the backend HDL code generation process",
                    false, // required
                    Collections.EMPTY_LIST  // default
            ));
            
            registerConfig(OUTPUT_DIR, new ConfigFile(OUTPUT_DIR, "HDL Output Dir",
                    "-d", // cla
                    "Specify the destination directory for generated HDL",
                    false // required
                    // no default
            ));
            
            ConfigFile topFile = (ConfigFile)this.get(ConfigGroup.TOP_MODEL_FILE);
            for (String filter: new HashSet<String>(topFile.getFilters().keySet()))
            {
                topFile.removeFilter(filter);
            }
            topFile.addFilter("*.xlim", "XLIM");
        }
        
        public ConfigGroup canonicalize ()
        {
            ConfigGroup canon = super.canonicalize();
            return canon;
        }
        
        public ConfigGroup getEmptyConfig ()
        {
            return new HDLConfigGroup();
        }
    }    
}

