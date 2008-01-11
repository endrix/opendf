package net.sf.opendf.config;

public class ConfigString extends AbstractConfig
{
    private final String defaultValue;
    private String value = null;
    
    public ConfigString (String id, String name, String cla, String desc, boolean required, String defaultValue)
    {
        super(id, name, cla, desc, required);
        assert defaultValue != null : "Cannot specify null as default value";
        this.defaultValue = defaultValue;
    }
    
    public void setValue (String value)
    {
        assert value != null : "Cannot specify null value for config string";
        this.value = value;
    }
    
    public String getValue ()
    {
        return isUserSpecified() ? this.value:this.defaultValue;
    }
    
    @Override
    public int getType ()
    {
        return TYPE_STRING;
    }

    @Override
    public void unset ()
    {
        this.value = null;
    }
    
    @Override
    public boolean isUserSpecified ()
    {
        return this.value != null && !this.defaultValue.equals(this.value);
    }

}
