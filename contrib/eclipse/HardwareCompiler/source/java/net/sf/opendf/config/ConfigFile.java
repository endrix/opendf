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
    
    public void addFilter (String filter, String description)
    {
        assert filter != null : "Cannot specify null filter";
        assert filter.length() > 0 : "Cannot specify empty filter";
        filters.put(filter, description);
    }
    
    public Map<String, String> getFilters ()
    {
        return Collections.unmodifiableMap(this.filters);
    }

    public File getValueFile ()
    {
        return new File(getValue());
    }

    public boolean validate ()
    {
        if (!super.validate()) return false;

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
        return validExtension;
    }
    
    @Override
    public int getType ()
    {
        return TYPE_FILE;
    }
    
    public static class Dir extends ConfigFile
    {
        public Dir (String id, String name, String cla, String desc, boolean required)
        {
            super(id, name, cla, desc, required);
        }

        public int getType ()
        {
            return TYPE_DIR;
        }
    }
}
