package net.sf.opendf.config;

import java.io.File;
import java.util.*;

public class ConfigList extends AbstractConfig
{
    private final List defaultValue;
    private List value = Collections.EMPTY_LIST;
    
    public ConfigList (String id, String name, String cla, String desc, boolean required, List defaultValue)
    {
        super(id, name, cla, desc, required);
        this.defaultValue = defaultValue;
    }

    public void setValue (String s)
    {
        StringTokenizer st = new StringTokenizer(s, File.pathSeparator);
        List<String> paths = new ArrayList();
        while (st.hasMoreTokens())
        {
            paths.add(st.nextToken());
        }
        setValue(paths);
    }
    public void setValue (List l)
    {
        assert l != null : "Cannot set config list to null";
        this.value = l;
    }
    
    public List getValue ()
    {
        return isUserSpecified() ? this.value : this.defaultValue;
    }
    
    @Override
    public void unset ()
    {
        this.value = Collections.EMPTY_LIST;
    }
    
    @Override
    public int getType ()
    {
        return TYPE_LIST;
    }

    @Override
    public boolean isUserSpecified ()
    {
        return this.value.size() > 0;
    }

}
