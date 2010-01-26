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

import java.util.ArrayList;
import java.util.List;

public class ConfigInt extends AbstractConfig
{
    private final int defaultValue;
    private int value = -1;
    private boolean userSpecified = false;
    private String error = null;
    private int radix = 10;
    
    public ConfigInt (String id, String name, String cla, String desc, boolean required, int defaultValue)
    {
        super(id, name, cla, desc, required);
        this.defaultValue = defaultValue;
        unset();
    }

    public void setValue (String value, boolean userSpecified)
    {
        // Allow correctly formatted decimal and hex strings
        try
        {
            String valueString = value;
            if (value.startsWith("0x") || value.startsWith("0X"))
            {
                this.radix = 16;
                valueString = valueString.substring(2);
            }
            else
            {
                this.radix = 10;
            }
            
            setValue(Integer.parseInt(valueString, radix), userSpecified);
        }catch (NumberFormatException nfe)
        {
            this.error = nfe.toString();
        }
    }
    
    public void setValue (int value, boolean userSpecified)
    {
        this.error = null;
        this.value = value;
        this.userSpecified = userSpecified;
    }
    
    public Integer getValue ()
    {
        return this.value;
    }

    /**
     * Returns a String view of the value specified in this config based on the radix used
     * when setting the value (only applies on setValue(String, boolean)).
     * 
     * @return
     */
    public String getValueString ()
    {
        String vString = Integer.toString(getValue(), radix); 
        return (this.radix == 16 ? "0x":"") + vString;
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
        setValue(this.defaultValue, false);
    }
    
    @Override
    public boolean isUserSpecified ()
    {
        return this.userSpecified;
    }

    public List<ConfigError> getErrors ()
    {
        List<ConfigError> errs = new ArrayList(super.getErrors());
        if (this.error != null)
            errs.add(new ConfigError(this.error, null));
        return errs;
    }
}
