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
package net.sf.opendf.eclipse.plugin.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import net.sf.opendf.config.ConfigBoolean;
import net.sf.opendf.config.ConfigInt;
import net.sf.opendf.config.ConfigList;
import net.sf.opendf.config.ConfigMap;
import net.sf.opendf.config.ConfigSet;
import net.sf.opendf.config.ConfigString;
import net.sf.opendf.config.ConfigUpdateIF;
import net.sf.opendf.util.logging.Logging;

/**
 *  A utility class to ensure one-way dependencies between projects.  This wrapper turns
 *  an ILaunchConfiguration into a class for updating the config.
 *  
 * @author imiller
 *
 */
public class ConfigUpdateWrapper implements ConfigUpdateIF
{
    private ILaunchConfigurationWorkingCopy launchConfigWorkingCopy=null;
    private ILaunchConfiguration launchConfig;

    public ConfigUpdateWrapper (ILaunchConfiguration config)
    {
        this.launchConfig = config;
        if (config instanceof ILaunchConfigurationWorkingCopy)
            this.launchConfigWorkingCopy = (ILaunchConfigurationWorkingCopy)config;
    }

    private static String keyToUserSpec (String key)
    {
        return key + "$user.specified";
    }

    public void exportConfig (ConfigBoolean config)
    {
        if (launchConfigWorkingCopy == null)  throw new UnsupportedOperationException("Cannot update non working copy of configuration");
        launchConfigWorkingCopy.setAttribute(config.getID(), config.getValue().booleanValue());
        launchConfigWorkingCopy.setAttribute(keyToUserSpec(config.getID()), config.isUserSpecified());
    }

    public void exportConfig (ConfigInt config)
    {
        if (launchConfigWorkingCopy == null)  throw new UnsupportedOperationException("Cannot update non working copy of configuration");
        launchConfigWorkingCopy.setAttribute(config.getID(), config.getValue().intValue());
        launchConfigWorkingCopy.setAttribute(keyToUserSpec(config.getID()), config.isUserSpecified());
    }

    public void exportConfig (ConfigList config)
    {
        if (launchConfigWorkingCopy == null)  throw new UnsupportedOperationException("Cannot update non working copy of configuration");
        launchConfigWorkingCopy.setAttribute(config.getID(), config.getValue());
        launchConfigWorkingCopy.setAttribute(keyToUserSpec(config.getID()), config.isUserSpecified());
    }

    public void exportConfig (ConfigMap config)
    {
        if (launchConfigWorkingCopy == null)  throw new UnsupportedOperationException("Cannot update non working copy of configuration");
        launchConfigWorkingCopy.setAttribute(config.getID(), config.getValue());
        launchConfigWorkingCopy.setAttribute(keyToUserSpec(config.getID()), config.isUserSpecified());
    }

    public void exportConfig (ConfigSet config)
    {
        if (launchConfigWorkingCopy == null)  throw new UnsupportedOperationException("Cannot update non working copy of configuration");
        launchConfigWorkingCopy.setAttribute(config.getID(), new ArrayList(config.getValue()));
        launchConfigWorkingCopy.setAttribute(keyToUserSpec(config.getID()), config.isUserSpecified());
    }

    public void exportConfig (ConfigString config)
    {
        if (launchConfigWorkingCopy == null)  throw new UnsupportedOperationException("Cannot update non working copy of configuration");
        launchConfigWorkingCopy.setAttribute(config.getID(), config.getValue());
        launchConfigWorkingCopy.setAttribute(keyToUserSpec(config.getID()), config.isUserSpecified());
    }

    public void importConfig (ConfigBoolean config)
    {
        try {
            config.setValue(
                    launchConfig.getAttribute(config.getID(), config.getValue().booleanValue()), 
                    launchConfig.getAttribute(keyToUserSpec(config.getID()), false));
        }catch (CoreException ce) {
            Logging.dbg().warning("Could not update configuration from launch config. " + ce);
            throw new RuntimeException(ce);
        }
    }

    public void importConfig (ConfigInt config)
    {
        try {
            config.setValue(
                    launchConfig.getAttribute(config.getID(), config.getValue().intValue()), 
                    launchConfig.getAttribute(keyToUserSpec(config.getID()), false));
        }catch (CoreException ce) {
            Logging.dbg().warning("Could not update configuration from launch config. " + ce);
            throw new RuntimeException(ce);
        }
    }

    public void importConfig (ConfigList config)
    {
        try {
            config.setValue(
                    launchConfig.getAttribute(config.getID(), config.getValue()), 
                    launchConfig.getAttribute(keyToUserSpec(config.getID()), false));
        }catch (CoreException ce) {
            Logging.dbg().warning("Could not update configuration from launch config. " + ce);
            throw new RuntimeException(ce);
        }
    }

    public void importConfig (ConfigMap config)
    {
        try {
            config.setValue(
                    launchConfig.getAttribute(config.getID(), config.getValue()), 
                    launchConfig.getAttribute(keyToUserSpec(config.getID()), false));
        }catch (CoreException ce) {
            Logging.dbg().warning("Could not update configuration from launch config. " + ce);
            throw new RuntimeException(ce);
        }
    }

    public void importConfig (ConfigSet config)
    {
        try {
            // Convert from the list which is pushed into the iconfig back to a set view
            config.setValue(
                    new LinkedHashSet(launchConfig.getAttribute(config.getID(), config.getValue())), 
                    launchConfig.getAttribute(keyToUserSpec(config.getID()), false));
        }catch (CoreException ce) {
            Logging.dbg().warning("Could not update configuration from launch config. " + ce);
            throw new RuntimeException(ce);
        }
    }

    public void importConfig (ConfigString config)
    {
        try {
            config.setValue(
                    launchConfig.getAttribute(config.getID(), config.getValue()), 
                    launchConfig.getAttribute(keyToUserSpec(config.getID()), false));
        }catch (CoreException ce) {
            Logging.dbg().warning("Could not update configuration from launch config. " + ce);
            throw new RuntimeException(ce);
        }
    }

}
