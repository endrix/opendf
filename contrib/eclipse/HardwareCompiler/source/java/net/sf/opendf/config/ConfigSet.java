package net.sf.opendf.config;

import java.util.*;

public class ConfigSet extends AbstractConfig
{
    private final Set defaultValue;
    private Set value = Collections.EMPTY_SET;
    private boolean userSpecified = false;
    
    public ConfigSet (String id, String name, String cla, String desc, boolean required, Set defaultValue)
    {
        super(id, name, cla, desc, required);
        this.defaultValue = defaultValue;
    }

    public void setValue (Collection l, boolean userSpecified)
    {
        assert l != null : "Cannot set config Set to null";
        Set setView = l instanceof Set ? ((Set)l):new HashSet(l);
        assert l.size() == setView.size() : "Set view of collection does not match original collection (config set)";
        this.value = setView;
        this.userSpecified = userSpecified;
    }
    
    public Set getValue ()
    {
        return isUserSpecified() ? this.value : this.defaultValue;
    }
    
    @Override
    public int getType ()
    {
        return TYPE_SET;
    }

    @Override
    public void unset ()
    {
        this.value = Collections.EMPTY_SET;
        this.userSpecified = false;
    }
    
    @Override
    public boolean isUserSpecified ()
    {
        return this.userSpecified;
    }

}
