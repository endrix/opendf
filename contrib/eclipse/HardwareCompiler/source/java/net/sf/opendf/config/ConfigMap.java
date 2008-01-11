package net.sf.opendf.config;

import java.util.*;

public class ConfigMap extends AbstractConfig
{
    private final Map defaultValue;
    private Map value = Collections.EMPTY_MAP;
    
    public ConfigMap (String id, String name, String cla, String desc, boolean required, Map defaultValue)
    {
        super(id, name, cla, desc, required);
        this.defaultValue = defaultValue;
    }

    public void setValue (Map m)
    {
        assert m != null : "Cannot set config map to null";
        this.value = m;
    }
    
    public Map getValue ()
    {
        return isUserSpecified() ? this.value : this.defaultValue;
    }
    
    @Override
    public int getType ()
    {
        return TYPE_MAP;
    }

    @Override
    public void unset ()
    {
        this.value = Collections.EMPTY_MAP;
    }
    
    @Override
    public boolean isUserSpecified ()
    {
        return this.value.size() > 0;
    }

}
