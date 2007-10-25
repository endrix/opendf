
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

import net.sf.caltrop.cal.main.Cal2CalML;
import net.sf.caltrop.util.logging.Logging;
import net.sf.caltrop.util.exception.*;

import java.util.*;
import java.io.*;

/**
 * SSAGenerator converts CAL source code (or CALML) to a Static Single
 * Assignment form, represented as an XLIM (XML Language Independent
 * Model) document.  
 *
 * <p>Created: Fri Dec 15 12:33:37 2006
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id:$
 */
public class SSAGenerator extends XSLTTransformRunner
{
    private static final String CALEXT = ".cal";
    private static final String CALMLEXT = ".calml";
    
    private static String[] parserTransforms = {
        "net/sf/caltrop/cal/transforms/CanonicalizePortTags.xslt",
        "net/sf/caltrop/cal/transforms/AddInputTypes.xslt",
        "net/sf/caltrop/cal/transforms/ReplaceOld.xslt",
        
//         "net/sf/caltrop/cal/transforms/VariableAnnotator.xslt",
//         "net/sf/caltrop/cal/transforms/ContextInfoAnnotator.xslt",
//         "net/sf/caltrop/cal/transforms/CanonicalizeOperators.xslt",
//         "net/sf/caltrop/cal/transforms/AnnotateFreeVars.xslt",
//         "net/sf/caltrop/cal/transforms/DependencyAnnotator.xslt",
//         "net/sf/caltrop/cal/transforms/VariableSorter.xslt"
    };

    private static String[] ssaTransforms = {
        "net/sf/caltrop/cal/transforms/xlim/AnnotateActionQIDs.xslt",
        "net/sf/caltrop/cal/transforms/xlim/CopyQIDToAction.xslt",
        "net/sf/caltrop/cal/transforms/xlim/EnumerateStates.xslt",
        "net/sf/caltrop/cal/transforms/xlim/MergePriorityInequalities.xslt",
        // Now in Elaborator
        "net/sf/caltrop/cal/transforms/xlim/AddDirectives.xslt",
        //"net/sf/caltrop/cal/transforms/xlim/SetActorParameters.xslt",
        "net/sf/caltrop/cal/transforms/xlim/AddDefaultTypes.xslt",
        "net/sf/caltrop/cal/transforms/xlim/AnnotateDecls.xslt",

        "net/sf/caltrop/cal/transforms/EvaluateConstantExpressions.xslt",
        "net/sf/caltrop/cal/checks/problemSummary.xslt",
        "net/sf/caltrop/cal/transforms/EliminateDeadCode.xslt",
        "net/sf/caltrop/cal/transforms/AddID.xslt",
        
        "net/sf/caltrop/cal/transforms/xlim/VariableUsage.xslt",
        "net/sf/caltrop/cal/transforms/xlim/SSANotes.xslt",
        "net/sf/caltrop/cal/transforms/xlim/TrueSource.xslt",
        "net/sf/caltrop/cal/transforms/xlim/UsedInGuard.xslt",
        "net/sf/caltrop/cal/transforms/xlim/Dimensions.xslt"
    };

    private static String[] xlimTransforms = {
        "net/sf/caltrop/cal/transforms/xlim/MakeOneHotXLIM.xslt"
    };

    // Flags which are always passed to the transform runner.
    private static String[] rXwXFlags = {"-r"};
    
    /**
     * The non-null input file name.
     */
    private File inputFile;

    /**
     * Creates a new SSAGenerator object and parses the input
     * arguments, using them to set up an appropriate compilation
     * environment.
     */
    protected SSAGenerator (String[] args) throws FileNotFoundException
    {
        String inputFileName = null;
        final List<String> unparsedArgs = parseBaselineArguments(args);
        for (Iterator iter = unparsedArgs.iterator(); iter.hasNext();)
        {
            String arg = (String)iter.next();
            if (arg.startsWith("-")) // catch unknown options
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
     * The SSAGenerator frontend takes in a filename, either cal
     * or calml and runs the complete code generation path to XLIM on
     * the specified file.  Optional arguments are detailed in the
     * usage. 
     */
    public static void main (String args[])
    {
        try
        {
            SSAGenerator codeGen = new SSAGenerator(args);
            codeGen.runTransforms();
        }
        catch (Exception e)
        {
            ExceptionHandler handler = new ReportingExceptionHandler();
            handler.process(e);
            System.exit(-1);
        }
    }

    protected String[] getParserTransforms ()
    {
        return this.parserTransforms;
    }

    protected String[] getSSATransforms ()
    {
        return this.ssaTransforms;
    }
    
    protected String[] getXlimTransforms ()
    {
        return this.xlimTransforms;
    }

    /**
     * Runs the transformations to compile CAL or CALML to pcalml,
     * then to hwcalml, and finally to XLIM.
     */
    protected void runTransforms () throws SubProcessException
    {
        Logging.user().info("Compiling " + this.inputFile);
        
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

        final String prefix;
        final String noRootName = this.inputFile.getName();
        
        // Decide how to generate the CALML based on the input file type
        final File calml;
        if (this.inputFile.getName().toLowerCase().endsWith(CALMLEXT))
        {
            prefix = noRootName.substring(0,noRootName.length()-CALMLEXT.length());

            calml = this.inputFile;
        }
        else if (this.inputFile.getName().toLowerCase().endsWith(CALEXT))
        {
            prefix = noRootName.substring(0,noRootName.length()-CALEXT.length());

            // cal2calml *.cal -> *.calml
            try {
                calml = Cal2CalML.compileSource(this.inputFile.getAbsolutePath());
            }catch (Exception e) { // Cal2CalML throws generic exception.  Re-type it here
                throw new SubProcessException("Could not compile CAL source to CALML." + e.getMessage());
            }
        }
        else
        {
            throw new IllegalArgumentException("Input file name \""+this.inputFile.getName()+"\" must have "+CALEXT+" or "+CALMLEXT+" extension to filename");
        }

        // Parser *.calml -> *.pcalml
        final File pcalml = genIntermediateFile(prefix, "pcalml");
        runReadXMLWriteXML(rXwXRunFlags, calml, pcalml, getParserTransforms());
        
        // SSA *.pcalml -> *.ssacalml
        final File ssacalml = genIntermediateFile(prefix, "ssacalml");
        runReadXMLWriteXML(rXwXRunFlags, pcalml, ssacalml, getSSATransforms());
        
        // xlim *.ssacalml -> *.xlim
        final File xlim = new File(this.getRunDir(), prefix + ".xlim");
        runReadXMLWriteXML(rXwXRunFlags, ssacalml, xlim, getXlimTransforms());
    }
    
    private static void usage (PrintStream ps)
    {
        ps.println("Usage: java SSAGenerator [options] file{"+CALEXT+"|"+CALMLEXT+"}");
        ps.println("Options:");
        
        baselineUsage(ps);

        // No other options
    }
    
}

