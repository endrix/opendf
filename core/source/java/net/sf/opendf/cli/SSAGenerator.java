
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

import java.io.File;
import java.util.*;

import net.sf.opendf.config.ConfigBoolean;
import net.sf.opendf.config.ConfigFile;
import net.sf.opendf.config.ConfigGroup;
import net.sf.opendf.config.ConfigList;
import net.sf.opendf.config.TransformationConfigGroup;
import net.sf.opendf.util.exception.ExceptionHandler;
import net.sf.opendf.util.exception.ReportingExceptionHandler;
import net.sf.opendf.util.io.ClassLoaderStreamLocator;
import net.sf.opendf.util.io.MultiLocatorStreamLocator;
import net.sf.opendf.util.io.StreamLocator;
import net.sf.opendf.util.logging.Logging;
import net.sf.opendf.util.xml.Util;
import net.sf.opendf.xslt.util.NodeListenerIF;
import net.sf.opendf.xslt.util.XSLTProcessCallbacks;

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
        "net/sf/opendf/cal/checks/callbackProblemSummary.xslt",
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
     * The non-null input file.
     */
    private File inputFile;

    /**
     * The configuration is necessary for setting up elaboration of the model.
     */
    private final ConfigGroup configs;
    
    private boolean doCache; 
    
    // A listener that is registered to pick up semantic check reports
    // from CAL or NL source reading.  This class allows suppression
    // of any report based on values passed in via --suppress-message
    private final NodeListenerIF reportListener;
    
    /**
     * Creates a new SSAGenerator object and parses the input
     * arguments, using them to set up an appropriate compilation
     * environment.
     */
    //protected SSAGenerator (String[] args) throws FileNotFoundException
    protected SSAGenerator (ConfigGroup configs)
    {
        super(configs);
        this.configs = configs;
        this.inputFile= ((ConfigFile)configs.get(ConfigGroup.TOP_MODEL_FILE)).getValueFile();
        List<String> ids = (List<String>)((ConfigList)configs.get(ConfigGroup.MESSAGE_SUPPRESS_IDS)).getValue();
        this.reportListener = new NodeErrorListener(ids);
        setRunDir(this.inputFile.getAbsoluteFile().getParentFile());
        
        final ConfigFile cachePath = (ConfigFile)this.configs.get(ConfigGroup.CACHE_DIR);
        this.doCache = true;
        if ("".equals(cachePath.getValue()))
        {
            this.doCache = false;
        }
        else if (!cachePath.getValueFile().exists())
        {
            Logging.user().warning("Creating non existant cache directory " + cachePath.getValueFile().getAbsolutePath());
            if (!cachePath.getValueFile().mkdirs())
            {
                Logging.user().warning("Could not create cache dir, continuing compilation without caching");
                this.doCache = false;
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
        ConfigGroup configuration = new TransformationConfigGroup();
        ConfigFile topFile = (ConfigFile)configuration.get(ConfigGroup.TOP_MODEL_FILE);
        Set<String> filters = new HashSet(topFile.getFilters().keySet());
        for (String key : filters)
            topFile.removeFilter(key);
        topFile.addFilter("*.calml", "CalML");
        topFile.addFilter("*.cal", "Cal");
        
        // Default behavior is to inline and post process 
        ((ConfigBoolean)configuration.get(ConfigGroup.ELABORATE_PP)).setValue(true, false);
        ((ConfigBoolean)configuration.get(ConfigGroup.ELABORATE_INLINE)).setValue(true, false);
        
        try
        {
            configuration = parseConfig(args, configuration);
        } catch (InvalidConfigurationException ice)
        {
            Logging.user().severe("Could not parse command line: " + ice.getMessage());
            return;
        }
        
        try
        {
            SSAGenerator codeGen = new SSAGenerator(configuration);
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

        final String noRootName = this.inputFile.getName();
        final String prefix = noRootName.substring(0, noRootName.lastIndexOf('.'));
        
        // Elaboration is required b/c it performs operator canonicalization (and other necessary steps).
        boolean elaborate = ((ConfigBoolean)configs.get(ConfigGroup.ELABORATE_TOP)).getValue().booleanValue();
        if (!elaborate)
        {
            Logging.user().warning("Generation of SSA requires elaboration of the instance model.  Turning on elaboration for instance model.");
        }
        
        final Node calmlNode;
        if (true)
        {
            try
            {
                calmlNode = Elaborator.elaborateModel(this.configs, null, SSAGenerator.class.getClassLoader());
            }catch (Exception e ){
                throw new SubProcessException("Could not elaborate top model",e);
            }
            /*
            // Register a listener which will report any issues in loading
            // back to the user.
            XSLTProcessCallbacks.registerListener(XSLTProcessCallbacks.SEMANTIC_CHECKS, reportListener);

            try
            {
                String[] modelPath = (String[])((ConfigList)this.configs.get(ConfigGroup.MODEL_PATH)).getValue().toArray(new String[0]);
                String cachePath = this.configs.get(ConfigGroup.CACHE_DIR).getValue().toString();
                String topClass = this.configs.get(ConfigGroup.TOP_MODEL_NAME).getValue().toString();
                Map<String, String> params = ((ConfigMap)this.configs.get(ConfigGroup.TOP_MODEL_PARAMS)).getValue(); 

                ClassLoader classLoader = new SimulationClassLoader(SSAGenerator.class.getClassLoader(), modelPath, this.doCache?cachePath:null);
                initializeLocators(modelPath, SSAGenerator.class.getClassLoader());
                calmlNode = elaborate(topClass, modelPath, classLoader, params, true, true);
            } catch (Exception e) {
                // clean up after ourselves.
                XSLTProcessCallbacks.removeListener(XSLTProcessCallbacks.SEMANTIC_CHECKS, reportListener);
                throw new SubProcessException("Could not elaborate top model",e);
            }
            
            // No longer needed.
            XSLTProcessCallbacks.removeListener(XSLTProcessCallbacks.SEMANTIC_CHECKS, reportListener);
            */
        }
        
        Node xlim = calmlToXlim(calmlNode, this.getRunDir(), prefix, this.isPreserveFiles());
        // Write out the xlim
        writeFile(new File(this.getRunDir(), prefix+".xlim"), Util.createXML(xlim));
    }

    public Node calmlToXlim (Node calml, File rundir, String prefix, boolean saveIntermediate)
    {
        final Node xlim;
        // The stream locator must handle resources referenced in this class AND in
        // any subclass (as provided via getXXXTransforms()).
        final StreamLocator locator = new MultiLocatorStreamLocator(
                new StreamLocator[]{
                        new ClassLoaderStreamLocator(SSAGenerator.class.getClassLoader()),
                        new ClassLoaderStreamLocator(getClass().getClassLoader())
                }
                );
        try
        {
            // Register a listener which will report any issues in loading
            // back to the user.
            XSLTProcessCallbacks.registerListener(XSLTProcessCallbacks.SEMANTIC_CHECKS, this.reportListener);
            
            // Because this class may be subclassed, ensure that the obtained resources are located based on the classloader for the subclass
            final Node pcalml = Util.applyTransformsAsResources(calml, getParserTransforms(), locator);
            if (saveIntermediate) writeFile(new File(rundir, prefix+".pcalml"), Util.createXML(pcalml));
            
            final Node ssacalml = Util.applyTransformsAsResources(pcalml, getSSATransforms(), locator);
            if (saveIntermediate) writeFile(new File(rundir, prefix+".ssacalml"), Util.createXML(ssacalml));
            
            xlim = Util.applyTransformsAsResources(ssacalml, getXlimTransforms(), locator);
            if (saveIntermediate) writeFile(new File(rundir, prefix+".xlim"), Util.createXML(xlim));
            
            XSLTProcessCallbacks.removeListener(XSLTProcessCallbacks.SEMANTIC_CHECKS, this.reportListener);
        } catch (Exception e) {
            XSLTProcessCallbacks.removeListener(XSLTProcessCallbacks.SEMANTIC_CHECKS, this.reportListener);
            throw new RuntimeException("Could not complete CALML to XLIM tranformation", e);
        }
        
        return xlim;
    }

}

