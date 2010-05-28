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


package net.sf.opendf.cli;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import net.sf.opendf.cal.main.ReadXMLWriteText;
import net.sf.opendf.cal.main.ReadXMLWriteXML;
import net.sf.opendf.config.*;
import net.sf.opendf.config.AbstractConfig.ConfigError;
import net.sf.opendf.util.logging.Logging;
import net.sf.opendf.util.exception.*;

/**
 * XSLTTransformRunner is the superclass of those CLI frontends which
 * sequence a series of XSLT transformations.  Common functionality
 * includes the creation of intermediate files and launching of the
 * XSLT transformation.
 *
 * <p>Created: Mon Dec 17 10:55:16 2006
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id:$
 */
public abstract class XSLTTransformRunner
{
    /** If true then the transformations should run with no terminal
     * output. */
    private boolean quiet = false;
    /** If true then 'checkpoint' intermediate files in the XSLT
     * transformation process should be preserved for the user to inpsect. */
    private boolean preserveFiles = false;
    /** Never null.  Represents the directory in which the
     * transformations are running */ 
    private File runDir;

    protected XSLTTransformRunner (ConfigGroup configs)
    {
        this.runDir = ((ConfigFile)configs.get(ConfigGroup.RUN_DIR)).getValueFile();
        this.preserveFiles = ((ConfigBoolean)configs.get(ConfigGroup.XSLT_PRESERVE_INTERMEDIATE)).getValue().booleanValue();
        try {
            String runfast = System.getProperty("opendf.debug.xslt.runfast");
            this.runfast = runfast == null ? true:runfast.toUpperCase().startsWith("T");
        } catch (SecurityException se) {
            this.runfast = true;
        }
    }
    /**
     * Build a new XSLTTransformRunner
     */
    /*
    protected XSLTTransformRunner () throws FileNotFoundException
    {
        try
        {
            this.runDir = new File(System.getProperty("user.dir"));
            String runfast = System.getProperty("opendf.debug.xslt.runfast");
            this.runfast = runfast == null ? true:runfast.toUpperCase().startsWith("T");
        }
        catch (SecurityException se)
        {
            throw new FileNotFoundException("Insufficient permissions to determine current run dir");
        }
    }
    */
    
    /**
     * Run the sequence of transformations defined by the specific subclass
     */
    protected abstract void runTransforms () throws SubProcessException;

    /**
     * Set the run directory to the specified value.
     *
     * @param runDir a non-null existing directory.
     */
    protected void setRunDir (File runDir)
    {
        if (runDir == null || !runDir.exists() || !runDir.isDirectory())
        {
            throw new IllegalArgumentException("Illegal run directory specified " + runDir);
        }
        this.runDir = runDir;
    }
    /**
     * Returns a valid File object representing the current working
     * directory for the XSLT transforms.
     */
    protected File getRunDir ()
    {
        return this.runDir;
    }

    /**
     * Set the verbosity of the transforms.
     *
     * @param isQuiet true if the transformations should run with no
     * terminal output
     */
    protected void setQuiet (boolean isQuiet)
    {
        this.quiet = isQuiet;
    }
    /**
     * Returns true if the run is to be silent.
     */
    protected boolean isQuiet ()
    {
        return this.quiet;
    }

    /**
     * Set the flag for whether to preserve intermediate files.
     */
    protected void setPreserveFiles (boolean preserve)
    {
        this.preserveFiles = preserve;
    }
    /**
     * Returns true if the compilation is to preserve intermediate
     * files. 
     */
    protected boolean isPreserveFiles ()
    {
        return this.preserveFiles;
    }
    
    
    /**
     * Parse the array of String arguments to this transfomer for all
     * known command line args.  A List of all unknown arguments is
     * returned.  This method will take certain actions based on the
     * parsed arguments (eg setting verbosity, printing version info,
     * etc). 
     *
     * @param args an array of non-null string arguments to be parsed
     * @return a List of Strings, the unparseable arguments in the
     * same order as they appeared in the original args array.
     */
    protected final List<String> parseBaselineArguments (String[] args)
    {
        final List unparsed = new ArrayList();
        int i=0;
        boolean version = false;
        while (i < args.length)
        {
            if (args[i].equals("-q"))
            {
                this.quiet = true;
                if (Logging.user().isLoggable(Level.WARNING))
                {
                    // If WARNING level is allowed, then make it the
                    // least restrictive level.  If it is not, then do
                    // NOT further relax to the WARNING level
                    Logging.setUserLevel(Level.WARNING);
                }                
            }
            else if (args[i].equals("--preserve") || args[i].equals("-p"))
            {
                setPreserveFiles(true);
            }
            else if (args[i].equals("--version"))
            {
                version = true;
            }
            else
            {
                unparsed.add(args[i]);
            }
            i++;
        }

        if (version)
        {
            VersionInfo.printVersion();
            System.exit(0);
        }
        
        return unparsed;
    }

    /**
     * Prints, to the specified print stream, the known options that
     * are parsed by {@link parseBaselineArguments}.
     */
    protected static void baselineUsage (PrintStream ps)
    {
        ps.println("  -q             Run quietly");
        ps.println("  --preserve     Preserve intermediate files");
        ps.println("  --version      Display version information and quit");
    }

    protected boolean runfast = true;
    
    /**
     * Run the ReadXMLWriteXML process with the specified arguments.
     *
     * @param flags an array of String objects representing the
     * command line options passed to ReadXMLWriteXML.
     * @param infile the input file name, must exist or exception is
     * thrown
     * @param outfile the output file name to be generated
     * @param xforms the array of XSLT transformations that are to be
     * run this single invocation of ReadXMLWriteXML.
     * @throws SubProcessException if the infile does not exist, the
     * outfile is not generated, or ANY exception is thrown by ReadXMLWriteXML.
     */
    protected void runReadXMLWriteXML (String[] flags, File infile, File outfile, String[] xforms) throws SubProcessException
    {
        if (!infile.exists())
        {
            throw new SubProcessException("Intermediate format file " + infile + " does not exist.  Cannot complete run");
        }

        if (this.runfast)
        {
            String rxwxArguments[] = new String[flags.length + 2 + xforms.length];
            //String rxwxArguments[] = new String[flags.length + 2 + 1];
        
            int index = 0;

            System.arraycopy(flags, 0, rxwxArguments, 0, flags.length);
            index += flags.length;

            rxwxArguments[index] = infile.getAbsolutePath();
            index++;

            rxwxArguments[index] = outfile.getAbsolutePath();
            index++;

            System.arraycopy(xforms, 0, rxwxArguments, index, xforms.length);
            index += xforms.length;

            try
            {
                if (Logging.dbg().isLoggable(Level.FINER))
                {
                    String dbg = "";
                    for (int i=0; i < rxwxArguments.length; i++) dbg += rxwxArguments[i] + " ";
                    Logging.dbg().finer("XSLT Transform: readXMLWriteXML " + dbg);
                }
                ReadXMLWriteXML.main(rxwxArguments);
            }
            catch (Exception e)
            {
                // Debug hook for saving the source DOM
                (new SaveDOMExceptionHandler()).process(e);
                throw new SubProcessException("Sub process reported exception during processing of " + infile + ".  Message: "+ e.getMessage(), e);
            }
        }
        else
        {

            String rxwxArguments[] = new String[flags.length + 2 + 1];
            int index = 0;
            System.arraycopy(flags, 0, rxwxArguments, 0, flags.length);
            index += flags.length;

            rxwxArguments[index] = infile.getAbsolutePath();
            index++;

            rxwxArguments[index] = outfile.getAbsolutePath();
            index++;

            File previous = infile;
            for (int i=0; i < xforms.length; i++)
            {
                try
                {
                    rxwxArguments[index-2] = previous.getAbsolutePath();
                    previous = new File(outfile.getParent(), i+"_"+outfile.getName());
                    if (i == xforms.length-1) 
                        rxwxArguments[index-1] = outfile.getAbsolutePath();
                    else
                        rxwxArguments[index-1] = previous.getAbsolutePath();
                    System.out.println("Applying " + xforms[i] + " output: " + i + " to " + rxwxArguments[index-2]);

                    rxwxArguments[index] = xforms[i];
                    ReadXMLWriteXML.main(rxwxArguments);
                }
                catch (Exception e)
                {
                    // Debug hook for saving the source DOM
                    (new SaveDOMExceptionHandler()).process(e);
                    throw new SubProcessException("Sub process reported exception during processing of " + infile + ".  Message: "+ e.getMessage(), e);
                }
            }
        }
        
        
        if (!outfile.exists())
        {
            throw new SubProcessException("Failed to generate file "+ outfile + ".  Cannot complete run");
        }
    }

    /**
     * Run the ReadXMLWriteText process with the specified arguments.
     *
     * @param flags an array of String objects representing the
     * command line options passed to ReadXMLWriteXML.
     * @param infile the input file name, must exist or exception is
     * thrown
     * @param outfile the output file name to be generated
     * @param xforms the array of XSLT transformations that are to be
     * run this single invocation of ReadXMLWriteXML.
     * @throws SubProcessException if the infile does not exist, the
     * outfile is not generated, or ANY exception is thrown by ReadXMLWriteXML.
     */
    protected void runReadXMLWriteText (String[] flags, File infile, File outfile, String[] xforms) throws SubProcessException
    {
        if (!infile.exists())
        {
            throw new SubProcessException("Intermediate format file " + infile + " does not exist.  Cannot complete run");
        }
        
        String rxwtArguments[] = new String[flags.length + 2 + xforms.length];
        int index = 0;

        System.arraycopy(flags, 0, rxwtArguments, 0, flags.length);
        index += flags.length;

        rxwtArguments[index] = infile.getAbsolutePath();
        index++;

        rxwtArguments[index] = outfile.getAbsolutePath();
        index++;

        System.arraycopy(xforms, 0, rxwtArguments, index, xforms.length);
        index += xforms.length;

        try
        {
            if (Logging.dbg().isLoggable(Level.FINER))
            {
                String dbg = "";
                for (int i=0; i < rxwtArguments.length; i++) dbg += rxwtArguments[i] + " ";
                Logging.dbg().finer("XSLT Transform: readXMLWriteText " + dbg);
            }
            ReadXMLWriteText.main(rxwtArguments);
        }
        catch (Exception e)
        {
            // Debug hook for saving the source DOM
            (new SaveDOMExceptionHandler()).process(e);
            throw new SubProcessException("Sub process reported exception during processing of " + infile + ".  Message: "+ e.getMessage(), e);
        }


        if (!outfile.exists())
        {
            throw new SubProcessException("Failed to generate file "+ outfile + ".  Cannot complete run");
        }
    }

    /**
     * Create a File handle, appropriate for use as an intermediate
     * file in the transformation chain.  The file will be created in
     * the current working directory ({@link getRunDir}).  The file
     * will be automatically removed on close of the JVM unless the
     * {@link setPreserveFiles} method is set to true.
     *
     * @param prefix the initial portion of the file name
     * @param suffix the file name extension
     */
    protected File genIntermediateFile (String prefix, String suffix)
    {
        try
        {
            final File newFile;
            if (!this.isPreserveFiles())
            {
                String prefixExt = prefix;
                for (int i=prefixExt.length(); i < 3; i++)
                    prefixExt = prefixExt + "_";
                newFile = File.createTempFile(prefixExt, "."+suffix);
                newFile.deleteOnExit();
            }
            else
            {
                newFile = new File(runDir, prefix + "." + suffix);
            }
            
            return newFile;
        }
        catch (IOException ioe)
        {
            Logging.dbg().severe(ioe.getMessage());
            Logging.user().severe("Could not create intermediate file " + prefix + "." + suffix);
        }
        throw new RuntimeException("File creation failed");
    }

    protected static void writeFile(File file, String s)
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

    protected static ConfigGroup parseConfig (String args[], ConfigGroup configuration) throws InvalidConfigurationException
    {
        List<String> unparsed = ConfigCLIParseFactory.parseCLI(args, configuration);
        Logging.dbg().fine("Unparsed CLI: " + unparsed);

        if (((ConfigBoolean)configuration.get(ConfigGroup.VERSION)).getValue().booleanValue())
        {
            configuration.usage(Logging.user(), Level.INFO);
            throw new InvalidConfigurationException(configuration.get(ConfigGroup.VERSION).getCLA()+" specified");
        }
        
        //ConfigString topName = (ConfigString)configuration.get(ConfigGroup.TOP_MODEL_NAME);
        ConfigFile topFile = (ConfigFile)configuration.get(ConfigGroup.TOP_MODEL_FILE);
        if (!topFile.isUserSpecified())
        {
            // Take the first unparsed arg with no leading '-'.
            for (String arg : new ArrayList<String>(unparsed))
            {
                if (!arg.startsWith("-"))
                {
                    unparsed.remove(arg);
                    topFile.setValue(arg, true);
                }
            }
        }
        
        configuration = configuration.canonicalize();

        boolean valid = unparsed.isEmpty();
        for (AbstractConfig cfg : configuration.getConfigs().values())
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
            if (unparsed.size() > 0)
                Logging.user().info("Unknown args: " + unparsed);
            configuration.usage(Logging.user(), Level.INFO);
            throw new InvalidConfigurationException("Unknown arguments"); 
        }
        
        if (Logging.dbg().isLoggable(Level.INFO))
        {
            Logging.dbg().info("Canonicalized configuration: ");
            configuration.debug(System.out);
        }
        
        return configuration;
    }
    
    protected static class InvalidConfigurationException extends Exception
    {
        public InvalidConfigurationException (String msg)
        {
            super(msg);
        }
    }


    /**
     * The exception class that is thrown on any error.
     */
    protected static class SubProcessException extends Exception
    {
        public SubProcessException (String msg)
        {
            super(msg);
        }
        
        public SubProcessException (String msg, Throwable subException)
        {
            super(msg, subException);
        }
    }

    private static class SaveDOMExceptionHandler extends UnravelingExceptionHandler
    {
        protected ExceptionHandler[] getHandlers ()
        {
            return handlers;
        }
    
        private static final ExceptionHandler handlers[] = {
            new TypedExceptionHandler() 
            {
                protected Class getHandledClass() { return DOMProcessingException.class; }
                public boolean handle (Throwable t)
                {
                    if (Logging.dbg().isLoggable(Level.FINE))
                    {
                        try
                        {
                            File tmpFile = File.createTempFile("err",".xml",new File("."));
                            Logging.dbg().fine("Error during processing of DOM.  Saving result to " + tmpFile.getAbsolutePath());
                            PrintStream ps = new PrintStream(tmpFile);
                            ps.print(net.sf.opendf.util.xml.Util.createXML(((DOMProcessingException)t).getDOMNode()));
                            ps.flush();
                            ps.close();
                        }
                        catch (Exception e)
                        {
                            Logging.dbg().severe("Could not save source DOM");
                        }
                    }
                    return true;
                }
            },
        
            new TypedExceptionHandler() 
            {
                protected Class getHandledClass() { return LocatableException.class; }
                public boolean handle (Throwable t)
                {
                    Logging.dbg().warning("Error found in: " + ((LocatableException)t).getLocation());
                    return true;
                }
            }
        };
    }
    

    
}
