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

public class ConfigBoolean extends AbstractConfig
{
    private final boolean defaultValue;
    private boolean value = true;
    private boolean userSpecified = false;
    
    public ConfigBoolean (String id, String name, String cla, String desc, boolean required, boolean defaultValue)
    {
        super(id, name, cla, desc, required);
        this.defaultValue = defaultValue;
        unset();
    }

    public void setValue (boolean value, boolean userSpecified)
    {
        this.value = value;
        this.userSpecified = userSpecified;
    }
    
    public Boolean getValue ()
    {
        return this.value;
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
        setValue(this.defaultValue, false);
    }
    
    @Override
    public boolean isUserSpecified ()
    {
        return this.userSpecified;
    }

    /**
     * Returns the negated form of the command line argument (CLA) for
     * this boolean config.
     *
     */
    public String getNegatedKey ()
    {
        int cnt = 0;
        String test = this.getCLA();
        while (test.startsWith("-"))
        {
            cnt++;
            test = test.substring(1);
        }
        
        String negKey = "";
        for (int i=0; i < cnt; i++)
            negKey += "-";
        negKey = negKey + "no-" + test;
        return negKey;
    }
    
}
