package net.sf.opendf.hardware.cli;


import net.sf.opendf.cli.*;
import net.sf.opendf.config.*;
import net.sf.opendf.util.logging.Logging;
import net.sf.opendf.util.exception.*;

import java.util.*;
import java.io.*;

/**
 * XLIMCodeGeneration augments the XLIM code generation with synthesis
 * checks.
 *
 * <p>Created: Fri Dec 15 12:33:37 2006
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id:$
 */
public class XLIMCodeGeneration extends SSAGenerator
{

    private static String[] extraCALMLXForms = {
        "net/sf/opendf/cal/checks/synthesisChecks.xslt",
        "net/sf/opendf/cal/checks/callbackProblemSummary.xslt"
    };


    protected XLIMCodeGeneration (ConfigGroup configs)
    {
        super(configs);
    }
    
    protected String[] getSSATransforms ()
    {
        String[] original = super.getSSATransforms();
        int newLength = original.length+extraCALMLXForms.length;
        String[] augmented = new String[newLength];

        for (int i=0; i < original.length; i++)
            augmented[i] = original[i];
        for (int i=original.length; i < newLength; i++)
            augmented[i] = extraCALMLXForms[i-original.length];
        
        return augmented;
    }
    
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
            XLIMCodeGeneration codeGen = new XLIMCodeGeneration(configuration);
            codeGen.runTransforms();
        }
        catch (Exception e)
        {
            (new ReportingExceptionHandler()).process(e);
            System.exit(-1);
        }
    }

    
}

