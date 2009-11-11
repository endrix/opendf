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

import java.io.*;
import java.util.*;

public class ConfigFile extends ConfigString
{
    /**
     * A map of filters to their textual descriptions.  For example "*.nl":"Network"
     */
    private Map<String,String> filters = new LinkedHashMap();
    
    public ConfigFile (String id, String name, String cla, String desc, boolean required, String defaultValue)
    {
        super(id, name, cla, desc, required, defaultValue);
    }
    public ConfigFile (String id, String name, String cla, String desc, boolean required)
    {
        this(id, name, cla, desc, required, "");
    }

    /**
     * Specifies a filename filter which defines a valid file for this configuration.
     * @param filter
     * @param description
     */
    public void addFilter (String filter, String description)
    {
        assert filter != null : "Cannot specify null filter";
        assert filter.length() > 0 : "Cannot specify empty filter";
        filters.put(filter, description);
    }
    
    public void removeFilter (String filter)
    {
        this.filters.remove(filter);
    }
    
    public Map<String, String> getFilters ()
    {
        return Collections.unmodifiableMap(this.filters);
    }

    public void setValue (File file, boolean userSpecified)
    {
        if (file == null) throw new IllegalArgumentException("Cannot specify null file for config");
        setValue(file.getPath(), userSpecified);
    }
    
    public File getValueFile ()
    {
        return new File(getValue());
    }

    public List<ConfigError> getErrors ()
    {
        List<ConfigError> errs = new ArrayList(super.getErrors());

        // If the user has specified the file, check any specified filter extensions
        boolean validExtension = true;
        if (isUserSpecified())
        {
            if (this.filters.keySet().size() > 0)
            {
                validExtension = false;
                String fileName = this.getValueFile().getName();
                for (String filter : this.filters.keySet())
                {
                    validExtension |= fileName.endsWith(filter.substring(filter.indexOf('.')));
                }
            }
        }
        if (!validExtension)
            errs.add(new ConfigError("Filename extension invalid.  Expecting one of: "+this.filters.keySet().toString(), null));
        return errs;
    }
    
    @Override
    public int getType ()
    {
        return TYPE_FILE;
    }
    
    /**
     * Returns a clone of this ConfigFile which has the same filters specified in a unique map.
     */
    public Object clone () throws CloneNotSupportedException
    {
        ConfigFile config = (ConfigFile)super.clone();
        config.filters = new LinkedHashMap(this.filters);
        return config;
    }
    
    public static class Dir extends ConfigFile
    {
        public Dir (String id, String name, String cla, String desc, boolean required)
        {
            super(id, name, cla, desc, required);
        }
        
        public Dir (String id, String name, String cla, String desc, boolean required, String defaultValue)
        {
            super(id, name, cla, desc, required, defaultValue);
        }

        public int getType ()
        {
            return TYPE_DIR;
        }
    }
    

    
}
