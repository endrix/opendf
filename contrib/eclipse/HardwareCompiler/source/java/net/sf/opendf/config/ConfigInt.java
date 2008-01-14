package net.sf.opendf.config;

public class ConfigInt extends AbstractConfig
{
    private final int defaultValue;
    private int value = -1;
    private boolean userSpecified = false;
    
    public ConfigInt (String id, String name, String cla, String desc, boolean required, int defaultValue)
    {
        super(id, name, cla, desc, required);
        this.defaultValue = defaultValue;
    }

    public void setValue (int value, boolean userSpecified)
    {
        this.value = value;
        this.userSpecified = userSpecified;
    }
    
    public Integer getValue ()
    {
        return isUserSpecified() ? this.value : this.defaultValue;
    }
    
    @Override
    public int getType ()
    {
        return TYPE_INT;
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
