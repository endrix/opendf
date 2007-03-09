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

package net.sf.caltrop.cli;

import java.util.*;
import java.io.*;

import net.sf.caltrop.cal.main.Cal2CalML;
import net.sf.caltrop.util.Logging;

/**
 * DRCChecker is a front end to the SystemBuilder tool which simply
 * runs the appropriate Design Rule Checks on the input source file
 * based on the type and/or command line arguments.
 *
 * <p>Created: Fri Dec 15 12:33:37 2006
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id:$
 */
public class DRCChecker extends XSLTTransformRunner
{
    private static final String CALMLEXT = ".calml";
    private static final String CALEXT = ".cal";
    
    private static String[] calmlCheckers = {"calmlChecks.xslt"};
    private static String[] simCheckers = {}; // TBD
    private static String[] synthCheckers = {}; // TBD

    private static String[] rXwXFlags = {"-r"};
    /**
     * The non-null input file name.
     */
    private File inputFile;

    /** The baseline checks are those which apply to any further
     * parsing of calml (eg syntactical type errors). */
    private static final int BASELINE = 1;
    /** Simulation checks are those which apply to
     * simulation/interpretation path only */
    private static final int SIMULATION = 2;
    /** Sythesis checks are those which apply to the
     * synthesis/codegeneration path only */
    private static final int SYNTHESIS = 4;

    /** Specifies the level of checks to run, a bitwise-or of the
     * above flags */
    private int level = BASELINE;

    /** A flag indicating the result of the checks as pass/fail */
    private boolean passed = true;
    
    /**
     * Creates a new DRCChecker object and parses the input
     * arguments, using them to set up an appropriate compilation
     * environment.
     */
    private DRCChecker (String[] args) throws FileNotFoundException
    {
        String inputFileName = null;
        final List<String> unparsedArgs = parseBaselineArguments(args);
        for (Iterator iter = unparsedArgs.iterator(); iter.hasNext();)
        {
            String arg = (String)iter.next();
            if (arg.startsWith("--basic"))
            {
                this.level = BASELINE;
            }
            else if (arg.startsWith("--sim"))
            {
                Logging.user().warning("Simulation checks not yet supported, doing baseline only");
            }
            else if (arg.startsWith("--synth"))
            {
                Logging.user().warning("Synthesis checks not yet supported, doing baseline only");
            }
            else if (arg.startsWith("-")) // catch unknown options
            {
                usage(System.out);
                System.exit(-1);
            }
            else if (inputFileName == null)
            {
                inputFileName = arg;
            }
            else
            {
                usage(System.out);
                System.exit(-1);
            }
        }

        if (inputFileName == null)
        {
            throw new FileNotFoundException("No input file name specified");
        }
        
        this.inputFile = new File(inputFileName);
        if (!inputFile.exists())
        {
            throw new FileNotFoundException("Could not find input file \""+inputFileName+"\" (at " + inputFile.getAbsolutePath() + ")");
        }
        setRunDir(inputFile.getAbsoluteFile().getParentFile());
    }
    
    
    /**
     * The DRCChecker frontend takes in a filename (calml) and
     * performs the specified checks on the input file.
     */
    public static void main (String args[])
    {
        DRCChecker drc = null;
        
        try
        {
            drc = new DRCChecker(args);
            drc.runTransforms();
        }
        catch (Exception e)
        {
            Logging.dbg().warning(e.getMessage());
            Logging.user().fine(e.getMessage());
            Logging.user().severe("An error has occurred.  Exiting abnormally.\n");
            System.exit(-1);
        }

        if (!drc.passed)
        {
            System.exit(-1);
        }
    }

    /**
     * Subsequent to {@link runTransforms} this method will return the
     * resulting status of the checks.
     */
    protected boolean passed ()
    {
        return this.passed;
    }

    /**
     * Runs the specified checks
     */
    protected void runTransforms () throws SubProcessException
    {
        Logging.user().info("DRC Checking " + this.inputFile);
        
        this.passed = true;

        if (this.inputFile.getName().toLowerCase().endsWith(CALEXT))
        {
            // cal2calml *.cal -> *.calml
            try {
                final File calml = Cal2CalML.compileSource(this.inputFile.getAbsolutePath());
                calml.deleteOnExit(); // if we created it... we need to delete it
                this.inputFile = calml;
            }catch (Exception e) { // Cal2CalML throws generic exception.  Re-type it here
                this.passed = false;
                throw new SubProcessException("Could not compile CAL source to CALML." + e.getMessage());
            }
        }

        
        final String[] rXwXRunFlags;
        if (this.isQuiet())
        {
            rXwXRunFlags = new String[rXwXFlags.length + 1];
            rXwXRunFlags[0] = "-q";
            System.arraycopy(rXwXFlags, 0, rXwXRunFlags, 1, rXwXFlags.length);
        }
        else
        {
            rXwXRunFlags = rXwXFlags;
        }

        if (!this.inputFile.getName().toLowerCase().endsWith(CALMLEXT))
        {
            throw new IllegalArgumentException("Input file name \""+this.inputFile.getName()+"\" must have "+CALMLEXT+" extension to filename");
        }

        final List<String> checkList = new ArrayList();
        if ((level & BASELINE) != 0)
            for (int i=0; i < calmlCheckers.length; i++) checkList.add(calmlCheckers[i]);
        if ((level & SIMULATION) != 0)
            for (int i=0; i < simCheckers.length; i++) checkList.add(simCheckers[i]);
        if ((level & SYNTHESIS) != 0)
            for (int i=0; i < synthCheckers.length; i++) checkList.add(synthCheckers[i]);
        String[] checks = new String[checkList.size()];
        checks = checkList.toArray(checks);
        
        boolean preserve = this.isPreserveFiles();
        this.setPreserveFiles(false);
        final File nullOut = genIntermediateFile("null", "out");
        try
        {
            runReadXMLWriteXML(rXwXRunFlags, this.inputFile, nullOut, checks);
        } catch (SubProcessException se)
        {
            Throwable throwable = se;
            while(throwable != null)
            {
                if (throwable instanceof net.sf.saxon.instruct.TerminationException)
                {
                    Logging.user().severe("File " + this.inputFile.getName() + " contains errors.  Please fix these errors and recompile");
                    this.passed = false;
                    break;
                }
                throwable = throwable.getCause();
            }
            // If we didn't find the reason we were looking for,
            // re-throw the sub process exception.  Its truly an error 
            if (throwable == null) 
            {
                throw se;
            }
        }
        
        this.setPreserveFiles(preserve);
    }
    
    private static void usage (PrintStream ps)
    {
        ps.println("Usage: java DRCChecker [options] file."+CALMLEXT);
        ps.println("Options:");
        
        baselineUsage(ps);

        ps.println("  --basic: perform only the baseline DRC checks");
        ps.println("  --sim:   perform the simulation DRC checks (not yet supported)");
        ps.println("  --synth: perform the code-generation DRC checks (not yet supported)");
    }
    
}

