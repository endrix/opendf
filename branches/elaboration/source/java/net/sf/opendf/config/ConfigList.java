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
        unset();
    }

    public boolean allowMultiples () { return true; }

    public void addValue (List l, boolean userSpecified)
    {
        if (!userSpecified || !isUserSpecified())
            setValue(l, userSpecified);
        else
        {
            List merged = new ArrayList(getValue());
            merged.addAll(l);
            setValue(merged, userSpecified);
        }
    }
    
    public void setValue (String s, boolean userSpecified)
    {
        if (s == null) throw new IllegalArgumentException("Cannot specify null list for config");
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
        if (l == null) throw new IllegalArgumentException("Cannot specify null List for config");
        this.value = new ArrayList(l);
        this.userSpecified = userSpecified;
    }
    
    public List getValue ()
    {
        return Collections.unmodifiableList(this.value);
    }
    
    @Override
    public void unset ()
    {
        this.userSpecified = false;
        this.value = Collections.EMPTY_LIST; // clear the list first
        setValue(this.defaultValue, false);
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

    public Object clone () throws CloneNotSupportedException
    {
        ConfigList config = (ConfigList)super.clone();
        config.value = new ArrayList(this.value);
        return config;
    }
        
}
