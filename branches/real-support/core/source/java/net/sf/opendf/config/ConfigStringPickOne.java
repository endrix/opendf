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
import java.util.logging.Level;


/**
 * A configuration whose valid values are one of a collection of valid values.
 * @author imiller
 *
 */
public class ConfigStringPickOne extends ConfigString
{

    private List<String> allowableValues = Collections.EMPTY_LIST;

    public ConfigStringPickOne(String id, String name, String cla, String desc,
            boolean required, String defaultValue, List<String> allowable)
    {
        super(id, name, cla, desc, required, defaultValue);
        if (!allowable.contains(defaultValue))
            throw new IllegalArgumentException("Default value not contained in allowable values.");
        
        this.allowableValues = Collections.unmodifiableList(allowable);
    }

    public List<String> getAllowable ()
    {
        return this.allowableValues;
    }
    
    public int getType ()
    {
        return TYPE_PICKONE;
    }
    
    @Override
    public List<ConfigError> getErrors ()
    {
        List<ConfigError> errs = new ArrayList(super.getErrors());
        
        if (!allowableValues.contains(getValue()))
        {
            errs.add(new ConfigError("Specified value '"+getValue()+"' is not allowed.", this.allowableValues));
        }
        return errs;
    }


    public static class ConfigLogLevels extends ConfigStringPickOne
    {
        public static final List<String> LOG_LEVELS;
        static
        {
            List levels = new ArrayList();
            levels.add(Level.ALL.getName());
            levels.add(Level.FINEST.getName());
            levels.add(Level.FINER.getName());
            levels.add(Level.FINE.getName());
            levels.add(Level.CONFIG.getName());
            levels.add(Level.INFO.getName());
            levels.add(Level.WARNING.getName());
            levels.add(Level.SEVERE.getName());
            levels.add(Level.OFF.getName());
            LOG_LEVELS = Collections.unmodifiableList(levels);
        }
        public ConfigLogLevels(String id, String name, String cla, String desc,
                boolean required, String defaultValue)
        {
            super(id, name, cla, desc, required, defaultValue, LOG_LEVELS);
        }
    }
}
