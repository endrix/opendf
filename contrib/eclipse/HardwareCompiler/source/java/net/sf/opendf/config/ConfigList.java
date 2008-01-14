package net.sf.opendf.config;

import java.io.File;
import java.util.*;

public class ConfigList extends AbstractConfig
{
    private final List defaultValue;
    private List value = Collections.EMPTY_LIST;
    private boolean userSpecified = false;
    
    public ConfigList (String id, String name, String cla, String desc, boolean required, List defaultValue)
    {
        super(id, name, cla, desc, required);
        this.defaultValue = defaultValue;
    }

    public void setValue (String s, boolean userSpecified)
    {
        StringTokenizer st = new StringTokenizer(s, File.pathSeparator);
        List<String> paths = new ArrayList();
        while (st.hasMoreTokens())
        {
            paths.add(st.nextToken());
        }
        setValue(paths, userSpecified);
    }
    public void setValue (List l, boolean userSpecified)
    {
        assert l != null : "Cannot set config list to null";
        this.value = l;
        this.userSpecified = userSpecified;
    }
    
    public List getValue ()
    {
        return isUserSpecified() ? this.value : this.defaultValue;
    }
    
    @Override
    public void unset ()
    {
        this.value = Collections.EMPTY_LIST;
        this.userSpecified = false;
    }
    
    @Override
    public int getType ()
    {
        return TYPE_LIST;
    }

    @Override
    public boolean isUserSpecified ()
    {
        return this.userSpecified;
    }

}
