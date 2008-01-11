package net.sf.opendf.config;

import java.util.*;

public class ConfigGroup
{
    public static final String TOP_MODEL_NAME = "model.top.name";
    public static final String TOP_MODEL_FILE = "model.top.file";
    public static final String RUN_DIR = "config.run.dir";
    public static final String OUTPUT_FILE_NAME = "output.file.name";
    public static final String ACTOR_OUTPUT_DIR = "output.actor.dir";
    public static final String CACHE_DIR = "cache.dir";
    public static final String GEN_HDL_SIM_MODEL = "output.hdl.simmodel";
    public static final String MODEL_PATH = "model.search.path";
    
    public static final String ENABLE_ASSERTIONS = "assertions.enable";
    public static final String VERBOSE = "verbose";
    public static final String QUIET = "quiet";
    
    
    private final Map<String, AbstractConfig> configs = new HashMap();
    public ConfigGroup ()
    {
        final ConfigFile topFile = new ConfigFile (TOP_MODEL_FILE, "Top Model File", 
                "", // cla 
                "Specifies the top level model file to be compiled", 
                true // required 
        );
        topFile.addFilter("*.nl", "Network");
        topFile.addFilter("*.cal", "CAL Source");
        topFile.addFilter("*.xdf", "Structural Network");
        configs.put(TOP_MODEL_FILE,  topFile);
        
        configs.put(TOP_MODEL_NAME,new ConfigNonEmptyString (TOP_MODEL_NAME, "Top Model Name", 
                "", // cla 
                "Specifies the top level model name for compilation", 
                true, // required 
                "" // default
        )); 
        
        configs.put(RUN_DIR, new ConfigFile.Dir (RUN_DIR, "Compilation working directory (run directory)",
                "-rundir", 
                "Specify the working directory for compilation.  It is from this directory that relative paths are based from.",
                false
        ));
        final ConfigFile oFile =  new ConfigFile (OUTPUT_FILE_NAME, "Output File Name",
                "-o", 
                "Specify the compilation output filename",
                false
        );
        oFile.addFilter("*.vhd", "VHDL");
        configs.put(OUTPUT_FILE_NAME, oFile);

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
        configs.put(MODEL_PATH, new ConfigList (GEN_HDL_SIM_MODEL, "Generate HDL sim model",
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
