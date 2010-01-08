/* 
BEGINCOPYRIGHT X
    
    Copyright (c) 2008, Xilinx Inc.
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

package net.sf.opendf.config;

import java.util.*;
import java.io.*;

public class SynthesisConfigGroup extends ConfigGroup
{    
    public SynthesisConfigGroup ()
    {
        super();
        
        final ConfigFile oFile =  new ConfigFile (HDL_OUTPUT_FILE, "Output File",
                "-o", 
                "Specify the compilation output file location",
                false
        );
        oFile.addFilter("*.vhd", "VHDL");
        registerConfig(HDL_OUTPUT_FILE, oFile);
        
        // Modify the config group to set elaboration post processing and inlining to true.
        ConfigBoolean postProcess = ((ConfigBoolean)get(ConfigGroup.ELABORATE_PP));
        postProcess.setValue(true, false);
        ConfigBoolean inline = ((ConfigBoolean)get(ConfigGroup.ELABORATE_INLINE));
        inline.setValue(true, false);

        registerConfig(ACTOR_OUTPUT_DIR, new ConfigFile (ACTOR_OUTPUT_DIR, "Actor Output Directory",
                "-ao",
                "The directory into which compiled actor instances are written",
                false,
                "Actors"
        ));
        registerConfig(GEN_HDL_SIM_MODEL, new ConfigBoolean (GEN_HDL_SIM_MODEL, "Generate HDL Sim Model",
                "--genSimModel", // cla
                "Generate an HDL simulation model and test fixture",
                false, // required
                false // default
        ));
        
        registerConfig(XSLT_PRESERVE_INTERMEDIATE, new ConfigBoolean (XSLT_PRESERVE_INTERMEDIATE, 
                "Preserve Compiler Intermediate Files",
                "--preserve", // cla
                "Preserve significant intermediate xml files in multi-stage transformation",
                false, // required
                false // default
        ));
        
        String restDefault = (File.pathSeparator.equals(";")) ? "planAhead.bat" : "planAhead";
        registerConfig(REST_EXEC_LOCATION, new ConfigFile (REST_EXEC_LOCATION, "Resource estimator location",
                "--resourceExec",
                "The location of the resource estimator executable",
                false,
                restDefault
        ));
        registerConfig(SYNTH_DO_REST, new ConfigBoolean (SYNTH_DO_REST, "Enable resource estimation",
                "--resourceEstimate",
                "Perform resource estimation on the design",
                false, // required
                false // default
        ));
    }
    
    public ConfigGroup getEmptyConfigGroup ()
    {
        return new SynthesisConfigGroup();
    }
    
    @Override
    public ConfigGroup canonicalize ()
    {
        ConfigGroup canon = super.canonicalize();
        
        // If the top model name is specified push it to the output file name (if unspecified)
        ConfigString topName = (ConfigString)canon.get(TOP_MODEL_NAME);
        ConfigFile oFile = (ConfigFile)canon.get(HDL_OUTPUT_FILE);
        if (!oFile.isUserSpecified() && topName.isUserSpecified())
        {
            oFile.setValue(topName.getValue() + ".vhd", false);
        }
        
        // Turn relative paths into absolute paths based on run directory
        File runDir = ((ConfigFile)canon.get(RUN_DIR)).getValueFile();
        if (runDir.getPath().length() > 0)
        {
            // The super class should have already made the run dir absolute.
            assert runDir.isAbsolute() : "Unexpected condition.  Run dir not absolute.";
            
            ConfigFile oDir = (ConfigFile)canon.get(HDL_OUTPUT_FILE);
            if (!oDir.getValueFile().isAbsolute())
            {
                oDir.setValue(makeAbsolute(runDir, oDir.getValueFile()), oDir.isUserSpecified());
            }
            
            ConfigFile aoDir = (ConfigFile)canon.get(ACTOR_OUTPUT_DIR);
            if (!aoDir.getValueFile().isAbsolute())
            {
                aoDir.setValue(makeAbsolute(runDir, aoDir.getValueFile()), aoDir.isUserSpecified());
            }
        }
        
        return canon;
    }

}
