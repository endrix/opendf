package net.sf.opendf.config;

import java.util.*;
import java.io.*;

public abstract class AbstractConfig
{
    public static final int TYPE_INT    = 1;
    public static final int TYPE_STRING = 2;
    public static final int TYPE_BOOL   = 3;
    public static final int TYPE_LIST   = 4;
    public static final int TYPE_SET    = 5;
    public static final int TYPE_MAP    = 6;
    public static final int TYPE_FILE   = 7;
    public static final int TYPE_DIR    = 8;
    
    private boolean isRequired = false;
    private String id;
    private String name;
    private String desc;
    private String cla;
    
    public AbstractConfig (String id, String name, String cla, String desc, boolean required)
    {
        this.isRequired = required;
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.cla = cla;
    }
    
    public String getID () { return this.id; }
    public String getName () { return this.name; }
    public String getDescription () { return this.desc; }
    public String getCLA () { return this.cla; }
    
    /**
     * Throws UnsupportedOperationException unless overidden in a subclass
     */
    public void setValue (int value) { throw new UnsupportedOperationException("int is an unsupported value type for " + getClass()); }
    /**
     * Throws UnsupportedOperationException unless overidden in a subclass
     */
    public void setValue (String value) { throw new UnsupportedOperationException("String is an unsupported value type for " + getClass()); }
    /**
     * Throws UnsupportedOperationException unless overidden in a subclass
     */
    public void setValue (boolean value) { throw new UnsupportedOperationException("boolean is an unsupported value type for " + getClass()); }
    /**
     * Throws UnsupportedOperationException unless overidden in a subclass
     */
    public void setValue (List value) { throw new UnsupportedOperationException("List is an unsupported value type for " + getClass()); }
    /**
     * Throws UnsupportedOperationException unless overidden in a subclass
     */
    public void setValue (Set value) { throw new UnsupportedOperationException("Set is an unsupported value type for " + getClass()); }
    /**
     * Throws UnsupportedOperationException unless overidden in a subclass
     */
    public void setValue (Map value) { throw new UnsupportedOperationException("Map is an unsupported value type for " + getClass()); }
    /**
     * Throws UnsupportedOperationException unless overidden in a subclass
     */
    public void setValue (File value) { throw new UnsupportedOperationException("File is an unsupported value type for " + getClass()); }

    /**
     * Specifies the number of additional arguments required for this configuration.
     * 
     * @return a non-negative int value
     */
    public int numArgs () { return 1; }
    
    public boolean validate ()
    {
        return this.isRequired && isUserSpecified();
    }
    
    /**
     * Used to clear all user specification (revert to default value)
     */
    public abstract void unset ();
    
    public abstract boolean isUserSpecified ();
    
    public abstract int getType ();
    
}
