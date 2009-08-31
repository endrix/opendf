package net.sf.opendf.config;

import java.util.*;

import net.sf.opendf.config.AbstractConfig.ConfigError;

public class ModelParameterMap extends ConfigMap
{

    public ModelParameterMap (String id, String name, String cla, String desc,
            boolean required, Map defaultValue)
    {
        super(id, name, cla, desc, required, defaultValue);
    }

    @Override
    public List<ConfigError> getErrors ()
    {
        List<ConfigError> errs = new ArrayList(super.getErrors());
        Map<String, String> values = getValue();
        for (String key : values.keySet())
        {
            if (key == null) errs.add(new ConfigError("Null parameter key not allowed", new MapError(key)));
            if (values.get(key) == null) errs.add(new ConfigError("Null value not allowed", new MapError(key)));
            if (values.get(key).equals("")) errs.add(new ConfigError("Value not specified for: "+key, new MapError(key)));
        }
        return errs;
    }


    
}
