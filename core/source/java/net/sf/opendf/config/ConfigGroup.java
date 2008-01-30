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
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.opendf.util.logging.Logging;

public abstract class ConfigGroup implements Cloneable
{
    /** The top model file is only valid as a user specification.  Other configurations
     * may be derived from its value (eg run dir, top model name, etc).  The top model name
     * and the model path should be used for loading the design.  
     */
    public static final String TOP_MODEL_FILE = "model.top.file";
    
    // Common for all configs
    public static final String VERSION = "version";
    public static final String LOG_LEVEL_USER = "logging.user.level";
    public static final String LOG_LEVEL_DBG  = "logging.dbg.level";
    public static final String LOG_LEVEL_SIM = "logging.sim.level";
    // These are private as they are NOT intended to be directly accessed
    private static final String QQ = "logging.quiet.all";
    private static final String Q = "logging.quiet.some";
    private static final String VV = "logging.verbose.all";
    private static final String V = "logging.verbose.some";
    
    public static final String TOP_MODEL_NAME = "model.top.name";
    public static final String RUN_DIR = "config.run.dir";
    public static final String CACHE_DIR = "cache.dir";
    public static final String MODEL_PATH = "model.search.path";
    public static final String TOP_MODEL_PARAMS = "model.top.parameters";
    public static final String MESSAGE_SUPPRESS_IDS = "logging.message.suppress.ids";
    public static final String ELABORATE_TOP = "model.elaborate";
    public static final String ELABORATE_PP = "model.elaborate.postprocess";
    public static final String ELABORATE_INLINE = "model.elaborate.inline";
    
    // Simulation only
    public static final String SIM_INPUT_FILE = "sim.input.stimulus.file";
    public static final String SIM_INTERPRET_STIMULUS = "sim.input.stimulus.interpret";
    public static final String SIM_OUTPUT_FILE = "sim.output.results.file";
    public static final String SIM_TIME = "sim.max.time";
    public static final String SIM_STEPS = "sim.max.steps";
    public static final String SIM_MAX_ERRORS = "sim.max.errors";
    public static final String SIM_BUFFER_IGNORE = "sim.buffer.bounds.ignore";
    public static final String SIM_BUFFER_RECORD = "sim.bbr";
    public static final String SIM_BUFFER_SIZE_WARNING = "sim.buffer.size.warning";
    public static final String SIM_TRACE = "sim.trace";
    public static final String SIM_TYPE_CHECK = "sim.types.check";
    public static final String ENABLE_ASSERTIONS = "assertions.enable";
    
    // synthesis only
    public static final String HDL_OUTPUT_FILE= "synth.output.file.name";
    public static final String ACTOR_OUTPUT_DIR = "synth.output.actor.dir";
    public static final String GEN_HDL_SIM_MODEL = "synth.output.hdl.simmodel";

    // XSLT Transformations only
    public static final String XSLT_PRESERVE_INTERMEDIATE = "xslt.transform.preserve";
    
    private Map<String, AbstractConfig> configs = new HashMap();
    
    public ConfigGroup ()
    {
        registerConfig(VERSION, new ConfigBoolean(VERSION, "Version", 
                "--version", 
                "Report version id and usage information and then terminate",
                false, // required
                false // default
                ) );
        
        registerConfig(LOG_LEVEL_USER, new ConfigStringPickOne.ConfigLogLevels(LOG_LEVEL_USER, "User Log Level",
                "-userlog", 
                "Specify the level of messages directed to user console.", 
                false, // required 
                Logging.user().getLevel().getName() // default
                ) );

        registerConfig(LOG_LEVEL_DBG, new ConfigStringPickOne.ConfigLogLevels(LOG_LEVEL_DBG, "Debug Log Level",
                "-dbglog", 
                "Specify the level of debug messages directed to console.", 
                false, // required 
                Logging.dbg().getLevel().getName() // default
                ) );

        registerConfig(LOG_LEVEL_SIM, new ConfigStringPickOne.ConfigLogLevels(LOG_LEVEL_SIM, "Simulation Log Level",
                "-simlog", 
                "Specify the level of simulation messages directed to console.", 
                false, // required 
                Logging.simout().getLevel().getName() // default
                ) );
        
        // This config exists for short-cut setting of logging via the command line.
        registerConfig(Q, new ConfigLoggingComposite(Q, "Run Quiet",
                "-q",
                "Run with minimal output to console",
                false, // required
                new ConfigString[]{
                (ConfigString)this.get(LOG_LEVEL_USER),
                (ConfigString)this.get(LOG_LEVEL_DBG)
                //, (ConfigString)this.getConfigs(LOG_LEVEL_SIM) 
                },
                Level.WARNING.getName()
                ) );
        // This config exists for short-cut setting of logging via the command line.
        registerConfig(QQ, new ConfigLoggingComposite(QQ, "Run Silent",
                "-qq",
                "Run with no output to console",
                false, // required
                new ConfigString[]{
                (ConfigString)this.get(LOG_LEVEL_USER),
                (ConfigString)this.get(LOG_LEVEL_DBG)
                //, (ConfigString)this.getConfigs(LOG_LEVEL_SIM) 
                },
                Level.OFF.getName()
                ) );
        // This config exists for short-cut setting of logging via the command line.
        registerConfig(V, new ConfigLoggingComposite(V, "Verbose",
                "-v",
                "Verbose output",
                false, // required
                new ConfigString[]{
                (ConfigString)this.get(LOG_LEVEL_USER),
                (ConfigString)this.get(LOG_LEVEL_DBG)
                //, (ConfigString)this.getConfigs(LOG_LEVEL_SIM) 
                },
                Level.FINE.getName()
                ) );
        // This config exists for short-cut setting of logging via the command line.
        registerConfig(VV, new ConfigLoggingComposite(VV, "Very Verbose",
                "-vv",
                "Extremely Verbose output",
                false, // required
                new ConfigString[]{
                (ConfigString)this.get(LOG_LEVEL_USER),
                (ConfigString)this.get(LOG_LEVEL_DBG)
                //, (ConfigString)this.getConfigs(LOG_LEVEL_SIM) 
                },
                Level.ALL.getName()
                ) );
        
        
        
        final ConfigFile topFile = new ConfigFile (TOP_MODEL_FILE, "Top Model File", 
                "", // cla 
                "Specifies the top level model file to be compiled", 
                false // required 
        );
        topFile.addFilter("*.nl", "Network");
        topFile.addFilter("*.cal", "CAL Source");
        topFile.addFilter("*.xdf", "Structural Network");
        registerConfig(TOP_MODEL_FILE,  topFile);
        
        registerConfig(TOP_MODEL_NAME,new ConfigNonEmptyString (TOP_MODEL_NAME, "Top Model Name", 
                "", // cla 
                "Specifies the top level model file for compilation", 
                true, // required 
                "" // default
        )); 
        
        registerConfig(RUN_DIR, new ConfigFile.Dir (RUN_DIR, "Working Directory (run directory)",
                "-rundir", 
                "Specify the working directory for compilation.  It is from this directory that relative paths are based from.",
                false,
                "."
        ));

        registerConfig(CACHE_DIR, new ConfigFile (CACHE_DIR, "Cache Directory",
                "-cache",
                "The directory used for storing cache files.  Caching is disabled if unspecified.",
                false,
                "cache"
        ));
        
        registerConfig(MODEL_PATH, new ConfigList (MODEL_PATH, "Model Search Path",
                "-mp", // cla
                "Specification of the model search path",
                false, // required
                Collections.singletonList(".")// default
        ));
        
        registerConfig(TOP_MODEL_PARAMS, new ModelParameterMap(TOP_MODEL_PARAMS, "Model Parameters",
                "-D", // cla
                "Specify top level model parameters <key>=<value>",
                false, // required
                Collections.EMPTY_MAP // default
        ));
    
        
        registerConfig(MESSAGE_SUPPRESS_IDS, new ConfigList (MESSAGE_SUPPRESS_IDS, "Suppressable Messages",
                "--suppress-message", // cla
                "Messages with the specified prefix are suppressed",
                false, // required
                Collections.EMPTY_LIST //default
        ));

        registerConfig(ELABORATE_TOP, new ConfigBoolean(ELABORATE_TOP, "Elaborate", 
                "-e",
                "Elaborate the top model prior to simulation",
                false, // required
                true // default
                ));
        
        registerConfig(ELABORATE_PP, new ConfigBoolean(ELABORATE_PP, "Elaboration Post Process", 
                "-pp",
                "Peform post processing after elaboration",
                false, // required
                false // default
                ));
        
        registerConfig(ELABORATE_INLINE, new ConfigBoolean(ELABORATE_INLINE, "Elaboration Inline Code", 
                "-inline",
                "Inline Actor source into elaborated network",
                false, // required
                false // default
                ));
    };
    
    protected void registerConfig (String key, AbstractConfig cfg)
    {
        this.configs.put(key, cfg);
    }
    
    public abstract ConfigGroup getEmptyConfigGroup ();
    
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
    
    /**
     * Update this ConfigGroup from the data in the specified configuration. 
     * 
     * @param configuration
     * @param keys a Collection of which configuration entries to update
     */
    public void updateConfig (ConfigUpdateIF configuration, Collection<String> keys)
    {
        Map<String, AbstractConfig> configMap = this.getConfigs();
        for (String key : keys)
        {
            assert configMap.containsKey(key) : "Key does not exist in the configuration. ";
            switch (configMap.get(key).getType())
            {
            case AbstractConfig.TYPE_BOOL:
                configuration.importConfig((ConfigBoolean)configMap.get(key));
                break;
            case AbstractConfig.TYPE_STRING:
            case AbstractConfig.TYPE_PICKONE:
                configuration.importConfig((ConfigString)configMap.get(key));
                break;
            case AbstractConfig.TYPE_FILE:
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

    /**
     * Push the values in this ConfigGroup to the specified configuration.
     * 
     * @param configuration
     * @param keys
     */
    public void pushConfig (ConfigUpdateIF configuration, Collection<String> keys)
    {
        Map<String, AbstractConfig> configMap = this.getConfigs();
        for (String key : keys)
        {
            assert configMap.containsKey(key) : "Key does not exist in the configuration. ";
            switch (configMap.get(key).getType())
            {
            case AbstractConfig.TYPE_BOOL:
                configuration.exportConfig((ConfigBoolean)configMap.get(key));
                break;
            case AbstractConfig.TYPE_STRING:
            case AbstractConfig.TYPE_PICKONE:
                configuration.exportConfig((ConfigString)configMap.get(key));
                break;
            case AbstractConfig.TYPE_FILE:
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
            
            // Make the cache directory absolute only if it is not empty (and not already absolute)
            ConfigFile cacheDir = (ConfigFile)canon.get(CACHE_DIR);
            if (!"".equals(cacheDir.getValue()) && !cacheDir.getValueFile().isAbsolute())
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
    
    protected static File makeAbsolute (File parent, File child)
    {
        File full = new File(parent, child.getPath());
        return full.getAbsoluteFile();
    }
    
    public void usage (Logger logger, Level level)
    {
        final int colSpacing = 5;
        final String valueString = " <value>";

        final Map<String, AbstractConfig> byCLA = new HashMap();
        // Calculate the max width of any entry in the first column
        int firstColumnWidth = 0;
        for (String key : getConfigs().keySet())
        {
            AbstractConfig cfg = getConfigs().get(key);
            firstColumnWidth = Math.max(firstColumnWidth,
                cfg.getCLA().length() + (cfg.numArgs()*valueString.length()));
            byCLA.put(cfg.getCLA(), cfg);
        }


        // Print out the two column display
        //for (String key : getConfigs().keySet())
        List<String>clas = new ArrayList(byCLA.keySet());
        Collections.sort(clas);
        for (String arg : clas)
        {
            //AbstractConfig cfg = getConfigs().get(key);
            //String arg = cfg.getCLA();
            AbstractConfig cfg = byCLA.get(arg);
            if ("".equals(arg))
            {
                for (int i=0; i < cfg.numArgs(); i++)
                    arg += "\'string\'" + (i == cfg.numArgs()-1 ? "":" ");
            }
            else
            {
                for (int i=0; i < cfg.numArgs(); i++)
                    arg += " <value>";
            }
            for (int i=arg.length(); i < firstColumnWidth+colSpacing; i++)
                arg += " ";
            String desc = cfg.getDescription() + " [" + cfg.getName() +"]";
            logger.log(level, "\t" + arg + "\t" + desc);
        }
        logger.log(level, "Boolean options may be negated with -no-xxx or --no-xxx");
    }
    
    public Object clone () throws CloneNotSupportedException
    {
        ConfigGroup config = (ConfigGroup)super.clone();
        config.configs = new HashMap();
        for (String key : this.configs.keySet())
            config.registerConfig(key, (AbstractConfig)this.configs.get(key).clone());
        return config;
    }

    public void debug (PrintStream ps)
    {
        ps.println(super.toString());
        for (String key : this.configs.keySet())
        {
            ps.println("\t" + key + "=>" + this.configs.get(key));
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

    private static class ConfigLoggingComposite extends ConfigBoolean
    {
        private List<ConfigString> loggers;
        private String specValue;
        public ConfigLoggingComposite(String id, String name, String cla, String desc,
                boolean required, ConfigString[] channels, String value)
        {
            super(id, name, cla, desc, required, false);
            this.loggers = new ArrayList();
            for (int i=0; i < channels.length; i++) loggers.add(channels[i]);
            this.specValue = value;
        }

        public void setValue (boolean value, boolean userSpecified)
        {
            super.setValue(value, userSpecified);

            if (value)
            {
                for (ConfigString cfg : this.loggers)
                {
                    cfg.setValue(this.specValue, userSpecified);
                }
            }
        }
    }
    
}
