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

import java.io.File;
import java.io.PrintStream;
import java.util.*;
import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;

public class ConfigGroup implements Cloneable
{
    /** The top model file is only valid as a user specification.  Other configurations
     * may be derived from its value (eg run dir, top model name, etc).  The top model name
     * and the model path should be used for loading the design.  
     */
    public static final String TOP_MODEL_FILE = "model.top.file";
    public static final String TOP_MODEL_NAME = "model.top.name";
    public static final String RUN_DIR = "config.run.dir";
    public static final String OUTPUT_FILE= "output.file.name";
    public static final String ACTOR_OUTPUT_DIR = "output.actor.dir";
    public static final String CACHE_DIR = "cache.dir";
    public static final String GEN_HDL_SIM_MODEL = "output.hdl.simmodel";
    public static final String MODEL_PATH = "model.search.path";
    public static final String TOP_MODEL_PARAMS= "model.top.parameters";
    
    public static final String ENABLE_ASSERTIONS = "assertions.enable";
    public static final String VERBOSE = "verbose";
    public static final String QUIET = "quiet";
    
    
    private Map<String, AbstractConfig> configs = new HashMap();
    
    public ConfigGroup ()
    {
        final ConfigFile topFile = new ConfigFile (TOP_MODEL_FILE, "Top Model File", 
                "", // cla 
                "Specifies the top level model file to be compiled", 
                false // required 
        );
        topFile.addFilter("*.nl", "Network");
        topFile.addFilter("*.cal", "CAL Source");
        topFile.addFilter("*.xdf", "Structural Network");
        configs.put(TOP_MODEL_FILE,  topFile);
        
        configs.put(TOP_MODEL_NAME,new ConfigNonEmptyString (TOP_MODEL_NAME, "Top Model Name", 
                "", // cla 
                "Specifies the top level model file for compilation", 
                true, // required 
                "" // default
        )); 
        
        configs.put(RUN_DIR, new ConfigFile.Dir (RUN_DIR, "Compilation working directory (run directory)",
                "-rundir", 
                "Specify the working directory for compilation.  It is from this directory that relative paths are based from.",
                false,
                "."
        ));
        final ConfigFile oFile =  new ConfigFile (OUTPUT_FILE, "Output File",
                "-o", 
                "Specify the compilation output file location",
                false
        );
        oFile.addFilter("*.vhd", "VHDL");
        configs.put(OUTPUT_FILE, oFile);

        configs.put(ACTOR_OUTPUT_DIR, new ConfigFile (ACTOR_OUTPUT_DIR, "Actor output directory",
                "-ao",
                "The directory into which compiled actor instances are written",
                false,
                "Actors"
        ));
        configs.put(CACHE_DIR, new ConfigFile (CACHE_DIR, "Cache directory",
                "-cache",
                "The directory used for storing cache files.  Cacheing is disabled if unspecified.",
                false,
                "cache"
        ));
        configs.put(GEN_HDL_SIM_MODEL, new ConfigBoolean (GEN_HDL_SIM_MODEL, "Generate HDL sim model",
                "--genSimModel", // cla
                "Generate an HDL simulation model and test fixture",
                false, // required
                false // default
        ));
        configs.put(MODEL_PATH, new ConfigList (MODEL_PATH, "Model search path",
                "-mp", // cla
                "Specification of the model search path",
                false, // required
                Collections.singletonList(".")// default
        ));
        
        configs.put(ENABLE_ASSERTIONS, new ConfigBoolean (ENABLE_ASSERTIONS, "Enable Assertions",
                "-ea", // cla
                "Turns on CAL assertion checking",
                false, // required
                false // default
        ));
        
        configs.put(TOP_MODEL_PARAMS, new ConfigMap(TOP_MODEL_PARAMS, "Model Parameters",
                "-D", // cla
                "Specify top level model parameters <key>=<value>",
                false, // required
                Collections.EMPTY_MAP // default
        ));
    };
    
    /**
     * Returns an unmodifiable view of the configs.
     * @return an unmodifiable map
     */
    public Map<String, AbstractConfig> getConfigs ()
    {
        return Collections.unmodifiableMap(this.configs);
    }
    
    public AbstractConfig get (String key)
    {
        return configs.get(key);
    }
    
    public void updateConfig (ConfigUpdateIF configuration)
    {
        Map<String, AbstractConfig> configMap = this.getConfigs();
        for (String key : configMap.keySet())
        {
            switch (configMap.get(key).getType())
            {
            case AbstractConfig.TYPE_BOOL:
                configuration.importConfig((ConfigBoolean)configMap.get(key));
                break;
            case AbstractConfig.TYPE_STRING:
                configuration.importConfig((ConfigString)configMap.get(key));
                break;
            case AbstractConfig.TYPE_FILE:
                configuration.importConfig((ConfigFile)configMap.get(key));
                break;
            case AbstractConfig.TYPE_DIR:
                configuration.importConfig((ConfigFile)configMap.get(key));
                break;
            case AbstractConfig.TYPE_INT :
                configuration.importConfig((ConfigInt)configMap.get(key));
                break;
            case AbstractConfig.TYPE_LIST :
                configuration.importConfig((ConfigList)configMap.get(key));
                break;
            case AbstractConfig.TYPE_MAP :
                configuration.importConfig((ConfigMap)configMap.get(key));
                break;
            case AbstractConfig.TYPE_SET :
                configuration.importConfig((ConfigSet)configMap.get(key));
                break;
            default :
                throw new IllegalArgumentException("Unknown config type " + configMap.get(key).getType() + " for " + key);
            }
        }
    }

    public void pushConfig (ConfigUpdateIF configuration)
    {
        Map<String, AbstractConfig> configMap = this.getConfigs();
        for (String key : configMap.keySet())
        {
            switch (configMap.get(key).getType())
            {
            case AbstractConfig.TYPE_BOOL:
                configuration.exportConfig((ConfigBoolean)configMap.get(key));
                break;
            case AbstractConfig.TYPE_STRING:
                configuration.exportConfig((ConfigString)configMap.get(key));
                break;
            case AbstractConfig.TYPE_FILE:
                configuration.exportConfig((ConfigFile)configMap.get(key));
                break;
            case AbstractConfig.TYPE_DIR:
                configuration.exportConfig((ConfigFile)configMap.get(key));
                break;
            case AbstractConfig.TYPE_INT :
                configuration.exportConfig((ConfigInt)configMap.get(key));
                break;
            case AbstractConfig.TYPE_LIST :
                configuration.exportConfig((ConfigList)configMap.get(key));
                break;
            case AbstractConfig.TYPE_MAP :
                configuration.exportConfig((ConfigMap)configMap.get(key));
                break;
            case AbstractConfig.TYPE_SET :
                configuration.exportConfig((ConfigSet)configMap.get(key));
                break;
            default :
                throw new IllegalArgumentException("Unknown config type " + configMap.get(key).getType() + " for " + key);
            }
        }
    }
    /**
     * Creates a complete copy of this configuration group and all of its settings in which 
     * all values have be canonicalized.  This includes conversion of relative paths to absolute 
     * paths based on the RUN_DIR config (if non empty).
     * @return
     */
    public ConfigGroup canonicalize ()
    {
        ConfigGroup canon = null;
        try {
            canon = (ConfigGroup)this.clone();
        }catch (CloneNotSupportedException cnse) {
            throw new RuntimeException("Canonicalization of configuration failed with internal error.", cnse);
        }
        
        // If the input file name was specified, but the top model name was not, pull it over
        ConfigFile iFile = (ConfigFile)canon.get(TOP_MODEL_FILE);
        ConfigString topName = (ConfigString)canon.get(TOP_MODEL_NAME);
        if (iFile.isUserSpecified() && !topName.isUserSpecified())
        {
            String name = iFile.getValueFile().getName();
            if (name.indexOf('.') > 0)
            name = name.substring(0, name.lastIndexOf('.'));
            topName.setValue(name, true); // (needs to be true for oFile test)
        }
        
        // If the top model name is specified push it to the output file name (if unspecified)
        ConfigFile oFile = (ConfigFile)canon.get(OUTPUT_FILE);
        if (!oFile.isUserSpecified() && topName.isUserSpecified())
        {
            oFile.setValue(topName.getValue() + ".vhd", false);
        }
        
        // Turn relative paths into absolute paths based on run directory
        File runDir = ((ConfigFile)canon.get(RUN_DIR)).getValueFile();
        if (runDir.getPath().length() > 0)
        {
            if (!runDir.isAbsolute())
            {
                ConfigFile runDirConfig = (ConfigFile)canon.get(RUN_DIR);
                runDirConfig.setValue(runDir.getAbsoluteFile(), runDirConfig.isUserSpecified());
                runDir = runDirConfig.getValueFile();
            }
            
            ConfigFile oDir = (ConfigFile)canon.get(OUTPUT_FILE);
            if (!oDir.getValueFile().isAbsolute())
            {
                oDir.setValue(makeAbsolute(runDir, oDir.getValueFile()), oDir.isUserSpecified());
            }
            
            ConfigFile aoDir = (ConfigFile)canon.get(ACTOR_OUTPUT_DIR);
            if (!aoDir.getValueFile().isAbsolute())
            {
                aoDir.setValue(makeAbsolute(runDir, aoDir.getValueFile()), aoDir.isUserSpecified());
            }
            
            ConfigFile cacheDir = (ConfigFile)canon.get(CACHE_DIR);
            if (!cacheDir.getValueFile().isAbsolute())
            {
                cacheDir.setValue(makeAbsolute(runDir, cacheDir.getValueFile()), cacheDir.isUserSpecified());
            }
            
            // For each entry in the model path that is not absolute, make it absolute based on the run dir
            ConfigList mpConfig = (ConfigList)canon.get(MODEL_PATH);
            List<String> newMP = new ArrayList();
            for (String entry : ((List<String>)mpConfig.getValue()))
            {
                File testFile = new File(entry);
                if (!testFile.isAbsolute())
                    newMP.add(makeAbsolute(runDir, testFile).getAbsolutePath());
                else
                    newMP.add(entry);
            }
            mpConfig.setValue(newMP, mpConfig.isUserSpecified());
        }
        
        return canon;
    }
    
    private static File makeAbsolute (File parent, File child)
    {
        File full = new File(parent, child.getPath());
        return full.getAbsoluteFile();
    }
    
    public Object clone () throws CloneNotSupportedException
    {
        ConfigGroup config = (ConfigGroup)super.clone();
        config.configs = new HashMap();
        for (String key : this.configs.keySet())
            config.configs.put(key, (AbstractConfig)this.configs.get(key).clone());
        return config;
    }

    public void debug (PrintStream ps)
    {
        ps.println(super.toString());
        for (String key : this.configs.keySet())
        {
            ps.println(key + "=>" + this.configs.get(key));
        }
    }
    public String toString ()
    {
        return super.toString() + "(" + this.configs + ")";
    }
    
    private static class ConfigNonEmptyString extends ConfigString
    {
        public ConfigNonEmptyString (String id, String name, String cla, String desc, boolean required, String defaultValue)
        {
            super(id, name, cla, desc, required, defaultValue);
        }
        
        public boolean validate ()
        {
            if (!super.validate()) return false;
            if (getValue() == null) return false;
            if (getValue().length() <= 0) return false;
            return true;
        }
    }

}
