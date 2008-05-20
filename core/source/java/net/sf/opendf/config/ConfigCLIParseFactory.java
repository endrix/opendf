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

import net.sf.opendf.util.logging.Logging;

/**
 * The ConfigCLIParseFactory is a utility for parsing command line arguments and 
 * updating a config store ({@link ConfigGroup}).  
 * 
 * @author imiller
 *
 */
public class ConfigCLIParseFactory
{

    /**
     * Parses the array of command line arguments, updating the config.
     * 
     * @param args
     * @param configs
     * @return a List of String command line arguments that remain unparsed.
     */
    public static List<String> parseCLI (String[] args, ConfigGroup configs)
    {
        List list = new ArrayList();
        for (int i=0; i < args.length; i++)
            list.add(args[i]);
        return parseCLI(list, configs);
    }
    
    /**
     * Parses the specified list of Strings into the given {@link ConfigGroup}, returning any unparsed 
     * arguments in the order that they originally appeared.
     * 
     * @param args
     * @param configs
     * @return a List of Strings from the original args which remain unparsed.
     */
    public static List<String> parseCLI (List<String> args, ConfigGroup configs)
    {
        List<String> varArgs = new LinkedList(args);
        List<String> unparsedArgs = new ArrayList();

        Map<String, AbstractConfig> claMap = new HashMap();
        // negateMap is a mapping of boolean configs to a key prefixed
        // with -no- or --no- depending on the original cla.
        Map<String, AbstractConfig> negateMap = new HashMap();
        for (String key : configs.getConfigs().keySet())
        {
            AbstractConfig cfg = configs.get(key);
            if (cfg.getCLA().length() > 0)
            {
                claMap.put(cfg.getCLA(), cfg);
                if ((cfg.getType() == AbstractConfig.TYPE_BOOL) &&
                    cfg.numArgs() == 0)
                {
                    String negKey = ((ConfigBoolean)cfg).getNegatedKey();
                    negateMap.put(negKey, cfg);
                }
            }
        }
        Logging.dbg().fine("CLAMap(pre) " + claMap);
        Logging.dbg().fine("negate map(pre) " + negateMap);
        
        while (!varArgs.isEmpty())
        {
            String arg = varArgs.remove(0);
            Logging.dbg().info("Parsing " + arg);
            AbstractConfig config = claMap.get(arg);
            Logging.dbg().info("Parsing " + arg);
            boolean negate = false;
            if (config == null)
            {
                config = negateMap.get(arg);
                negate = config != null;
            }
            if (config == null)
            {
                Logging.dbg().warning("Unknown command line argument: " + arg);
                unparsedArgs.add(arg);
                continue;
            }
            
            String[] additional = new String[config.numArgs()];
            for (int i=0; i < additional.length; i++)
            {
                if (varArgs.isEmpty())
                {
                    Logging.user().severe("Insufficient additional command line arguments for " + arg);
                    unparsedArgs.add(arg);
                    for (int j=0; j < i-1; j++)
                        unparsedArgs.add(additional[j]);
                    continue;
                }
                additional[i] = varArgs.remove(0); 
            }
            
            if (!config.allowMultiples() && config.isUserSpecified())
            {
                Logging.user().severe("Cannot set multiple values for " + arg);
                unparsedArgs.add(arg);
                for (int j=0; j < additional.length; j++)
                    unparsedArgs.add(additional[j]);
                continue;
            }
            
            switch (config.getType())
            {
            case AbstractConfig.TYPE_BOOL : assert additional.length == 0 : "Boolean config expects 0 additional tokens";
                if (!negate) ((ConfigBoolean)config).setValue(true, true);
                else ((ConfigBoolean)config).setValue(false, true);
                break;
            case AbstractConfig.TYPE_DIR : assert additional.length == 1 : "Dir config expects 1 additional tokens";
                ((ConfigFile)config).setValue(additional[0], true);
                break;
            case AbstractConfig.TYPE_FILE : assert additional.length == 1 : "File config expects 1 additional tokens";
                ((ConfigFile)config).setValue(additional[0], true);
                break;
            case AbstractConfig.TYPE_INT : assert additional.length == 1 : "int config expects 1 additional tokens";
                if (additional[0].startsWith("0x") || additional[0].startsWith("0X"))
                    ((ConfigInt)config).setValue(Integer.parseInt(additional[0], 16), true);
                else
                    ((ConfigInt)config).setValue(Integer.parseInt(additional[0]), true);
                break;
            case AbstractConfig.TYPE_LIST : assert additional.length == 1 : "list config expects 1 additional tokens";
                // ((ConfigList)config).addValue(Collections.singletonList(additional[0]), true);
               ((ConfigList)config).setValue(additional[0], true);
                break;
            case AbstractConfig.TYPE_MAP : assert additional.length == 1 : "map config expects 1 additional tokens";
                if (additional[0].indexOf('=') < 0)
                    Logging.user().severe("Missing '=' in map configuration value: " + arg + ": " + additional[0]);
                String mkey = additional[0].substring(0, additional[0].indexOf('='));
                String mval = additional[0].length() > (additional[0].indexOf('=')+1) ? additional[0].substring(additional[0].indexOf('=')+1): "";
                ((ConfigMap)config).addValue(Collections.singletonMap(mkey, mval), true);
                break;
            case AbstractConfig.TYPE_SET : assert additional.length == 1 : "set config expects 1 additional tokens";
                ((ConfigSet)config).addValue(Collections.singleton(additional[0]), true);
                break;
            case AbstractConfig.TYPE_STRING : assert additional.length == 1 : "string config expects 1 additional tokens";
                ((ConfigString)config).setValue(additional[0], true);
                break;
            }
        }
        
        Logging.dbg().fine("CLAMap(post) " + claMap);
        Logging.dbg().fine("Unparsed: " + unparsedArgs);
        return unparsedArgs;
    }
    
}
