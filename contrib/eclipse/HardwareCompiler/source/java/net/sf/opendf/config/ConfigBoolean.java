package net.sf.opendf.config;

public class ConfigBoolean extends AbstractConfig
{
    private final boolean defaultValue;
    private boolean value = true;
    private boolean userSpecified = false;
    
    public ConfigBoolean (String id, String name, String cla, String desc, boolean required, boolean defaultValue)
    {
        super(id, name, cla, desc, required);
        this.defaultValue = defaultValue;
    }

    public void setValue (boolean value, boolean userSpecified)
    {
        this.value = value;
        this.userSpecified = userSpecified;
    }
    
    public Boolean getValue ()
    {
        return isUserSpecified() ? this.value : this.defaultValue;
    }
    
    /**
     * Returns 0 always.
     */
    public int numArgs ()
    {
        return 0;
    }
    
    @Override
    public int getType ()
    {
        return TYPE_BOOL;
    }

    @Override
    public void unset ()
    {
        this.userSpecified = false;
    }
    
    @Override
    public boolean isUserSpecified ()
    {
        return this.userSpecified;
    }

}
