/* 
BEGINCOPYRIGHT X
    
    Copyright (c) 2008, Xilinx Inc.
    All rights reserved.
    
    Redistribution and use in source and binary forms, 
    with or without modification, are permitted provided 
    that the following conditions are met:
    - Redistributions of source code must retain the above 
      copyright notice, this list of conditions and the 
      following disclaimer.
    - Redistributions in binary form must reproduce the 
      above copyright notice, this list of conditions and 
      the following disclaimer in the documentation and/or 
      other materials provided with the distribution.
    - Neither the name of the copyright holder nor the names 
      of its contributors may be used to endorse or promote 
      products derived from this software without specific 
      prior written permission.
    
    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
    CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
    INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
    MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
    DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
    CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
    SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
    NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
    LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
    HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
    OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
    
ENDCOPYRIGHT
*/
package net.sf.opendf.config;

import java.util.*;
import java.io.*;

public abstract class AbstractConfig implements Cloneable
{
    public static final int TYPE_INT     = 1;
    public static final int TYPE_STRING  = 2;
    public static final int TYPE_PICKONE = 3;
    public static final int TYPE_BOOL    = 4;
    public static final int TYPE_LIST    = 5;
    public static final int TYPE_SET     = 6;
    public static final int TYPE_MAP     = 7;
    public static final int TYPE_FILE    = 8;
    public static final int TYPE_DIR     = 9;
    
    private boolean isRequired = false;
    private String id;
    private String name;
    private String desc;
    private String cla;
    
    /**
     * Constructs a new configuration element.
     * 
     * @param id an internally used identifier that is unique across all configs in a ConfigGroup
     * @param name a user visible, short, name for the config
     * @param cla the command line argument used to specify this config (including any leading -)
     * @param desc a longer description of the config.
     * @param required set to true if the user must specify this configuration.
     */
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
     * Returns true if multiple settings of the value are legal.
     * 
     * @return true if multiple settings of the value are legal.
     */
    public boolean allowMultiples () { return false; }
    
    /**
     * Retrieve the configuration value as an object.  The type of object will depend on the 
     * configuration type as specified by {@link getType}.
     * @return
     */
    public abstract Object getValue ();

    /**
     * Set the config to the specified value.  The boolean parameter is used to differentiate 
     * between internal configuration settings and those explicitly specified by the user. 
     * 
     * @param value
     * @param userSpecified
     */
    public void setValue (int value, boolean userSpecified) { throw new UnsupportedOperationException("int is an unsupported value type for " + getClass()); }
    public void setValue (String value, boolean userSpecified) { throw new UnsupportedOperationException("String is an unsupported value type for " + getClass()); }
    public void setValue (boolean value, boolean userSpecified) { throw new UnsupportedOperationException("boolean is an unsupported value type for " + getClass()); }
    public void setValue (List value, boolean userSpecified) { throw new UnsupportedOperationException("List is an unsupported value type for " + getClass()); }
    public void setValue (Set value, boolean userSpecified) { throw new UnsupportedOperationException("Set is an unsupported value type for " + getClass()); }
    public void setValue (Map value, boolean userSpecified) { throw new UnsupportedOperationException("Map is an unsupported value type for " + getClass()); }
    public void setValue (File value, boolean userSpecified) { throw new UnsupportedOperationException("File is an unsupported value type for " + getClass()); }

    /**
     * Specifies the number of additional arguments required for this configuration.
     * 
     * @return a non-negative int value
     */
    public int numArgs () { return 1; }
    
    public boolean validate ()
    {
        return getErrors().size() == 0;
    }
    
    public List<ConfigError> getErrors ()
    {
        if (!this.isRequired  || isUserSpecified())
            return Collections.EMPTY_LIST;
        else
            return Collections.singletonList(new ConfigError("Config '"+getName()+"' is required but not user specified.", null));
    }
    
    /**
     * Used to clear all user specification (revert to default value)
     */
    public abstract void unset ();
    
    public abstract boolean isUserSpecified ();
    
    public abstract int getType ();
    
    public Object clone () throws CloneNotSupportedException
    {
        return super.clone();
    }
    
    public String toString ()
    {
        return super.toString().replaceAll("net.sf.opendf.config.","") + " " + getID() +"<usr:"+isUserSpecified()+"> =" + getValue();
    }
    
    public static class ConfigError
    {
        private String message = "unknown";
        private Object data = null;
        
        ConfigError (String msg, Object dat)
        {
            this.message = msg;
            this.data = dat;
        }
        /**
         * Gets a String indication of the error.
         * @return
         */
        public String getMessage () { return this.message; }
        /**
         * Returns an object associated with the error, may be used for providing additional information regarding the error condition.
         * @return an Object, may be  null
         */
        public Object getData () { return this.data; }
        
        public String toString ()
        {
            return super.toString().replaceAll("net.sf.opendf.config.","") + " <" + getMessage() + ">";
        }
    }
}
