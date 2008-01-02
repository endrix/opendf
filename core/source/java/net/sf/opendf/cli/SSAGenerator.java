
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

import net.sf.opendf.cal.main.Cal2CalML;
import net.sf.opendf.util.logging.Logging;
import net.sf.opendf.util.exception.*;
import net.sf.opendf.util.xml.Util;

import java.util.*;
import java.io.*;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Node;

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
        "net/sf/opendf/cal/transforms/CanonicalizePortTags.xslt",
        "net/sf/opendf/cal/transforms/AddInputTypes.xslt",
        "net/sf/opendf/cal/transforms/ReplaceOld.xslt",
        
//         "net/sf/opendf/cal/transforms/VariableAnnotator.xslt",
//         "net/sf/opendf/cal/transforms/ContextInfoAnnotator.xslt",
//         "net/sf/opendf/cal/transforms/CanonicalizeOperators.xslt",
//         "net/sf/opendf/cal/transforms/AnnotateFreeVars.xslt",
//         "net/sf/opendf/cal/transforms/DependencyAnnotator.xslt",
//         "net/sf/opendf/cal/transforms/VariableSorter.xslt"
    };

    private static String[] ssaTransforms = {
        "net/sf/opendf/cal/transforms/xlim/AnnotateActionQIDs.xslt",
        "net/sf/opendf/cal/transforms/xlim/CopyQIDToAction.xslt",
        "net/sf/opendf/cal/transforms/xlim/EnumerateStates.xslt",
        "net/sf/opendf/cal/transforms/xlim/MergePriorityInequalities.xslt",
        // Now in Elaborator
        "net/sf/opendf/cal/transforms/xlim/AddDirectives.xslt",
        //"net/sf/opendf/cal/transforms/xlim/SetActorParameters.xslt",
        "net/sf/opendf/cal/transforms/xlim/AddDefaultTypes.xslt",
        "net/sf/opendf/cal/transforms/xlim/AnnotateDecls.xslt",

        "net/sf/opendf/cal/transforms/EvaluateConstantExpressions.xslt",
        "net/sf/opendf/cal/checks/problemSummary.xslt",
        "net/sf/opendf/cal/transforms/EliminateDeadCode.xslt",
        "net/sf/opendf/cal/transforms/AddID.xslt",
        
        "net/sf/opendf/cal/transforms/xlim/VariableUsage.xslt",
        "net/sf/opendf/cal/transforms/xlim/SSANotes.xslt",
        "net/sf/opendf/cal/transforms/xlim/TrueSource.xslt",
        "net/sf/opendf/cal/transforms/xlim/UsedInGuard.xslt",
        "net/sf/opendf/cal/transforms/xlim/Dimensions.xslt"
    };

    private static String[] xlimTransforms = {
        "net/sf/opendf/cal/transforms/xlim/MakeOneHotXLIM.xslt"
    };

    // Flags which are always passed to the transform runner.
    private static String[] rXwXFlags = {"-r"};
    
    /**
     * The non-null input file name.
     */
    private String inputFileName = null;
    private File inputFile;

    /**
     * Creates a new SSAGenerator object and parses the input
     * arguments, using them to set up an appropriate compilation
     * environment.
     */
    protected SSAGenerator (String[] args) throws FileNotFoundException
    {
        final List<String> unparsedArgs = parseBaselineArguments(args);
        for (Iterator iter = unparsedArgs.iterator(); iter.hasNext();)
        {
            String arg = (String)iter.next();
            if (arg.startsWith("-")) // catch unknown options
            {
                usage(System.out);
                System.exit(-1);
            }
            else if (this.inputFileName == null)
            {
                this.inputFileName = arg;
            }
            else
            {
                usage(System.out);
                System.exit(-1);
            }
        }
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

    protected void initialize () throws SubProcessException
    {
        if (this.inputFileName == null)
        {
            throw new SubProcessException("Null input file name", new FileNotFoundException("No input file name specified"));
        }
        
        this.inputFile = new File(this.inputFileName);
        
        if (!this.inputFile.exists())
        {
            throw new SubProcessException("Input file does not exist", new FileNotFoundException("Could not find input file \""+this.inputFileName+"\" (at " + this.inputFile.getAbsolutePath() + ")"));
        }
        
        setRunDir(this.inputFile.getAbsoluteFile().getParentFile());
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
        initialize();
        
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

        final Node calmlNode;
        try
        {
            calmlNode = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(calml);
        }
        catch (Exception e)
        {
            throw new SubProcessException("Could not build XML for input CALML", e);
        }

        /*
        // Parser *.calml -> *.pcalml
        final File pcalml = genIntermediateFile(prefix, "pcalml");
        runReadXMLWriteXML(rXwXRunFlags, calml, pcalml, getParserTransforms());
        
        // SSA *.pcalml -> *.ssacalml
        final File ssacalml = genIntermediateFile(prefix, "ssacalml");
        runReadXMLWriteXML(rXwXRunFlags, pcalml, ssacalml, getSSATransforms());
        
        // xlim *.ssacalml -> *.xlim
        final File xlim = new File(this.getRunDir(), prefix + ".xlim");
        runReadXMLWriteXML(rXwXRunFlags, ssacalml, xlim, getXlimTransforms());
        */
        Node xlim = calmlToXlim(calmlNode, this.getRunDir(), prefix, this.isPreserveFiles());
        // ensure that we write out the xlim
        writeFile(new File(this.getRunDir(), prefix+".xlim"), Util.createXML(xlim));
    }

    public Node calmlToXlim (Node calml, File rundir, String prefix, boolean saveIntermediate)
    {
        final Node xlim;
        try
        {
            final Node pcalml = Util.applyTransformsAsResources(calml, getParserTransforms());
            if (saveIntermediate) writeFile(new File(rundir, prefix+".pcalml"), Util.createXML(pcalml));
            
            final Node ssacalml = Util.applyTransformsAsResources(pcalml, getSSATransforms());
            if (saveIntermediate) writeFile(new File(rundir, prefix+".ssacalml"), Util.createXML(ssacalml));
            
            xlim = Util.applyTransformsAsResources(ssacalml, getXlimTransforms());
            if (saveIntermediate) writeFile(new File(rundir, prefix+".xlim"), Util.createXML(xlim));
        } catch (Exception e) {
            throw new RuntimeException("Could not complete CALML to XLIM tranformation", e);
        }
        
        return xlim;
    }
    
    
    private static void usage (PrintStream ps)
    {
        ps.println("Usage: java SSAGenerator [options] file{"+CALEXT+"|"+CALMLEXT+"}");
        ps.println("Options:");
        
        baselineUsage(ps);

        // No other options
    }
    
}

