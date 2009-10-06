package net.sf.opendf.config;

public class ConfigGroupCopyWrapper implements ConfigUpdateIF
{
    private ConfigGroup source = null;
    
    public ConfigGroupCopyWrapper (ConfigGroup source)
    {
        this.source = source;
    }

    public void exportConfig (ConfigBoolean config) { throw new UnsupportedOperationException("Cannot update the source configuration via the config copy wrapper"); }
    public void exportConfig (ConfigInt config) { throw new UnsupportedOperationException("Cannot update the source configuration via the config copy wrapper"); }
    public void exportConfig (ConfigList config) { throw new UnsupportedOperationException("Cannot update the source configuration via the config copy wrapper"); }
    public void exportConfig (ConfigMap config) { throw new UnsupportedOperationException("Cannot update the source configuration via the config copy wrapper"); }
    public void exportConfig (ConfigSet config) { throw new UnsupportedOperationException("Cannot update the source configuration via the config copy wrapper"); }
    public void exportConfig (ConfigString config) { throw new UnsupportedOperationException("Cannot update the source configuration via the config copy wrapper"); }

    public void importConfig (ConfigBoolean config)
    {
        ConfigBoolean src = (ConfigBoolean)source.get(config.getID());
        if (src != null)
        {
            config.setValue(src.getValue().booleanValue(), src.isUserSpecified());
        }
    }

    public void importConfig (ConfigInt config)
    {
        ConfigInt src = (ConfigInt)source.get(config.getID());
        if (src != null)
        {
            config.setValue(src.getValue().intValue(), src.isUserSpecified());
        }
    }

    public void importConfig (ConfigList config)
    {
        ConfigList src = (ConfigList)source.get(config.getID());
        if (src != null)
        {
            config.setValue(src.getValue(), src.isUserSpecified());
        }
    }

    public void importConfig (ConfigMap config)
    {
        ConfigMap src = (ConfigMap)source.get(config.getID());
        if (src != null)
        {
            config.setValue(src.getValue(), src.isUserSpecified());
        }
    }

    public void importConfig (ConfigSet config)
    {
        ConfigSet src = (ConfigSet)source.get(config.getID());
        if (src != null)
        {
            config.setValue(src.getValue(), src.isUserSpecified());
        }
    }

    public void importConfig (ConfigString config)
    {
        ConfigString src = (ConfigString)source.get(config.getID());
        if (src != null)
        {
            config.setValue(src.getValue(), src.isUserSpecified());
        }
    }

}
